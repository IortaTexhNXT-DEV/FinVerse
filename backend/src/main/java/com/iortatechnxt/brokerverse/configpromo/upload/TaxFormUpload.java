package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * TX-02 Tax forms: the tax returns and remittance forms the company files, with their frequency,
 * due date and accounts, on Tax Codes & Forms (Forms tab).
 */
@Component
public class TaxFormUpload extends ConfigUploadHandler {

  static final String CODE = "Form code";
  static final String NAME = "Name";
  static final String AUTHORITY = "Authority";
  static final String FREQUENCY = "Frequency";
  static final String WORKSHEET = "Worksheet";
  static final String MONTHS = "Months after period end";
  static final String DAY = "Due day";
  static final String PAYABLE = "Tax payable account";
  static final String CREDIT = "Credit account";
  static final String FROM = "Tracked from";

  private static final String TABLE = "tax_form";
  private static final String NONE = "NONE";
  private static final List<String> AUTHORITIES = List.of("BIR", "LGU", "BFP");
  private static final List<String> FREQUENCIES =
      List.of("MONTHLY", "QUARTERLY", "MONTHLY_EXCEPT_QUARTER_END", "ANNUAL");
  private static final List<String> WORKSHEETS = List.of("VAT", "EWT", NONE);
  private static final int CODE_LENGTH = 20;
  private static final int NAME_LENGTH = 150;
  private static final int MAX_MONTHS = 12;
  private static final int MAX_DAY = 31;

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public TaxFormUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_TAX_FORM";
  }

  @Override
  public String templateId() {
    return "TX-02";
  }

  @Override
  public String title() {
    return "Tax forms";
  }

  @Override
  public String screen() {
    return "Tax & Statutory > Tax Codes & Forms";
  }

  @Override
  public String permission() {
    return "TAX_MANAGE";
  }

  @Override
  public String approvePermission() {
    return "MASTER_AUTHORIZE";
  }

  @Override
  public String filledBy() {
    return "Head, Comptrollership (Tax)";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(CODE, "BIR or local form number", "2550Q")
            .format("Up to 20 characters"),
        BulkColumn.required(NAME, "Name of the return", "Quarterly VAT return")
            .format("Text, up to 150"),
        BulkColumn.required(AUTHORITY, "Who the form is filed with", "BIR")
            .codes(AUTHORITIES.toArray(String[]::new)),
        BulkColumn.required(FREQUENCY, "How often the form is filed", "QUARTERLY")
            .codes(FREQUENCIES.toArray(String[]::new)),
        BulkColumn.required(
                WORKSHEET,
                "Computation worksheet behind the form (NONE: prepared outside the system,"
                    + " reminder only)",
                "VAT")
            .codes(WORKSHEETS.toArray(String[]::new)),
        new BulkColumn(
                MONTHS,
                "Months after the end of the period in which the form is due",
                true,
                Type.NUMBER,
                "0")
            .allowed("A whole number from 0 to 12"),
        new BulkColumn(DAY, "Day of the month the form is due", true, Type.NUMBER, "25")
            .allowed("A whole number from 1 to 31 (31 = month end)"),
        BulkColumn.optional(PAYABLE, "Account the tax payable is posted to", "")
            .master("postable account of the chart of accounts"),
        BulkColumn.optional(
                CREDIT, "Input VAT account credited against the output VAT (VAT return only)", "")
            .master("postable account of the chart of accounts"),
        new BulkColumn(FROM, "First period the filing is tracked", true, Type.DATE, "2028-01-01"));
  }

  @Override
  public List<String> rules() {
    return List.of(
        "The layout is the tab " + templateId() + " of the master data and configuration workbook.",
        "A row whose form code already exists updates that form; uploading the same file again"
            + " changes nothing.",
        "A form with a worksheet and a tax payable account is tracked until its payment clears"
            + " the tax payable; any other form is a reminder on the tax calendar.",
        "The rows are applied only after a second user approves the upload.");
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(CODE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    check(errors, row.text(CODE).length() > CODE_LENGTH, CODE, "use up to 20 characters");
    check(errors, row.text(NAME).length() > NAME_LENGTH, NAME, "use up to 150 characters");
    oneOf(errors, AUTHORITY, row.text(AUTHORITY), AUTHORITIES);
    oneOf(errors, FREQUENCY, row.text(FREQUENCY), FREQUENCIES);
    oneOf(errors, WORKSHEET, row.text(WORKSHEET), WORKSHEETS);
    whole(errors, MONTHS, row.number(MONTHS), 0, MAX_MONTHS);
    whole(errors, DAY, row.number(DAY), 1, MAX_DAY);
    postable(errors, context, PAYABLE, row.text(PAYABLE));
    postable(errors, context, CREDIT, row.text(CREDIT));
    return errors;
  }

  private void postable(List<String> errors, BulkContext context, String header, String code) {
    if (code != null
        && !db.exists(
            "select 1 from coa_account where company_id = ? and code = ? and postable",
            context.companyId(),
            code)) {
      errors.add(error(header, code + " is not a postable account of the company"));
    }
  }

  /** A form is tracked when its payment clears a tax payable computed on a worksheet. */
  static boolean tracked(String worksheet, String payableAccount) {
    return !NONE.equals(worksheet) && payableAccount != null;
  }

  private static Map<String, Object> key(BulkRow row, BulkContext context) {
    return columns("company_id", context.companyId(), "code", row.text(CODE));
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
            "authority", row.text(AUTHORITY),
            "frequency", row.text(FREQUENCY),
            "worksheet", row.text(WORKSHEET),
            "due_months_after", row.number(MONTHS).intValueExact(),
            "due_day", row.number(DAY).intValueExact(),
            "payable_account_code", row.text(PAYABLE),
            "credit_account_code", row.text(CREDIT),
            "track_filing", tracked(row.text(WORKSHEET), row.text(PAYABLE)),
            "effective_from", row.date(FROM)),
        context,
        "TaxForm");
    return row.text(CODE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select code, name, authority, frequency, worksheet, due_months_after, due_day,"
                + " payable_account_code, credit_account_code, effective_from from tax_form"
                + " where company_id = ? order by code",
            companyId)
        .stream()
        .map(
            r ->
                exportRow(
                    CODE, r.get("code"),
                    NAME, r.get("name"),
                    AUTHORITY, r.get("authority"),
                    FREQUENCY, r.get("frequency"),
                    WORKSHEET, r.get("worksheet"),
                    MONTHS, r.get("due_months_after"),
                    DAY, r.get("due_day"),
                    PAYABLE, r.get("payable_account_code"),
                    CREDIT, r.get("credit_account_code"),
                    FROM, r.get("effective_from")))
        .toList();
  }
}
