package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalStatus;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalNumbers;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalService;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalSlipService;
import com.iortatechnxt.brokerverse.productmaint.domain.ProposalFile;
import com.iortatechnxt.brokerverse.productmaint.domain.ProposalFileRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.TermsRecord;
import com.iortatechnxt.brokerverse.productmaint.service.TermsSources.Facts;
import com.iortatechnxt.brokerverse.productmaint.service.TermsSources.InsurerTerms;
import com.iortatechnxt.brokerverse.productmaint.service.TermsTableService.TermsTable;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insurer selection and proposal generation of BDOI's FRS: the requestor selects one or more
 * insurers that responded and proceeds to the proposal (FRPM.008.01, the quotation request then
 * shows Terms Agreed and the TSU officer is told), and TSU generates a separate proposal slip for
 * each selected insurer from the Final Terms for Proposal (FRPM.009.02, FRPM.013.01), versioned and
 * named {@code ProposalSlip_<Reference>_<Insurer>_<Version>_MMDDYYYY.pdf}. For a quotation request
 * the generation submits the proposal for the approval of a second TSU officer.
 */
@Service
@Transactional
public class TermsProposalService {

  /** Event of the selection. */
  public static final String AGREED_EVENT = "PM_TERMS_AGREED";

  private static final Set<RequestStage> PACKAGE_STAGES =
      EnumSet.of(
          RequestStage.NEGOTIATION,
          RequestStage.TERMS_REVIEW,
          RequestStage.FOR_MKT_REVIEW,
          RequestStage.REQUIREMENTS_PREP);

  private static final Set<ProposalStatus> QUOTATION_SELECTABLE =
      EnumSet.of(ProposalStatus.QS_SENT, ProposalStatus.TERMS_RECEIVED);

