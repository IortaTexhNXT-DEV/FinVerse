package com.iortatechnxt.brokerverse.brokerclaims.report;

import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.service.LossLines;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Claims by Location / Claims-prone Locations (BRCLM.038, FR-CM-063; CLQ16): per location key, city
 * and province the number of claims with a loss in the period, paid and outstanding; a location is
 * claims-prone with at least {@code BCL_PRONE_MIN_CLAIMS} claims in the last {@code
 * BCL_PRONE_YEARS} years of the period. Filters: catastrophe code and product line. With a location
 * key the report drills down to the claims of that location. A claim at several locations counts at
 * each of them with its full amounts.
 */
@Component
public class ProneLocationsReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "BCL-PRONE-LOCATIONS";

  private static final String FROM = "periodFrom";
  private static final String TO = "periodTo";
  private static final String CATASTROPHE = "catastropheCode";
  private static final String LOCATION = "locationKey";
  private static final String WINDOW = "windowFrom";
  private static final String CLAIMS = "claims";
  private static final int DEFAULT_MIN_CLAIMS = 3;
  private static final int DEFAULT_YEARS = 3;

  private static final String TOTALS =
      "with totals as (select l.claim_id, sum(l.paid) as paid, sum(l.outstanding) as outstanding"
          + " from ("
          + LossLines.SQL
          + ") l group by l.claim_id)";

  private static final String SCOPE_FROM =
      " from bcl_claim_location cl join bcl_claim c on c.id = cl.claim_id"
          + " join totals t on t.claim_id = c.id";

  private static final String SCOPE_WHERE =
      " where c.company_id = :companyId and c.loss_date between :periodFrom and :periodTo"
          + " and (cast(:catastropheCode as varchar) is null or c.catastrophe_code = :catastropheCode)"
          + " and (cast(:lineCode as varchar) is null or c.line_code = :lineCode)";

  private static final String LOCATIONS_SQL =
      TOTALS
          + " select coalesce(cl.location_key, '') as location_key, max(cl.address) as address,"
          + " max(cl.city) as city, max(cl.province) as province,"
          + " count(distinct c.id) as claims,"
          + " count(distinct c.id) filter (where c.loss_date >= :windowFrom) as window_claims,"
          + " sum(t.paid) as paid, sum(t.outstanding) as outstanding"
          + SCOPE_FROM
          + SCOPE_WHERE
          + " group by coalesce(cl.location_key, '') order by claims desc, location_key";

  private static final String DRILL_SQL =
      TOTALS
          + " select c.claim_no, c.loss_date, cl.address, cl.city, cl.province,"
          + " coalesce(k.label, c.catastrophe_code) as catastrophe,"
          + " coalesce(s.label, c.status_code) as status, t.paid, t.outstanding"
          + SCOPE_FROM
          + " left join lov_value k on k.type_code = 'BCL_CATASTROPHE' and k.code = c.catastrophe_code"
          + " left join lov_value s on s.type_code = 'BCL_CLAIM_STATUS' and s.code = c.status_code"
          + SCOPE_WHERE
          + " and coalesce(cl.location_key, '') = :locationKey"
          + " order by c.loss_date desc, c.claim_no";

  private final BclReportSql sql;

  /**
   * Creates the report.
   *
   * @param sql report SQL
   */
  public ProneLocationsReport(BclReportSql sql) {
    this.sql = sql;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(BclReportSql.COMPANY, "Company", ParameterType.COMPANY));
    params.add(ParameterSpec.optional(FROM, "Period from", ParameterType.DATE));
    params.add(ParameterSpec.required(TO, "Period to", ParameterType.DATE).withDefault("TODAY"));
    params.add(ParameterSpec.optional(CATASTROPHE, "Catastrophe code", ParameterType.TEXT));
    params.add(ParameterSpec.optional(BclReportSql.LINE, "Product line", ParameterType.TEXT));
    params.add(ParameterSpec.optional(LOCATION, "Location key (drill-down)", ParameterType.TEXT));
    return ReportMetadata.claimsHandling(
        CODE,
        "Claims by Location / Claims-prone Locations",
        "Claims, paid and outstanding per location with the claims-prone flag (BRCLM.038)",
        params);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Map<String, Object> args = BclReportSql.args(p);
    LocalDate to = (LocalDate) args.get(TO);
    int years = sql.parameter(ClaimCodes.PARAM_PRONE_YEARS, DEFAULT_YEARS);
    int minClaims = sql.parameter(ClaimCodes.PARAM_PRONE_MIN_CLAIMS, DEFAULT_MIN_CLAIMS);
    LocalDate window = to.minusYears(years);
    if (args.get(FROM) == null) {
      args.put(FROM, window);
    }
    args.put(WINDOW, window);
    args.put("arn", null);
    args.put("policyYear", null);
    if (args.get(LOCATION) != null) {
      return drillDown(p, args);
    }
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> row : sql.rows(LOCATIONS_SQL, args)) {
      Map<String, Object> out = new LinkedHashMap<>(row);
      long recent = ((Number) row.get("window_claims")).longValue();
      out.put("prone", recent >= minClaims ? "Yes" : "No");
      rows.add(out);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("location_key", "Location Key"),
            ReportColumn.text("address", "Address"),
            ReportColumn.text("city", "City"),
            ReportColumn.text("province", "Province"),
            ReportColumn.count(CLAIMS, "Claims"),
            ReportColumn.count("window_claims", "Claims in Last " + years + " Years"),
            ReportColumn.amount("paid", "Paid"),
            ReportColumn.amount("outstanding", "O/S"),
            ReportColumn.text("prone", "Claims-prone"))
        .rows(rows)
        .presorted()
        .note(
            "Claims-prone = at least "
                + minClaims
                + " claims with a loss in the last "
                + years
                + " years (BCL_PRONE_MIN_CLAIMS, BCL_PRONE_YEARS; CLQ16). Enter a location key to"
                + " list its claims.")
        .build();
  }

  private ReportResult drillDown(ReportParameters p, Map<String, Object> args) {
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("claim_no", "Claim Number"),
            ReportColumn.date("loss_date", "Date of Loss"),
            ReportColumn.text("address", "Address"),
            ReportColumn.text("city", "City"),
            ReportColumn.text("catastrophe", "Catastrophe"),
            ReportColumn.text("status", "Claim Status"),
            ReportColumn.amount("paid", "Paid"),
            ReportColumn.amount("outstanding", "O/S"))
        .rows(sql.rows(DRILL_SQL, args))
        .presorted()
        .build();
  }
}
