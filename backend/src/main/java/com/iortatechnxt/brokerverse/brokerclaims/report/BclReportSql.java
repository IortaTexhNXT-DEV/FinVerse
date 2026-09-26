package com.iortatechnxt.brokerverse.brokerclaims.report;

import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimAgeing;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared parameters, SQL and row handling of the Claims Handling reports (CLAIMS_BROKING_DESIGN 10,
 * BRCLM.026-034/038/040-043): every report is for the company of the header, with the common
 * filters handling branch, claims unit, handler, insurer, product line, Marketing team and account
 * officer; ages are computed as of the report date; SQL dates become local dates. File names follow
 * {@code <Report>_<date of extraction>} (report engine).
 */
@Component
@Transactional(readOnly = true)
public class BclReportSql {

  /** Company. */
  public static final String COMPANY = "companyId";

  /** As-of date. */
  public static final String AS_OF = "asOf";

  /** Handling branch. */
  public static final String BRANCH = "branchId";

  /** Claims unit. */
  public static final String UNIT = "unitCode";

  /** Claim handler. */
  public static final String HANDLER = "handler";

  /** Insurer. */
  public static final String INSURER = "insurerCode";

  /** Product line. */
  public static final String LINE = "lineCode";

  /** Marketing team. */
  public static final String TEAM = "salesTeam";

  /** Account officer. */
  public static final String OFFICER = "accountOfficer";

  /** Common filters on a claim aliased {@code c}. */
  static final String FILTERS =
      " and (cast(:branchId as bigint) is null or c.branch_id = :branchId)"
          + " and (cast(:unitCode as varchar) is null or c.unit_code = :unitCode)"
          + " and (cast(:handler as varchar) is null or lower(c.handler) = lower(:handler))"
          + " and (cast(:insurerCode as varchar) is null or c.lead_insurer_code = :insurerCode"
          + " or exists (select 1 from bcl_insurer_claim f where f.claim_id = c.id"
          + " and f.insurer_code = :insurerCode))"
          + " and (cast(:lineCode as varchar) is null or c.line_code = :lineCode)"
          + " and (cast(:salesTeam as varchar) is null or c.sales_team = :salesTeam)"
          + " and (cast(:accountOfficer as varchar) is null"
          + " or lower(c.account_officer) = lower(:accountOfficer))";

  /** Insurer names of a claim (the insurer lines, else the lead insurer). */
  static final String INSURERS =
      "coalesce((select string_agg(coalesce((select i.name from cat_insurer i"
          + " where i.company_id = c.company_id and i.party_code = x.insurer_code limit 1),"
          + " x.insurer_code), ', ' order by x.id) from bcl_insurer_claim x"
          + " where x.claim_id = c.id), (select i.name from cat_insurer i"
          + " where i.company_id = c.company_id and i.party_code = c.lead_insurer_code limit 1),"
          + " c.lead_insurer_code)";

  /** Columns of the claim lists (p.42-43) with the insurer claim numbers. */
  static final String CLAIM_SELECT =
      "select c.id, c.claim_no, c.claimant_name, c.assured_name, c.arn, c.policy_no,"
          + " coalesce(ln.label, c.loss_nature) as loss_nature,"
          + " coalesce(ct.label, c.claim_type) as claim_type, c.loss_date, c.reported_date,"
          + " c.currency, c.claim_amount, c.deductible, c.status_code,"
          + " coalesce(s.label, c.status_code) as status, c.phase, c.closure_kind, "
          + INSURERS
          + " as insurers, coalesce(c.lead_insurer_code, '') as lead_insurer,"
          + " (select string_agg(x.insurer_claim_no, ', ' order by x.id) from bcl_insurer_claim x"
          + " where x.claim_id = c.id and x.insurer_claim_no is not null) as insurer_claim_nos,"
          + " c.next_action_plan, c.status_since, c.closed_on, c.next_follow_up_date, c.handler,"
          + " c.sales_team, c.account_officer, c.date_settled, c.settlement_amount,"
          + " coalesce(st.label, c.settlement_type_code) as settlement_type"
          + " from bcl_claim c"
          + " left join lov_value s on s.type_code = 'BCL_CLAIM_STATUS' and s.code = c.status_code"
          + " left join lov_value ln on ln.type_code = 'BCL_LOSS_NATURE' and ln.code = c.loss_nature"
          + " left join lov_value ct on ct.type_code = 'BCL_CLAIM_TYPE' and ct.code = c.claim_type"
          + " left join lov_value st on st.type_code = 'BCL_SETTLEMENT_TYPE'"
          + " and st.code = c.settlement_type_code"
          + " where c.company_id = :companyId"
          + FILTERS;

