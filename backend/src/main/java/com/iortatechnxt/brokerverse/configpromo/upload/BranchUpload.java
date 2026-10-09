package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.util.EmailAddresses;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * D0-02 Branches: the branches and invoicing branches of the company, on Branches (Setup &
 * Administration); the legacy branch codes go into the code map BRANCH of the Migration Console.
 */
@Component
public class BranchUpload extends ConfigUploadHandler {

  static final String CODE = "Branch code";
  static final String NAME = "Branch name";
  static final String REGION = "Region";
  static final String ADDRESS = "Address";
  static final String OPENED = "Opening date";
  static final String HEAD_OFFICE = "Head office";
  static final String FOREX = "Foreign currency authorised";
  static final String PHONE = "Contact phone";
  static final String EMAIL = "Contact e-mail";
  static final String MANAGER = "Branch manager";
  static final String REST_DAYS = "Weekly rest days";
  static final String LEGACY = "Legacy branch codes";
  static final String BIR_CODE = "BIR branch code";
  static final String RDO = "RDO code";

  /** The BIR registration columns added after the workbook tab. */
  private static final Set<String> TAX_REGISTRATION = Set.of(BIR_CODE, RDO);

  private static final Pattern BIR_CODE_FORMAT = Pattern.compile("\\d{3,5}");

  private static final int DAY_ABBREVIATION = 3;
  private static final Pattern CODE_FORMAT = Pattern.compile("[A-Z0-9]{1,10}");
  private static final String TABLE = "org_branch";
  private static final String CODE_MAP = "BRANCH";
  private static final List<String> DAYS = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

  private final LegacyCodeMaps codeMaps;

  /**
   * Creates the handler.
   *
   * @param db database support
   * @param codeMaps code maps of the Migration Console
   */
  public BranchUpload(UploadSupport db, LegacyCodeMaps codeMaps) {
    super(db);
    this.codeMaps = codeMaps;
  }

  @Override
  public String code() {
    return "CFG_BRANCH";
  }

  @Override
  public String templateId() {
    return "D0-02";
  }

  @Override
  public String title() {
    return "Branches";
  }

