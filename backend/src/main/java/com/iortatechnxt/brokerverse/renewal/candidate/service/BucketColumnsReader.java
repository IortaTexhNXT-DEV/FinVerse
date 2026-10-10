package com.iortatechnxt.brokerverse.renewal.candidate.service;

import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalStatusNames;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The columns of the bucket panels and lists that BDOI's FRS adds to a renewal row (FRRN.002.05,
 * FRRN.002.08): the renewal status in BDOI's names, the invoice number of the renewal, the
 * commission amount, the client's mailing address, the padlock of an account locked after posting
 * and placement, the effective date of the latest endorsement of the term and the remarks of the
 * latest return. Read for a whole chunk in one query.
 */
@Component
public class BucketColumnsReader {

  private static final String SQL =
      "select c.id, (select cl.address_line from crm_client cl where cl.id = c.client_id) as address,"
          + " (select a.status from acc_account a where a.arn = c.renewal_arn"
          + " and a.company_id = c.company_id) as account_status,"
          + " (select max(e.effective_date) from rnw_candidate_endorsement e"
          + " where e.candidate_id = c.id) as endorsed_on,"
          + " (select h.comment from wf_case w join wf_case_history h on h.case_id = w.id"
          + " where w.entity_type = 'RenewalCandidate' and w.entity_id = cast(c.id as varchar)"
          + " and h.action like 'return%' order by h.id desc limit 1) as return_remarks"
          + " from rnw_candidate c where c.id in (:ids)";

  private static final Set<String> PLACED = Set.of("PLACED", "POLICY_ISSUED", "BOOKED");
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final NamedParameterJdbcTemplate jdbc;
  private final RenewalStatusNames statusNames;

  /**
   * Creates the reader.
   *
   * @param jdbc SQL
   * @param statusNames status names
   */
  public BucketColumnsReader(NamedParameterJdbcTemplate jdbc, RenewalStatusNames statusNames) {
    this.jdbc = jdbc;
    this.statusNames = statusNames;
  }

  /**
   * The columns of a chunk of renewals.
   *
   * @param candidates renewals
   * @return columns by renewal id
   */
  public Map<Long, BucketColumns> read(List<RenewalCandidate> candidates) {
    Map<Long, BucketColumns> result = new HashMap<>();
    if (candidates.isEmpty()) {
      return result;
    }
    Map<Long, Map<String, Object>> rows = new HashMap<>();
    jdbc.queryForList(SQL, Map.of("ids", candidates.stream().map(RenewalCandidate::getId).toList()))
        .forEach(r -> rows.put(((Number) r.get("id")).longValue(), r));
    for (RenewalCandidate c : candidates) {
      Map<String, Object> r = rows.getOrDefault(c.getId(), Map.of());
      String accountStatus = (String) r.get("account_status");
      String placement = c.getPlacement().getStatus();
      result.put(
          c.getId(),
          new BucketColumns(
              statusNames.of(
                  c.getStage().name(),
                  c.getFlags().isReturned(),
                  placement == null ? accountStatus : placement),
              c.getRenewedInvoiceNo(),
              commission(c),
              (String) r.get("address"),
              accountStatus != null && PLACED.contains(accountStatus),
              r.get("endorsed_on") instanceof Date d ? d.toLocalDate() : null,
              (String) r.get("return_remarks")));
    }
    return result;
  }

  private static BigDecimal commission(RenewalCandidate c) {
    var p = c.getSnapshot().premium();
    if (p == null || p.basicPremium() == null || p.commissionRate() == null) {
      return null;
    }
    return p.basicPremium().multiply(p.commissionRate()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
  }

  /**
   * BDOI's columns of a renewal row.
   *
   * @param statusName renewal status in the names of the setting
   * @param invoiceNo invoice number of the renewal
   * @param commissionAmount commission amount of the expiring premium
   * @param mailingAddress client's mailing address
   * @param placementLocked locked after posting and placement (padlock)
   * @param endorsedOn effective date of the latest endorsement of the term
   * @param returnRemarks remarks of the latest return
   */
  public record BucketColumns(
      String statusName,
      String invoiceNo,
      BigDecimal commissionAmount,
      String mailingAddress,
      boolean placementLocked,
      LocalDate endorsedOn,
      String returnRemarks) {}
}
