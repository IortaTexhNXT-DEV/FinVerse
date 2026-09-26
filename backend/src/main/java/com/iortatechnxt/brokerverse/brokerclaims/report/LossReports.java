package com.iortatechnxt.brokerverse.brokerclaims.report;

import com.iortatechnxt.brokerverse.brokerclaims.service.LossLines;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The loss reports (BRCLM.030/032/040, p.43-44; FR-CL-062/065): the Loss Experience per claim and
 * insurer line grouped by client and cover, and the Loss Ratio per cover and policy year (losses =
 * paid + O/S over the signed gross premium of the ledger invoices of the cover and year x 100),
 * grouped by client, product line or insurer. The figures come from {@link LossLines}, which also
 * serves Renewal and the account page.
 */
public final class LossReports {

  /** Loss Experience. */
  public static final String EXPERIENCE = "BCL-LOSS-EXPERIENCE";

  /** Loss Ratio. */
  public static final String RATIO = "BCL-LOSS-RATIO";

  private static final String GROUPING = "grouping";
  private static final int CLIENT_POSITION = 3;
  private static final String LOSS_FROM = "lossFrom";
  private static final String LOSS_TO = "lossTo";
  private static final String CLIENT = "clientCode";
  private static final String ARN = "arn";
  private static final String POLICY_YEAR = "policyYear";
  private static final String GROUP_KEY = "group_key";
  private static final String PREMIUM = "premium";
  private static final String LOSSES = "losses";

  /** Loss lines filtered like the claim lists (on the loss line alias {@code l}). */
  private static final String LINE_FILTERS =
      " where l.company_id = :companyId"
          + " and (cast(:clientCode as varchar) is null or l.client_code = :clientCode)"
          + " and (cast(:lossFrom as date) is null or l.loss_date >= :lossFrom)"
          + " and (cast(:lossTo as date) is null or l.loss_date <= :lossTo)"
          + " and exists (select 1 from bcl_claim c where c.id = l.claim_id"
          + BclReportSql.FILTERS
          + ")";

  private static final String EXPERIENCE_SQL =
      "select l.*, l.paid + l.outstanding as total,"
          + " coalesce(ln.label, l.loss_nature) as loss_nature_label,"
          + " coalesce(ct.label, l.claim_type) as claim_type_label,"
          + " coalesce(s.label, l.status_code) as status_label,"
          + " coalesce((select i.name from cat_insurer i where i.company_id = l.company_id"
          + " and i.party_code = l.insurer_code limit 1), l.insurer_code) as insurer_name,"
          + " l.client_code || ' ' || coalesce(l.assured_name, '') as client,"
          + " l.arn || ' / ' || l.policy_year as cover from ("
          + LossLines.SQL
          + ") l"
          + " left join lov_value ln on ln.type_code = 'BCL_LOSS_NATURE' and ln.code = l.loss_nature"
          + " left join lov_value ct on ct.type_code = 'BCL_CLAIM_TYPE' and ct.code = l.claim_type"
          + " left join lov_value s on s.type_code = 'BCL_CLAIM_STATUS' and s.code = l.status_code"
          + LINE_FILTERS
          + " order by l.client_code, l.arn, l.policy_year, l.claim_no, l.insurer_code";

  private static final String RATIO_SQL =
      "with losses as (select l.arn, l.policy_year, count(distinct l.claim_id) as claims,"
          + " sum(l.paid) as paid, sum(l.outstanding) as outstanding from ("
          + LossLines.SQL
          + ") l"
          + LINE_FILTERS
          + " group by l.arn, l.policy_year),"
          + " covers as (select distinct on (c.arn, c.policy_year) c.arn, c.policy_year,"
          + " c.client_code, c.assured_name, c.line_code, c.lead_insurer_code, c.currency"
          + " from bcl_claim c where c.company_id = :companyId order by c.arn, c.policy_year, c.id desc),"
          + " premium as (select o.arn, o.policy_year, sum(case when o.kind in"
          + " ('ENDORSEMENT_MINUS', 'CANCELLATION') then -abs(o.gross_premium)"
          + " else abs(o.gross_premium) end) as premium from ops_invoice o"
          + " where o.company_id = :companyId and not o.cancelled group by o.arn, o.policy_year)"
          + " select v.*, l.claims, l.paid, l.outstanding, l.paid + l.outstanding as losses,"
          + " coalesce(p.premium, 0) as premium from losses l"
          + " join covers v on v.arn = l.arn and v.policy_year = l.policy_year"
          + " left join premium p on p.arn = l.arn and p.policy_year = l.policy_year"
          + " order by v.client_code, v.arn, v.policy_year";

  private LossReports() {}

  private static List<ParameterSpec> lossParameters() {
    List<ParameterSpec> params = BclReportSql.rangeFilters(LOSS_FROM, LOSS_TO, "Loss date", false);
    params.add(CLIENT_POSITION, ParameterSpec.optional(CLIENT, "Client code", ParameterType.TEXT));
    return params;
  }

  /** Loss Experience (BRCLM.030, p.43). */
  @Component
  public static class Experience implements ReportDefinition {

