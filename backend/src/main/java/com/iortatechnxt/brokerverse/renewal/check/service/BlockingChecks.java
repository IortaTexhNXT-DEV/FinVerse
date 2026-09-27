package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResultRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSeverity;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalOverride;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalOverrideRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The blocking checks of a renewal (RENEWAL_DESIGN section 7.1; BRRN.023/031/032/035): posting, the
 * Renewal Advice and the acceptance are refused while a check of the latest run fails with a bucket
 * severity, unless that check is overridden with remarks.
 */
@Component
@Transactional(readOnly = true)
public class BlockingChecks {

  private final CheckResultRepository results;
  private final RenewalOverrideRepository overrides;

  /**
   * Creates the guard.
   *
   * @param results check results
   * @param overrides overrides
   */
  public BlockingChecks(CheckResultRepository results, RenewalOverrideRepository overrides) {
    this.results = results;
    this.overrides = overrides;
  }

  /**
   * The failing checks of the latest run that are not overridden.
   *
   * @param candidate candidate
   * @return failing results
   */
  public List<CheckResult> failing(RenewalCandidate candidate) {
    if (candidate.getLastCheckRunId() == null) {
      return List.of();
    }
    Set<String> overridden =
        overrides.findByCandidateIdAndActiveTrue(candidate.getId()).stream()
            .map(BlockingChecks::checkOf)
            .filter(c -> c != null)
            .collect(Collectors.toSet());
    return results.findByRunIdOrderByIdAsc(candidate.getLastCheckRunId()).stream()
        .filter(CheckResult::failed)
        .filter(
            r -> r.getSeverity() != CheckSeverity.INFO && r.getSeverity() != CheckSeverity.SYSTEM)
        .filter(r -> !overridden.contains(r.getCheckCode()))
        .toList();
  }

  /**
   * Refuses an action while a check blocks it.
   *
   * @param candidate candidate
   * @param action what is refused, e.g. "be posted"
   */
  public void require(RenewalCandidate candidate, String action) {
    List<CheckResult> failing = failing(candidate);
    if (!failing.isEmpty()) {
      throw new BusinessRuleException(
          "RNW_CHECK_BLOCKING",
          "Renewal "
              + candidate.getRenewalRef()
              + " cannot "
              + action
              + ": "
              + failing.stream().map(CheckResult::getMessage).collect(Collectors.joining("; ")));
    }
  }

  /**
   * The check an override covers.
   *
   * @param override override
   * @return check code, null when the override does not cover a check
   */
  public static String checkOf(RenewalOverride override) {
    return switch (override.getKind()) {
      case CHECK -> override.getCheckCode();
      case OUTSTANDING_BALANCE -> OutstandingPremiumCheck.CODE;
      case INSURER_MISMATCH -> InsurerResponseMatchCheck.CODE;
      default -> null;
    };
  }
}
