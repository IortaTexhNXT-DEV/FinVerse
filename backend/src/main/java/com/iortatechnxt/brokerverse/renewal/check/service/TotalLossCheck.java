package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Total loss claims of the expiring term (BDOI Renewal FRS FRRN.007.02, FRRN.008.01): a claim of
 * the expiring account settled with a total-loss settlement type ({@value #SETTLEMENT_TYPES})
 * classifies the renewal Not for Renewal with the reason Total Loss Claim, in the Non-Renewable
 * panel. A system check: it tags the renewal and does not change its bucket.
 */
@Component
public class TotalLossCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "TOTAL_LOSS";

  /** Parameter: settlement types of a total loss. */
  public static final String SETTLEMENT_TYPES = "RNW_TOTAL_LOSS_SETTLEMENT_TYPES";

  private static final String SQL =
      "select c.claim_no from bcl_claim c where c.arn = :arn and c.policy_year = :year"
          + " and c.settlement_type_code = any(cast(:types as varchar[])) order by c.claim_no";

  private final NamedParameterJdbcTemplate jdbc;
  private final SystemParameterService parameters;

  /**
   * Creates the check.
   *
   * @param jdbc SQL
   * @param parameters system parameters
   */
  public TotalLossCheck(NamedParameterJdbcTemplate jdbc, SystemParameterService parameters) {
    this.jdbc = jdbc;
    this.parameters = parameters;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    RenewalCandidate c = context.candidate();
    List<String> types = parameters.items(SETTLEMENT_TYPES);
    if (c.getExpiringArn() == null || c.getPolicyYear() == null || types.isEmpty()) {
      return Verdict.notApplicable("No BIBS account or no total-loss settlement type");
    }
    List<String> claims =
        jdbc.queryForList(
            SQL,
            Map.of(
                "arn",
                c.getExpiringArn(),
                "year",
                c.getPolicyYear(),
                "types",
                types.stream().map(String::strip).toArray(String[]::new)),
            String.class);
    return claims.isEmpty()
        ? Verdict.pass("No total loss claim on the expiring term")
        : Verdict.fail("Total loss claim " + String.join(", ", claims), String.join(",", claims));
  }
}
