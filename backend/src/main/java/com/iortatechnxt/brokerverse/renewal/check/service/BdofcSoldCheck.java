package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Check {@code BDOFC_SOLD} (FRRN.012.07): a renewal account matched by PN in an uploaded BDOFC or
 * BDOSOLD report is evaluated again and, by its severity (Review by default), held for Marketing
 * with the remarks of the report.
 */
@Component
public class BdofcSoldCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "BDOFC_SOLD";

  private static final String SQL =
      "select report_kind || ': ' || remarks from rnw_bdofc_line where candidate_id = :id"
          + " and matching_status = 'MATCHED' order by id desc limit 1";

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the check.
   *
   * @param jdbc lines of the reports
   */
  public BdofcSoldCheck(NamedParameterJdbcTemplate jdbc) {
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
    List<String> lines = jdbc.queryForList(SQL, Map.of("id", c.getId()), String.class);
    return lines.isEmpty()
        ? Verdict.pass("Not in a BDOFC or BDOSOLD report")
        : Verdict.fail("Listed in the " + lines.get(0), null);
  }
}
