package com.iortatechnxt.brokerverse.renewal.candidate.api;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateDtos.CandidateRow;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateDtos.MoneyColumns;
import com.iortatechnxt.brokerverse.renewal.check.service.ClaimsCheck;
import com.iortatechnxt.brokerverse.renewal.check.service.OutstandingPremiumCheck;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotPremium;
import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResultRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalScope.Scope;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Maps renewals to list rows: the premium columns come from the snapshot, the outstanding premium
 * and the claims from the latest check run, read for a whole chunk in one query. LAMD and Contact
 * Center see the rows without the premium columns (RQ21).
 */
@Component
public class CandidateRowMapper {

  private static final Pattern CLAIMS = Pattern.compile("claims (\\d+), open (\\d+)");
  private static final List<String> MONEY_CHECKS =
      List.of(OutstandingPremiumCheck.CODE, ClaimsCheck.CODE);

  private final CheckResultRepository results;
  private final Clock clock;

  /**
   * Creates the mapper.
   *
   * @param results check results
   * @param clock clock
   */
  public CandidateRowMapper(CheckResultRepository results, Clock clock) {
    this.results = results;
    this.clock = clock;
  }

  /**
   * Maps a chunk.
   *
   * @param candidates renewals
   * @param scope data scope of the user
   * @return rows in the same order
   */
  public List<CandidateRow> rows(List<RenewalCandidate> candidates, Scope scope) {
    LocalDate today = BusinessClock.today(clock);
    if (scope.hidePremium()) {
      return candidates.stream()
          .map(c -> CandidateRow.of(c, MoneyColumns.HIDDEN, c.daysToExpiry(today)))
          .toList();
    }
    Map<Long, Map<String, CheckResult>> latest = latest(candidates);
    return candidates.stream()
        .map(
            c ->
                CandidateRow.of(
                    c, money(c, latest.getOrDefault(c.getId(), Map.of())), c.daysToExpiry(today)))
        .toList();
  }

  /**
   * Maps one renewal.
   *
   * @param candidate renewal
   * @param scope data scope of the user
   * @return row
   */
  public CandidateRow row(RenewalCandidate candidate, Scope scope) {
    return rows(List.of(candidate), scope).get(0);
  }

  private Map<Long, Map<String, CheckResult>> latest(List<RenewalCandidate> candidates) {
    List<Long> runs =
        candidates.stream()
            .map(RenewalCandidate::getLastCheckRunId)
            .filter(Objects::nonNull)
            .toList();
    Map<Long, Map<String, CheckResult>> byCandidate = new HashMap<>();
    if (runs.isEmpty()) {
      return byCandidate;
    }
    for (CheckResult r : results.findByRunIdInAndCheckCodeIn(runs, MONEY_CHECKS)) {
      byCandidate
          .computeIfAbsent(r.getCandidateId(), k -> new HashMap<>())
          .put(r.getCheckCode(), r);
    }
    return byCandidate;
  }

  private static MoneyColumns money(RenewalCandidate c, Map<String, CheckResult> checks) {
    SnapshotPremium p = c.getSnapshot().premium();
    CheckResult outstanding = checks.get(OutstandingPremiumCheck.CODE);
    CheckResult claims = checks.get(ClaimsCheck.CODE);
    Integer claimCount = null;
    String claimStatus = null;
    if (claims != null) {
      claimStatus = claims.getMessage();
      Matcher m = claims.getDetail() == null ? null : CLAIMS.matcher(claims.getDetail());
      claimCount = m != null && m.find() ? Integer.valueOf(m.group(1)) : null;
    }
    return new MoneyColumns(
        p == null ? null : p.currency(),
        p == null ? null : p.basicPremium(),
        p == null ? null : p.grossPremium(),
        p == null ? null : p.totalSumInsured(),
        p == null ? null : p.premiumRate(),
        p == null ? null : p.commissionRate(),
        outstanding(outstanding),
        claimCount,
        claimStatus);
  }

  private static BigDecimal outstanding(CheckResult r) {
    if (r == null || r.getOutcome() != CheckOutcome.FAIL || r.getDetail() == null) {
      return r == null || r.getOutcome() == CheckOutcome.NOT_APPLICABLE ? null : BigDecimal.ZERO;
    }
    try {
      return new BigDecimal(r.getDetail());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