  @Override
  public String screen() {
    return "Setup & Administration > Branches";
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
        BulkColumn.required(CODE, "Code of the branch; unique", "HO").format("Up to 10 capitals"),
        BulkColumn.required(NAME, "Name shown on screens and documents", "Head Office"),
        BulkColumn.optional(REGION, "Region of the branch", "NCR"),
        BulkColumn.optional(ADDRESS, "Address printed on documents", "Makati City"),
        new BulkColumn(OPENED, "Date the branch opened", true, Type.DATE, "2010-01-01"),
        new BulkColumn(HEAD_OFFICE, "Y for one branch only", true, Type.YES_NO, "Y"),
        new BulkColumn(FOREX, "May receive and pay in foreign currency", true, Type.YES_NO, "Y"),
        BulkColumn.optional(PHONE, "Branch telephone", "(02) 8888-0000"),
        BulkColumn.optional(EMAIL, "Branch mailbox", "ho@company.example.ph"),
        BulkColumn.optional(MANAGER, "Head of the branch", ""),
        BulkColumn.optional(REST_DAYS, "Days the branch is closed each week", "SAT,SUN")
            .format("SAT, SUN"),
        BulkColumn.optional(LEGACY, "Legacy branch codes for the code map BRANCH", "001")
            .format("Codes separated by commas"),
        BulkColumn.optional(BIR_CODE, "Branch code of the BIR registration", "00000")
            .format("3 to 5 digits"),
        BulkColumn.optional(RDO, "Revenue District Office of the branch", "050")
            .format("Three digits, optionally a letter"));
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
    if (!CODE_FORMAT.matcher(row.text(CODE)).matches()) {
      errors.add(error(CODE, "use up to 10 capital letters or digits"));
    }
    String birCode = row.text(BIR_CODE);
    check(
        errors,
        birCode != null && !BIR_CODE_FORMAT.matcher(birCode).matches(),
        BIR_CODE,
        "3 to 5 digits");
    String rdo = row.text(RDO);
    check(
        errors,
        rdo != null && !CompanyUpload.RDO_FORMAT.matcher(rdo).matches(),
        RDO,
        "three digits, optionally followed by a letter (e.g. 050 or 047A)");
    if (row.text(EMAIL) != null && !EmailAddresses.isValid(row.text(EMAIL))) {
      errors.add(error(EMAIL, row.text(EMAIL) + " is not an e-mail address"));
    }
    if (row.text(REST_DAYS) != null && restDays(row.text(REST_DAYS)).isEmpty()) {
      errors.add(error(REST_DAYS, "write the days as MON, TUE, WED, THU, FRI, SAT, SUN"));
    }
    if (yes(row, HEAD_OFFICE)) {
      Optional<String> other =
          db.text(
              "select code from org_branch where company_id = ? and head_office and code <> ?",
              context.companyId(),
              row.text(CODE));
      other.ifPresent(o -> errors.add(error(HEAD_OFFICE, o + " is already the head office")));
    }
    if (!LegacyCodeMaps.codes(row.text(LEGACY)).isEmpty()) {
      codeMaps.blocked(CODE_MAP).ifPresent(m -> errors.add(error(LEGACY, m)));
    }
    return errors;
  }

  /** "SAT, SUN" as the ISO day numbers kept by the branch ("6,7"); empty when not readable. */
  static Optional<String> restDays(String text) {
    List<Integer> days = new ArrayList<>();
    for (String part : text.toUpperCase(Locale.ROOT).split("[,;\\s]+")) {
      int index =
          DAYS.indexOf(
              part.length() > DAY_ABBREVIATION ? part.substring(0, DAY_ABBREVIATION) : part);
      if (index < 0) {
        return Optional.empty();
      }
      days.add(index + 1);
    }
    return Optional.of(
        days.stream().sorted().map(String::valueOf).collect(Collectors.joining(",")));
  }

  private static String dayNames(Object stored) {
    if (stored == null || stored.toString().isBlank()) {
      return null;
    }
    return Arrays.stream(stored.toString().split(","))
        .map(d -> DayOfWeek.of(Integer.parseInt(d.strip())).name().substring(0, DAY_ABBREVIATION))
        .collect(Collectors.joining(","));
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return db.action(TABLE, key(row, context));
  }

  private static Map<String, Object> key(BulkRow row, BulkContext context) {
    return columns("company_id", context.companyId(), "code", row.text(CODE));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    db.upsert(
        TABLE,
        key(row, context),
        columns(
            "name",
            row.text(NAME),
            "region",
            row.text(REGION),
            "address",
            row.text(ADDRESS),
            "opening_date",
            row.date(OPENED),
            "head_office",
            yes(row, HEAD_OFFICE),
            "forex_authorized",
            yes(row, FOREX),
            "contact_phone",
            row.text(PHONE),
            "contact_email",
            row.text(EMAIL),
            "manager_name",
            row.text(MANAGER),
            "weekly_holidays",
            row.text(REST_DAYS) == null ? null : restDays(row.text(REST_DAYS)).orElse(null),
            "bir_branch_code",
            row.text(BIR_CODE),
            "rdo_code",
            row.text(RDO)),
        context,
        "Branch");
    codeMaps.map(
        context.companyId(),
        CODE_MAP,
        LegacyCodeMaps.codes(row.text(LEGACY)),
        row.text(CODE),
        row.text(NAME),
        context.jobNo());
    return row.text(CODE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select code, name, region, address, opening_date, head_office, forex_authorized,"
                + " contact_phone, contact_email, manager_name, weekly_holidays, bir_branch_code, rdo_code"
                + " from org_branch where company_id = ? order by code",
            companyId)
        .stream()
        .map(
            r ->
                exportRow(
                    CODE, r.get("code"),
                    NAME, r.get("name"),
                    REGION, r.get("region"),
                    ADDRESS, r.get("address"),
                    OPENED, r.get("opening_date"),
                    HEAD_OFFICE, r.get("head_office"),
                    FOREX, r.get("forex_authorized"),
                    PHONE, r.get("contact_phone"),
                    EMAIL, r.get("contact_email"),
                    MANAGER, r.get("manager_name"),
                    REST_DAYS, dayNames(r.get("weekly_holidays")),
                    BIR_CODE, r.get("bir_branch_code"),
                    RDO, r.get("rdo_code")))
        .toList();
  }
}
