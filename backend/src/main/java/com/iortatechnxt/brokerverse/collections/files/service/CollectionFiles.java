package com.iortatechnxt.brokerverse.collections.files.service;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import com.iortatechnxt.brokerverse.collections.common.service.ClxSettings;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile.Frequency;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile.Spec;
import com.iortatechnxt.brokerverse.collections.files.service.FilePeriods.Period;
import com.iortatechnxt.brokerverse.collections.report.ClxReportSql;
import com.iortatechnxt.brokerverse.collections.report.ItemReports;
import com.iortatechnxt.brokerverse.collections.report.ReversalReports;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistFilter.Scope;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistQueryService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * The Collections files (BRCLXN.024-029, 045) and the worklist export (caveat p.93):
 *
 * <ul>
 *   <li>daily - Outstanding PR List and Full Production Report (month to date), at once;
 *   <li>weekly - DP PR and PR 2307 for reversal of the Saturday-Friday week, one file per sales
 *       unit and invoicing branch with tagged accounts, available the next Monday 08:00;
 *   <li>monthly - DP PR and PR 2307 for reversal of the previous month, generated on the first
 *       working day of the head office calendar and available from 08:00;
 *   <li>export - the Outstanding PR List of a segment and unit on request, refused above {@code
 *       CLX_EXPORT_MAX_ROWS}, to download from the Files screen.
 * </ul>
 */
@Service
public class CollectionFiles {

  private static final String TAGGED_SCOPES =
      "select distinct i.sales_unit, i.branch_id, b.code as branch_code"
          + " from clx_disposition d join clx_item i on i.id = d.item_id"
          + " left join org_branch b on b.id = i.branch_id"
          + " where d.company_id = :companyId and d.ops_action = :action"
          + " and cast(d.created_at at time zone 'Asia/Manila' as date) between :from and :to"
          + " order by 1, 3";
  private static final DateTimeFormatter EXPORT_KEY = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final Map<String, String> REVERSALS =
      Map.of(ReversalReports.DP, "DP_REVERSAL", ReversalReports.PR2307, "CWT2307_REVERSAL");

