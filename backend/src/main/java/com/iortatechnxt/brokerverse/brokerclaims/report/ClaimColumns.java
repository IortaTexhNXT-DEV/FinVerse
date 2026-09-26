package com.iortatechnxt.brokerverse.brokerclaims.report;

import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.ArrayList;
import java.util.List;

/**
 * Columns of the Claims lists (BRD report list p.42-43, FRS section 6.1): the outstanding list and
 * its variants, the settled list, and the age columns shared by the ageing reports.
 */
final class ClaimColumns {

  /** Age this stage (days in the current status). */
  static final ReportColumn AGE_THIS_STAGE =
      new ReportColumn("age_this_stage", "Age this Stage", ColumnType.NUMBER, false);

  /** Age overall (days since reported). */
  static final ReportColumn AGE_OVERALL =
      new ReportColumn("age_overall", "Age Overall", ColumnType.NUMBER, false);

  /** One per row, totalled: the number of claims of a group. */
  static final ReportColumn CLAIMS = ReportColumn.count("claims", "Claims");

  private ClaimColumns() {}

  /**
   * The identity columns of every list.
   *
   * @return claim, claimant, assured, loss and amounts
   */
  static List<ReportColumn> head() {
    return List.of(
        ReportColumn.text("claim_no", "Claim Number"),
        ReportColumn.text("claimant_name", "Name of Claimant"),
        ReportColumn.text("assured_name", "Assured's Name"),
        ReportColumn.text("loss_nature", "Nature / Type of Loss"),
        ReportColumn.date("loss_date", "Date of Loss"),
        ReportColumn.date("reported_date", "Date Reported"),
        ReportColumn.text("currency", "Currency"),
        ReportColumn.amount("claim_amount", "Claim Amount"),
        ReportColumn.amount("deductible", "Deductible"));
  }

  /**
   * The Marketing columns closing every list.
   *
   * @return handler, Marketing team and account officer
   */
  static List<ReportColumn> tail() {
    return List.of(
        ReportColumn.text("handler", "Claim Handler"),
        ReportColumn.text("sales_team", "Marketing Team"),
        ReportColumn.text("account_officer", "Account Officer"));
  }

  /**
   * The List of all Outstanding Claims (p.42) with the insurer claim numbers.
   *
   * @return columns
   */
  static List<ReportColumn> outstanding() {
    List<ReportColumn> columns = new ArrayList<>(head());
    columns.add(ReportColumn.text("status", "Claim Status"));
    columns.add(ReportColumn.text("insurers", "Insurance Company"));
    columns.add(ReportColumn.text("insurer_claim_nos", "Insurer Claim Nos."));
    columns.add(ReportColumn.text("claim_type", "Claim Type"));
    columns.add(ReportColumn.text("next_action_plan", "Follow Ups / Remarks"));
    columns.add(AGE_THIS_STAGE);
    columns.add(AGE_OVERALL);
    columns.add(ReportColumn.date("next_follow_up_date", "Next Follow Up"));
    columns.addAll(tail());
    return columns;
  }

  /**
   * The List of all Settled Claims (p.42).
   *
   * @return columns
   */
  static List<ReportColumn> settled() {
    List<ReportColumn> columns = new ArrayList<>(head());
    columns.add(ReportColumn.date("date_settled", "Date Settled"));
    columns.add(ReportColumn.amount("settlement_amount", "Settlement Amount"));
    columns.add(ReportColumn.text("settlement_type", "Type of Settlement"));
    columns.add(AGE_OVERALL);
    columns.add(ReportColumn.text("status", "Claim Status"));
    columns.add(ReportColumn.text("insurer_claim_nos", "Insurer Claim Nos."));
    columns.addAll(tail());
    return columns;
  }
}
