package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.commission.domain.DpprBatch;
import com.iortatechnxt.brokerverse.commission.domain.DpprBatchLine;
import com.iortatechnxt.brokerverse.commission.domain.DpprBatchLineRepository;
import com.iortatechnxt.brokerverse.commission.domain.DpprBatchRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Legacy direct payment PR reversal batches (DATA_MIGRATION_DESIGN 14.4 E; BRID 7.1): legacy
 * invoices the client paid directly to the insurer, uploaded or taken from the Collections "DP PR
 * for reversal" tags; requested by the Commission user, approved by the Commission team lead (the
 * maker never approves); each line then reverses the open premium receivable and due to insurer of
 * its invoice in its own transaction.
 */
@Service
@Transactional
public class DpprBatchService {

  private static final String ENTITY = "DpprBatch";

  private static final String CANDIDATES =
      "select i.invoice_no, i.legacy_invoice_no, i.source_system, i.policy_no, i.client_code,"
          + " i.insurer_code, i.currency,"
          + " (select coalesce(sum(c.balance), 0) from ops_invoice_component c"
          + " where c.invoice_id = i.id and c.balance > 0 and c.component in"
          + " ('BASIC', 'DST', 'PREMIUM_TAX_VAT', 'LGT', 'FST', 'OTHER')) as open_pr,"
          + " exists (select 1 from clx_disposition d join clx_item x on x.id = d.item_id"
          + " where x.invoice_no = i.invoice_no and d.ops_action = 'DP_REVERSAL'"
          + " and d.superseded_by is null) as tagged"
          + " from ops_invoice i where i.company_id = ? and i.ledger_context = 'LEGACY'"
          + " and exists (select 1 from ops_invoice_component c where c.invoice_id = i.id"
          + " and c.balance > 0 and c.component in"
          + " ('BASIC', 'DST', 'PREMIUM_TAX_VAT', 'LGT', 'FST', 'OTHER'))"
          + " and (? = false or exists (select 1 from clx_disposition d join clx_item x"
          + " on x.id = d.item_id where x.invoice_no = i.invoice_no"
          + " and d.ops_action = 'DP_REVERSAL' and d.superseded_by is null))"
          + " and not exists (select 1 from cmr_dppr_batch_line l join cmr_dppr_batch b"
          + " on b.id = l.batch_id where l.invoice_no = i.invoice_no and l.status <> 'FAILED'"
          + " and b.status <> 'CANCELLED') order by i.invoice_no limit 500";

  private final DpprBatchRepository batches;
  private final DpprBatchLineRepository lines;
  private final DpprOpenPremium open;
  private final DpprBatchPoster poster;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final TransactionTemplate tx;
  private final JdbcTemplate jdbc;

  /**
   * Creates the service.
   *
   * @param batches batches
   * @param lines lines
   * @param open open premium receivable of an invoice
   * @param poster line poster
   * @param numbers batch numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   * @param txManager transactions (the lines post one by one after the approval)
   * @param jdbc JDBC (candidates)
   */
  @SuppressWarnings("java:S107") // collaborators
  public DpprBatchService(
      DpprBatchRepository batches,
      DpprBatchLineRepository lines,
      DpprOpenPremium open,
      DpprBatchPoster poster,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock,
      PlatformTransactionManager txManager,
      JdbcTemplate jdbc) {
    this.batches = batches;
    this.lines = lines;
    this.open = open;
    this.poster = poster;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
    this.tx = new TransactionTemplate(txManager);
    this.jdbc = jdbc;
  }

  /**
   * Batches of a company.
   *
   * @param companyId company
   * @return batches, newest first
   */
  @Transactional(readOnly = true)
  public List<DpprBatch> list(Long companyId) {
    return batches.findByCompanyIdOrderByIdDesc(companyId);
  }

