package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.CwtPath;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtBatch;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtBatchRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtTag;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtTag.CwtDetails;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtTag.TaggedInvoice;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtTagRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BIR 2307 reversals (CSHID.026/027, MKTID.010/013, DBMID.001; workflow {@code OPS_CWT_2307},
 * OQ16). Marketing Collection tags the 2% CWT of an invoice with a {@code CWT-} reference;
 * Cashiering receives it with the CWT copy checklist, settles the cash path with an AR, and
 * validates the certificates of one insurer in a batch: the PR is reclassified to PR2307 ({@code
 * OPS_CWT_RECLASS}), the transaction report is posted and routed to Disbursement, which releases
 * the certificates to the insurer ({@code OPS_CWT_DTIP_OFFSET}, PR zeroed).
 */
@Service
@Transactional
public class CwtService {

  /** Entity type of the work case. */
  public static final String ENTITY = "CwtTag";

  private static final String WORKFLOW = "OPS_CWT_2307";
  private static final String VALIDATING = "VALIDATING";
  private static final Set<String> LIVE =
      Set.of(CwtTag.TAGGED, VALIDATING, "REPORT_POSTED", "WITH_DISBURSEMENT");
  private static final Set<RemittanceStatus> REMITTED =
      Set.of(RemittanceStatus.PARTIALLY_REMITTED, RemittanceStatus.FULLY_REMITTED);

