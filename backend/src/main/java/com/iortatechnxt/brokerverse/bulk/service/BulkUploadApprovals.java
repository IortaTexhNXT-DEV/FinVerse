package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJobRepository;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJobStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Maker-checker of the uploads whose handler names an approval permission (the uploads of the
 * configuration screens): the uploader submits the validated upload, a second user holding the
 * permission approves it (the valid rows are then applied, one transaction each) or rejects it.
 */
@Service
public class BulkUploadApprovals {

  private static final String ENTITY = "BulkJob";

  private final BulkService bulk;
  private final BulkHandlerRegistry registry;
  private final BulkJobRepository jobs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param bulk bulk uploads
   * @param registry handlers
   * @param jobs jobs
   * @param audit audit trail
   * @param currentUser current user
   * @param txManager transaction manager
   * @param clock clock
   */
  public BulkUploadApprovals(
      BulkService bulk,
      BulkHandlerRegistry registry,
      BulkJobRepository jobs,
      AuditTrailService audit,
      CurrentUser currentUser,
      PlatformTransactionManager txManager,
      Clock clock) {
    this.bulk = bulk;
    this.registry = registry;
    this.jobs = jobs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.tx = new TransactionTemplate(txManager);
    this.clock = clock;
  }

  /**
   * Submits a validated upload for approval.
   *
   * @param jobId job
   * @return the job
   */
  public BulkJob submit(Long jobId) {
    return tx.execute(
        s -> {
          BulkJob job = bulk.job(jobId);
          BulkImportHandler handler = registry.get(job.getHandlerCode());
          if (handler.approvePermission() == null) {
            throw new BusinessRuleException(
                "BULK_NO_APPROVAL", "This upload is processed directly; it needs no approval");
          }
          if (job.getValidRows() == 0) {
            throw new BusinessRuleException(
                "BULK_NOTHING_TO_APPROVE", "The upload has no valid row to apply");
          }
          job.submit(currentUser.username(), clock.instant());
          audit.record(
              ENTITY,
              job.getJobNo(),
              AuditAction.SUBMIT,
              handler.title() + ": " + job.getValidRows() + " valid row(s) submitted for approval");
          return jobs.save(job);
        });
  }

  /**
   * Approves a submitted upload and applies its valid rows (a second user with the handler's
   * approval permission).
   *
   * @param jobId job
   * @param note remarks
   * @return the completed job
   */
  public BulkJob approve(Long jobId, String note) {
    BulkJob approved =
        required(
            tx.execute(
                s -> {
                  BulkJob job = bulk.job(jobId);
                  BulkImportHandler handler = registry.get(job.getHandlerCode());
                  requireApprover(handler);
                  job.approve(currentUser.username(), clock.instant(), note);
                  audit.record(ENTITY, job.getJobNo(), AuditAction.AUTHORIZE, "Approved for apply");
                  return jobs.save(job);
                }));
    return bulk.applyValidRows(approved, registry.get(approved.getHandlerCode()));
  }

  /**
   * Rejects a submitted upload; nothing is applied.
   *
   * @param jobId job
   * @param note reason
   * @return the job
   */
  public BulkJob reject(Long jobId, String note) {
    return tx.execute(
        s -> {
          BulkJob job = bulk.job(jobId);
          requireApprover(registry.get(job.getHandlerCode()));
          job.reject(currentUser.username(), clock.instant(), note);
          audit.record(ENTITY, job.getJobNo(), AuditAction.REJECT, "Rejected: " + note);
          return jobs.save(job);
        });
  }

  /**
   * Uploads waiting for approval of the handlers whose approval permission the user holds.
   *
   * @return jobs, oldest first
   */
  public List<BulkJob> waiting() {
    return jobs.findByStatusOrderByIdAsc(BulkJobStatus.SUBMITTED).stream()
        .filter(j -> registry.find(j.getHandlerCode()).map(this::mayApprove).orElse(false))
        .toList();
  }

  private boolean mayApprove(BulkImportHandler handler) {
    return handler.approvePermission() != null
        && currentUser.hasAuthority(handler.approvePermission());
  }

  private void requireApprover(BulkImportHandler handler) {
    if (!mayApprove(handler)) {
      throw new BusinessRuleException(
          "BULK_APPROVAL_NOT_ALLOWED", "You may not approve uploads of " + handler.title());
    }
  }

  /** The result of a transaction callback that always returns one. */
  private static <T> T required(T result) {
    if (result == null) {
      throw new IllegalStateException("The transaction returned no result");
    }
    return result;
  }
}