  /**
   * A batch.
   *
   * @param batchNo number
   * @return the batch
   */
  @Transactional(readOnly = true)
  public DpprBatch get(String batchNo) {
    return batches
        .findByBatchNo(batchNo)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, batchNo));
  }

  /**
   * Lines of a batch.
   *
   * @param batch batch
   * @return lines
   */
  @Transactional(readOnly = true)
  public List<DpprBatchLine> lines(DpprBatch batch) {
    return lines.findByBatchIdOrderByLineNoAsc(batch.getId());
  }

  /**
   * Legacy invoices with an open premium receivable, not on another batch.
   *
   * @param companyId company
   * @param taggedOnly only those tagged "DP PR for reversal" by Collections
   * @return candidates
   */
  @Transactional(readOnly = true)
  public List<Candidate> candidates(Long companyId, boolean taggedOnly) {
    return jdbc.query(
        CANDIDATES,
        (rs, i) ->
            new Candidate(
                rs.getString("invoice_no"),
                rs.getString("legacy_invoice_no"),
                rs.getString("source_system"),
                rs.getString("policy_no"),
                rs.getString("client_code"),
                rs.getString("insurer_code"),
                rs.getString("currency"),
                rs.getBigDecimal("open_pr"),
                rs.getBoolean("tagged")),
        companyId,
        taggedOnly);
  }

  /**
   * A legacy invoice that may be reversed.
   *
   * @param invoiceNo invoice
   * @param legacyInvoiceNo legacy invoice number
   * @param sourceSystem legacy system
   * @param policyNo policy
   * @param clientCode client
   * @param insurerCode insurer
   * @param currency currency
   * @param openPremium open premium receivable
   * @param tagged tagged "DP PR for reversal" by Collections
   */
  public record Candidate(
      String invoiceNo,
      String legacyInvoiceNo,
      String sourceSystem,
      String policyNo,
      String clientCode,
      String insurerCode,
      String currency,
      BigDecimal openPremium,
      boolean tagged) {}

  /**
   * Opens a batch.
   *
   * @param companyId company
   * @param reason reason
   * @return the batch
   */
  public DpprBatch create(Long companyId, String reason) {
    requireText(reason, "Give the reason of the batch");
    DpprBatch batch =
        batches.save(
            new DpprBatch(
                companyId,
                numbers.next("DPR-" + BusinessClock.today(clock).getYear()),
                reason.strip()));
    audit.record(ENTITY, batch.getBatchNo(), AuditAction.CREATE, reason);
    return batch;
  }

  /**
   * Adds a legacy invoice; its whole open premium receivable is reversed.
   *
   * @param batchNo batch
   * @param invoiceNo legacy invoice
   * @param amount amount of the upload, checked against the open premium receivable (null takes it)
   * @param reason reason
   * @return the line
   */
  public DpprBatchLine addInvoice(
      String batchNo, String invoiceNo, BigDecimal amount, String reason) {
    DpprBatch batch = get(batchNo);
    batch.requireStatus(DpprBatch.Status.DRAFT);
    OpsInvoice invoice = open.legacy(invoiceNo);
    if (!invoice.getCompanyId().equals(batch.getCompanyId())) {
      throw new ResourceNotFoundException("Operations invoice", invoiceNo);
    }
    BigDecimal openPr = open.of(invoiceNo);
    requireAmount(invoiceNo, amount, openPr);
    if (lines.taken(invoiceNo)) {
      throw new BusinessRuleException(
          "CMR_BATCH_AMOUNT", invoiceNo + " is already on a reversal batch");
    }
    DpprBatchLine line =
        lines.save(
            new DpprBatchLine(batch.getId(), batch.getLineCount() + 1, invoiceNo, openPr, reason));
    batch.lineAdded(openPr);
    return line;
  }

  /**
   * Removes a line of a draft.
   *
   * @param batchNo batch
   * @param lineId line
   */
  public void removeLine(String batchNo, Long lineId) {
    DpprBatch batch = get(batchNo);
    DpprBatchLine line =
        lines
            .findById(lineId)
            .filter(l -> l.getBatchId().equals(batch.getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Batch line", lineId));
    batch.lineRemoved(line.getAmount());
    lines.delete(line);
  }

  /**
   * Submits a draft for approval.
   *
   * @param batchNo batch
   * @return the batch
   */
  public DpprBatch submit(String batchNo) {
    DpprBatch batch = get(batchNo);
    batch.submit(currentUser.username(), clock.instant());
    audit.record(ENTITY, batchNo, AuditAction.SUBMIT, batch.getLineCount() + " line(s)");
    return batch;
  }

  /**
   * Approves the batch and posts its lines.
   *
   * @param batchNo batch
   * @param comment comment
   * @return the batch
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public DpprBatch approve(String batchNo, String comment) {
    DpprBatch batch = tx.execute(s -> approveStep(batchNo, comment));
    if (batch == null) {
      return null;
    }
    int posted = 0;
    int failed = 0;
    List<DpprBatchLine> pending = tx.execute(s -> lines(batch));
    for (DpprBatchLine line : pending == null ? List.<DpprBatchLine>of() : pending) {
      try {
        poster.post(batch, line.getId());
        posted++;
      } catch (RuntimeException e) {
        poster.fail(line.getId(), e.getMessage());
        failed++;
      }
    }
    int done = posted;
    int refused = failed;
    return tx.execute(s -> executed(batchNo, done, refused));
  }

  private DpprBatch approveStep(String batchNo, String comment) {
    DpprBatch batch = get(batchNo);
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, batch.getSubmittedBy())
        || CurrentUser.sameUser(user, batch.getCreatedBy())) {
      throw new BusinessRuleException(
          "CMR_BATCH_MAKER_CHECKER", "The requester of a batch does not approve it");
    }
    batch.approve(user, clock.instant());
    audit.record(ENTITY, batchNo, AuditAction.AUTHORIZE, blank(comment) ? "Approved" : comment);
    return batch;
  }

  private DpprBatch executed(String batchNo, int posted, int failed) {
    DpprBatch batch = get(batchNo);
    batch.executed(posted, failed, clock.instant());
    audit.record(
        ENTITY, batchNo, AuditAction.UPDATE, posted + " line(s) posted, " + failed + " refused");
    return batch;
  }

  /**
   * Returns a batch to its requester.
   *
   * @param batchNo batch
   * @param reason reason
   * @return the batch
   */
  public DpprBatch returnBatch(String batchNo, String reason) {
    requireText(reason, "Give the reason of the return");
    DpprBatch batch = get(batchNo);
    batch.returnToDraft(reason.strip());
    audit.record(ENTITY, batchNo, AuditAction.REJECT, reason);
    return batch;
  }

  /**
   * Cancels a draft.
   *
   * @param batchNo batch
   * @return the batch
   */
  public DpprBatch cancel(String batchNo) {
    DpprBatch batch = get(batchNo);
    batch.cancel();
    audit.record(ENTITY, batchNo, AuditAction.DEACTIVATE, "Cancelled");
    return batch;
  }

  private static void requireAmount(String invoiceNo, BigDecimal amount, BigDecimal openPr) {
    if (openPr.signum() <= 0) {
      throw new BusinessRuleException(
          "CMR_BATCH_AMOUNT", invoiceNo + " has no open premium receivable");
    }
    if (amount != null && amount.compareTo(openPr) != 0) {
      throw new BusinessRuleException(
          "CMR_BATCH_AMOUNT",
          invoiceNo
              + ": the amount differs from the open premium receivable of "
              + openPr.toPlainString());
    }
  }

  private static boolean blank(String text) {
    return text == null || text.isBlank();
  }

  private static void requireText(String text, String message) {
    if (blank(text)) {
      throw new BusinessRuleException("CMR_REASON_REQUIRED", message);
    }
  }
}
