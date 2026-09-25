package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.QueuedEmail;
import com.iortatechnxt.brokerverse.opsledger.service.ExtractRepositoryService;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycle;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtract;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends a production register to the insurer (PRCID.003/007/008): the cover letter from the {@code
 * PRODRECON_COVER_LETTER} template, the workbook password protected with the password in a separate
 * e-mail (password convention parked, BRD-1 Q07 / OQ29), the sent time and recipients on the
 * extract, and the cycle moved to "awaiting insurer feedback". The default recipients are the
 * insurer's placement e-mail addresses (catalog).
 */
@Service
@Transactional
public class ReconSendService {

  /** Purpose of the e-mails. */
  public static final String PURPOSE = "PRODRECON";

  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy");

  private final ProductionExtractService extracts;
  private final ReconCycleService cycles;
  private final ExtractRepositoryService repository;
  private final DocTemplateService templates;
  private final MessageService messages;
  private final InsurerService insurers;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param extracts extracts
   * @param cycles cycles
   * @param repository extract repository (the workbook)
   * @param templates document templates (cover letter)
   * @param messages outbound e-mail
   * @param insurers insurer profiles (name, e-mail)
   * @param workflow workflow engine
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ReconSendService(
      ProductionExtractService extracts,
      ReconCycleService cycles,
      ExtractRepositoryService repository,
      DocTemplateService templates,
      MessageService messages,
      InsurerService insurers,
      WorkflowService workflow,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.extracts = extracts;
    this.cycles = cycles;
    this.repository = repository;
    this.templates = templates;
    this.messages = messages;
    this.insurers = insurers;
    this.workflow = workflow;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Sends an extract (user action, {@code RECON_SEND}).
   *
   * @param extractId extract
   * @param to recipients; the insurer's addresses when empty
   * @param cc copy recipients
   * @return the extract
   */
  public ReconExtract send(Long extractId, List<String> to, List<String> cc) {
    return send(extractId, to, cc, false);
  }

  /**
   * Sends an extract.
   *
   * @param extractId extract
   * @param to recipients; the insurer's addresses when empty
   * @param cc copy recipients
   * @param automatic sent by the schedule (system action)
   * @return the extract
   */
  public ReconExtract send(Long extractId, List<String> to, List<String> cc, boolean automatic) {
    ReconExtract extract = extracts.require(extractId);
    ReconCycle cycle = cycles.requireOpen(extract.getCycleId());
    String insurerName = insurerName(cycle);
    List<String> recipients = to == null || to.isEmpty() ? insurerEmails(cycle) : to;
    if (recipients.isEmpty()) {
      throw new BusinessRuleException(
          "RECON_NO_RECIPIENT",
          "Enter the insurer's e-mail address: "
              + cycle.getInsurerCode()
              + " has no placement e-mail in the catalog");
    }
    if (extract.getFileId() == null) {
      throw new ResourceNotFoundException("Production register file", extract.getExtractNo());
    }
    byte[] file = repository.download(extract.getFileId()).getContent();
    MergedText letter = coverLetter(extract, cycle, insurerName);
    QueuedEmail queued =
        messages.queueEmail(
            new OutboundEmail(
                cycle.getCompanyId(),
                PURPOSE,
                recipients,
                cc,
                letter.title() + " " + extract.getExtractNo(),
                letter.text(),
                List.of(
                    new MessageFile(extract.getFileName(), ProductionRegisterWorkbook.XLSX, file)),
                new OutboundEmail.Protection(null, true, null),
                new RecordLink(
                    ReconCycleService.ENTITY, String.valueOf(cycle.getId()), cycle.getCycleNo())));
    extract.sent(
        clock.instant(), currentUser.username(), queued.messageId(), String.join(", ", recipients));
    cycle.sent(clock.instant());
    move(cycle, extract, automatic);
    audit.record(
        "ReconExtract",
        extract.getExtractNo(),
        AuditAction.UPDATE,
        "Sent to " + String.join(", ", recipients) + " (message " + queued.messageId() + ")");
    return extract;
  }

  private void move(ReconCycle cycle, ReconExtract extract, boolean automatic) {
    String action = ReconCycle.EXTRACTED.equals(cycle.getStage()) ? "send" : "resend";
    TransitionNote note = TransitionNote.comment("Register " + extract.getExtractNo() + " sent");
    String id = String.valueOf(cycle.getId());
    if (automatic) {
      workflow.systemTransition(ReconCycleService.ENTITY, id, action, note);
    } else {
      workflow.transition(ReconCycleService.ENTITY, id, action, note);
    }
  }

  private MergedText coverLetter(ReconExtract extract, ReconCycle cycle, String insurerName) {
    return templates.merge(
        "PRODRECON_COVER_LETTER",
        LocalDate.now(clock),
        Map.of(
            "insurerName", insurerName,
            "extractNo", extract.getExtractNo(),
            "bookingFrom", extract.getBookingFrom(),
            "bookingTo", extract.getBookingTo(),
            "productionMonth", MONTH.format(cycle.getProductionMonth()),
            "rowCount", extract.getRowCount()));
  }

  private String insurerName(ReconCycle cycle) {
    return profile(cycle).map(InsurerProfile::getName).orElse(cycle.getInsurerCode());
  }

  private List<String> insurerEmails(ReconCycle cycle) {
    return profile(cycle).map(InsurerProfile::getPlacementEmailList).orElse(List.of());
  }

  private Optional<InsurerProfile> profile(ReconCycle cycle) {
    return insurers.insurers(cycle.getCompanyId()).stream()
        .filter(i -> i.getPartyCode().equals(cycle.getInsurerCode()))
        .findFirst();
  }
}
