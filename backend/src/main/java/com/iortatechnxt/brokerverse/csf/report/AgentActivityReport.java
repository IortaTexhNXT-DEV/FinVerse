package com.iortatechnxt.brokerverse.csf.report;

import com.iortatechnxt.brokerverse.csf.domain.ActivityAction;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Agent Activity ({@value #CODE}; FR-CSF-042, FRS section 6.1.2): the counts per agent and day by
 * action (searches, views, downloads, RA and e-policy resends, uploads, verifications, contact
 * changes, referrals), or the detail rows with client, reference and time. The layout parameter
 * chooses; an optional action filter applies to both.
 */
@Component
public class AgentActivityReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "CSF-ACTIVITY";

  private static final String LAYOUT = "layout";
  private static final String SUMMARY = "Summary";
  private static final String DETAIL = "Detail";
  private static final String ACTION = "action";
  private static final String ALL = "All";

  private static final String WHERE =
      " from csf_activity a left join sec_user u on lower(u.username) = lower(a.agent)"
          + " where a.company_id = :company and "
          + CsfReportSupport.day("a.occurred_at")
          + " between :from and :to"
          + " and (:agent = '' or lower(a.agent) = :agent)"
          + " and (:client = '' or upper(a.client_code) = :client)"
          + " and (:action = '' or a.action = :action)";

  private static final String SUMMARY_SQL =
      "select coalesce(u.full_name, a.agent) as agent, "
          + CsfReportSupport.day("a.occurred_at")
          + " as day,"
          + " count(*) filter (where a.action = 'SEARCH') as searches,"
          + " count(*) filter (where a.action = 'VIEW') as views,"
          + " count(*) filter (where a.action = 'DOWNLOAD') as downloads,"
          + " count(*) filter (where a.action = 'RESEND_RA') as ra_resends,"
          + " count(*) filter (where a.action = 'RESEND_EPOLICY') as epolicy_resends,"
          + " count(*) filter (where a.action = 'UPLOAD') as uploads,"
          + " count(*) filter (where a.action = 'VERIFY') as verifications,"
          + " count(*) filter (where a.action = 'CONTACT_CHANGE') as changes,"
          + " count(*) filter (where a.action = 'REFERRAL') as referrals,"
          + " count(*) as total"
          + WHERE
          + " group by 1, 2 order by 1, 2";

  private static final String DETAIL_SQL =
      "select coalesce(u.full_name, a.agent) as agent, "
          + CsfReportSupport.time("a.occurred_at")
          + " as occurred_at, a.action, a.client_code, a.reference, a.detail"
          + WHERE
          + " order by 1, a.occurred_at, a.id";

  private final CsfReportSupport support;

  /**
   * Creates the report.
   *
   * @param support parameters, SQL and labels
   */
  public AgentActivityReport(CsfReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<String> actions = new ArrayList<>();
    actions.add(ALL);
    for (ActivityAction a : ActivityAction.values()) {
      actions.add(label(a));
    }
    return ReportMetadata.customerService(
        CODE,
        "Agent Activity",
        "Searches, servicing views, downloads, resends, uploads and contact changes per agent and day",
        CsfReportSupport.parameters(
            ParameterSpec.select(LAYOUT, "Layout", List.of(SUMMARY, DETAIL), SUMMARY),
            ParameterSpec.select(ACTION, "Action", actions, ALL),
            ParameterSpec.optional(CsfReportSupport.CLIENT, "Client code", ParameterType.TEXT)));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String action = actionCode(p.optionalText(ACTION).orElse(ALL));
    boolean detail = DETAIL.equals(p.optionalText(LAYOUT).orElse(SUMMARY));
    List<Map<String, Object>> rows =
        support.rows(detail ? DETAIL_SQL : SUMMARY_SQL, p, Map.of(ACTION, action));
    if (detail) {
      rows.forEach(r -> r.put(ACTION, labelOfCode(r.get(ACTION))));
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("occurred_at", "Date / Time"),
              ReportColumn.text(ACTION, "Action"),
              ReportColumn.text("client_code", "Client"),
              ReportColumn.text("reference", "Reference"),
              ReportColumn.text("detail", "Detail"))
          .groupBy("agent", "Agent")
          .rows(rows)
          .presorted()
          .withoutGrandTotal()
          .build();
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.date("day", "Day"),
            ReportColumn.count("searches", "Searches"),
            ReportColumn.count("views", "Views"),
            ReportColumn.count("downloads", "Downloads"),
            ReportColumn.count("ra_resends", "RA Resends"),
            ReportColumn.count("epolicy_resends", "E-policy Resends"),
            ReportColumn.count("uploads", "Uploads"),
            ReportColumn.count("verifications", "Verifications"),
            ReportColumn.count("changes", "Contact Changes"),
            ReportColumn.count("referrals", "Referrals"),
            ReportColumn.count("total", "Total"))
        .groupBy("agent", "Agent")
        .rows(rows)
        .presorted()
        .build();
  }

  private static String actionCode(String label) {
    for (ActivityAction a : ActivityAction.values()) {
      if (label(a).equals(label)) {
        return a.name();
      }
    }
    return "";
  }

  private static String labelOfCode(Object code) {
    for (ActivityAction a : ActivityAction.values()) {
      if (a.name().equals(code)) {
        return label(a);
      }
    }
    return code == null ? null : code.toString();
  }

  /**
   * The label of an action in the report.
   *
   * @param a action
   * @return label
   */
  static String label(ActivityAction a) {
    return switch (a) {
      case RESEND_RA -> "Renewal advice resend";
      case RESEND_EPOLICY -> "E-policy resend";
      case CONTACT_CHANGE -> "Contact change";
      case VERIFY -> "Verification";
      default -> CsfReportSupport.words(a.name().toLowerCase(Locale.ROOT));
    };
  }
}
