package com.iortatechnxt.brokerverse.renewal.budget.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalBudget;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Budget Upload (BDOI Renewal FRS FRRN.042.03): one row per budget record and month with the new,
 * renewal and organic budget; a row creates the record of its key or updates the month of an
 * existing record. Excel (.xlsx) and CSV files; the template downloads from the upload screen.
 */
@Component
public class RenewalBudgetBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_BUDGET";

  static final String YEAR = "Fiscal Year";
  static final String MEASURE = "Measure";
  static final String SEGMENT = "Market Segment";
  static final String REGION = "Region";
  static final String TEAM = "Team";
  static final String SUB_TEAM = "Sub-Team";
  static final String UNIT_HEAD = "Unit Head";
  static final String SECTION_HEAD = "Section Head";
  static final String TEAM_HEAD = "Team Head";
  static final String TEAM_LEAD = "Team Lead";
  static final String OFFICER = "Account Officer";
  static final String MONTH = "Month";
  static final String NEW = "New Budget";
  static final String RENEWAL = "Renewal Budget";
  static final String ORGANIC = "Organic Budget";

  private final RenewalBudgetService budgets;
  private final RenewalBudgetValidator validator;

  /**
   * Creates the handler.
   *
   * @param budgets budget service
   * @param validator validations of a record
   */
  public RenewalBudgetBulkHandler(RenewalBudgetService budgets, RenewalBudgetValidator validator) {
    this.budgets = budgets;
    this.validator = validator;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - Annual Renewal Budget";
  }

  @Override
  public String permission() {
    return Permission.RNW_BUDGET.name();
  }

  @Override
  public String filledBy() {
    return "The Business Administrator, from the approved annual budget of the renewal units";
  }

  @Override
  public String uploadPath() {
    return "Renewal > Renewal Annual Budget, button Upload Budget";
  }

  @Override
  public List<String> rules() {
    return List.of(
        "One row per budget record and month; the other months of the record are unchanged.",
        "Region is required for the segments other than Corporate; Team and Sub-Team for Corporate.",
        "The Account Officer is not required for CBG.",
        "Budget values cannot be negative.");
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        new BulkColumn(YEAR, "Fiscal year of the budget", true, BulkColumn.Type.NUMBER, "2027"),
        BulkColumn.optional(MEASURE, "PREMIUM (basic premium, default) or COMMISSION", "PREMIUM"),
        BulkColumn.required(SEGMENT, "Market segment code", "RETAIL"),
        BulkColumn.optional(REGION, "Region (not for Corporate)", "NCR"),
        BulkColumn.optional(TEAM, "Team (Corporate only)", ""),
        BulkColumn.optional(SUB_TEAM, "Sub-team (Corporate only)", ""),
        BulkColumn.optional(UNIT_HEAD, "User name of the Unit Head", "uhead"),
        BulkColumn.optional(SECTION_HEAD, "User name of the Section Head", ""),
        BulkColumn.optional(TEAM_HEAD, "User name of the Team Head", ""),
        BulkColumn.optional(TEAM_LEAD, "User name of the Team Lead", "mkttl"),
        BulkColumn.optional(OFFICER, "User name of the Account Officer (not for CBG)", "ao"),
        new BulkColumn(MONTH, "Month 1 to 12", true, BulkColumn.Type.NUMBER, "1"),
        new BulkColumn(NEW, "Monthly new budget", false, BulkColumn.Type.NUMBER, "250000.00"),
        new BulkColumn(
            RENEWAL, "Monthly renewal budget", false, BulkColumn.Type.NUMBER, "800000.00"),
        new BulkColumn(
            ORGANIC, "Monthly organic budget", false, BulkColumn.Type.NUMBER, "120000.00"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return input(row).key().text() + "|" + row.text(MONTH);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    BigDecimal month = row.number(MONTH);
    if (month == null || month.intValue() < 1 || month.intValue() > RenewalBudget.MONTHS) {
      errors.add("Month must be 1 to 12");
      return errors;
    }
    errors.addAll(validator.validate(input(row)));
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    RenewalBudget saved = budgets.save(context.companyId(), input(row));
    return saved.getFiscalYear() + " " + saved.getSegment() + " month " + row.text(MONTH);
  }

  private static RenewalBudgetService.BudgetInput input(BulkRow row) {
    BigDecimal year = row.number(YEAR);
    BigDecimal month = row.number(MONTH);
    String measure = row.text(MEASURE);
    RenewalBudget.Key key =
        new RenewalBudget.Key(
            year == null ? 0 : year.intValue(),
            measure == null ? RenewalBudgetService.PREMIUM : measure.toUpperCase(Locale.ROOT),
            row.text(SEGMENT),
            row.text(REGION),
            row.text(TEAM),
            row.text(SUB_TEAM),
            row.text(OFFICER));
    RenewalBudget.Heads heads =
        new RenewalBudget.Heads(
            row.text(UNIT_HEAD), row.text(SECTION_HEAD), row.text(TEAM_HEAD), row.text(TEAM_LEAD));
    List<RenewalBudget.Month> months =
        month == null
            ? List.of()
            : List.of(
                new RenewalBudget.Month(
                    month.intValue(),
                    amount(row, NEW),
                    amount(row, RENEWAL),
                    amount(row, ORGANIC)));
    return new RenewalBudgetService.BudgetInput(key, heads, months);
  }

  private static BigDecimal amount(BulkRow row, String header) {
    BigDecimal v = row.number(header);
    return v == null ? BigDecimal.ZERO : v;
  }
}