  /** Claims outstanding on the as-of date (phases NEW, IN_PROGRESS and TEMP_CLOSED). */
  static final String OUTSTANDING_ON =
      " and c.reported_date <= :asOf and (c.phase <> 'CLOSED' or c.closed_on > :asOf)";

  private static final int DEFAULT_PAST_DUE = 90;

  private final NamedParameterJdbcTemplate jdbc;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the helper.
   *
   * @param jdbc named-parameter JDBC
   * @param parameters business parameters
   * @param clock clock
   */
  public BclReportSql(
      NamedParameterJdbcTemplate jdbc, SystemParameterService parameters, Clock clock) {
    this.jdbc = jdbc;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * The company and the common filters.
   *
   * @return parameters
   */
  static List<ParameterSpec> filters() {
    List<ParameterSpec> specs = new ArrayList<>();
    specs.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    specs.add(ParameterSpec.optional(BRANCH, "Handling branch", ParameterType.BRANCH));
    specs.add(ParameterSpec.optional(UNIT, "Claims unit", ParameterType.TEXT));
    specs.add(ParameterSpec.optional(HANDLER, "Claim handler", ParameterType.TEXT));
    specs.add(ParameterSpec.optional(INSURER, "Insurer code", ParameterType.TEXT));
    specs.add(ParameterSpec.optional(LINE, "Product line", ParameterType.TEXT));
    specs.add(ParameterSpec.optional(TEAM, "Marketing team", ParameterType.TEXT));
    specs.add(ParameterSpec.optional(OFFICER, "Account officer", ParameterType.TEXT));
    return specs;
  }

  /**
   * The common filters with the as-of date first.
   *
   * @return parameters
   */
  static List<ParameterSpec> asOfFilters() {
    List<ParameterSpec> specs = filters();
    specs.add(
        1, ParameterSpec.required(AS_OF, "As of date", ParameterType.DATE).withDefault("TODAY"));
    return specs;
  }

  /**
   * The common filters with a date range.
   *
   * @param from lower bound name ({@code xxxFrom})
   * @param to upper bound name ({@code xxxTo})
   * @param label label of the range, e.g. "Date settled"
   * @param required whether the range is required (defaults: start of month, today)
   * @return parameters
   */
  static List<ParameterSpec> rangeFilters(String from, String to, String label, boolean required) {
    List<ParameterSpec> specs = filters();
    if (required) {
      specs.add(
          1,
          ParameterSpec.required(from, label + " from", ParameterType.DATE)
              .withDefault("MONTH_START"));
      specs.add(
          2, ParameterSpec.required(to, label + " to", ParameterType.DATE).withDefault("TODAY"));
    } else {
      specs.add(1, ParameterSpec.optional(from, label + " from", ParameterType.DATE));
      specs.add(2, ParameterSpec.optional(to, label + " to", ParameterType.DATE));
    }
    return specs;
  }

  /**
   * The bind values of the parameters of a report: every declared parameter (null when absent),
   * texts stripped, dates as dates.
   *
   * @param p parameters
   * @return bind values
   */
  static Map<String, Object> args(ReportParameters p) {
    Map<String, Object> args = new HashMap<>();
    for (ParameterSpec spec : p.metadata().parameters()) {
      String name = spec.name();
      Object value =
          switch (spec.type()) {
            case DATE -> p.optionalDate(name).orElse(null);
            case COMPANY, BRANCH -> p.optionalLong(name).orElse(null);
            case NUMBER -> p.optionalDecimal(name).map(Number::intValue).orElse(null);
            default ->
                p.optionalText(name).map(String::strip).filter(s -> !s.isEmpty()).orElse(null);
          };
      args.put(name, value);
    }
    return args;
  }

  /**
   * The as-of date of a report, refused in the future (FR-CL-060).
   *
   * @param p parameters
   * @return as-of date
   */
  LocalDate asOf(ReportParameters p) {
    LocalDate asOf = p.date(AS_OF);
    if (asOf.isAfter(today())) {
      throw new BusinessRuleException(
          "INVALID_REPORT_PARAMETERS", "The as-of date cannot be in the future");
    }
    return asOf;
  }

  /**
   * Today in the Philippines.
   *
   * @return business date
   */
  LocalDate today() {
    return ClaimAgeing.today(clock);
  }

  /**
   * The ageing bucket bounds ({@code BCL_AGEING_BUCKETS}).
   *
   * @return increasing bounds
   */
  List<Integer> bounds() {
    return parameters.items(ClaimCodes.PARAM_AGEING_BUCKETS).stream()
        .map(String::strip)
        .filter(s -> s.matches("\\d+"))
        .map(Integer::valueOf)
        .sorted()
        .toList();
  }

  /**
   * The past due threshold ({@code BCL_PAST_DUE_DAYS}).
   *
   * @return days
   */
  int pastDueDays() {
    return parameters.intValue(ClaimCodes.PARAM_PAST_DUE_DAYS, DEFAULT_PAST_DUE);
  }

  /**
   * An integer parameter of the Claims category.
   *
   * @param key parameter
   * @param fallback value when unset
   * @return value
   */
  int parameter(String key, int fallback) {
    return parameters.intValue(key, fallback);
  }

  /**
   * Runs a constant query and adds the ages of each claim row as of a date: {@code age_this_stage}
   * (from {@code status_since}) and {@code age_overall} (from {@code reported_date} to {@code
   * closed_on} or the as-of date); SQL dates become local dates.
   *
   * @param sql constant SQL
   * @param args bind values
   * @param asOf as-of date
   * @return rows
   */
  List<Map<String, Object>> claimRows(String sql, Map<String, Object> args, LocalDate asOf) {
    return jdbc.queryForList(sql, args).stream().map(r -> aged(r, asOf)).toList();
  }

  /**
   * Runs a constant query; SQL dates and timestamps become local dates (Philippine time).
   *
   * @param sql constant SQL
   * @param args bind values
   * @return rows
   */
  List<Map<String, Object>> rows(String sql, Map<String, Object> args) {
    return jdbc.queryForList(sql, args).stream().map(BclReportSql::local).toList();
  }

  private static Map<String, Object> aged(Map<String, Object> row, LocalDate asOf) {
    Object since = row.get("status_since");
    Object closedOn = row.get("closed_on");
    Map<String, Object> out = local(row);
    out.put(
        "age_this_stage",
        since instanceof Timestamp t ? ClaimAgeing.ageThisStage(t.toInstant(), asOf) : 0);
    LocalDate reported = (LocalDate) out.get("reported_date");
    LocalDate closed = closedOn instanceof Date d ? d.toLocalDate() : null;
    out.put("age_overall", reported == null ? 0 : ClaimAgeing.ageOverall(reported, closed, asOf));
    return out;
  }

  private static Map<String, Object> local(Map<String, Object> row) {
    Map<String, Object> out = new LinkedHashMap<>();
    row.forEach((column, value) -> out.put(column, localValue(value)));
    return out;
  }

  private static Object localValue(Object value) {
    if (value instanceof Timestamp stamp) {
      return ClaimAgeing.dateOf(stamp.toInstant());
    }
    return value instanceof Date day ? day.toLocalDate() : value;
  }
}