  private final TermsSources sources;
  private final TermsTableService tables;
  private final TermsDocuments documents;
  private final ProposalFileRepository files;
  private final ProposalService proposals;
  private final ProposalNumbers numbers;
  private final PackageRequests packages;
  private final DocumentService attachments;
  private final WorkflowService workflow;
  private final NoticeDelivery delivery;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param sources record facts
   * @param tables comparative tables
   * @param documents proposal slips
   * @param files generated proposal slips
   * @param proposals quotation requests
   * @param numbers proposal slip numbers
   * @param packages package requests
   * @param attachments documents of the records
   * @param workflow workflow
   * @param delivery notices
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public TermsProposalService(
      TermsSources sources,
      TermsTableService tables,
      TermsDocuments documents,
      ProposalFileRepository files,
      ProposalService proposals,
      ProposalNumbers numbers,
      PackageRequests packages,
      DocumentService attachments,
      WorkflowService workflow,
      NoticeDelivery delivery,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.sources = sources;
    this.tables = tables;
    this.documents = documents;
    this.files = files;
    this.proposals = proposals;
    this.numbers = numbers;
    this.packages = packages;
    this.attachments = attachments;
    this.workflow = workflow;
    this.delivery = delivery;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Selects the insurers for the proposal and proceeds to the proposal: only insurers that
   * responded with terms can be selected.
   *
   * @param record the record
   * @param insurers insurer codes
   * @param comment comment
   * @return the table
   */
  public TermsTable proceed(TermsRecord record, List<String> insurers, String comment) {
    Facts facts = sources.facts(record);
    List<String> chosen = checkSelection(facts, insurers);
    if (record.quotation()) {
      ProposalRequest p = proposals.get(record.id());
      if (!QUOTATION_SELECTABLE.contains(p.getStatus())) {
        throw new BusinessRuleException(
            "PM_PROPOSAL_STAGE", "Insurers are selected while the insurers' terms come in");
      }
      if (p.getStatus() == ProposalStatus.QS_SENT) {
        workflow.transition(
            ProposalService.ENTITY,
            String.valueOf(record.id()),
            "proceed_to_proposal",
            TransitionNote.comment(comment));
      }
      p.closeTerms();
      tellTsu(p.getQsSubmittedBy(), facts, record, chosen);
    } else {
      RequestStage stage = packages.get(record.id()).getStatus();
      if (!PACKAGE_STAGES.contains(stage)) {
        throw new BusinessRuleException(
            "PM_PROPOSAL_STAGE", "Insurers are selected while the package terms are negotiated");
      }
    }
    tables.select(record, chosen);
    audit.record(
        record.type(),
        facts.requestNo(),
        AuditAction.UPDATE,
        "Insurers selected for the proposal: "
            + facts.insurers().stream()
                .filter(i -> chosen.contains(i.insurerCode()))
                .map(TermsSources.InsurerTerms::insurerName)
                .collect(Collectors.joining(", ")));
    return tables.table(record);
  }

  private static List<String> checkSelection(Facts facts, List<String> insurers) {
    List<String> chosen = insurers == null ? List.of() : insurers.stream().distinct().toList();
    if (chosen.isEmpty()) {
      throw new BusinessRuleException(
          "PM_PROPOSAL_INSURER", "Select at least one insurer for the proposal");
    }
    for (String code : chosen) {
      InsurerTerms terms =
          facts.insurers().stream()
              .filter(i -> i.insurerCode().equals(code))
              .findFirst()
              .orElseThrow(
                  () ->
                      new BusinessRuleException(
                          "PM_TERMS_INSURER", code + " is not an insurer of this request"));
      if (!terms.responded() || "NOT_COVERED".equals(terms.answer())) {
        throw new BusinessRuleException(
            "PM_PROPOSAL_NO_TERMS", terms.insurerName() + " has not given terms to propose");
      }
    }
    return chosen;
  }

  private void tellTsu(String officer, Facts facts, TermsRecord record, List<String> chosen) {
    if (officer == null) {
      return;
    }
    delivery.toUser(
        officer,
        new Notice(
            facts.reference() + ": Terms Agreed",
            "The requestor selected "
                + String.join(", ", chosen)
                + " for the proposal of "
                + facts.clientName()
                + ". Generate the proposal.",
            "/proposals/" + record.id(),
            record.type(),
            String.valueOf(record.id())),
        AGREED_EVENT,
        false);
  }

  /**
   * Generates a proposal slip for each selected insurer (a new version each time), stores them on
   * the record and, for a quotation request, submits the proposal for approval.
   *
   * @param record the record
   * @param comment comment
   * @return the files generated
   */
  public List<ProposalFile> generate(TermsRecord record, String comment) {
    TermsTable table = tables.table(record);
    if (table.selectedInsurers().isEmpty()) {
      throw new BusinessRuleException(
          "PM_PROPOSAL_INSURER", "Select the insurers and proceed to the proposal first");
    }
    if (table.finalTerms().values().stream().allMatch(v -> v == null || v.isBlank())) {
      throw new BusinessRuleException(
          "PM_FINAL_TERMS", "Complete the Final Terms for Proposal first");
    }
    Facts facts = sources.facts(record);
    if (record.quotation()) {
      submitQuotation(record, table.selectedInsurers().get(0), comment);
    }
    List<ProposalFile> out = new ArrayList<>();
    String user = currentUser.username();
    for (String code : table.selectedInsurers()) {
      String name = insurerName(table, code);
      int version = nextVersion(record, code);
      MessageFile slip = documents.proposalSlip(facts, table, new String[] {code, name}, version);
      Long attachmentId =
          attachments
              .upload(
                  new AttachmentTarget(record.type(), String.valueOf(record.id())),
                  List.of(new UploadedFile(slip.fileName(), slip.content())),
                  new UploadOptions(
                      ProposalSlipService.SLIP_DOCUMENT,
                      false,
                      facts.reference(),
                      "Proposal slip of " + name + " version " + version))
              .get(0)
              .getId();
      out.add(
          files.save(
              new ProposalFile(
                  record,
                  new String[] {code, name},
                  version,
                  slip.fileName(),
                  attachmentId,
                  new ProposalFile.Generated(user, clock.instant()))));
    }
    audit.record(
        record.type(),
        facts.requestNo(),
        AuditAction.UPDATE,
        "Proposal slips generated: "
            + String.join(", ", out.stream().map(ProposalFile::getFileName).toList()));
    return out;
  }

  private void submitQuotation(TermsRecord record, String firstInsurer, String comment) {
    ProposalRequest p = proposals.get(record.id());
    if (p.getStatus() != ProposalStatus.TERMS_RECEIVED) {
      throw new BusinessRuleException(
          "PM_PROPOSAL_STAGE", "The proposal is generated once the terms are agreed");
    }
    workflow.transition(
        ProposalService.ENTITY,
        String.valueOf(record.id()),
        "submit_ps",
        TransitionNote.comment(comment));
    p.prepareProposalSlip(
        p.getPsNo() == null ? numbers.proposalSlip() : p.getPsNo(),
        firstInsurer,
        currentUser.username());
  }

  private static String insurerName(TermsTable table, String code) {
    return table.columns().stream()
        .filter(c -> c.insurerCode().equals(code))
        .map(c -> c.insurerName())
        .findFirst()
        .orElse(code);
  }

  private int nextVersion(TermsRecord record, String insurerCode) {
    return files.findByRecordTypeAndRecordIdOrderByIdDesc(record.type(), record.id()).stream()
            .filter(f -> f.getInsurerCode().equals(insurerCode))
            .mapToInt(ProposalFile::getVersionNo)
            .max()
            .orElse(0)
        + 1;
  }

  /**
   * The proposal slips generated, newest first.
   *
   * @param record the record
   * @return files
   */
  @Transactional(readOnly = true)
  public List<ProposalFile> files(TermsRecord record) {
    return files.findByRecordTypeAndRecordIdOrderByIdDesc(record.type(), record.id());
  }

  /**
   * The comparative table with the fields chosen for the client.
   *
   * @param record the record
   * @param excel true for Excel, false for PDF
   * @return file
   */
  @Transactional(readOnly = true)
  public MessageFile comparative(TermsRecord record, boolean excel) {
    return documents.comparative(sources.facts(record), tables.table(record), excel);
  }
}
