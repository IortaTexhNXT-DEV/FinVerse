package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * TX-01 Party tax profiles: the registered TIN, payee class, registered name and address, VAT
 * treatment, default ATC and tax status of clients, insurers, intermediaries and payees, on Party
 * Tax Profiles. The rules are those of the screen.
 */
@Component
public class PartyTaxProfileUpload extends ConfigUploadHandler {

  static final String PARTY = "Party code";
  static final String TIN = "TIN";
  static final String BRANCH = "Branch code";
  static final String PAYEE = "Payee class";
  static final String NAME = "Registered name";
  static final String LAST = "Last name";
  static final String FIRST = "First name";
  static final String MIDDLE = "Middle name";
  static final String ADDRESS = "Registered address";
  static final String ZIP = "ZIP code";
  static final String VAT = "VAT treatment";
  static final String ATC = "Default ATC";
  static final String AGENT = "Withholding agent";
  static final String TOP = "Top withholding agent";
  static final String GOVERNMENT = "Government payor";
  static final String CERTIFICATE = "Tax exemption certificate no.";
  static final String VALID_FROM = "Certificate valid from";
  static final String VALID_TO = "Certificate valid to";

  private static final String TABLE = "tax_party_profile";
  private static final String PAYEE_COLUMN = "payee_class";
  private static final String INDIVIDUAL = "INDIVIDUAL";
  private static final String HEAD_OFFICE = "000";
  private static final List<String> PAYEE_CLASSES = List.of(INDIVIDUAL, "CORPORATE");
  private static final List<String> VAT_TREATMENTS = List.of("REGULAR", "ZERO_RATED", "EXEMPT");
  private static final Pattern TIN_FORMAT = Pattern.compile("[0-9-]{9,20}");
  private static final Pattern BRANCH_FORMAT = Pattern.compile("[0-9]{3,5}");
  private static final int TIN_DIGITS = 9;
  private static final int MAX_TIN_DIGITS = 14;
  private static final String NO_TIN = "000000000";

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public PartyTaxProfileUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_PARTY_TAX_PROFILE";
  }

  @Override
  public String templateId() {
    return "TX-01";
  }

  @Override
  public String title() {
    return "Party tax profiles";
  }

  @Override
  public String screen() {
    return "Tax & Statutory > Party Tax Profiles";
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
        BulkColumn.required(PARTY, "Code of the client, insurer, partner or payee", "C-000101")
            .master("client, insurer, business partner or payee"),
        BulkColumn.required(TIN, "Taxpayer identification number as registered", "123-456-789")
            .format("Digits and dashes, up to 20"),
        BulkColumn.optional(BRANCH, "TIN branch code (000 or 00000 for the head office)", "00000")
            .format("3 to 5 digits"),
        BulkColumn.required(PAYEE, "Individual or corporate", "CORPORATE")
            .codes(PAYEE_CLASSES.toArray(String[]::new)),
        BulkColumn.required(
            NAME, "Name as registered with the BIR", "Juan Dela Cruz Trading Corp."),
        BulkColumn.optional(LAST, "Last name of an individual", "").when("individuals"),
        BulkColumn.optional(FIRST, "First name of an individual", "").when("individuals"),
        BulkColumn.optional(MIDDLE, "Middle name of an individual", ""),
        BulkColumn.optional(ADDRESS, "Address as registered with the BIR", "Makati City"),
        BulkColumn.optional(ZIP, "ZIP code of the registered address", "1226"),
        BulkColumn.optional(
                VAT, "How a premium without VAT is classified; blank = REGULAR", "REGULAR")
            .codes(VAT_TREATMENTS.toArray(String[]::new)),
        BulkColumn.optional(ATC, "Withholding tax code applied by default", "")
            .master("withholding tax code of D0-10 Tax codes"),
        new BulkColumn(
            AGENT, "Y when the party withholds tax on its payments", false, Type.YES_NO, "Y"),
        new BulkColumn(TOP, "Y for a top withholding agent", false, Type.YES_NO, "N"),
        new BulkColumn(GOVERNMENT, "Y for a government payor", false, Type.YES_NO, "N"),
        BulkColumn.optional(CERTIFICATE, "Number of the tax exemption certificate", ""),
        new BulkColumn(VALID_FROM, "First day of the certificate", false, Type.DATE, "")
            .when("certificate number given"),
        new BulkColumn(VALID_TO, "Last day of the certificate", false, Type.DATE, "")
            .when("certificate number given"));
  }

  @Override
  public List<String> rules() {
    return List.of(
        "The layout is the tab " + templateId() + " of the master data and configuration workbook.",
        "A row for a party that already has a profile updates it; uploading the same file again"
            + " changes nothing.",
        "A top withholding agent or a government payor is always a withholding agent.",
        "The rows are applied only after a second user approves the upload.");
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(PARTY);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    if (party(row, context).isEmpty()) {
      errors.add(error(PARTY, row.text(PARTY) + " is not a party of the company"));
    }
    String digits = row.text(TIN).replaceAll("\\D", "");
    check(
        errors,
        !TIN_FORMAT.matcher(row.text(TIN)).matches()
            || digits.length() < TIN_DIGITS
            || digits.length() > MAX_TIN_DIGITS
            || NO_TIN.equals(digits.substring(0, Math.min(TIN_DIGITS, digits.length()))),
        TIN,
        "a TIN has 9 digits (plus a branch code), with or without dashes");
    String branch = row.text(BRANCH);
    check(
        errors,
        branch != null && !BRANCH_FORMAT.matcher(branch).matches(),
        BRANCH,
        "use 3 to 5 digits");
    oneOf(errors, PAYEE, row.text(PAYEE), PAYEE_CLASSES);
    oneOf(errors, VAT, row.text(VAT), VAT_TREATMENTS);
    boolean individual = INDIVIDUAL.equals(row.text(PAYEE));
    check(errors, individual && row.text(LAST) == null, LAST, "mandatory for an individual");
    check(errors, individual && row.text(FIRST) == null, FIRST, "mandatory for an individual");
    atc(errors, row, context);
    certificate(errors, row);
    return errors;
  }

  private void atc(List<String> errors, BulkRow row, BulkContext context) {
    String atc = row.text(ATC);
    if (atc == null) {
      return;
    }
    List<Map<String, Object>> code =
        db.rows(
            "select tax_type, payee_class from tax_code where company_id = ? and code = ?",
            context.companyId(),
            atc);
    if (code.isEmpty()) {
      errors.add(error(ATC, atc + " is not a tax code of the company"));
    } else if (!"EWT".equals(code.get(0).get("tax_type"))) {
      errors.add(error(ATC, atc + " is not a withholding tax code"));
    } else if (code.get(0).get(PAYEE_COLUMN) != null
        && !code.get(0).get(PAYEE_COLUMN).equals(row.text(PAYEE))) {
      errors.add(
          error(
              ATC,
              atc
                  + " applies to "
                  + label((String) code.get(0).get(PAYEE_COLUMN))
                  + " payees only"));
    }
  }

  private static void certificate(List<String> errors, BulkRow row) {
    if (row.text(CERTIFICATE) == null) {
      return;
    }
    LocalDate from = row.date(VALID_FROM);
    LocalDate to = row.date(VALID_TO);
    check(errors, from == null, VALID_FROM, "mandatory when a certificate number is given");
    check(errors, to == null, VALID_TO, "mandatory when a certificate number is given");
    check(
        errors,
        from != null && to != null && to.isBefore(from),
        VALID_TO,
        "is before the first day of the certificate");
  }

  private Optional<Long> party(BulkRow row, BulkContext context) {
    return db.id(
        "select id from pty_party where company_id = ? and code = ?",
        context.companyId(),
        row.text(PARTY));
  }

  private Map<String, Object> key(BulkRow row, BulkContext context) {
    return columns("company_id", context.companyId(), "party_id", party(row, context).orElse(null));
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return db.action(TABLE, key(row, context));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    String digits = row.text(TIN).replaceAll("\\D", "");
    String branch = branch(row, digits);
    boolean individual = INDIVIDUAL.equals(row.text(PAYEE));
    boolean certificate = row.text(CERTIFICATE) != null;
    boolean top = yes(row, TOP);
    boolean government = yes(row, GOVERNMENT);
    db.upsert(
        TABLE,
        key(row, context),
        columns(
            "party_code",
            row.text(PARTY),
            "tin",
            digits.substring(0, TIN_DIGITS),
            "branch_code",
            branch,
            PAYEE_COLUMN,
            row.text(PAYEE),
            "registered_name",
            row.text(NAME),
            "last_name",
            individual ? row.text(LAST) : null,
            "first_name",
            individual ? row.text(FIRST) : null,
            "middle_name",
            individual ? row.text(MIDDLE) : null,
            "registered_address",
            row.text(ADDRESS),
            "zip_code",
            row.text(ZIP),
            "vat_treatment",
            Objects.requireNonNullElse(row.text(VAT), "REGULAR"),
            "default_atc_code",
            row.text(ATC),
            "withholding_agent",
            yes(row, AGENT) || top || government,
            "top_withholding_agent",
            top,
            "government_payor",
            government,
            "exemption_certificate_no",
            row.text(CERTIFICATE),
            "exemption_valid_from",
            certificate ? row.date(VALID_FROM) : null,
            "exemption_valid_to",
            certificate ? row.date(VALID_TO) : null),
        context,
        "PartyTaxProfile");
    return row.text(PARTY);
  }

  /** The branch code given, or the digits after the nine of the TIN, or the head office. */
  private static String branch(BulkRow row, String digits) {
    if (row.text(BRANCH) != null) {
      return row.text(BRANCH);
    }
    return digits.length() > TIN_DIGITS ? digits.substring(TIN_DIGITS) : HEAD_OFFICE;
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select p.code as party_code, t.tin, t.branch_code, t.payee_class, t.registered_name,"
                + " t.last_name, t.first_name, t.middle_name, t.registered_address, t.zip_code,"
                + " t.vat_treatment, t.default_atc_code, t.withholding_agent,"
                + " t.top_withholding_agent, t.government_payor, t.exemption_certificate_no,"
                + " t.exemption_valid_from, t.exemption_valid_to from tax_party_profile t"
                + " join pty_party p on p.id = t.party_id where t.company_id = ? order by p.code",
            companyId)
        .stream()
        .map(PartyTaxProfileUpload::export)
        .toList();
  }

  private static Map<String, String> export(Map<String, Object> r) {
    Map<String, String> row =
        exportRow(
            PARTY, r.get("party_code"),
            TIN, r.get("tin"),
            BRANCH, r.get("branch_code"),
            PAYEE, r.get(PAYEE_COLUMN),
            NAME, r.get("registered_name"),
            LAST, r.get("last_name"),
            FIRST, r.get("first_name"),
            MIDDLE, r.get("middle_name"),
            ADDRESS, r.get("registered_address"),
            ZIP, r.get("zip_code"),
            VAT, r.get("vat_treatment"),
            ATC, r.get("default_atc_code"));
    row.putAll(
        exportRow(
            AGENT, r.get("withholding_agent"),
            TOP, r.get("top_withholding_agent"),
            GOVERNMENT, r.get("government_payor"),
            CERTIFICATE, r.get("exemption_certificate_no"),
            VALID_FROM, r.get("exemption_valid_from"),
            VALID_TO, r.get("exemption_valid_to")));
    return row;
  }
}
