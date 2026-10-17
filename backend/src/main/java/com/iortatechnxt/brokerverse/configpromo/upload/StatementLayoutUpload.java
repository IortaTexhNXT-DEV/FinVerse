package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** D0-14 Bank statement layouts, on Bank Statement Layouts (Setup & Administration). */
@Component
public class StatementLayoutUpload extends ConfigUploadHandler {
  private static final String COLUMN_FORMAT = "Column header or number";

  static final String BANK_ACCOUNT = "Bank account code";
  static final String NAME = "Layout name";
  static final String DATE = "Date column";
  static final String DESCRIPTION = "Description column";
  static final String REFERENCE = "Reference column";
  static final String DEBIT = "Debit column";
  static final String CREDIT = "Credit column";
  static final String AMOUNT = "Amount column";
  static final String BALANCE = "Balance column";
  static final String PATTERN = "Date format";

  private static final String TABLE = "brs_statement_layout";

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public StatementLayoutUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_STATEMENT_LAYOUT";
  }

  @Override
  public String templateId() {
    return "D0-14";
  }

  @Override
  public String title() {
    return "Bank statement layouts";
  }

  @Override
  public String screen() {
    return "Setup & Administration > Bank Statement Layouts";
  }

  @Override
  public String permission() {
    return "RECONCILIATION_MANAGE";
  }

  @Override
  public String approvePermission() {
    return "MASTER_AUTHORIZE";
  }

  @Override
  public String filledBy() {
    return "Head, Comptrollership";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(BANK_ACCOUNT, "Bank account of the statements", "BNK-CA-001")
            .master("bank account"),
        BulkColumn.required(NAME, "Name of the layout", "Bank CSV statement"),
        BulkColumn.required(DATE, "Column of the posting date", "Posting Date")
            .format(COLUMN_FORMAT),
        BulkColumn.required(DESCRIPTION, "Column of the description", "Description")
            .format(COLUMN_FORMAT),
        BulkColumn.optional(REFERENCE, "Column of the reference", "Reference No.")
            .format(COLUMN_FORMAT),
        BulkColumn.optional(DEBIT, "Column of the debits", "Debit").format(COLUMN_FORMAT),
        BulkColumn.optional(CREDIT, "Column of the credits", "Credit").format(COLUMN_FORMAT),
        BulkColumn.optional(AMOUNT, "Column of a signed amount, instead of debit and credit", "")
            .format(COLUMN_FORMAT),
        BulkColumn.optional(BALANCE, "Column of the balance", "Balance").format(COLUMN_FORMAT),
        BulkColumn.required(PATTERN, "How the bank writes dates", "MM/dd/yyyy")
            .format("For example dd/MM/yyyy"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(BANK_ACCOUNT);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    if (!db.exists(
        "select 1 from pay_bank_account where company_id = ? and code = ?",
        context.companyId(),
        row.text(BANK_ACCOUNT))) {
      errors.add(
          error(BANK_ACCOUNT, row.text(BANK_ACCOUNT) + " is not a bank account of the company"));
    }
    if (row.text(AMOUNT) == null && (row.text(DEBIT) == null || row.text(CREDIT) == null)) {
      errors.add(error(AMOUNT, "give the amount column, or the debit and the credit columns"));
    }
    try {
      DateTimeFormatter.ofPattern(row.text(PATTERN));
    } catch (IllegalArgumentException e) {
      errors.add(error(PATTERN, row.text(PATTERN) + " is not a date format"));
    }
    return errors;
  }

  private static Map<String, Object> key(BulkRow row, BulkContext context) {
    return columns("company_id", context.companyId(), "bank_account_code", row.text(BANK_ACCOUNT));
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return db.action(TABLE, key(row, context));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    db.upsert(
        TABLE,
        key(row, context),
        columns(
            "name", row.text(NAME),
            "date_column", row.text(DATE),
            "description_column", row.text(DESCRIPTION),
            "reference_column", row.text(REFERENCE),
            "debit_column", row.text(DEBIT),
            "credit_column", row.text(CREDIT),
            "amount_column", row.text(AMOUNT),
            "balance_column", row.text(BALANCE),
            "date_pattern", row.text(PATTERN)),
        context,
        "Bank statement layout");
    return row.text(BANK_ACCOUNT);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select bank_account_code, name, date_column, description_column, reference_column,"
                + " debit_column, credit_column, amount_column, balance_column, date_pattern"
                + " from brs_statement_layout where company_id = ? order by bank_account_code",
            companyId)
        .stream()
        .map(
            r ->
                exportRow(
                    BANK_ACCOUNT, r.get("bank_account_code"),
                    NAME, r.get("name"),
                    DATE, r.get("date_column"),
                    DESCRIPTION, r.get("description_column"),
                    REFERENCE, r.get("reference_column"),
                    DEBIT, r.get("debit_column"),
                    CREDIT, r.get("credit_column"),
                    AMOUNT, r.get("amount_column"),
                    BALANCE, r.get("balance_column"),
                    PATTERN, r.get("date_pattern")))
        .toList();
  }
}
