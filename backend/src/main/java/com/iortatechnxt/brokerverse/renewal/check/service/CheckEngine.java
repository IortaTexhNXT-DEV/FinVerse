package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.BucketHistory;
import com.iortatechnxt.brokerverse.renewal.domain.BucketHistoryRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResultRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CheckRun;
import com.iortatechnxt.brokerverse.renewal.domain.CheckRunRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSetting;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSettingRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSeverity;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.DispositionSource;
import com.iortatechnxt.brokerverse.renewal.domain.OverrideKind;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalOverride;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalOverrideRepository;
import com.iortatechnxt.brokerverse.renewal.extraction.service.SnapshotRefresher;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the checks of a renewal (BRRN.020/023; RENEWAL_DESIGN section 8): every active check bean
 * with the severity of its setting, the bucket from the active rule set, the check run and results,
 * the bucket history, the flag chips and the system action the checks call for. Runs after the
 * extraction, at initiation, after every upload and event, and nightly; records move between
 * buckets as conditions change (BRRN.023 AC 3). An active bucket override holds while the computed
 * bucket stays the one it overrode.
 */
@Service
@Transactional
public class CheckEngine {

  private final List<RenewalCheck> checks;
  private final CheckSettingRepository settings;
  private final CheckContextFactory contexts;
  private final BucketRules bucketRules;
  private final CheckRunRepository runs;
  private final CheckResultRepository results;
  private final BucketHistoryRepository history;
  private final RenewalOverrideRepository overrides;
  private final RenewalParameters parameters;
  private final SnapshotRefresher snapshots;
  private final Clock clock;

  /**
   * Creates the engine.
   *
   * @param checks every check bean
   * @param settings check settings
   * @param contexts check contexts
   * @param bucketRules bucket rules
   * @param runs check runs
   * @param results check results
   * @param history bucket history
   * @param overrides overrides
   * @param parameters renewal parameters (RMU unit)
   * @param snapshots snapshot refresh of booked renewals
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public CheckEngine(
      List<RenewalCheck> checks,
      CheckSettingRepository settings,
      CheckContextFactory contexts,
      BucketRules bucketRules,
      CheckRunRepository runs,
      CheckResultRepository results,
      BucketHistoryRepository history,
      RenewalOverrideRepository overrides,
      RenewalParameters parameters,
      SnapshotRefresher snapshots,
      Clock clock) {
    this.checks = checks.stream().sorted(Comparator.comparing(RenewalCheck::code)).toList();
    this.settings = settings;
    this.contexts = contexts;
    this.bucketRules = bucketRules;
    this.runs = runs;
    this.results = results;
    this.history = history;
    this.overrides = overrides;
    this.parameters = parameters;
    this.snapshots = snapshots;
    this.clock = clock;
  }

  /**
   * Runs the checks of a saved renewal.
   *
   * @param candidate candidate
   * @param trigger what ran them
   * @return evaluation
   */
  public Evaluation run(RenewalCandidate candidate, CheckTrigger trigger) {
    LocalDate today = BusinessClock.today(clock);
    Instant now = clock.instant();
    snapshots.refresh(candidate);
    CheckContext context = contexts.of(candidate, today);
    Map<String, CheckSetting> byCode =
        settings.findAll().stream()
            .collect(Collectors.toMap(CheckSetting::getCheckCode, Function.identity()));
    List<Finding> findings = new ArrayList<>();
    for (RenewalCheck check : checks) {
      CheckSetting setting = byCode.get(check.code());
      if (setting != null && !setting.isEnabled()) {
        continue;
      }
      RenewalCheck.Verdict v = check.evaluate(context);
      CheckSeverity severity = setting == null ? CheckSeverity.FAIL_REVIEW : setting.getSeverity();
      findings.add(new Finding(check.code(), v.outcome(), severity, v.message(), v.detail()));
    }
    BucketRules.Decision decision =
        pinned(
            candidate,
            bucketRules.bucketOf(
                candidate.getCompanyId(), findings, today, pinnedVersion(candidate)));
    Bucket before = candidate.getBucket();
    CheckRun run =
        runs.save(
            new CheckRun(
                candidate,
                trigger,
                now,
                new CheckRun.Outcome(
                    before,
                    decision.bucket(),
                    decision.ruleSetVersion(),
                    (int) findings.stream().filter(Finding::failed).count())));
    findings.forEach(
        f ->
            results.save(
                new CheckResult(
                    run, f.code(), f.outcome(), f.severity(), f.message(), f.detail())));
    if (before != decision.bucket()) {
      history.save(
          new BucketHistory(
              candidate.getId(),
              new BucketHistory.Change(before, decision.bucket()),
              new BucketHistory.RuleRef(decision.ruleSetVersion(), decision.ruleId(), run.getId()),
              BucketHistory.Cause.RULE,
              null));
    }
    candidate.evaluated(decision.bucket(), decision.ruleSetVersion(), run.getId(), now);
    Evaluation evaluation =
        new Evaluation(decision.bucket(), run.getId(), findings, systemTag(findings));
    flags(candidate, evaluation, now);
    endPassedOverrides(candidate, evaluation);
    return evaluation;
  }

