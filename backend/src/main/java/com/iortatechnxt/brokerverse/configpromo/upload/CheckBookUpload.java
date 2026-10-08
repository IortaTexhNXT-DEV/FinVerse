package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** D0-12 Check books of the bank accounts, on Bank Accounts and Checks (Disbursement). */
@Component
public class CheckBookUpload extends ConfigUploadHandler {

  static final String BANK_ACCOUNT = "Bank account code";
  static final String FIRST = "First check number";
  static final String LAST = "Last check number";
  static final String NEXT = "Next check number";
  static final String RECEIVED = "Received on";

  private static final String TABLE = "pay_cheque_book";
  private static final Pattern DIGITS = Pattern.compile("\\d{1,15}");

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public CheckBookUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_CHECK_BOOK";
  }

  @Override
  public String templateId() {
    return "D0-12";
  }

  @Override
  public String title() {
    return "Check books";
  }

  @Override
  public String screen() {
    return "Disbursement > Bank Accounts and Checks";
  }

  @Override
  public String permission() {
    return "MASTER_MAINTAIN";
  }

  @Override
  public String approvePermission() {
    return "MASTER_AUTHORIZE";
  }

  @Override
  public String filledBy() {
    return "Head, Comptrollership; Treasury";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(BANK_ACCOUNT, "Bank account of the book", "BDO-CA-001")
            .master("bank account"),
        BulkColumn.required(FIRST, "First check number of the book", "0001001").format("Digits"),
        BulkColumn.required(LAST, "Last check number of the book", "0001100").format("Digits"),
        BulkColumn.required(NEXT, "Next check number to issue", "0001001").format("Digits"),
        new BulkColumn(RECEIVED, "Date the book was received", true, Type.DATE, "2027-12-01"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(BANK_ACCOUNT) + "|" + row.text(FIRST);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    for (String h : List.of(FIRST, LAST, NEXT)) {
      if (!DIGITS.matcher(row.text(h)).matches()) {
        errors.add(error(h, "enter the check number in digits"));
      }
    }
    Optional<Long> account = bankAccount(row, context);
    if (account.isEmpty()) {
      errors.add(
          error(BANK_ACCOUNT, row.text(BANK_ACCOUNT) + " is not a bank account of the company"));
    }
    if (!errors.isEmpty()) {
      return errors;
    }
    long first = Long.parseLong(row.text(FIRST));
    long last = Long.parseLong(row.text(LAST));
    long next = Long.parseLong(row.text(NEXT));
    if (first > last) {
      errors.add(error(LAST, "is below the first check number"));
    }
    if (next < first || next > last + 1) {
      errors.add(error(NEXT, "must be between the first and the last check number"));
    }
    if (db.exists(
        "select 1 from pay_cheque_book where bank_account_id = ? and first_no <> ?"
            + " and first_no <= ? and last_no >= ? and status <> 'CANCELLED'",
        account.get(),
        first,
        last,
        first)) {
      errors.add(error(FIRST, "the range overlaps another check book of the account"));
    }
    return errors;
  }

  private Optional<Long> bankAccount(BulkRow row, BulkContext context) {
    return db.id(
        "select id from pay_bank_account where company_id = ? and code = ?",
        context.companyId(),
        row.text(BANK_ACCOUNT));
  }

  private Map<String, Object> key(BulkRow row, BulkContext context) {
    return columns(
        "bank_account_id",
        bankAccount(row, context).orElse(null),
        "first_no",
        Long.valueOf(row.text(FIRST)));
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
            "last_no", Long.valueOf(row.text(LAST)),
            "next_no", Long.valueOf(row.text(NEXT)),
            "received_on", row.date(RECEIVED),
            "status", "ACTIVE"),
        context,
        "Check book");
    return row.text(BANK_ACCOUNT) + " " + row.text(FIRST) + "-" + row.text(LAST);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select a.code, c.first_no, c.last_no, c.next_no, c.received_on from pay_cheque_book c"
                + " join pay_bank_account a on a.id = c.bank_account_id where a.company_id = ?"
                + " order by a.code, c.first_no",
            companyId)
        .stream()
        .map(
            r ->
                exportRow(
                    BANK_ACCOUNT, r.get("code"),
                    FIRST, r.get("first_no"),
                    LAST, r.get("last_no"),
                    NEXT, r.get("next_no"),
                    RECEIVED, r.get("received_on")))
        .toList();
  }
}
