package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJobRepository;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJobStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Uploads waiting for approval, in the approval inbox of the holders of their permission. */
@Component
public class BulkApprovalSource implements PendingApprovalSource {

  /** Screen where an upload waiting for approval is reviewed and decided. */
  public static final String LINK = "/admin/config-uploads?job=";

  private final BulkJobRepository jobs;
  private final BulkHandlerRegistry registry;

  /**
   * Creates the source.
   *
   * @param jobs jobs
   * @param registry handlers
   */
  public BulkApprovalSource(BulkJobRepository jobs, BulkHandlerRegistry registry) {
    this.jobs = jobs;
    this.registry = registry;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> pending = new ArrayList<>();
    for (BulkJob job : jobs.findByStatusOrderByIdAsc(BulkJobStatus.SUBMITTED)) {
      Optional<BulkImportHandler> handler = registry.find(job.getHandlerCode());
      if (handler.isEmpty()
          || handler.get().approvePermission() == null
          || !viewer.can(handler.get().approvePermission())
          || !viewer.mayApproveItemOf(job.getCreatedBy())
          || !viewer.mayApproveItemOf(job.getSubmittedBy())) {
        continue;
      }
      pending.add(
          new PendingApproval(
              "CONFIGURATION",
              handler.get().title(),
              job.getJobNo(),
              job.getValidRows() + " row(s) of " + job.getFileName(),
              null,
              null,
              job.getSubmittedBy(),
              job.getSubmittedAt(),
              job.getCompanyId(),
              LINK + job.getId()));
    }
    return pending;
  }
}