  /**
   * The rule set version of a renewal evaluated since its initiation: a new version applies only to
   * renewals not yet initiated (FR-RN-112 R3). The run at initiation takes the active version.
   */
  private static Integer pinnedVersion(RenewalCandidate c) {
    boolean evaluatedSinceInitiation =
        c.getInitiatedAt() != null
            && c.getEvaluatedAt() != null
            && c.getEvaluatedAt().isAfter(c.getInitiatedAt());
    return evaluatedSinceInitiation ? c.getBucketRuleVersion() : null;
  }

  /** An active bucket override holds while the computed bucket is still the one it replaced. */
  private BucketRules.Decision pinned(RenewalCandidate candidate, BucketRules.Decision computed) {
    Optional<RenewalOverride> pin =
        overrides.findByCandidateIdAndActiveTrue(candidate.getId()).stream()
            .filter(o -> o.getKind() == OverrideKind.BUCKET)
            .findFirst();
    if (pin.isEmpty()) {
      return computed;
    }
    if (computed.bucket().name().equals(pin.get().getFromValue())) {
      return new BucketRules.Decision(
          Bucket.valueOf(pin.get().getToValue()), computed.ruleSetVersion(), computed.ruleId());
    }
    pin.get().end();
    return computed;
  }

  private Optional<Evaluation.SystemTag> systemTag(List<Finding> findings) {
    return findings.stream()
        .filter(f -> f.failed() && f.severity() == CheckSeverity.SYSTEM)
        .map(this::tagOf)
        .flatMap(Optional::stream)
        .findFirst();
  }

  private Optional<Evaluation.SystemTag> tagOf(Finding f) {
    if (RiskCodeRenewableCheck.CODE.equals(f.code())) {
      return Optional.of(
          new Evaluation.SystemTag(
              parameters.riskCodeReason(), DispositionSource.SYSTEM_CHECK, f.message(), null));
    }
    if (TotalLossCheck.CODE.equals(f.code())) {
      return Optional.of(
          new Evaluation.SystemTag(
              RenewalCodes.REASON_TOTAL_LOSS, DispositionSource.SYSTEM_CHECK, f.message(), null));
    }
    if (LamdStatusCheck.CODE.equals(f.code())) {
      boolean paidOff = "PAID_OFF".equals(f.detail());
      return Optional.of(
          new Evaluation.SystemTag(
              paidOff ? RenewalCodes.REASON_LOAN_PAID : RenewalCodes.REASON_RMU,
              DispositionSource.LAMD,
              f.message(),
              paidOff ? null : parameters.rmuUnit().orElse(null)));
    }
    return Optional.empty();
  }

  private static void flags(RenewalCandidate candidate, Evaluation e, Instant now) {
    boolean kyc =
        e.finding(KycDueCheck.CODE).map(f -> f.outcome() == CheckOutcome.INFO).orElse(false);
    boolean claims =
        e.finding(ClaimsCheck.CODE).map(f -> f.failed() || f.detail() != null).orElse(false);
    boolean endorsement =
        e.finding(EndorsementPendingCheck.CODE).map(Finding::failed).orElse(false);
    boolean outstanding =
        e.finding(OutstandingPremiumCheck.CODE).map(Finding::failed).orElse(false);
    candidate.getFlags().fromChecks(kyc, claims, endorsement, outstanding, now);
  }

  /** A check override ends once the check passes (a later failure needs a new override). */
  private void endPassedOverrides(RenewalCandidate candidate, Evaluation e) {
    for (RenewalOverride o : overrides.findByCandidateIdAndActiveTrue(candidate.getId())) {
      String code = BlockingChecks.checkOf(o);
      if (code != null && e.finding(code).map(f -> !f.failed()).orElse(true)) {
        o.end();
      }
    }
  }
}
