package com.iortatechnxt.brokerverse.brokerclaims.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The loss experience of a cover (BRCLM.030/040, FR-CL-062/065; CLAIMS_BROKING_DESIGN 3.1 and 12.2,
 * decision D4): claim count, open claims, the status of each claim, paid, outstanding and total,
 * for an account (ARN) and optionally one policy year. Public read API of {@code brokerclaims}:
 * Renewal (With Claim Y/N, number and status of claims, the CLAIMS check, the Account History tab)
 * and the account page's Claims tab read the loss experience only through this service; nothing
 * depends on {@code brokerclaims} otherwise. The figures are those of the Loss Experience report
 * ({@link LossLines}).
 */
@Service
@Transactional(readOnly = true)
public class ClaimExperienceQueryService {

  private static final String CLAIMS =
      "select l.claim_id, l.claim_no, l.policy_year, l.loss_date, l.status_code,"
          + " coalesce(s.label, l.status_code) as status_label, l.phase, l.currency,"
          + " sum(l.paid) as paid, sum(l.outstanding) as outstanding from ("
          + LossLines.SQL
          + ") l left join lov_value s on s.type_code = 'BCL_CLAIM_STATUS'"
          + " and s.code = l.status_code"
          + " group by l.claim_id, l.claim_no, l.policy_year, l.loss_date, l.status_code, s.label,"
          + " l.phase, l.currency order by l.loss_date desc, l.claim_no";

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the service.
   *
   * @param jdbc named-parameter JDBC
   */
  public ClaimExperienceQueryService(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The loss experience of a cover.
   *
   * @param arn account reference number
   * @param policyYear policy year, null for every year of the account
   * @return counts, amounts and the claims (newest loss first)
   */
  public ClaimExperience summary(String arn, Integer policyYear) {
    Map<String, Object> args = new HashMap<>();
    args.put("companyId", null);
    args.put("arn", arn);
    args.put("policyYear", policyYear);
    List<ClaimLine> claims =
        jdbc.query(
            CLAIMS,
            args,
            (rs, n) ->
                new ClaimLine(
                    rs.getLong("claim_id"),
                    rs.getString("claim_no"),
                    rs.getInt("policy_year"),
                    rs.getDate("loss_date").toLocalDate(),
                    rs.getString("status_code"),
                    rs.getString("status_label"),
                    rs.getString("phase"),
                    rs.getString("currency"),
                    rs.getBigDecimal("paid"),
                    rs.getBigDecimal("outstanding")));
    BigDecimal paid = BigDecimal.ZERO;
    BigDecimal outstanding = BigDecimal.ZERO;
    int open = 0;
    Map<String, Integer> statuses = new LinkedHashMap<>();
    for (ClaimLine c : claims) {
      paid = paid.add(c.paid());
      outstanding = outstanding.add(c.outstanding());
      if (!"CLOSED".equals(c.phase())) {
        open++;
      }
      statuses.merge(c.statusLabel() == null ? c.phase() : c.statusLabel(), 1, Integer::sum);
    }
    return new ClaimExperience(
        arn,
        policyYear,
        claims.size(),
        open,
        paid,
        outstanding,
        paid.add(outstanding),
        statuses,
        new ArrayList<>(claims));
  }

  /**
   * The loss experience of a cover.
   *
   * @param arn account
   * @param policyYear policy year, null for all
   * @param claimCount claims
   * @param openCount outstanding claims (phase other than CLOSED)
   * @param paid amount paid (settled)
   * @param outstanding outstanding amount
   * @param total paid plus outstanding
   * @param statuses number of claims per status label
   * @param claims the claims
   */
  public record ClaimExperience(
      String arn,
      Integer policyYear,
      int claimCount,
      int openCount,
      BigDecimal paid,
      BigDecimal outstanding,
      BigDecimal total,
      Map<String, Integer> statuses,
      List<ClaimLine> claims) {

    /**
     * Whether the cover has a claim (Renewal "With Claim (Y/N)").
     *
     * @return true with at least one claim
     */
    public boolean withClaim() {
      return claimCount > 0;
    }
  }

  /**
   * One claim of the loss experience.
   *
   * @param claimId claim
   * @param claimNo claim number
   * @param policyYear policy year
   * @param lossDate loss date
   * @param statusCode status
   * @param statusLabel status label
   * @param phase phase
   * @param currency currency
   * @param paid amount paid
   * @param outstanding outstanding amount
   */
  public record ClaimLine(
      Long claimId,
      String claimNo,
      int policyYear,
      LocalDate lossDate,
      String statusCode,
      String statusLabel,
      String phase,
      String currency,
      BigDecimal paid,
      BigDecimal outstanding) {}
}
