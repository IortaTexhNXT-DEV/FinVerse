package com.iortatechnxt.brokerverse.brokerclaims.report;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The insurer side of the Claims Activity Log (BRCLM.023/041/042, FR-CM-066): insurer updates,
 * insurer reserve amendments and insurer location reference changes (old and new reference) of the
 * period, read from the tables of wave CL1-A ({@code bcl_insurer_update}, {@code
 * bcl_reserve_change}, {@code bcl_location_ref}).
 */
@Component
public class InsurerActivitySource implements ClaimActivitySource {

  private static final String CLAIM_FILTER =
      " and (cast(:claimNo as varchar) is null or c.claim_no = :claimNo)";

  private static final String SQL =
      "select u.recorded_at as at, u.recorded_by as who, c.claim_no, 'Insurer update' as activity,"
          + " coalesce(ic.insurer_code || ' ', '') || u.source || ' of ' || u.update_date"
          + " || coalesce(' ref. ' || u.reference, '') as detail, u.remarks as remark"
          + " from bcl_insurer_update u join bcl_claim c on c.id = u.claim_id"
          + " left join bcl_insurer_claim ic on ic.id = u.insurer_claim_id"
          + " where c.company_id = :companyId and u.recorded_at >= :fromTs and u.recorded_at < :toTs"
          + CLAIM_FILTER
          + " union all"
          + " select r.changed_at, r.changed_by, c.claim_no, 'Insurer reserve',"
          + " ic.insurer_code || ': ' || coalesce(cast(r.previous_amount as varchar), '-') || ' -> '"
          + " || r.new_amount, r.reason from bcl_reserve_change r"
          + " join bcl_insurer_claim ic on ic.id = r.insurer_claim_id"
          + " join bcl_claim c on c.id = ic.claim_id"
          + " where c.company_id = :companyId and r.changed_at >= :fromTs and r.changed_at < :toTs"
          + CLAIM_FILTER
          + " union all"
          + " select l.created_at, l.created_by, l.arn, 'Location reference',"
          + " 'Item ' || l.account_item_no || ' ' || l.insurer_code || ': '"
          + " || coalesce((select p.insurer_location_ref from bcl_location_ref p"
          + " where p.company_id = l.company_id and p.arn = l.arn"
          + " and p.account_item_no = l.account_item_no and p.insurer_code = l.insurer_code"
          + " and p.id < l.id order by p.id desc limit 1), '-') || ' -> ' || l.insurer_location_ref,"
          + " 'Effective ' || l.effective_from from bcl_location_ref l"
          + " where l.company_id = :companyId and l.created_at >= :fromTs and l.created_at < :toTs"
          + " and (cast(:claimNo as varchar) is null or exists (select 1 from bcl_claim c"
          + " join bcl_claim_location cl on cl.claim_id = c.id where c.claim_no = :claimNo"
          + " and c.arn = l.arn and cl.account_item_no = l.account_item_no))";

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the source.
   *
   * @param jdbc named-parameter JDBC
   */
  public InsurerActivitySource(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<ClaimActivity> activities(ActivityQuery query) {
    Map<String, Object> args = new HashMap<>();
    args.put("companyId", query.companyId());
    args.put("fromTs", Timestamp.from(query.from()));
    args.put("toTs", Timestamp.from(query.to()));
    args.put("claimNo", query.claimNo());
    return jdbc.query(
        SQL,
        args,
        (rs, n) ->
            new ClaimActivity(
                rs.getTimestamp("at").toInstant(),
                rs.getString("who"),
                rs.getString("claim_no"),
                rs.getString("activity"),
                rs.getString("detail"),
                rs.getString("remark")));
  }
}
