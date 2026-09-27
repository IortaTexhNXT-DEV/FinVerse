package com.iortatechnxt.brokerverse.eb.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Pending Items (EB-PENDING-ITEMS; FR-EB-057, 060): contracts, HMO cards, card replacements and
 * billings due in the period by type, with the programme, member, member change, responsible party,
 * due date, age past due at the end of the period, follow-ups sent and escalation.
 */
@Component
public class EbPendingItemsReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "EB-PENDING-ITEMS";

  private static final String STATUS = "status";

  private static final String SQL =
      "select i.item_type, p.programme_no || ' - ' || p.client_name as programme, i.subject,"
          + " coalesce(m.employee_no || ' ' || m.last_name, i.member_ref) as member,"
          + " coalesce(mc.change_no, i.member_change_ref) as member_change, i.responsible,"
          + " i.party_code, i.status, i.due_date, i.follow_ups_sent,"
          + " case when i.escalated_at is null then 'No' else 'Yes' end as escalated"
          + " from eb_tracked_item i join eb_programme p on p.id = i.programme_id"
          + " left join eb_member m on m.id = i.member_id"
          + " left join eb_member_change mc on mc.id = i.member_change_id"
          + " where i.company_id = :company and i.due_date between :from and :to"
          + EbReportSupport.PROGRAMME_FILTERS
          + " and (cast(:status as varchar) is null or i.status = :status)"
          + " and (cast(:insurer as varchar) is null or i.party_code = :insurer)"
          + " order by i.item_type, i.due_date, i.id";

  private final NbReportJdbc jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   */
  public EbPendingItemsReport(NbReportJdbc jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return EbReportSupport.metadata(
            CODE,
            "Employee Benefits Pending Items",
            "Contracts, cards and billings expected, their age past due and follow-ups",
            ParameterSpec.select(
                STATUS,
                "Status",
                List.of(EbReportSupport.ALL, "PENDING", "RECEIVED", "RELEASED", "CLOSED"),
                "PENDING"))
        .asDocument();
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    LocalDate end = p.date(EbReportSupport.TO);
    var args = EbReportSupport.args(p).with(STATUS, EbReportSupport.selected(p, STATUS));
    var rows =
        jdbc.rows(SQL, args.map()).stream()
            .map(
                r -> {
                  LocalDate due = (LocalDate) r.get("due_date");
                  long age = "PENDING".equals(r.get(STATUS)) ? ChronoUnit.DAYS.between(due, end) : 0;
                  r.put("age", Math.max(0, age));
                  return EbReportSupport.relabel(r, "item_type", "responsible", STATUS);
                })
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("programme", "Programme / Client"),
            ReportColumn.text("subject", "Subject"),
            ReportColumn.text("member", "Member"),
            ReportColumn.text("member_change", "Member Change"),
            ReportColumn.text("responsible", "Responsible"),
            ReportColumn.text("party_code", "Party"),
            ReportColumn.text(STATUS, "Status"),
            ReportColumn.date("due_date", "Due Date"),
            ReportColumn.count("age", "Days Past Due"),
            ReportColumn.count("follow_ups_sent", "Follow-ups"),
            ReportColumn.text("escalated", "Escalated"))
        .groupBy("item_type", "Item Type")
        .rows(rows)
        .presorted()
        .build();
  }
}
