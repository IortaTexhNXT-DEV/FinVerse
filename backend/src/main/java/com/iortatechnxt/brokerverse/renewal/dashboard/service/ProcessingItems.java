package com.iortatechnxt.brokerverse.renewal.dashboard.service;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.renewal.report.RenewalReportSupport;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Reads the accounts of the Processing dashboard (FRRN.003): the New Business and renewal accounts
 * submitted to Processing, with the dates of their workflow (submitted for placement, placement
 * sent, returned), the Placement Processor, the policy numbers, the e-policy and its transmittal,
 * and the placement tracking (policy status, issue and resolution date).
 */
@Component
public class ProcessingItems {

  private static final String CASE_HISTORY =
      " left join lateral (select h.occurred_at from wf_case_history h where h.case_id = w.id"
          + " and h.to_stage = '%s' order by h.id desc limit 1) %s on true";

  private static final String SQL =
      "select case when a.business_type = 'RENEWAL' then 'RENEWAL' else 'NEW_BUSINESS' end as kind,"
          + " a.id, a.arn as ref, a.arn, a.business_type, a.status as stage, a.client_name as assured,"
          + " a.line_code, a.product_code, a.market_segment as segment, a.sales_department as department,"
          + " a.sales_team as team, a.insurer_code, a.period_from as inception_date,"
          + " a.period_to as expiry_date, a.account_officer, a.total_sum_insured as sum_insured,"
          + " coalesce(a.net_premium, 0) as premium, coalesce(a.commission, 0) as commission,"
          + " a.direct_booking, a.booked_at, coalesce(p.packaged, false) as packaged,"
          + " (select string_agg(ap.policy_number, ', ') from acc_account_policy ap"
          + " where ap.account_id = a.id) as policy_numbers,"
          + " c.renewal_ref, c.unit_head, c.branch_code, coalesce(pa.processor, c.assigned_po) as processor,"
          + " sfp.occurred_at as submitted_at, pls.occurred_at as placed_at, ret.occurred_at as returned_at,"
          + " ret.reason_code as return_reason, ret.comment as return_remarks,"
          + " (select v.label from lov_value v where v.type_code = 'RETURN_REASON'"
          + " and v.code = ret.reason_code limit 1) as return_reason_name,"
          + " e.status as epolicy_status, e.created_at as policy_received_at, e.dispatched_at,"
          + " t.policy_status, t.with_issue, t.placement_issue, t.resolution_date,"
          + " t.transmittal_status, t.transmittal_reason, t.transmitted_at, "
          + DashboardItems.labels(
              "a.line_code", "a.insurer_code", "a.market_segment", "a.company_id")
          + " from acc_account a"
          + " left join cat_product p on p.code = a.product_code"
          + " left join rnw_candidate c on c.renewal_arn = a.arn and c.company_id = a.company_id"
          + " left join rnw_processing_assignment pa on pa.company_id = a.company_id and pa.arn = a.arn"
          + " left join rnw_placement_tracking t on t.company_id = a.company_id and t.arn = a.arn"
          + " left join wf_case w on w.entity_type = 'Account' and w.entity_id = cast(a.id as varchar)"
          + String.format(CASE_HISTORY, "READY_FOR_PLACEMENT", "sfp")
          + String.format(CASE_HISTORY, "PLACED", "pls")
          + " left join lateral (select h.occurred_at, h.reason_code, h.comment from wf_case_history h"
          + " where h.case_id = w.id and h.to_stage = 'RETURNED_TO_MARKETING'"
          + " order by h.id desc limit 1) ret on true"
          + " left join lateral (select x.status, x.created_at, x.dispatched_at from iss_epolicy x"
          + " where x.account_id = a.id order by x.id desc limit 1) e on true"
          + " where a.company_id = :company and a.status not in ('DRAFT', 'VOIDED')"
          + " and (cast(:bt as varchar) is null or a.business_type = :bt)"
          + " and (a.period_from between :pfrom and :pto or a.status in ('SUBMITTED', 'AWAITING_PAYMENT',"
          + " 'READY_FOR_PLACEMENT', 'PLACED', 'RETURNED_BY_INSURER', 'POLICY_ISSUED', 'RETURNED_TO_MARKETING'))"
          + " and (cast(:seg as varchar) is null or a.market_segment = :seg)"
          + " and (cast(:ao as varchar) is null or a.account_officer = :ao)"
          + " and (cast(:scope_all as boolean) or a.sales_team = any(cast(:scope_units as varchar[]))"
          + " or a.account_officer = :scope_user or coalesce(pa.processor, c.assigned_po) = :scope_user)";

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;

  /**
   * Creates the reader.
   *
   * @param jdbc SQL
   * @param support data scope of the user
   */
  public ProcessingItems(NbReportJdbc jdbc, RenewalReportSupport support) {
    this.jdbc = jdbc;
    this.support = support;
  }

  /**
   * The accounts of the filters within the user's scope.
   *
   * @param f filters with the period and business type
   * @return items
   */
  public List<DashboardItem> items(DashboardFilter f) {
    Map<String, Object> args =
        support
            .allOf(f.companyId())
            .with("bt", f.businessType())
            .with("pfrom", f.from())
            .with("pto", f.to())
            .with("seg", f.segment())
            .with("ao", f.officer())
            .map();
    return jdbc.rows(SQL, args).stream().map(DashboardItem::new).toList();
  }
}
