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

/** D0-10 Tax codes: the tax codes with their ATC, rate and GL account, on Tax Codes & Forms. */
@Component
public class TaxCodeUpload extends ConfigUploadHandler {
  private static final String EWT = "EWT";

  static final String CODE = "Tax code";
  static final String NAME = "Name";
  static final String TYPE = "Tax type";
  static final String ATC = "ATC";
  static final String PAYEE = "Payee class";
  static final String RATE = "Rate %";
  static final String ACCOUNT = "GL account";
  static final String NATURE = "Income nature";
  static final String FROM = "Effective from";
  static final String TO = "Effective to";

  private static final String TABLE = "tax_code";
  private static final List<String> TYPES =
      List.of(
          "VAT_OUTPUT",
          "VAT_INPUT",
          "VAT_ZERO_RATED",
          "VAT_EXEMPT",
          "PREMIUM_TAX",
          "DST",
          "LGT",
          "FST",
          EWT);
  private static final List<String> PAYEE_CLASSES = List.of("INDIVIDUAL", "CORPORATE");
  private static final Pattern CODE_FORMAT = Pattern.compile("[A-Z0-9_-]{1,20}");
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public TaxCodeUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_TAX_CODE";
  }

  @Override
  public String templateId() {
    return "D0-10";
  }

  @Override
  public String title() {
    return "Tax codes";
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
        BulkColumn.required(CODE, "Code of the tax", "EWT_WC158").format("Up to 20 capitals"),
        BulkColumn.required(NAME, "Name of the tax", "EWT 1% - goods"),
        BulkColumn.required(TYPE, "Kind of tax", EWT).codes(TYPES.toArray(String[]::new)),
        BulkColumn.optional(ATC, "Alphanumeric tax code of the BIR", "WC158").when(EWT),
        BulkColumn.optional(PAYEE, "Payee class of the withholding", "CORPORATE")
            .codes(PAYEE_CLASSES.toArray(String[]::new)),
        new BulkColumn(RATE, "Rate in percent", true, Type.NUMBER, "1")
            .allowed("A number from 0 to 100"),
        BulkColumn.required(ACCOUNT, "Account the tax posts to", "2150.02")
            .master("account of the chart of accounts"),
        BulkColumn.optional(NATURE, "Nature of the income", "Purchase of goods"),
        new BulkColumn(FROM, "First day of the rate", true, Type.DATE, "2028-01-01"),
        new BulkColumn(TO, "Last day of the rate; blank = open", false, Type.DATE, ""));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(CODE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    if (!CODE_FORMAT.matcher(row.text(CODE)).matches()) {
      errors.add(error(CODE, "use up to 20 capital letters, digits, - or _"));
    }
    oneOf(errors, TYPE, row.text(TYPE), TYPES);
    oneOf(errors, PAYEE, row.text(PAYEE), PAYEE_CLASSES);
    if (EWT.equals(row.text(TYPE)) && row.text(ATC) == null) {
      errors.add(error(ATC, "mandatory for a withholding tax"));
    }
    BigDecimal rate = row.number(RATE);
    if (rate.signum() < 0 || rate.compareTo(HUNDRED) > 0) {
      errors.add(error(RATE, "enter a rate from 0 to 100"));
    }
    if (!db.account(context.companyId(), row.text(ACCOUNT))) {
      errors.add(error(ACCOUNT, row.text(ACCOUNT) + " is not an account of the company"));
    }
    if (row.date(TO) != null && row.date(TO).isBefore(row.date(FROM))) {
      errors.add(error(TO, "is before the effective from date"));
    }
    return errors;
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
            "tax_type", row.text(TYPE),
            "atc", row.text(ATC),
            "payee_class", row.text(PAYEE),
            "rate", row.number(RATE),
            "gl_account_code", row.text(ACCOUNT),
            "income_nature", row.text(NATURE),
            "effective_from", row.date(FROM),
            "effective_to", row.date(TO)),
        context,
        "Tax code");
    return row.text(CODE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select code, name, tax_type, atc, payee_class, rate, gl_account_code, income_nature,"
                + " effective_from, effective_to from tax_code where company_id = ? order by code",
            companyId)
        .stream()
        .map(
            r ->
                exportRow(
                    CODE, r.get("code"),
                    NAME, r.get("name"),
                    TYPE, r.get("tax_type"),
                    ATC, r.get("atc"),
                    PAYEE, r.get("payee_class"),
                    RATE, r.get("rate"),
                    ACCOUNT, r.get("gl_account_code"),
                    NATURE, r.get("income_nature"),
                    FROM, r.get("effective_from"),
                    TO, r.get("effective_to")))
        .toList();
  }
}
