package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** D0-01 Company: the company records, on Companies (Setup & Administration). */
@Component
public class CompanyUpload extends ConfigUploadHandler {

  static final String CODE = "Company code";
  static final String NAME = "Registered name";
  static final String TIN = "TIN";
  static final String ADDRESS = "Registered address";
  static final String CURRENCY = "Base currency";
  static final String MONTH = "Fiscal year start month";
  static final String BACK = "Back-value days";
  static final String FORWARD = "Forward-value days";
  static final String RETAINED = "Retained earnings account";
  static final String RDO = "RDO code";
  static final String VAT_REGISTERED = "VAT registered";
  static final String CAS_NO = "CAS permit no.";
  static final String CAS_DATE = "CAS permit date";
  static final String EINVOICING_NO = "E-invoicing permit no.";
  static final String EINVOICING_DATE = "E-invoicing permit date";

  /** The BIR registration columns added after the workbook tab (company tax registration). */
  private static final Set<String> TAX_REGISTRATION =
      Set.of(RDO, VAT_REGISTERED, CAS_NO, CAS_DATE, EINVOICING_NO, EINVOICING_DATE);

  static final Pattern RDO_FORMAT = Pattern.compile("\\d{3}[A-Z]?");

  private static final Pattern CODE_FORMAT = Pattern.compile("[A-Z0-9]{1,10}");
  private static final Pattern TIN_FORMAT = Pattern.compile("\\d{3}-\\d{3}-\\d{3}-\\d{3,5}");
  private static final int MAX_DAYS = 365;
  private static final int MONTHS = 12;

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public CompanyUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_COMPANY";
  }

  @Override
  public String templateId() {
    return "D0-01";
  }

  @Override
  public String title() {
    return "Company";
  }

  @Override
  public String screen() {
    return "Setup & Administration > Companies";
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
    return "Head, Comptrollership";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(CODE, "Code of the company on every record", "IBRK")
            .format("Up to 10 capitals"),
        BulkColumn.required(NAME, "Name as registered with the SEC", "Insurance Brokers, Inc."),
        BulkColumn.required(TIN, "Tax identification number with branch code", "000-000-000-00000")
            .format("000-000-000-00000"),
        BulkColumn.required(ADDRESS, "Address printed on receipts and BIR forms", "Makati City"),
        BulkColumn.required(CURRENCY, "Currency of the books", BulkColumn.BASE_CURRENCY_EXAMPLE)
            .format("ISO 4217"),
        new BulkColumn(MONTH, "First month of the financial year", true, Type.NUMBER, "1")
            .allowed("A whole number from 1 to 12"),
        new BulkColumn(
                BACK, "How many days before today a posting may be dated", true, Type.NUMBER, "5")
            .allowed("A whole number from 0 to 365"),
        new BulkColumn(
                FORWARD, "How many days after today a posting may be dated", true, Type.NUMBER, "0")
            .allowed("A whole number from 0 to 365"),
        BulkColumn.required(RETAINED, "Account that receives the year-end result", "3200")
            .master("account of the chart of accounts"),
        BulkColumn.optional(RDO, "Revenue District Office of the registration", "050")
            .format("Three digits, optionally a letter"),
        new BulkColumn(
            VAT_REGISTERED, "Y when the company is VAT-registered", false, Type.YES_NO, "Y"),
        BulkColumn.optional(CAS_NO, "Permit of the computerised accounting system", ""),
        new BulkColumn(CAS_DATE, "Date of the CAS permit", false, Type.DATE, ""),
        BulkColumn.optional(EINVOICING_NO, "Permit of the electronic invoicing", ""),
        new BulkColumn(EINVOICING_DATE, "Date of the e-invoicing permit", false, Type.DATE, ""));
  }

  @Override
  public Set<String> optionalHeaders() {
    return TAX_REGISTRATION;
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(CODE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    String code = row.text(CODE);
    if (!CODE_FORMAT.matcher(code).matches()) {
      errors.add(error(CODE, "use up to 10 capital letters or digits"));
    }
    if (!TIN_FORMAT.matcher(row.text(TIN)).matches()
        && !db.exists(
            "select 1 from org_company where code = ? and tax_id = ?",
            row.text(CODE),
            row.text(TIN))) {
      errors.add(error(TIN, "write the TIN as 000-000-000-00000"));
    }
    if (!db.currency(row.text(CURRENCY))) {
      errors.add(error(CURRENCY, row.text(CURRENCY) + " is not an active currency"));
    }
    checkTaxRegistration(row, errors);
    whole(errors, row, MONTH, 1, MONTHS);
    whole(errors, row, BACK, 0, MAX_DAYS);
    whole(errors, row, FORWARD, 0, MAX_DAYS);
    Optional<Long> company = companyId(code);
    if (company.isPresent()
        && db.exists("select 1 from coa_account where company_id = ?", company.get())
        && !db.account(company.get(), row.text(RETAINED))) {
      errors.add(error(RETAINED, row.text(RETAINED) + " is not an account of " + code));
    }
    return errors;
  }

  private static void checkTaxRegistration(BulkRow row, List<String> errors) {
    String rdo = row.text(RDO);
    check(
        errors,
        rdo != null && !RDO_FORMAT.matcher(rdo).matches(),
        RDO,
        "three digits, optionally followed by a letter (e.g. 050 or 047A)");
    check(
        errors,
        row.text(CAS_DATE) != null && row.text(CAS_NO) == null,
        CAS_NO,
        "give the number of the CAS permit");
    check(
        errors,
        row.text(EINVOICING_DATE) != null && row.text(EINVOICING_NO) == null,
        EINVOICING_NO,
        "give the number of the e-invoicing permit");
  }

  static void whole(List<String> errors, BulkRow row, String header, int min, int max) {
    whole(errors, header, row.number(header), min, max);
  }

  private Optional<Long> companyId(String code) {
    return db.id("select id from org_company where code = ?", code);
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return db.action("org_company", columns("code", row.text(CODE)));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    db.upsert(
        "org_company",
        columns("code", row.text(CODE)),
        columns(
            "name", row.text(NAME),
            "tax_id", row.text(TIN),
            "address", row.text(ADDRESS),
            "base_currency", row.text(CURRENCY),
            "fiscal_year_start_month", row.number(MONTH),
            "back_value_days", row.number(BACK),
            "forward_value_days", row.number(FORWARD),
            "retained_earnings_account", row.text(RETAINED),
            "rdo_code", row.text(RDO),
            "vat_registered", row.text(VAT_REGISTERED) == null || row.yes(VAT_REGISTERED),
            "cas_permit_no", row.text(CAS_NO),
            "cas_permit_date", row.date(CAS_DATE),
            "einvoicing_permit_no", row.text(EINVOICING_NO),
            "einvoicing_permit_date", row.date(EINVOICING_DATE)),
        context,
        "Company");
    return row.text(CODE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select code, name, tax_id, address, base_currency, fiscal_year_start_month,"
                + " back_value_days, forward_value_days, retained_earnings_account, rdo_code,"
                + " vat_registered, cas_permit_no, cas_permit_date, einvoicing_permit_no,"
                + " einvoicing_permit_date from org_company order by code")
        .stream()
        .map(
            r ->
                exportRow(
                    CODE, r.get("code"),
                    NAME, r.get("name"),
                    TIN, r.get("tax_id"),
                    ADDRESS, r.get("address"),
                    CURRENCY, r.get("base_currency"),
                    MONTH, r.get("fiscal_year_start_month"),
                    BACK, r.get("back_value_days"),
                    FORWARD, r.get("forward_value_days"),
                    RETAINED, r.get("retained_earnings_account"),
                    RDO, r.get("rdo_code"),
                    VAT_REGISTERED, r.get("vat_registered"),
                    CAS_NO, r.get("cas_permit_no"),
                    CAS_DATE, r.get("cas_permit_date"),
                    EINVOICING_NO, r.get("einvoicing_permit_no"),
                    EINVOICING_DATE, r.get("einvoicing_permit_date")))
        .toList();
  }
}
