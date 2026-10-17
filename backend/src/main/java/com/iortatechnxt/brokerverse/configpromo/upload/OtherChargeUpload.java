package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Other charges of the products (documentation, notarial and authentication fees), on Rates &amp;
 * Taxes: one row per charge, line or product and effective date (the charges of the checklist PM-04
 * that BIBS bills with the premium when OTHER_CHARGES_ENABLED is on).
 */
@Component
public class OtherChargeUpload extends ConfigUploadHandler {

  static final String CODE = "Charge code";
  static final String NAME = "Name";
  static final String LINE = "Line code";
  static final String PRODUCT = "Product code";
  static final String BASIS = "Basis";
  static final String VALUE = "Amount or rate %";
  static final String VAT = "VAT treatment";
  static final String ACCOUNT = "GL account";
  static final String EFFECTIVE = "Effective from / to";

  private static final String TABLE = "cat_other_charge";
  private static final String RATE = "RATE";
  private static final List<String> BASES = List.of("AMOUNT", RATE);
  private static final List<String> VAT_TREATMENTS = List.of("VATABLE", "EXEMPT", "ZERO_RATED");
  private static final Pattern CODE_FORMAT = Pattern.compile("[A-Z0-9_]{1,30}");
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public OtherChargeUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_OTHER_CHARGE";
  }

  @Override
  public String templateId() {
    return "PM-04C";
  }

  @Override
  public String title() {
    return "Other charges";
  }

  @Override
  public String screen() {
    return "Product Maintenance > Rates & Taxes > Other Charges";
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
    return "Product Owner, Product Maintenance; Head, Comptrollership (Tax)";
  }

  @Override
  public List<String> rules() {
    return List.of(
        "One row per charge and line or product; blank line and product = every product.",
        "A row whose charge, line, product and effective date exist updates that charge;"
            + " uploading the same file again changes nothing.",
        "The rows are applied only after a second user approves the upload.");
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(CODE, "Code of the charge", "NOTARIAL_FEE").format("Up to 30 capitals"),
        BulkColumn.required(NAME, "Name on the invoice", "Notarial fee"),
        BulkColumn.optional(LINE, "Line of the charge; blank = every line", "MOTOR").master("line"),
        BulkColumn.optional(PRODUCT, "Product of the charge; blank = every product", "")
            .master("product"),
        BulkColumn.required(BASIS, "Fixed amount or rate of the net premium", "AMOUNT")
            .codes(BASES.toArray(String[]::new)),
        new BulkColumn(VALUE, "Amount, or rate from 0 to 100", true, Type.NUMBER, "150"),
        BulkColumn.required(VAT, "VAT treatment of the charge", "VATABLE")
            .codes(VAT_TREATMENTS.toArray(String[]::new)),
        BulkColumn.required(ACCOUNT, "Postable account the charge is credited to", "4190")
            .master("account of the chart of accounts"),
        BulkColumn.required(
                EFFECTIVE, "First day; last day after a semicolon, blank = open", "01-Jan-2028")
            .format("dd-MMM-yyyy; dd-MMM-yyyy"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return String.join(
        "|",
        row.text(CODE),
        String.valueOf(row.text(LINE)),
        String.valueOf(row.text(PRODUCT)),
        String.valueOf(row.text(EFFECTIVE)));
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    check(
        errors,
        !CODE_FORMAT.matcher(row.text(CODE)).matches(),
        CODE,
        "use up to 30 capital letters, digits or _");
    String line = row.text(LINE);
    check(
        errors,
        line != null && !db.exists("select 1 from cat_product_line where code = ?", line),
        LINE,
        line + " is not a line");
    String product = row.text(PRODUCT);
    check(
        errors,
        product != null && !db.exists("select 1 from cat_product where code = ?", product),
        PRODUCT,
        product + " is not a product");
    oneOf(errors, BASIS, row.text(BASIS), BASES);
    oneOf(errors, VAT, row.text(VAT), VAT_TREATMENTS);
    BigDecimal value = row.number(VALUE);
    check(
        errors,
        value.signum() < 0 || RATE.equals(row.text(BASIS)) && value.compareTo(HUNDRED) > 0,
        VALUE,
        "enter an amount of zero or more, or a rate from 0 to 100");
    check(
        errors,
        !db.exists(
            "select 1 from coa_account where company_id = ? and code = ? and postable",
            context.companyId(),
            row.text(ACCOUNT)),
        ACCOUNT,
        row.text(ACCOUNT) + " is not a postable account of the company");
    check(
        errors,
        UploadCells.period(row.text(EFFECTIVE)).isEmpty(),
        EFFECTIVE,
        "give the first day as dd-MMM-yyyy and, after a semicolon, the last day");
    return errors;
  }

  private static Map<String, Object> key(BulkRow row) {
    return columns(
        "charge_code", row.text(CODE),
        "line_code", row.text(LINE),
        "product_code", row.text(PRODUCT),
        "effective_from", UploadCells.periodOf(row.text(EFFECTIVE)).from());
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return db.action(TABLE, key(row));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    db.upsert(
        TABLE,
        key(row),
        columns(
            "name", row.text(NAME),
            "basis", row.text(BASIS),
            "value", row.number(VALUE),
            "vat_treatment", row.text(VAT),
            "gl_account_code", row.text(ACCOUNT),
            "effective_to", UploadCells.periodOf(row.text(EFFECTIVE)).to()),
        context,
        "Other charge");
    return row.text(CODE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select charge_code, name, line_code, product_code, basis, value, vat_treatment,"
                + " gl_account_code, effective_from, effective_to from cat_other_charge"
                + " where record_status = 'ACTIVE' order by charge_code, line_code nulls first,"
                + " product_code nulls first, effective_from")
        .stream()
        .map(
            r ->
                exportRow(
                    CODE, r.get("charge_code"),
                    NAME, r.get("name"),
                    LINE, r.get("line_code"),
                    PRODUCT, r.get("product_code"),
                    BASIS, r.get("basis"),
                    VALUE, r.get("value"),
                    VAT, r.get("vat_treatment"),
                    ACCOUNT, r.get("gl_account_code"),
                    EFFECTIVE, UploadCells.range(r.get("effective_from"), r.get("effective_to"))))
        .toList();
  }
}
