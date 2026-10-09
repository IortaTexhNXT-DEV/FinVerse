package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecord;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecordRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.ReinstatementType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.TenderType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordReason;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordTender;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BDOI's cancellation and reinstatement records (FRS.CSH.03.01, 04.01): the cashier selects issued
 * ARs only or ORs only and saves one record per receipt with the reason ({@code CN-AR-<sequence>},
 * {@code RE-AR-<sequence>}, status Created); a reinstatement is full or, for an AR, partial by
 * account, with the information of CSHID.005. The records are then edited, cancelled and submitted
 * like the creation records.
 */
@Service
@Transactional
public class ReversalRecordService {

  private static final Set<RecordKind> REVERSALS =
      Set.of(RecordKind.CANCELLATION, RecordKind.REINSTATEMENT);

  private final ReceiptRecordRepository records;
  private final CashReceiptRepository receipts;
  private final ApplicationRepository applications;
  private final ReversalChecks checks;
  private final RecordNumbers numbers;
  private final CashieringDecisions decisions;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param records records
   * @param receipts receipts
   * @param applications applications (accounts of a receipt)
   * @param checks validations of the records
   * @param numbers record numbers
   * @param decisions settings
   * @param audit audit trail
   */
  public ReversalRecordService(
      ReceiptRecordRepository records,
      CashReceiptRepository receipts,
      ApplicationRepository applications,
      ReversalChecks checks,
      RecordNumbers numbers,
      CashieringDecisions decisions,
      AuditTrailService audit) {
    this.records = records;
    this.receipts = receipts;
    this.applications = applications;
    this.checks = checks;
    this.numbers = numbers;
    this.decisions = decisions;
    this.audit = audit;
  }

  /**
   * Saves one cancellation record per receipt selected (FRS.CSH.03.01.05 to 03.01.06.03).
   *
   * @param receiptIds issued ARs only or ORs only
   * @param reason reason and its text
   * @return the records, status Created
   */
  public List<ReceiptRecord> cancel(List<Long> receiptIds, RecordReason reason) {
    List<Receipt> selected = checks.selection(receiptIds);
    List<ReceiptRecord> saved = new ArrayList<>();
    for (Receipt receipt : selected) {
      checks.checkCancellation(receipt, reason);
      saved.add(save(RecordKind.CANCELLATION, receipt, reason, accounts(receipt, List.of())));
    }
    return saved;
  }

  /**
   * Saves one reinstatement record per receipt selected (FRS.CSH.04.01.05 to 04.01.07.03).
   *
   * @param receiptIds issued ARs only or ORs only
   * @param reason reason, remarks, type and the information of CSHID.005
   * @param selectedAccounts accounts of a partial reinstatement
   * @return the records, status Created
   */
  public List<ReceiptRecord> reinstate(
      List<Long> receiptIds, RecordReason reason, List<String> selectedAccounts) {
    if (!decisions.reinstatesIssuedReceipts()) {
      throw new BusinessRuleException(
          "REINSTATEMENT_SCOPE",
          "Reinstatement of an issued receipt is not enabled (setting "
              + CashieringDecisions.REINSTATEMENT_SCOPE
              + ")");
    }
    List<Receipt> selected = checks.selection(receiptIds);
    List<ReceiptRecord> saved = new ArrayList<>();
    for (Receipt receipt : selected) {
      checks.checkReinstatement(receipt, reason, selectedAccounts);
      List<String> chosen =
          reason.reinstatementType() == ReinstatementType.PARTIAL ? selectedAccounts : List.of();
      saved.add(save(RecordKind.REINSTATEMENT, receipt, reason, accounts(receipt, chosen)));
    }
    return saved;
  }

  /**
   * Changes the reason of a cancellation or reinstatement record in status Created or Returned
   * (FRS.CSH.03.01.07, 04.01.08); the change is logged.
   *
   * @param id record
   * @param reason new reason
   * @param selectedAccounts accounts of a partial reinstatement
   * @return the record
   */
  public ReceiptRecord edit(Long id, RecordReason reason, List<String> selectedAccounts) {
    ReceiptRecord record =
        records
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ReceiptRecordService.ENTITY, id));
    if (!REVERSALS.contains(record.getRecordKind())) {
      throw new BusinessRuleException(
          "RECORD_KIND_MISMATCH", record.getRecordNo() + " is not a cancellation or reinstatement");
    }
    Receipt receipt = receipt(record.getReceiptId());
    if (record.getRecordKind() == RecordKind.CANCELLATION) {
      checks.requireReason(ReversalChecks.CANCEL_REASON, reason);
      checks.requireCheckReason(receipt, reason);
    } else {
      checks.checkReinstatementDetails(receipt, reason, selectedAccounts);
    }
    String before = String.valueOf(record.getReason());
    List<String> chosen =
        reason.reinstatementType() == ReinstatementType.PARTIAL ? selectedAccounts : List.of();
    record.target(receipt, reason, accounts(receipt, chosen));
    audit.record(
        ReceiptRecordService.ENTITY,
        record.getRecordNo(),
        AuditAction.UPDATE,
        "Edited. Before: " + before + ". After: " + reason);
    return record;
  }

  private ReceiptRecord save(
      RecordKind kind, Receipt receipt, RecordReason reason, List<RecordAccount> accounts) {
    ReceiptRecord record =
        new ReceiptRecord(
            receipt.getCompanyId(), numbers.next(kind, receipt.getKind()), kind, receipt.getKind());
    record.target(receipt, reason, accounts);
    BigDecimal amount =
        accounts.stream().map(RecordAccount::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    record.money(
        new RecordTender(
            receipt.getMode() == PaymentMode.CHECK ? TenderType.CHECK : TenderType.CASH,
            receipt.getCurrency(),
            receipt.getBankAccount(),
            accounts.isEmpty() ? receipt.liveAmount() : amount,
            null,
            null,
            null,
            null,
            receipt.getReceiptDate(),
            null,
            null));
    ReceiptRecord saved = records.save(record);
    audit.record(
        ReceiptRecordService.ENTITY,
        saved.getRecordNo(),
        AuditAction.CREATE,
        "Saved for " + receipt.getReceiptNo() + ": " + reason);
    return saved;
  }

  /**
   * The accounts of a receipt with their applied amounts, the chosen ones only when given.
   *
   * @param receipt receipt
   * @param chosen account references, empty for all
   * @return accounts
   */
  List<RecordAccount> accounts(Receipt receipt, List<String> chosen) {
    List<RecordAccount> found = new ArrayList<>();
    for (Application app : applications.findByReceiptIdOrderByIdAsc(receipt.getId())) {
      boolean wanted = chosen.isEmpty() || chosen.contains(app.getInvoiceNo());
      if (app.isActive() && wanted) {
        found.add(new RecordAccount(app.getInvoiceNo(), app.getAmount()));
      }
    }
    if (receipt.getKind() == ReceiptKind.OR && found.isEmpty()) {
      receipt.getLines().stream()
          .filter(l -> l.getInvoiceNo() != null)
          .forEach(l -> found.add(new RecordAccount(l.getInvoiceNo(), l.getNet())));
    }
    return found;
  }

  private Receipt receipt(Long id) {
    return receipts
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(CashReceiptService.ENTITY, id));
  }
}