    private final BclReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public Experience(BclReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return ReportMetadata.claimsHandling(
          EXPERIENCE,
          "Loss Experience",
          "Paid, outstanding and total loss per claim and insurer, by client and cover"
              + " (BRCLM.030)",
          lossParameters());
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      Map<String, Object> args = BclReportSql.args(p);
      args.put(ARN, null);
      args.put(POLICY_YEAR, null);
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("assured_name", "Insured"),
              ReportColumn.text("claimant_name", "Claimant"),
              ReportColumn.text("policy_no", "Policy No."),
              ReportColumn.text("claim_no", "Claim Number"),
              ReportColumn.date("loss_date", "Date of Loss"),
              ReportColumn.text("loss_nature_label", "Nature of Loss"),
              ReportColumn.text("claim_type_label", "Type of Loss"),
              ReportColumn.amountNoTotal("deductible", "Deductible"),
              ReportColumn.text("insurer_name", "Insurer Name"),
              ReportColumn.text("insurer_claim_no", "Insurer Claim No."),
              ReportColumn.text("currency", "Currency"),
              ReportColumn.amount("paid", "Amount of Loss Paid"),
              ReportColumn.amount("outstanding", "Amount of Loss O/S"),
              ReportColumn.amount("total", "Total Loss"),
              ReportColumn.text("status_label", "Status"))
          .groupBy("client", "Client")
          .groupBy("cover", "Cover (ARN / Policy Year)")
          .rows(sql.rows(EXPERIENCE_SQL, args))
          .presorted()
          .note(
              "Paid = settled amount; O/S = insurer reserve less paid while open, never below"
                  + " zero (CLQ08).")
          .build();
    }
  }

  /** Loss Ratio (BRCLM.032, p.44). */
  @Component
  public static class Ratio implements ReportDefinition {

    private static final List<String> GROUPINGS = List.of("CLIENT", "LINE", "INSURER");

    private final BclReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public Ratio(BclReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      List<ParameterSpec> params = lossParameters();
      params.add(
          1, new ParameterSpec(GROUPING, "Grouping", ParameterType.SELECT, false, GROUPINGS, null));
      return ReportMetadata.claimsHandling(
          RATIO,
          "Loss Ratio",
          "Losses over the premium of the cover and policy year, by client, product line or"
              + " insurer (BRCLM.032)",
          params);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      String grouping =
          p.optionalText(GROUPING)
              .orElseThrow(
                  () ->
                      new BusinessRuleException(
                          "INVALID_REPORT_PARAMETERS", "Select the grouping"));
      Map<String, Object> args = BclReportSql.args(p);
      args.put(ARN, null);
      args.put(POLICY_YEAR, null);
      List<Map<String, Object>> rows = new ArrayList<>();
      for (Map<String, Object> row : sql.rows(RATIO_SQL, args)) {
        Map<String, Object> out = new LinkedHashMap<>(row);
        out.put(GROUP_KEY, groupKey(grouping, row));
        out.put("ratio", ratio((BigDecimal) row.get(LOSSES), (BigDecimal) row.get(PREMIUM)));
        rows.add(out);
      }
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("client_code", "Client"),
              ReportColumn.text("assured_name", "Insured"),
              ReportColumn.text(ARN, "Account (ARN)"),
              new ReportColumn(POLICY_YEAR, "Policy Year", ColumnType.NUMBER, false),
              ReportColumn.text("line_code", "Product Line"),
              ReportColumn.text("lead_insurer_code", "Insurer"),
              ReportColumn.text("currency", "Currency"),
              ReportColumn.count("claims", "Claims"),
              ReportColumn.amount(PREMIUM, "Premium"),
              ReportColumn.amount("paid", "Losses Paid"),
              ReportColumn.amount("outstanding", "Losses O/S"),
              ReportColumn.amount(LOSSES, "Total Losses"),
              ReportColumn.percent("ratio", "Loss Ratio %"))
          .groupBy(GROUP_KEY, groupLabel(grouping))
          .rows(rows)
          .note(
              "Premium = signed gross premium of the ledger invoices of the cover and policy year"
                  + " (originals, endorsements, cancellations; CLQ20).")
          .build();
    }

    private static String groupKey(String grouping, Map<String, Object> row) {
      return switch (grouping) {
        case "LINE" -> String.valueOf(row.get("line_code"));
        case "INSURER" -> String.valueOf(row.get("lead_insurer_code"));
        default -> row.get("client_code") + " " + row.get("assured_name");
      };
    }

    private static String groupLabel(String grouping) {
      return switch (grouping) {
        case "LINE" -> "Product Line";
        case "INSURER" -> "Insurer";
        default -> "Client";
      };
    }

    /**
     * Losses over premium x 100.
     *
     * @param losses losses
     * @param premium premium
     * @return ratio in percent with 2 decimals, null without premium
     */
    public static BigDecimal ratio(BigDecimal losses, BigDecimal premium) {
      if (losses == null || premium == null || premium.signum() == 0) {
        return null;
      }
      return losses.multiply(BigDecimal.valueOf(100)).divide(premium, 2, RoundingMode.HALF_UP);
    }
  }
}
