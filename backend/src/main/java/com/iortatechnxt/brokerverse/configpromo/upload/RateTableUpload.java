package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PM-04 Rate tables, on Rates &amp; Taxes: the premium charges and rating factors (TAX), the short
 * period table and the compulsory motor limits, effective-dated. A row with the same table, code
 * and effective date updates that rate.
 */
@Component
public class RateTableUpload extends ConfigUploadHandler {
  private static final String EFFECTIVE_TO = "effective_to";
  private static final String EFFECTIVE_FROM = "effective_from";

  static final String TABLE_COLUMN = "Table";
  static final String RATE = "Rate";
  static final String LINE = "Line code";
  static final String PERCENT = "Rate %";
  static final String MONTHS = "Months covered";
  static final String LIMIT = "Coverage / Limit / Premium";
  static final String EFFECTIVE = "Effective from / to";

  static final String TAX = "TAX";
  static final String SHORT_PERIOD = "SHORT_PERIOD";
  static final String MOTOR_LIMIT = "MOTOR_LIMIT";

  private static final List<String> TABLES = List.of(TAX, SHORT_PERIOD, MOTOR_LIMIT);
  private static final List<String> RATE_CODES =
      List.of(
          "DST",
          "PREMIUM_TAX",
          "VAT_PREMIUM",
          "FIRE_SERVICE_TAX",
          "VAT_COMMISSION",
          "MOTOR_OD_ANNUAL",
          "MOTOR_OD_MULTI_YEAR");
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int LIMIT_PARTS = 3;
  private static final int MAX_DECIMALS = 4;
  private static final int MAX_MONTHS = 12;

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public RateTableUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_RATE_TABLE";
  }

  @Override
  public String templateId() {
    return "PM-04";
  }

  @Override
  public String title() {
    return "Rate tables";
  }

  @Override
  public String screen() {
    return "Product Maintenance > Rates & Taxes";
  }

  @Override
  public String permission() {
    return "PRODUCT_MAINTAIN";
  }

  @Override
  public String approvePermission() {
    return "PRODUCT_AUTHORIZE";
  }

  @Override
  public String filledBy() {
    return "Product Owner, Product Maintenance; Head, Comptrollership (Tax)";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(TABLE_COLUMN, "Rate table of the row", TAX)
            .codes(TABLES.toArray(String[]::new)),
        BulkColumn.optional(RATE, "Premium charge or rating factor", "DST")
            .when(TAX)
            .codes(RATE_CODES.toArray(String[]::new)),
        BulkColumn.optional(LINE, "Line of the rate; blank = every line", "").master("line"),
        BulkColumn.optional(PERCENT, "Up to four decimals, 0 to 100", "12.5")
            .when("TAX, SHORT_PERIOD"),
        BulkColumn.optional(MONTHS, "Months covered, 1 to 12", "").when(SHORT_PERIOD),
        BulkColumn.optional(LIMIT, "BI or PD; limit; premium", "")
            .when(MOTOR_LIMIT)
            .format("BI; 100000.00; 350.00"),
        BulkColumn.required(
                EFFECTIVE, "First day; last day after a semicolon, blank = open", "01-Jan-2028")
            .format("dd-MMM-yyyy; dd-MMM-yyyy"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    String from = UploadCells.period(row.text(EFFECTIVE)).map(d -> d.from().toString()).orElse("");
    return String.join(
        "|",
        String.valueOf(row.text(TABLE_COLUMN)),
        String.valueOf(rateCode(row)),
        String.valueOf(row.text(LINE)),
        String.valueOf(row.text(MONTHS)),
        String.valueOf(UploadCells.parts(row.text(LIMIT), LIMIT_PARTS).subList(0, 2)),
        from);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    String table = row.text(TABLE_COLUMN);
    oneOf(errors, TABLE_COLUMN, table, TABLES);
    check(
        errors,
        UploadCells.period(row.text(EFFECTIVE)).isEmpty(),
        EFFECTIVE,
        "give the first day as dd-MMM-yyyy and, after a semicolon, the last day");
    String line = row.text(LINE);
    check(
        errors,
        line != null && !db.exists("select 1 from cat_product_line where code = ?", line),
        LINE,
        line + " is not a line");
    if (TAX.equals(table)) {
      check(
          errors,
          !RATE_CODES.contains(rateCode(row)),
          RATE,
          "use one of " + String.join(", ", RATE_CODES));
      percent(row, errors);
    } else if (SHORT_PERIOD.equals(table)) {
      percent(row, errors);
      BigDecimal months = UploadCells.number(row.text(MONTHS));
      check(errors, months == null, MONTHS, "enter the months covered");
      whole(errors, MONTHS, months, 1, MAX_MONTHS);
    } else if (MOTOR_LIMIT.equals(table)) {
      check(
          errors,
          motor(row).isEmpty(),
          LIMIT,
          "give BI or PD, the limit and the premium separated by semicolons");
    }
    return errors;
  }

  /** A compulsory motor limit: coverage, limit and premium. */
  private record MotorLimit(String coverage, BigDecimal limit, BigDecimal premium) {}

  private static java.util.Optional<MotorLimit> motor(BulkRow row) {
    List<String> parts = UploadCells.parts(row.text(LIMIT), LIMIT_PARTS);
    String coverage = parts.get(0) == null ? "" : parts.get(0).toUpperCase(Locale.ROOT);
    BigDecimal limit = UploadCells.number(parts.get(1));
    BigDecimal premium = UploadCells.number(parts.get(2));
    boolean valid =
        List.of("BI", "PD").contains(coverage)
            && limit != null
            && limit.signum() > 0
            && premium != null
            && premium.signum() >= 0;
    return valid
        ? java.util.Optional.of(new MotorLimit(coverage, limit, premium))
        : java.util.Optional.empty();
  }

  private static void percent(BulkRow row, List<String> errors) {
    BigDecimal rate = UploadCells.number(row.text(PERCENT));
    check(
        errors,
        rate == null
            || rate.signum() < 0
            || rate.compareTo(HUNDRED) > 0
            || rate.scale() > MAX_DECIMALS,
        PERCENT,
        "enter a rate from 0 to 100 with up to four decimals");
  }

  private static String rateCode(BulkRow row) {
    String rate = row.text(RATE);
    return rate == null ? null : rate.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
  }

  private static String table(BulkRow row) {
    return switch (row.text(TABLE_COLUMN)) {
      case TAX -> "cat_rate";
      case SHORT_PERIOD -> "cat_short_period_rate";
      default -> "cat_motor_limit";
    };
  }

  private static Map<String, Object> key(BulkRow row) {
    LocalDate from = UploadCells.periodOf(row.text(EFFECTIVE)).from();
    return switch (row.text(TABLE_COLUMN)) {
      case TAX ->
          columns("rate_code", rateCode(row), "line_code", row.text(LINE), EFFECTIVE_FROM, from);
      case SHORT_PERIOD ->
          columns(
              "months_covered",
              UploadCells.number(row.text(MONTHS)).intValue(),
              EFFECTIVE_FROM,
              from);
      default -> {
        MotorLimit m = motor(row).orElseThrow();
        yield columns("coverage", m.coverage(), "limit_amount", m.limit(), EFFECTIVE_FROM, from);
      }
    };
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return db.action(table(row), key(row));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    LocalDate to = UploadCells.periodOf(row.text(EFFECTIVE)).to();
    Map<String, Object> values =
        switch (row.text(TABLE_COLUMN)) {
          case TAX -> columns("rate", UploadCells.number(row.text(PERCENT)), EFFECTIVE_TO, to);
          case SHORT_PERIOD ->
              columns("percent_of_annual", UploadCells.number(row.text(PERCENT)), EFFECTIVE_TO, to);
          default -> columns("premium", motor(row).orElseThrow().premium(), EFFECTIVE_TO, to);
        };
    db.upsert(table(row), key(row), values, context, "Rate table " + label(row.text(TABLE_COLUMN)));
    return row.text(TABLE_COLUMN) + " " + key(row).values();
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    List<Map<String, String>> rows = new ArrayList<>();
    for (Map<String, Object> r :
        db.rows(
            "select rate_code, line_code, rate, effective_from, effective_to from cat_rate"
                + " where record_status = 'ACTIVE' order by rate_code, line_code nulls first, effective_from")) {
      rows.add(
          exportRow(
              TABLE_COLUMN, TAX,
              RATE, r.get("rate_code"),
              LINE, r.get("line_code"),
              PERCENT, r.get("rate"),
              EFFECTIVE, UploadCells.range(r.get(EFFECTIVE_FROM), r.get(EFFECTIVE_TO))));
    }
    for (Map<String, Object> r :
        db.rows(
            "select months_covered, percent_of_annual, effective_from, effective_to from cat_short_period_rate"
                + " where record_status = 'ACTIVE' order by effective_from, months_covered")) {
      rows.add(
          exportRow(
              TABLE_COLUMN, SHORT_PERIOD,
              PERCENT, r.get("percent_of_annual"),
              MONTHS, r.get("months_covered"),
              EFFECTIVE, UploadCells.range(r.get(EFFECTIVE_FROM), r.get(EFFECTIVE_TO))));
    }
    for (Map<String, Object> r :
        db.rows(
            "select coverage, limit_amount, premium, effective_from, effective_to from cat_motor_limit"
                + " where record_status = 'ACTIVE' order by effective_from, coverage, limit_amount")) {
      rows.add(
          exportRow(
              TABLE_COLUMN, MOTOR_LIMIT,
              LIMIT, UploadCells.join(r.get("coverage"), r.get("limit_amount"), r.get("premium")),
              EFFECTIVE, UploadCells.range(r.get(EFFECTIVE_FROM), r.get(EFFECTIVE_TO))));
    }
    return rows;
  }
}