  private final ScheduledFileService files;
  private final WorklistQueryService worklist;
  private final OrganizationService organization;
  private final NamedParameterJdbcTemplate jdbc;
  private final ClxSettings settings;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param files publication
   * @param worklist worklist (export size)
   * @param organization calendar
   * @param jdbc scopes of the weekly files
   * @param settings parameters
   * @param currentUser requestor of an export
   * @param clock clock
   */
  public CollectionFiles(
      ScheduledFileService files,
      WorklistQueryService worklist,
      OrganizationService organization,
      NamedParameterJdbcTemplate jdbc,
      ClxSettings settings,
      CurrentUser currentUser,
      Clock clock) {
    this.files = files;
    this.worklist = worklist;
    this.organization = organization;
    this.jdbc = jdbc;
    this.settings = settings;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The daily files of a business date (BRCLXN.045).
   *
   * @param companyId company
   * @param day business date
   * @return files
   */
  public List<ScheduledFile> daily(Long companyId, LocalDate day) {
    Period period = new Period(day.toString(), day.withDayOfMonth(1), day);
    List<ScheduledFile> out = new ArrayList<>();
    out.add(
        files.publish(
            companyId,
            spec(Frequency.DAILY, ItemReports.OUTSTANDING_PR, period, null),
            params(companyId, null, null),
            null));
    out.add(
        files.publish(
            companyId,
            spec(Frequency.DAILY, ItemReports.FULL_PRODUCTION, period, null),
            params(companyId, period, null),
            null));
    return out;
  }

  /**
   * The weekly files of the week ending on the last Friday (BRCLXN.028/029).
   *
   * @param companyId company
   * @param day business date (normally the Friday)
   * @return files
   */
  public List<ScheduledFile> weekly(Long companyId, LocalDate day) {
    Period week = FilePeriods.weekEnding(day);
    List<ScheduledFile> out = new ArrayList<>();
    REVERSALS.forEach(
        (code, action) -> {
          for (Map<String, Object> scope : taggedScopes(companyId, action, week)) {
            String unit = (String) scope.get("sales_unit");
            Object branch = scope.get("branch_id");
            Map<String, String> p = params(companyId, week, unit);
            if (branch != null) {
              p.put(ClxReportSql.BRANCH, branch.toString());
            }
            String label = "Unit " + orNone(unit) + " / Branch " + orNone(scope.get("branch_code"));
            out.add(
                files.publish(
                    companyId,
                    spec(Frequency.WEEKLY, code, week, label),
                    p,
                    FilePeriods.mondayAfter(week)));
          }
        });
    return out;
  }

  /**
   * The monthly files of the previous month, on the first working day (BRCLXN.024-027).
   *
   * @param companyId company
   * @param day business date, Philippine time
   * @return files; empty on other days
   */
  public List<ScheduledFile> monthly(Long companyId, LocalDate day) {
    if (!FilePeriods.isFirstWorkingDay(day, workingDays(companyId))) {
      return List.of();
    }
    Period month = FilePeriods.previousMonth(day);
    List<ScheduledFile> out = new ArrayList<>();
    for (String code : List.of(ReversalReports.DP, ReversalReports.PR2307)) {
      out.add(
          files.publish(
              companyId,
              spec(Frequency.MONTHLY, code, month, null),
              params(companyId, month, null),
              FilePeriods.officeOpens(day)));
    }
    return out;
  }

  /**
   * Exports the Outstanding PR List of a segment and unit (caveat p.93).
   *
   * @param companyId company
   * @param segment segment, null for all
   * @param salesUnit unit, null for all
   * @return the file to download from the Files screen
   */
  public ScheduledFile export(Long companyId, String segment, String salesUnit) {
    WorklistFilter filter =
        WorklistFilter.of(ItemStatus.OPEN)
            .with(new Scope(blank(segment), blank(salesUnit), null, null, null));
    long rows = worklist.count(companyId, filter);
    int cap = settings.exportMaxRows();
    if (rows > cap) {
      throw new BusinessRuleException(
          "CLX_EXPORT_TOO_LARGE",
          rows + " accounts exceed the export limit of " + cap + "; narrow the filters");
    }
    LocalDate today = LocalDate.now(clock);
    String key = "E" + clock.instant().atZone(FilePeriods.MANILA).format(EXPORT_KEY);
    Map<String, String> p = params(companyId, null, blank(salesUnit));
    if (blank(segment) != null) {
      p.put(ClxReportSql.SEGMENT, segment.strip());
    }
    String scope =
        currentUser.username() + " " + orNone(blank(segment)) + " / " + orNone(blank(salesUnit));
    return files.publish(
        companyId,
        spec(
            Frequency.ON_REQUEST, ItemReports.OUTSTANDING_PR, new Period(key, today, today), scope),
        p,
        null);
  }

  private List<Map<String, Object>> taggedScopes(Long companyId, String action, Period week) {
    Map<String, Object> args = new LinkedHashMap<>();
    args.put(ClxReportSql.COMPANY, companyId);
    args.put("action", action);
    args.put(ClxReportSql.FROM, week.from());
    args.put(ClxReportSql.TO, week.to());
    return jdbc.queryForList(TAGGED_SCOPES, args);
  }

  private Predicate<LocalDate> workingDays(Long companyId) {
    Optional<Branch> head =
        organization.listBranches(companyId).stream().filter(Branch::isHeadOffice).findFirst();
    return head.<Predicate<LocalDate>>map(b -> d -> organization.isWorkingDay(b, d))
        .orElse(
            d -> d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY);
  }

  private static Spec spec(Frequency frequency, String code, Period period, String scope) {
    return new Spec(frequency, code, period.key(), period.from(), period.to(), scope);
  }

  private static Map<String, String> params(Long companyId, Period period, String unit) {
    Map<String, String> p = new LinkedHashMap<>();
    p.put(ClxReportSql.COMPANY, companyId.toString());
    if (period != null) {
      p.put(ClxReportSql.FROM, period.from().toString());
      p.put(ClxReportSql.TO, period.to().toString());
    }
    if (unit != null) {
      p.put(ClxReportSql.UNIT, unit);
    }
    return p;
  }

  private static String blank(String v) {
    return v == null || v.isBlank() ? null : v.strip();
  }

  private static String orNone(Object v) {
    return v == null ? "(none)" : v.toString();
  }
}
