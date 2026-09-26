package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.PrintBatch;
import com.iortatechnxt.brokerverse.cashiering.domain.PrintBatchRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Batch printing of ARs and ORs (CSHID.019): the selected receipts (a selection or the result of a
 * filter by location, insurer or date) are printed into one merged PDF kept as the batch copy; each
 * receipt's print count is logged, cancelled receipts fail with a message, and the failures can be
 * retried in a new batch. Also issues Certificates of Payment (Annex II report 19).
 */
@Service
@Transactional
public class BatchPrintService {

  private static final String ENTITY = "PrintBatch";
  private static final String COP_LOG =
      "insert into csh_certificate_of_payment (company_id, receipt_id, receipt_no, policy_no,"
          + " requesting_unit, issued_at, issued_by) values (?, ?, ?, ?, ?, ?, ?)";
  private static final int MAX_BATCH = 500;

  private final PrintBatchRepository batches;
  private final CashReceiptRepository receipts;
  private final ReceiptDocument documents;
  private final DocumentNumberService numbers;
  private final JdbcTemplate jdbc;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param batches print batches
   * @param receipts receipts
   * @param documents receipt documents
   * @param numbers document numbers
   * @param jdbc JDBC
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public BatchPrintService(
      PrintBatchRepository batches,
      CashReceiptRepository receipts,
      ReceiptDocument documents,
      DocumentNumberService numbers,
      JdbcTemplate jdbc,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.batches = batches;
    this.receipts = receipts;
    this.documents = documents;
    this.numbers = numbers;
    this.jdbc = jdbc;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Prints receipts into one batch.
   *
   * @param companyId company
   * @param receiptIds receipts
   * @param criteria how they were selected
   * @return the batch
   */
  public PrintBatch print(Long companyId, Collection<Long> receiptIds, String criteria) {
    if (receiptIds.isEmpty() || receiptIds.size() > MAX_BATCH) {
      throw new BusinessRuleException(
          "PRINT_SELECTION", "Select between 1 and " + MAX_BATCH + " receipts to print");
    }
    PrintBatch batch =
        new PrintBatch(companyId, numbers.next("PRB-" + LocalDate.now(clock).getYear()), criteria);
    List<byte[]> pdfs = new ArrayList<>();
    for (Receipt r : receipts.findByIdInOrderByIdAsc(receiptIds)) {
      if (!r.getCompanyId().equals(companyId)) {
        continue;
      }
      if (r.getStatus() == ReceiptStatus.CANCELLED) {
        batch.add(
            new PrintBatch.Line(
                r.getId(), r.getReceiptNo(), PrintBatch.FAILED, "Receipt is cancelled"));
        continue;
      }
      pdfs.add(documents.pdf(r));
      r.printed(clock.instant());
      batch.add(new PrintBatch.Line(r.getId(), r.getReceiptNo(), PrintBatch.PRINTED, null));
    }
    batch.finish(batch.getBatchNo() + ".pdf", pdfs.isEmpty() ? null : ReceiptDocument.merge(pdfs));
    PrintBatch saved = batches.save(batch);
    audit.record(
        ENTITY,
        saved.getBatchNo(),
        AuditAction.EXPORT,
        saved.getPrintedCount() + " printed, " + saved.getFailedCount() + " failed: " + criteria);
    return saved;
  }

  /**
   * Prints the failed receipts of a batch again.
   *
   * @param batchId batch
   * @return the new batch
   */
  public PrintBatch retry(Long batchId) {
    PrintBatch failed = get(batchId);
    List<Long> ids =
        failed.getLines().stream()
            .filter(l -> PrintBatch.FAILED.equals(l.getStatus()))
            .map(PrintBatch.Line::getReceiptId)
            .toList();
    if (ids.isEmpty()) {
      throw new BusinessRuleException(
          "PRINT_NOTHING_FAILED", failed.getBatchNo() + " has no failures");
    }
    return print(failed.getCompanyId(), ids, "Retry of " + failed.getBatchNo());
  }

  /**
   * Issues a Certificate of Payment and logs it.
   *
   * @param receiptId receipt
   * @param policyNo policy number
   * @param requestingUnit requesting marketing unit
   * @return PDF
   */
  public byte[] certificateOfPayment(Long receiptId, String policyNo, String requestingUnit) {
    Receipt r =
        receipts
            .findById(receiptId)
            .orElseThrow(() -> new ResourceNotFoundException(CashReceiptService.ENTITY, receiptId));
    if (r.getStatus() == ReceiptStatus.CANCELLED) {
      throw new BusinessRuleException(
          "COP_CANCELLED_RECEIPT", "Receipt " + r.getReceiptNo() + " is cancelled");
    }
    byte[] pdf = documents.certificateOfPayment(r, policyNo, requestingUnit);
    jdbc.update(
        COP_LOG,
        r.getCompanyId(),
        r.getId(),
        r.getReceiptNo(),
        policyNo,
        requestingUnit,
        Timestamp.from(clock.instant()),
        currentUser.username());
    audit.record(
        CashReceiptService.ENTITY,
        r.getReceiptNo(),
        AuditAction.EXPORT,
        "Certificate of Payment for " + requestingUnit);
    return pdf;
  }

  /**
   * Batches of a company.
   *
   * @param companyId company
   * @param pageable page
   * @return batches, newest first
   */
  @Transactional(readOnly = true)
  public Page<PrintBatch> list(Long companyId, Pageable pageable) {
    return batches.findByCompanyIdOrderByIdDesc(companyId, pageable);
  }

  /**
   * One batch.
   *
   * @param id id
   * @return batch
   */
  @Transactional(readOnly = true)
  public PrintBatch get(Long id) {
    PrintBatch b =
        batches.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    Hibernate.initialize(b.getLines());
    return b;
  }
}
