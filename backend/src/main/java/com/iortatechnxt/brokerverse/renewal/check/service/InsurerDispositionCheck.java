package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Check {@code INSURER_DISPOSITION} (FRRN.013.02): the latest insurer disposition of the renewal
 * account from an insurer disposition file; a disposition other than Renew As Is / Approved or
 * Total Loss (Rejected, Renew with surcharge, Renew under another package, the non-renewal causes,
 * Apply minimum premium) holds the account in Review for the disposition of Marketing.
 */
@Component
public class InsurerDispositionCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "INSURER_DISPOSITION";

  private static final String SQL =
      "select m.outcome, m.label from rnw_insurer_disposition_line l"
          + " join rnw_insurer_disposition_map m on m.code = l.disposition_code"
          + " where l.candidate_id = :id order by l.id desc limit 1";

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the check.
   *
   * @param jdbc insurer dispositions
   */
  public InsurerDispositionCheck(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    RenewalCandidate c = context.candidate();
    if (c.getId() == null) {
      return Verdict.notApplicable("Not saved yet");
    }
    List<String[]> latest =
        jdbc.query(
            SQL,
            Map.of("id", c.getId()),
            (rs, i) -> new String[] {rs.getString("outcome"), rs.getString("label")});
    if (latest.isEmpty()) {
      return Verdict.notApplicable("No insurer disposition received");
    }
    String[] d = latest.get(0);
    return "REVIEW".equals(d[0])
        ? Verdict.fail("Insurer disposition: " + d[1], d[1])
        : Verdict.pass("Insurer disposition: " + d[1]);
  }
}
