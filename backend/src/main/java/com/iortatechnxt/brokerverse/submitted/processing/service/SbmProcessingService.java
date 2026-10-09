package com.iortatechnxt.brokerverse.submitted.processing.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleSet;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleSetRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleSetStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRun;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRunRepository;
import com.iortatechnxt.brokerverse.submitted.processing.service.PolicyProcessor.Processed;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processing runs of the masterlist (BRIDSP-09, 10; FRS FR-SP-021, 022): started by an intake, by
 * the daily job or by a user for a scope of records; each record goes through the steps of {@link
 * PolicyProcessor}. After the records, the handlers of the moved records are told of the new
 * buckets ({@code SBM_BUCKET_CHANGED}), the Sanitation Handlers of the fallout ({@code
 * SBM_FALLOUT}), which also raises the alert, and the handlers of breached records get a TOR task.
 */
@Service
@Transactional
public class SbmProcessingService {

  private static final String RUN_ENTITY = "SubmittedRun";

  private final SbmRunRepository runs;
  private final SbmPolicyRepository policies;
  private final SbmRuleSetRepository ruleSets;
  private final SbmRuleRepository rules;
  private final PolicyProcessor processor;
  private final LovService lovs;
  private final NotificationService notifications;
  private final AlertService alerts;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param runs runs
   * @param policies masterlist
   * @param ruleSets rule sets
   * @param rules rules
   * @param processor record processor
   * @param lovs lists of values (bucket actions)
   * @param notifications notifications
   * @param alerts alerts
   * @param numbers document numbers
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the run
  public SbmProcessingService(
      SbmRunRepository runs,
      SbmPolicyRepository policies,
      SbmRuleSetRepository ruleSets,
      SbmRuleRepository rules,
      PolicyProcessor processor,
      LovService lovs,
      NotificationService notifications,
      AlertService alerts,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.runs = runs;
    this.policies = policies;
    this.ruleSets = ruleSets;
    this.rules = rules;
    this.processor = processor;
    this.lovs = lovs;
    this.notifications = notifications;
    this.alerts = alerts;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Runs the steps for a scope of records in the caller's transaction.
   *
   * @param request company, trigger, scope and records
   * @return the finished run
   */
  public SbmRun run(RunRequest request) {
    SbmRun run = start(request);
    process(run.getId(), request.policyIds());
    return finish(run.getId());
  }

  /**
   * Starts a run.
   *
   * @param request company, trigger and scope
   * @return the run
   */
  public SbmRun start(RunRequest request) {
    SbmRun run =
        runs.save(
            new SbmRun(
                request.companyId(),
                numbers.next("SBR-" + BusinessClock.today(clock).getYear()),
                request.trigger(),
                request.scope(),
                clock.instant()));
    audit.record(RUN_ENTITY, run.getRunNo(), AuditAction.RUN, request.scope());
    return run;
  }

  /**
   * Processes records of a run (one chunk).
   *
   * @param runId run
   * @param policyIds records; those not processable any more are skipped
   */
  public void process(Long runId, List<Long> policyIds) {
    SbmRun run = require(runId);
    LocalDate today = BusinessClock.today(clock);
    RunContext ctx =
        new RunContext(run, activeRules(run.getCompanyId(), today), today, bucketActions(today));
    Map<String, Integer> movedByHandler = new TreeMap<>();
    for (SbmPolicy p : policies.findAllById(policyIds)) {
      if (!p.getStatus().isProcessable() || !p.getCompanyId().equals(run.getCompanyId())) {
        continue;
      }
      boolean wasFlagged = p.isInsurerApprovalRequired();
      Processed done = processor.process(ctx, p);
      run.count(done.outcome(), done.breached());
      if (done.bucketChanged() && p.getHandlerUsername() != null) {
        movedByHandler.merge(p.getHandlerUsername(), 1, Integer::sum);
      }
      if (done.breached() && !wasFlagged) {
        torTask(p);
      }
    }
    movedByHandler.forEach((handler, count) -> notifyMoved(run, handler, count));
  }

  /**
   * Finishes a run: fallout notice and alert.
   *
   * @param runId run
   * @return the run
   */
  public SbmRun finish(Long runId) {
    SbmRun run = require(runId);
    run.finish(clock.instant());
    if (run.getFallout() > 0) {
      notifications.notifyPermission(
          "SBM_PROCESS",
          new Notice(
              "Processing run " + run.getRunNo() + ": " + run.getFallout() + " in fallout",
              "Open the run to resolve the fallout.",
              "/submitted/runs/" + run.getId(),
              RUN_ENTITY,
              run.getRunNo()),
          SubmittedCodes.EVT_FALLOUT);
      alerts.raise(
          "SBM_FALLOUT",
          new AlertFacts(
              run.getCompanyId(),
              null,
              RUN_ENTITY,
              run.getRunNo(),
              DisplayFormat.countOf(run.getFallout(), "submitted policy", "submitted policies")
                  + " in fallout after run "
                  + run.getRunNo(),
              BigDecimal.valueOf(run.getFallout()),
              "SBM_FALLOUT:" + run.getRunNo()));
    }
    return run;
  }

  /**
   * A run by id.
   *
   * @param runId run
   * @return run
   */
  @Transactional(readOnly = true)
  public SbmRun require(Long runId) {
    return runs.findById(runId).orElseThrow(() -> new ResourceNotFoundException("Run", runId));
  }

  /**
   * The ids of the records a scheduled run takes: every record not yet in the renewal.
   *
   * @param companyId company
   * @return record ids
   */
  @Transactional(readOnly = true)
  public List<Long> scheduledScope(Long companyId) {
    return policies
        .findByCompanyIdAndStatusInOrderByIdAsc(
            companyId,
            List.of(
                SbmPolicyStatus.RECEIVED,
                SbmPolicyStatus.VALIDATED,
                SbmPolicyStatus.CLASSIFIED,
                SbmPolicyStatus.IN_REVIEW,
                SbmPolicyStatus.FOR_MANUAL_DISPOSITION))
        .stream()
        .map(SbmPolicy::getId)
        .toList();
  }

  /**
   * Whether approved rule sets are in force for a company (a run needs them).
   *
   * @param companyId company
   * @return true when a run can start
   */
  @Transactional(readOnly = true)
  public boolean hasActiveRules(Long companyId) {
    LocalDate today = BusinessClock.today(clock);
    return ruleSets.findByStatusInOrderByIdAsc(List.of(SbmRuleSetStatus.ACTIVE)).stream()
        .anyMatch(s -> s.getCompanyId().equals(companyId) && !s.getEffectiveFrom().isAfter(today));
  }

  private ActiveRules activeRules(Long companyId, LocalDate today) {
    List<SbmRuleSet> sets =
        ruleSets.findByStatusInOrderByIdAsc(List.of(SbmRuleSetStatus.ACTIVE)).stream()
            .filter(s -> s.getCompanyId().equals(companyId))
            .filter(s -> !s.getEffectiveFrom().isAfter(today))
            .toList();
    if (sets.isEmpty()) {
      throw new BusinessRuleException(
          "SBM_NO_ACTIVE_RULES",
          "No approved rule set is in force; approve the rule sets in Submitted Policies Setup first");
    }
    List<SbmRule> all = rules.findByRuleSetIdIn(sets.stream().map(SbmRuleSet::getId).toList());
    return ActiveRules.of(sets, all);
  }

  private Map<String, String> bucketActions(LocalDate today) {
    return lovs.activeValues(SubmittedCodes.LOV_BUCKET, today).stream()
        .filter(v -> v.getParentCode() != null)
        .collect(Collectors.toMap(LovValue::getCode, LovValue::getParentCode, (a, b) -> a));
  }

  private void notifyMoved(SbmRun run, String handler, int count) {
    notifications.notifyUser(
        handler,
        new Notice(
            count + " of your submitted policies changed bucket",
            "Processing run " + run.getRunNo(),
            "/submitted/masterlist",
            RUN_ENTITY,
            run.getRunNo()),
        SubmittedCodes.EVT_BUCKET_CHANGED);
  }

  private void torTask(SbmPolicy p) {
    String owner = p.getAoUsername() != null ? p.getAoUsername() : p.getHandlerUsername();
    Notice notice =
        new Notice(
            "Terms of Reference needed for " + p.getSbmNo(),
            p.getAssured().assuredName() + ": the policy exceeds the insurer limits",
            SubmittedCodes.link(p.getId()),
            SubmittedCodes.ENTITY,
            p.getId().toString());
    if (owner != null) {
      notifications.notifyUser(owner, notice);
    } else {
      notifications.notifyPermission("TOR_PREPARE", notice);
    }
  }

  /**
   * A run to start.
   *
   * @param companyId company
   * @param trigger trigger
   * @param scope scope description
   * @param policyIds records
   */
  public record RunRequest(
      Long companyId, SbmRun.Trigger trigger, String scope, List<Long> policyIds) {

    /** Defensive copy. */
    public RunRequest {
      policyIds = List.copyOf(policyIds);
    }
  }
}
