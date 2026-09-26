package com.iortatechnxt.brokerverse.prodrecon.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The unmatched summaries of Annex IV: per location (PRC-UNMATCHED-LOC #2, PRCID.036), per
 * Marketing AO / AB for booked and pre-booked accounts (PRC-UNMATCHED-AO #3, PRCID.037) and per
 * disposition (PRC-DISPOSITION #4, PRCID.039). Each counts the items and amounts of the insurer's
 * unmatched production grouped as the annex lists; the disposition list is parked (OQ31).
 */
@Configuration(proxyBeanMethods = false)
public class UnmatchedSummaryReports {

  private static final String DISPOSITION = "coalesce(i.disposition, 'NONE') as disposition";
  private static final String AO_AB =
      "coalesce(i.ao_username, '-') || ' / ' || coalesce(i.sales_unit, '-') as ao";
  private static final String UNMATCHED = " and i.status <> 'MATCHED'";
  private static final String MEASURES =
      ", count(*) as items, sum(" + ReconReportSupport.AMOUNT + ") as amount";

  /**
   * Unmatched accounts per location (PRCID.036).
   *
   * @param support report SQL
   * @return report
   */
  @Bean
  public ReportDefinition unmatchedPerLocationReport(ReconReportSupport support) {
    return new SqlReport(
        support,
        ReconReportSupport.metadata(
            "PRC-UNMATCHED-LOC",
            "Unmatched Accounts per Location",
            "Unmatched accounts per insurer, disposition and BDOI location (PRCID.036)"),
        "select c.insurer_code as insurer, "
            + DISPOSITION
            + ", coalesce(b.name, 'Not booked') as location"
            + MEASURES
            + ReconReportSupport.ITEMS_OF_PERIOD.replace(
                " where ", " left join org_branch b on b.id = i.branch_id where ")
            + UNMATCHED
            + " group by 1, 2, 3 order by 1, 2, 3",
        SqlReport.Group.INSURER,
        List.of(
            ReportColumn.text("disposition", "Disposition"),
            ReportColumn.text("location", "Location"),
            ReportColumn.count("items", "Items"),
            ReportColumn.amount("amount", "Amount")));
  }

  /**
   * Unmatched booked and pre-booked accounts per Marketing AO / AB (PRCID.037).
   *
   * @param support report SQL
   * @return report
   */
  @Bean
  public ReportDefinition unmatchedPerAoReport(ReconReportSupport support) {
    return new SqlReport(
        support,
        ReconReportSupport.metadata(
            "PRC-UNMATCHED-AO",
            "Unmatched Accounts per Marketing AO / AB",
            "Unmatched booked and pre-booked accounts per insurer, disposition and AO / AB"
                + " (PRCID.037)"),
        "select c.insurer_code as insurer, "
            + DISPOSITION
            + ", "
            + AO_AB
            + MEASURES
            + ReconReportSupport.ITEMS_OF_PERIOD
            + " and i.status in ('BDOI_ONLY', 'MATCHED_WITH_DISCREPANCY', 'UNMATCHED_PREBOOKED')"
            + " group by 1, 2, 3 order by 1, 2, 3",
        SqlReport.Group.INSURER,
        List.of(
            ReportColumn.text("disposition", "Disposition"),
            ReportColumn.text("ao", "Marketing AO / AB"),
            ReportColumn.count("items", "Items"),
            ReportColumn.amount("amount", "Amount")));
  }

  /**
   * Unmatched accounts per disposition (PRCID.039).
   *
   * @param support report SQL
   * @return report
   */
  @Bean
  public ReportDefinition perDispositionReport(ReconReportSupport support) {
    return new SqlReport(
        support,
        ReconReportSupport.metadata(
            "PRC-DISPOSITION",
            "Summary per Disposition",
            "Unmatched accounts per disposition, insurer and Marketing AO / AB (PRCID.039)"),
        "select "
            + DISPOSITION
            + ", c.insurer_code as insurer, "
            + AO_AB
            + MEASURES
            + ReconReportSupport.ITEMS_OF_PERIOD
            + UNMATCHED
            + " group by 1, 2, 3 order by 1, 2, 3",
        new SqlReport.Group("disposition", "Disposition"),
        List.of(
            ReportColumn.text("insurer", "Insurance Company"),
            ReportColumn.text("ao", "Marketing AO / AB"),
            ReportColumn.count("items", "Items"),
            ReportColumn.amount("amount", "Amount")));
  }
}
