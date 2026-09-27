package com.iortatechnxt.brokerverse.eb.report;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import com.iortatechnxt.brokerverse.eb.service.EbWorkingDays;
import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Turn-around Time (EB-TAT; FR-EB-062, TAT annex p.42): each activity stamped in the period
 * (received and released) with the working days taken, the target of its {@code EB_TAT_*}
 * parameter and whether it breached it; an open activity counts to the end of the period. The
 * renewal advice is measured in days before the expiry and is listed without a breach.
 */
@Component
public class EbTatReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "EB-TAT";

  private static final String ACTIVITY = "activity";

  private static final String SQL =
      "select v.activity_code as activity, v.team_code as team, v.account_officer as ao,"
          + " v.programme_no || ' - ' || v.client_name as programme, v.cycle_no, v.reference,"
          + " cast(v.received_at at time zone '" + BusinessClock.zoneId() + "' as date) as received,"
          + " cast(v.released_at at time zone '" + BusinessClock.zoneId() + "' as date) as released,"
          + " v.actor from eb_tat_v v join eb_programme p on p.id = v.programme_id"
          + " where v.company_id = :company"
          + " and cast(v.received_at at time zone '" + BusinessClock.zoneId()
          + "' as date) between :from and :to"
          + EbReportSupport.PROGRAMME_FILTERS
          + " and (cast(:activity as varchar) is null or v.activity_code = :activity)"
          + " and (cast(:businessType as varchar) is null or v.business_type = :businessType)"
          + " order by v.activity_code, v.received_at, v.id";

  private final NbReportJdbc jdbc;
  private final EbParameters parameters;
  private final EbWorkingDays workingDays;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   * @param parameters TAT targets
   * @param workingDays working-day calendar
   */
  public EbTatReport(NbReportJdbc jdbc, EbParameters parameters, EbWorkingDays workingDays) {
    this.jdbc = jdbc;
    this.parameters = parameters;
    this.workingDays = workingDays;
  }

  @Override
  public ReportMetadata metadata() {
    List<String> activities = new ArrayList<>(List.of(EbReportSupport.ALL));
    for (TatActivity a : TatActivity.values()) {
      activities.add(a.name());
    }
    return EbReportSupport.metadata(
            CODE,
            "Employee Benefits Turn-around Time",
            "Working days taken per activity against its target, with the breaches",
            ParameterSpec.select(ACTIVITY, "Activity", activities, EbReportSupport.ALL))
        .asDocument();
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(EbReportSupport.COMPANY);
    LocalDate end = p.date(EbReportSupport.TO);
    Predicate<LocalDate> working = workingDays.calendar(companyId);
    var args = EbReportSupport.args(p).with(ACTIVITY, EbReportSupport.selected(p, ACTIVITY));
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> row : jdbc.rows(SQL, args.map())) {
      rows.add(measure(row, end, working));
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("team", "Team"),
            ReportColumn.text("ao", "Account Officer"),
            ReportColumn.text("programme", "Programme / Client"),
            ReportColumn.text("reference", "Reference"),
            ReportColumn.date("received", "Received"),
            ReportColumn.date("released", "Released"),
            ReportColumn.count("days", "Working Days"),
            ReportColumn.count("target", "Target"),
            ReportColumn.text("breach", "Breach"))
        .groupBy(ACTIVITY, "Activity")
        .rows(rows)
        .presorted()
        .build();
  }

  private Map<String, Object> measure(
      Map<String, Object> row, LocalDate end, Predicate<LocalDate> working) {
    TatActivity activity = TatActivity.valueOf(row.get(ACTIVITY).toString());
    LocalDate received = (LocalDate) row.get("received");
    LocalDate released = row.get("released") == null ? end : (LocalDate) row.get("released");
    long days = 0;
    for (LocalDate d = received.plusDays(1); !d.isAfter(released); d = d.plusDays(1)) {
      if (working.test(d)) {
        days++;
      }
    }
    boolean measured = activity != TatActivity.RENEWAL_ADVICE;
    int target = parameters.tatDays(activity);
    row.put("days", days);
    row.put("target", measured ? target : null);
    row.put("breach", measured && days > target ? "Yes" : "No");
    row.put(ACTIVITY, EbReportSupport.label(activity.name()));
    return row;
  }
}
