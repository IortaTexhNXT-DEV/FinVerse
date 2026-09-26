package com.iortatechnxt.brokerverse.brokerclaims.service;

/**
 * The loss figures of a claim per insurer line (BRCLM.030/032, FR-CL-062; CLAIMS_BROKING_DESIGN
 * 10): one SQL shared by the Loss Experience and Loss Ratio reports and {@link
 * ClaimExperienceQueryService}, so Renewal, the account page and the reports show the same figures
 * (decision D4). Per claim and insurer line (a claim without insurer lines is one line of its lead
 * insurer):
 *
 * <ul>
 *   <li>reserve = the insurer reserve of the line, else the initial reserve of a claim without
 *       lines;
 *   <li>paid = the settled amount of the line, else the claim settlement amount at the line's share
 *       (equal shares when none is recorded);
 *   <li>O/S = max(reserve - paid, 0) while the claim is outstanding, 0 once closed (CLQ08);
 *   <li>total = paid + O/S.
 * </ul>
 *
 * <p>Parameters: {@code :companyId} (null = every company), {@code :arn} and {@code :policyYear}
 * (null = any).
 */
public final class LossLines {

  /** Loss lines as a sub-query aliased {@code l}; append conditions on {@code l.*}. */
  public static final String SQL =
      "select l.*, case when l.phase <> 'CLOSED' then greatest(l.reserve - l.paid, 0) else 0 end"
          + " as outstanding from (select c.id as claim_id, c.company_id, c.claim_no, c.client_code,"
          + " c.assured_name, c.claimant_name, c.arn, c.policy_year, c.policy_no, c.line_code,"
          + " c.loss_date, c.reported_date, c.loss_nature, c.claim_type, c.catastrophe_code,"
          + " c.deductible, c.claim_amount, c.currency, c.status_code, c.phase, c.handler,"
          + " c.sales_team, c.account_officer, c.branch_id, c.unit_code, c.lead_insurer_code,"
          + " coalesce(ic.insurer_code, c.lead_insurer_code) as insurer_code, ic.insurer_claim_no,"
          + " ic.share_pct,"
          + " coalesce(ic.reserve_amount, case when ic.id is null then c.initial_reserve end, 0)"
          + " as reserve,"
          + " coalesce(ic.settled_amount, round(c.settlement_amount * coalesce(ic.share_pct,"
          + " 100.0 / greatest(count(ic.id) over (partition by c.id), 1)) / 100, 2), 0) as paid"
          + " from bcl_claim c left join bcl_insurer_claim ic on ic.claim_id = c.id"
          + " where (cast(:companyId as bigint) is null or c.company_id = :companyId)"
          + " and (cast(:arn as varchar) is null or c.arn = :arn)"
          + " and (cast(:policyYear as integer) is null or c.policy_year = :policyYear)) l";

  private LossLines() {}
}