  private final CwtTagRepository tags;
  private final CwtBatchRepository batches;
  private final InvoiceLedgerQueryService ledger;
  private final ApplicationService applier;
  private final CwtPostings postings;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param tags 2307 tags
   * @param batches 2307 batches
   * @param ledger invoice ledger
   * @param applier application engine (expected 2%)
   * @param postings reclass, cash path and DTIP offset
   * @param workflow workflow
   * @param numbers document numbers
   * @param audit audit trail
   * @param clock clock
   */
  public CwtService(
      CwtTagRepository tags,
      CwtBatchRepository batches,
      InvoiceLedgerQueryService ledger,
      ApplicationService applier,
      CwtPostings postings,
      WorkflowService workflow,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.tags = tags;
    this.batches = batches;
    this.ledger = ledger;
    this.applier = applier;
    this.postings = postings;
    this.workflow = workflow;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Tags a 2307 reversal (MKTID.013): one live tag per invoice; the amount defaults to the 2% the
   * client withholds.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param details amount, path and certificate
   * @return the tag
   */
  public CwtTag tag(Long companyId, String invoiceNo, CwtDetails details) {
    OpsInvoice invoice =
        ledger
            .find(invoiceNo)
            .filter(i -> i.getCompanyId().equals(companyId))
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "CWT_INVOICE_UNKNOWN", "Unknown invoice " + invoiceNo));
    invoice.loadCollections();
    if (tags.existsByInvoiceNoAndStageIn(invoiceNo, LIVE)) {
      throw new BusinessRuleException(
          "CWT_ALREADY_TAGGED", "Invoice " + invoiceNo + " already has a 2307 tag in process");
    }
    BigDecimal amount = details.amount() != null ? details.amount() : expected(invoice);
    if (amount.signum() <= 0) {
      throw new BusinessRuleException(
          "CWT_AMOUNT", "Invoice " + invoiceNo + " has no 2% CWT portion outstanding");
    }
    CwtTag tag =
        tags.save(
            new CwtTag(
                numbers.next("CWT-" + LocalDate.now(clock).getYear()),
                new TaggedInvoice(
                    companyId,
                    invoiceNo,
                    invoice.getArn(),
                    invoice.getClientCode(),
                    invoice.getInsurerCode()),
                new CwtDetails(
                    amount,
                    details.path(),
                    details.certificateNo(),
                    details.periodFrom(),
                    details.periodTo(),
                    details.remarks()),
                REMITTED.contains(invoice.getRemittanceStatus())));
    workflow.start(
        new StartCase(
            companyId,
            WORKFLOW,
            new CaseRecord(
                ENTITY,
                tag.getId().toString(),
                tag.getReference(),
                "BIR 2307 " + invoice.getAssuredName() + " " + invoiceNo,
                "/cashiering/cwt",
                null),
            null));
    audit.record(
        ENTITY,
        tag.getReference(),
        AuditAction.CREATE,
        tag.getPath() + " " + amount + " on " + invoiceNo);
    return tag;
  }

  /**
   * The 2% an invoice still expects from its client's 2307.
   *
   * @param invoice invoice (components loaded)
   * @return withheld amount
   */
  public BigDecimal expected(OpsInvoice invoice) {
    return applier.balancesOf(invoice).withheld();
  }

  /**
   * Receives a tag for validation with the CWT copy checklist (CSHID.027).
   *
   * @param id tag
   * @param copyReceived whether the copy of the certificate was received
   * @return the tag
   */
  public CwtTag receive(Long id, boolean copyReceived) {
    CwtTag tag = get(id);
    tag.copyReceived(copyReceived);
    workflow.transition(ENTITY, id.toString(), "receive", TransitionNote.NONE);
    return tag;
  }

  /**
   * Updates the CWT copy checklist of a tag in validation (CSHID.027).
   *
   * @param id tag
   * @param copyReceived whether the copy of the certificate was received
   * @return the tag
   */
  public CwtTag checklist(Long id, boolean copyReceived) {
    CwtTag tag = get(id);
    requireStage(tag, VALIDATING);
    tag.copyReceived(copyReceived);
    audit.record(
        ENTITY, tag.getReference(), AuditAction.UPDATE, "CWT copy received: " + copyReceived);
    return tag;
  }

  /**
   * Settles a cash-path tag: AR for the 2% and application to the invoice (CSHID.026).
   *
   * @param id tag in validation
   * @param branchId receiving branch
   * @return the tag
   */
  public CwtTag settleCash(Long id, Long branchId) {
    CwtTag tag = get(id);
    requireStage(tag, VALIDATING);
    if (tag.getPath() != CwtPath.CASH) {
      throw new BusinessRuleException("CWT_NOT_CASH", tag.getReference() + " is a certificate tag");
    }
    tag.settledInCash(postings.cashPath(tag, branchId));
    workflow.transition(ENTITY, id.toString(), "settle_cash", TransitionNote.NONE);
    return tag;
  }

  /**
   * Validates certificate tags of one insurer against their CWT copies, posts the reclass and the
   * transaction report (CSHID.027).
   *
   * @param companyId company
   * @param tagIds tags in validation
   * @return the batch
   */
  public CwtBatch validateBatch(Long companyId, Collection<Long> tagIds) {
    List<CwtTag> selected = tags.findByIdInOrderByIdAsc(tagIds);
    if (selected.isEmpty() || selected.size() != tagIds.size()) {
      throw new BusinessRuleException("CWT_BATCH_EMPTY", "Select the 2307 tags to validate");
    }
    String insurer = selected.get(0).getInsurerCode();
    for (CwtTag t : selected) {
      requireBatchable(t, companyId, insurer);
    }
    BigDecimal total =
        selected.stream().map(CwtTag::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    CwtBatch batch =
        batches.save(
            new CwtBatch(
                companyId,
                numbers.next("CWB-" + LocalDate.now(clock).getYear()),
                insurer,
                selected.size(),
                total));
    for (CwtTag t : selected) {
      t.validated(batch.getId(), postings.reclass(t));
      workflow.transition(
          ENTITY, t.getId().toString(), "validate", TransitionNote.comment(batch.getBatchNo()));
    }
    audit.record(
        ENTITY, batch.getBatchNo(), AuditAction.POST, selected.size() + " certificates, " + total);
    return batch;
  }

  /**
   * Routes a validated batch to Disbursement, bypassing Remittance (CSHID.027, DBMID.001).
   *
   * @param batchId batch
   * @return the batch
   */
  public CwtBatch route(Long batchId) {
    CwtBatch batch = batch(batchId);
    if (!CwtBatch.REPORT_POSTED.equals(batch.getStatus())) {
      throw new BusinessRuleException(
          "CWT_BATCH_ROUTED", batch.getBatchNo() + " is " + batch.getStatus());
    }
    List<CwtTag> members = tags.findByBatchIdOrderByIdAsc(batch.getId());
    batch.routed(postings.route(batch, members.get(0).getInvoiceNo()), clock.instant());
    members.forEach(
        t -> workflow.transition(ENTITY, t.getId().toString(), "route", TransitionNote.NONE));
    audit.record(
        ENTITY,
        batch.getBatchNo(),
        AuditAction.SUBMIT,
        "Routed " + batch.getDisbursementRequestNo());
    return batch;
  }

  /**
   * Releases the certificates of a batch to the insurer: PR2307 settled against DTIP (DBMID.001).
   *
   * @param batchNo batch
   * @param system whether Disbursement's status triggered it
   * @return the batch
   */
  public CwtBatch release(String batchNo, boolean system) {
    CwtBatch batch =
        batches
            .findByBatchNo(batchNo)
            .orElseThrow(() -> new ResourceNotFoundException("CWT batch", batchNo));
    if (!CwtBatch.WITH_DISBURSEMENT.equals(batch.getStatus())) {
      return batch;
    }
    for (CwtTag t : tags.findByBatchIdOrderByIdAsc(batch.getId())) {
      t.offset(postings.dtipOffset(t));
      if (system) {
        workflow.systemTransition(
            ENTITY, t.getId().toString(), "release_to_insurer", TransitionNote.NONE);
      } else {
        workflow.transition(
            ENTITY, t.getId().toString(), "release_to_insurer", TransitionNote.NONE);
      }
    }
    batch.released(clock.instant());
    audit.record(
        ENTITY,
        batch.getBatchNo(),
        AuditAction.CLOSE,
        "Released to insurer " + batch.getInsurerCode());
    return batch;
  }

  /**
   * Tags in some stages.
   *
   * @param companyId company
   * @param stages stages
   * @param pageable page
   * @return tags, newest first
   */
  @Transactional(readOnly = true)
  public Page<CwtTag> list(Long companyId, Collection<String> stages, Pageable pageable) {
    return tags.findByCompanyIdAndStageInOrderByIdDesc(companyId, stages, pageable);
  }

  /**
   * Recent batches.
   *
   * @param companyId company
   * @return batches, newest first
   */
  @Transactional(readOnly = true)
  public List<CwtBatch> batches(Long companyId) {
    return batches.findTop50ByCompanyIdOrderByIdDesc(companyId);
  }

  /**
   * Tags of a batch.
   *
   * @param batchId batch
   * @return tags
   */
  @Transactional(readOnly = true)
  public List<CwtTag> tagsOf(Long batchId) {
    return tags.findByBatchIdOrderByIdAsc(batchId);
  }

  /**
   * One tag.
   *
   * @param id id
   * @return tag
   */
  @Transactional(readOnly = true)
  public CwtTag get(Long id) {
    return tags.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * One batch.
   *
   * @param id id
   * @return batch
   */
  @Transactional(readOnly = true)
  public CwtBatch batch(Long id) {
    return batches.findById(id).orElseThrow(() -> new ResourceNotFoundException("CWT batch", id));
  }

  private static void requireBatchable(CwtTag t, Long companyId, String insurer) {
    requireStage(t, VALIDATING);
    if (!t.getCompanyId().equals(companyId) || !t.getInsurerCode().equals(insurer)) {
      throw new BusinessRuleException(
          "CWT_BATCH_ONE_INSURER",
          "A 2307 batch holds the certificates of one insurer (CSHID.027)");
    }
    if (t.getPath() != CwtPath.CERTIFICATE) {
      throw new BusinessRuleException(
          "CWT_CASH_IN_BATCH", t.getReference() + " is settled in cash");
    }
    if (!t.isCwtCopyReceived()) {
      throw new BusinessRuleException(
          "CWT_COPY_MISSING", "The CWT copy of " + t.getReference() + " was not received");
    }
  }

  private static void requireStage(CwtTag t, String stage) {
    if (!stage.equals(t.getStage())) {
      throw new BusinessRuleException("CWT_WRONG_STAGE", t.getReference() + " is " + t.getStage());
    }
  }
}
