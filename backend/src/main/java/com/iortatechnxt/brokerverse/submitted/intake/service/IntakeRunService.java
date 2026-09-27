package com.iortatechnxt.brokerverse.submitted.intake.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJobRepository;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIntakeRun;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIntakeRun.FileFacts;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIntakeRunRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmProcessingService;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmProcessingService.RunRequest;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRun;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Intake runs (BRIDSP-01, 09; FRS FR-SP-001, 021): one run per source file, opened by its first
 * committed row and counted row by row; once the rows are committed the run closes, the handlers
 * are told of the new submissions, failures raise {@code SBM_INTAKE_FAILED}, and a processing run
 * starts for the records of the file (when approved rule sets are in force; otherwise the daily
 * run takes them).
 */
@Service
@Transactional
public class IntakeRunService {

  private final SbmIntakeRunRepository runs;
  private final SbmPolicyRepository policies;
  private final BulkJobRepository jobs;
  private final SbmProcessingService processing;
  private final NotificationService notifications;
  private final AlertService alerts;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param runs intake runs
   * @param policies masterlist
   * @param jobs bulk uploads (file name and hash)
   * @param processing processing runs
   * @param notifications notifications
   * @param alerts alerts
   * @param numbers document numbers
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the intake
  public IntakeRunService(
      SbmIntakeRunRepository runs,
      SbmPolicyRepository policies,
      BulkJobRepository jobs,
      SbmProcessingService processing,
      NotificationService notifications,
      AlertService alerts,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.runs = runs;
    this.policies = policies;
    this.jobs = jobs;
    this.processing = processing;
    this.notifications = notifications;
    this.alerts = alerts;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The run of an upload, opened by its first row.
   *
   * @param context bulk context (company and upload number)
   * @param sourceCode source
   * @return run
   */
  public SbmIntakeRun runOf(BulkContext context, String sourceCode) {
    return runs.findByCompanyIdAndBulkJobNo(context.companyId(), context.jobNo())
        .orElseGet(() -> open(context, sourceCode));
  }

  private SbmIntakeRun open(BulkContext context, String sourceCode) {
    Optional<BulkJob> job = jobs.findByJobNo(context.jobNo());
    SbmIntakeRun run =
        runs.save(
            new SbmIntakeRun(
                context.companyId(),
                numbers.next("SBI-" + BusinessClock.today(clock).getYear()),
                sourceCode,
                new FileFacts(
                    context.jobNo(),
                    job.map(BulkJob::getFileName).orElse(null),
                    job.map(BulkJob::getFileSha256).orElse(null)),
                clock.instant()));
    audit.record("SubmittedIntakeRun", run.getRunNo(), AuditAction.CREATE, "Intake of " + sourceCode);
    return run;
  }

  /**
   * Closes the run of an upload and starts the processing of its records.
   *
   * @param context bulk context
   * @param failed rows that failed at commit
   * @return the run, empty when no row was committed
   */
  public Optional<SbmIntakeRun> complete(BulkContext context, int failed) {
    Optional<SbmIntakeRun> found =
        runs.findByCompanyIdAndBulkJobNo(context.companyId(), context.jobNo());
    found.ifPresent(run -> close(run, failed));
    return found;
  }

  private void close(SbmIntakeRun run, int failed) {
    List<Long> ids =
        policies.findByIntakeRunIdOrderByIdAsc(run.getId()).stream()
            .filter(p -> p.getStatus().isProcessable())
            .map(SbmPolicy::getId)
            .toList();
    String processingRunNo = null;
    if (!ids.isEmpty() && processing.hasActiveRules(run.getCompanyId())) {
      SbmRun processed =
          processing.run(
              new RunRequest(
                  run.getCompanyId(),
                  SbmRun.Trigger.INTAKE,
                  "Intake " + run.getRunNo(),
                  ids));
      processingRunNo = processed.getRunNo();
    }
    run.complete(failed, clock.instant(), processingRunNo);
    if (run.getCreated() > 0) {
      notifications.notifyPermission(
          "SBM_INTAKE",
          new Notice(
              run.getCreated() + " new submitted policies",
              "Intake " + run.getRunNo() + " of " + run.getFileName(),
              "/submitted/intake",
              "SubmittedIntakeRun",
              run.getRunNo()),
          SubmittedCodes.EVT_NEW_SUBMISSION);
    }
    if (failed > 0) {
      alerts.raise(
          "SBM_INTAKE_FAILED",
          new AlertFacts(
              run.getCompanyId(),
              null,
              "SubmittedIntakeRun",
              run.getRunNo(),
              failed + " rows of " + run.getFileName() + " could not be loaded",
              BigDecimal.valueOf(failed),
              "SBM_INTAKE_FAILED:" + run.getRunNo()));
    }
  }

  /**
   * Intake runs, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return runs
   */
  @Transactional(readOnly = true)
  public Page<SbmIntakeRun> list(Long companyId, Pageable pageable) {
    return runs.findByCompanyIdOrderByIdDesc(companyId, pageable);
  }
}
