package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.MinBalanceItem;
import com.iortatechnxt.brokerverse.adjustment.domain.WriteOffAction;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Minimal balance file (ADJID.026, handler {@code MINIMAL_BALANCE_FILE}): one invoice and its
 * premium receivable balance per row. Only balances within the file range (10.00 to 100.00) whose
 * invoice and balance match the ledger are processed: a debit balance is written off, a credit
 * balance credited; the same file cannot be uploaded twice and an invoice is written off once.
 */
@Component
public class MinimalBalanceFileHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "MINIMAL_BALANCE_FILE";

  static final String INVOICE = "Invoice No";
  static final String BALANCE = "Balance";

  private final WriteOffService writeOffs;
  private final TransactionTemplate checks;

  /**
   * Creates the handler.
   *
   * @param writeOffs write-offs
   * @param txManager transaction manager (checks in their own read-only transaction)
   */
  public MinimalBalanceFileHandler(
      WriteOffService writeOffs, PlatformTransactionManager txManager) {
    this.writeOffs = writeOffs;
    this.checks = new TransactionTemplate(txManager);
    this.checks.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.checks.setReadOnly(true);
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Minimal balance write-off / credit file";
  }

  @Override
  public String permission() {
    return "ADJ_POST";
  }

  @Override
  public String instructions() {
    return "One row per invoice with its premium receivable balance as in the ledger. Only"
        + " balances from 10.00 to 100.00 (parameter MIN_BALANCE_FILE_RANGE) are processed: a"
        + " debit balance is written off, a credit balance (overpayment) is credited. Each invoice"
        + " is written off once.";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(
            INVOICE, "Invoice number of the Operations ledger", "BI-HO-2026-000001"),
        new BulkColumn(
            BALANCE, "Premium receivable balance", true, BulkColumn.Type.NUMBER, "35.50"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(INVOICE);
  }

  @Override
  public boolean blocksDuplicateFiles() {
    return true;
  }

  @Override
  public List<String> outcomeCategories() {
    return List.of(WriteOffAction.WRITE_OFF.name(), WriteOffAction.CREDIT.name());
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    checks.executeWithoutResult(
        s -> errors.addAll(writeOffs.check(row.text(INVOICE), row.number(BALANCE))));
    return errors;
  }

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    MinBalanceItem item = writeOffs.writeOff(row.text(INVOICE), context.jobNo(), true);
    return new BulkOutcome(item.getInvoiceNo(), item.getAction().name());
  }
}
