package com.iortatechnxt.brokerverse.renewal.epolicy.service;

import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Whether a renewal account can take an e-policy number (FRRN.033.01, FRRN.033.05): an active
 * renewal account submitted for placement or booked, with a policy number that no other active
 * renewal account already has.
 */
@Component
public class EpolicyEligibility {

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the check.
   *
   * @param jdbc duplicate policy numbers
   */
  public EpolicyEligibility(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The problem with an e-policy number for an account.
   *
   * @param c renewal
   * @param policyNo e-policy number
   * @return problem, null when it can be recorded
   */
  public String problem(RenewalCandidate c, String policyNo) {
    if (policyNo == null || policyNo.isBlank()) {
      return "E-Policy Number is required";
    }
    if (c.getStage() != RenewalStage.FOR_PLACEMENT_BOOKING
        && c.getStage() != RenewalStage.RENEWED) {
      return "The renewal account is not eligible for e-policy updating";
    }
    List<String> others =
        jdbc.queryForList(
            "select renewal_ref from rnw_candidate where company_id = :companyId"
                + " and epolicy_no = :no and id <> :id and stage <> 'CLOSED' order by renewal_ref",
            Map.of("companyId", c.getCompanyId(), "no", policyNo.strip(), "id", c.getId()),
            String.class);
    return others.isEmpty()
        ? null
        : "Duplicate Policy Number: " + policyNo.strip() + " already exists in " + others.get(0);
  }
}
