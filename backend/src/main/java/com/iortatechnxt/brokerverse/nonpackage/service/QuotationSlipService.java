package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.docgen.domain.DocTemplate;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail.Protection;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponseRepository;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalStatus;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Quotation Slip (BRNB.008/013/014): TSU selects the insurers of the panel, submits the slip
 * (numbered QS-yyyy, built from the QUOTATION_SLIP template) and a second TSU officer approves it;
 * approval sends one password-protected e-mail per insurer to its placement addresses (delivery log
 * in the messaging outbox) and opens a pending response per insurer (BRNB.009).
 */
@Service
@Transactional
public class QuotationSlipService {

  /** Purpose code of the quotation slip e-mails. */
  public static final String PURPOSE = "QUOTATION_SLIP";

  private static final int DEFAULT_REPLY_DAYS = 5;

  private final ProposalService proposals;
  private final InsurerResponseRepository responses;
  private final InsurerService insurers;
  private final ProposalNumbers numbers;
  private final ProposalDocuments documents;
  private final DocTemplateService templates;
  private final MessageService messages;
  private final SystemParameterService parameters;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param proposals PRF reads
   * @param responses insurer responses
   * @param insurers insurer panel
   * @param numbers numbering
   * @param documents quotation slip PDF
   * @param templates document templates
   * @param messages outbox
   * @param parameters business parameters
   * @param workflow workflow engine
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public QuotationSlipService(
      ProposalService proposals,
      InsurerResponseRepository responses,
      InsurerService insurers,
      ProposalNumbers numbers,
      ProposalDocuments documents,
      DocTemplateService templates,
      MessageService messages,
      SystemParameterService parameters,
      WorkflowService workflow,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.proposals = proposals;
    this.responses = responses;
    this.insurers = insurers;
    this.numbers = numbers;
    this.documents = documents;
    this.templates = templates;
    this.messages = messages;
    this.parameters = parameters;
    this.workflow = workflow;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Selects the panel insurers to approach (TSU, while the PRF is with TSU).
   *
   * @param id PRF
   * @param codes insurer party codes
   * @return the PRF
   */
  public ProposalRequest selectInsurers(Long id, List<String> codes) {
    ProposalRequest p = proposals.get(id);
    if (!ProposalStatus.TSU_EDITABLE.contains(p.getStatus())) {
      throw new BusinessRuleException(
          "QS_CLOSED", "Insurers are selected before the quotation slip is submitted");
    }
    List<String> selected = codes.stream().distinct().toList();
    selected.forEach(code -> insurers.requireUsableInsurer(p.getCompanyId(), code));
    p.selectInsurers(selected);
    audit.record(
        ProposalService.ENTITY,
        p.getPrfNo(),
        AuditAction.UPDATE,
        "Quotation slip insurers: " + String.join(", ", selected));
    return p;
  }

  /**
   * Submits the quotation slip for approval: numbers it and stamps the template version.
   *
   * @param id PRF
   * @param replyBy date the insurers should reply by; null for the configured delay
   * @param comment comment
   * @return the PRF
   */
  public ProposalRequest submit(Long id, LocalDate replyBy, String comment) {
    ProposalRequest p = proposals.get(id);
    if (p.getInsurers().isEmpty()) {
      throw new BusinessRuleException(
          "QS_NO_INSURER", "Select at least one insurer of the panel for the quotation slip");
    }
    LocalDate today = LocalDate.now(clock);
    DocTemplate template = templates.current(ProposalDocuments.QS_TEMPLATE, today);
    LocalDate reply =
        replyBy != null
            ? replyBy
            : today.plusDays(parameters.intValue("QUOTATION_SLIP_REPLY_DAYS", DEFAULT_REPLY_DAYS));
    workflow.transition(
        ProposalService.ENTITY, String.valueOf(id), "submit_qs", TransitionNote.comment(comment));
    p.prepareQuotationSlip(
        p.getQsNo() == null ? numbers.quotationSlip() : p.getQsNo(),
        template.getCode() + " v" + template.getVersionNo(),
        reply,
        currentUser.username());
    return p;
  }

  /**
   * Approves the quotation slip (four eyes) and sends it to every selected insurer.
   *
   * @param id PRF
   * @param comment comment
   * @return the PRF, now QS_SENT
   */
  public ProposalRequest approve(Long id, String comment) {
    ProposalRequest p = proposals.get(id);
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, p.getQsSubmittedBy())) {
      throw new BusinessRuleException(
          "QS_FOUR_EYES", "The quotation slip is approved by another TSU officer");
    }
    workflow.transition(
        ProposalService.ENTITY, String.valueOf(id), "approve_qs", TransitionNote.comment(comment));
    MessageFile slip = documents.quotationSlip(p);
    for (String code : p.getInsurers()) {
      InsurerProfile insurer = insurers.requireUsableInsurer(p.getCompanyId(), code);
      sendTo(p, insurer, slip);
      responses
          .findByProposalIdAndInsurerCode(p.getId(), code)
          .orElseGet(() -> responses.save(new InsurerResponse(p.getId(), code, insurer.getName())));
    }
    p.markQuotationSlipSent(user, clock.instant());
    audit.record(
        ProposalService.ENTITY,
        p.getPrfNo(),
        AuditAction.UPDATE,
        "Quotation slip " + p.getQsNo() + " sent to " + String.join(", ", p.getInsurers()));
    return p;
  }

  private void sendTo(ProposalRequest p, InsurerProfile insurer, MessageFile slip) {
    List<String> to = insurer.getPlacementEmailList();
    if (to.isEmpty()) {
      throw new BusinessRuleException(
          "INSURER_NO_EMAIL", insurer.getName() + " has no placement e-mail address");
    }
    messages.queueEmail(
        new OutboundEmail(
            p.getCompanyId(),
            PURPOSE,
            to,
            List.of(),
            "Quotation slip " + p.getQsNo() + " - " + p.getClientName(),
            "Dear "
                + insurer.getName()
                + ",\n\nPlease find attached our quotation slip "
                + p.getQsNo()
                + " for "
                + p.getProductCode()
                + ". Kindly send your best terms on or before "
                + p.getQsReplyBy()
                + ". The document is password protected; the password follows separately.\n\n"
                + "BDO Insurance and Reinsurance Brokers, Inc.",
            List.of(slip),
            new Protection(null, true, null),
            new RecordLink(ProposalService.ENTITY, String.valueOf(p.getId()), p.getPrfNo())));
  }

  /**
   * The quotation slip PDF.
   *
   * @param id PRF
   * @return file
   */
  @Transactional(readOnly = true)
  public MessageFile pdf(Long id) {
    ProposalRequest p = proposals.get(id);
    if (p.getQsNo() == null) {
      throw new BusinessRuleException("QS_NOT_PREPARED", "No quotation slip has been prepared yet");
    }
    return documents.quotationSlip(p);
  }
}
