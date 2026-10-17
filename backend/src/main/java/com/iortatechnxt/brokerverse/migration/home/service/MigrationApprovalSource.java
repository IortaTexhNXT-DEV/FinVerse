package com.iortatechnxt.brokerverse.migration.home.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchStatus;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.migration.load.domain.Resubmission;
import com.iortatechnxt.brokerverse.migration.load.domain.ResubmissionRepository;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapVersion;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapVersionRepository;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MapVersionStatus;
import com.iortatechnxt.brokerverse.migration.object.domain.MigObjectDecision;
import com.iortatechnxt.brokerverse.migration.object.domain.MigObjectDecisionRepository;
import com.iortatechnxt.brokerverse.migration.trueup.domain.MigTrueup;
import com.iortatechnxt.brokerverse.migration.trueup.domain.MigTrueupRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The migration items of the universal approval inbox (My Approvals): decisions waiting for the
 * business owner (G1), code map versions waiting for approval (G2), batches waiting for load
 * approval (G4), rollback requests, resubmissions of corrected rows and FY2027 true-ups. The maker
 * of an item never sees it.
 */
@Component
public class MigrationApprovalSource implements PendingApprovalSource {

  private static final String MODULE = MigrationCodes.MODULE;

  private final MigObjectDecisionRepository decisions;
  private final CodeMapVersionRepository versions;
  private final MigBatchRepository batches;
  private final ResubmissionRepository resubmissions;
  private final MigTrueupRepository trueups;

  /**
   * Creates the source.
   *
   * @param decisions decisions
   * @param versions map versions
   * @param batches batches
   * @param resubmissions resubmissions
   * @param trueups true-ups
   */
  public MigrationApprovalSource(
      MigObjectDecisionRepository decisions,
      CodeMapVersionRepository versions,
      MigBatchRepository batches,
      ResubmissionRepository resubmissions,
      MigTrueupRepository trueups) {
    this.decisions = decisions;
    this.versions = versions;
    this.batches = batches;
    this.resubmissions = resubmissions;
    this.trueups = trueups;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> out = new ArrayList<>();
    if (viewer.can("MIG_DECISION_APPROVE")) {
      for (MigObjectDecision d :
          decisions.findByStatusOrderBySubmittedAtAsc(MigObjectDecision.Status.FOR_DECISION)) {
        if (viewer.mayApproveItemOf(d.getSubmittedBy())) {
          out.add(
              item(
                  "Migration decision",
                  d.getDecisionNo(),
                  d.getObjectCode() + " as " + d.getProposedClass(),
                  d.getSubmittedBy(),
                  d.getSubmittedAt(),
                  d.getCompanyId(),
                  "/migration/objects?object=" + d.getObjectCode()));
        }
      }
    }
    if (viewer.can("MIG_MAPPING_APPROVE")) {
      for (CodeMapVersion v : versions.findByStatusOrderBySetCodeAsc(MapVersionStatus.SUBMITTED)) {
        if (viewer.mayApproveItemOf(v.getSubmittedBy())) {
          out.add(
              item(
                  "Code map version",
                  v.label(),
                  "Code map " + v.getSetCode(),
                  v.getSubmittedBy(),
                  v.getSubmittedAt(),
                  null,
                  "/migration/maps?set=" + v.getSetCode()));
        }
      }
    }
    batchItems(viewer, out);
    otherItems(viewer, out);
    return out;
  }

  private void batchItems(ApprovalViewer viewer, List<PendingApproval> out) {
    boolean load = viewer.can("MIG_LOAD_APPROVE");
    boolean rollback = viewer.can("MIG_ROLLBACK_APPROVE");
    if (!load && !rollback) {
      return;
    }
    for (MigBatch b :
        batches.findByStatusInOrderByIdAsc(
            EnumSet.of(BatchStatus.VALIDATED, BatchStatus.ROLLBACK_REQUESTED))) {
      if (load && b.getStatus() == BatchStatus.VALIDATED) {
        loadItem(viewer, b, out);
      } else if (rollback && b.getStatus() == BatchStatus.ROLLBACK_REQUESTED) {
        rollbackItem(viewer, b, out);
      }
    }
  }

  private static void loadItem(ApprovalViewer viewer, MigBatch b, List<PendingApproval> out) {
    if (viewer.mayApproveItemOf(b.getValidatedBy())) {
      out.add(
          item(
              "Migration load",
              b.getBatchNo(),
              "Load of object " + b.getObjectCode(),
              b.getValidatedBy(),
              b.getValidatedAt(),
              b.getCompanyId(),
              link(b)));
    }
  }

  private static void rollbackItem(ApprovalViewer viewer, MigBatch b, List<PendingApproval> out) {
    if (viewer.mayApproveItemOf(b.getRollbackRequestedBy())) {
      out.add(
          item(
              "Migration rollback",
              b.getBatchNo(),
              "Rollback of object " + b.getObjectCode(),
              b.getRollbackRequestedBy(),
              b.getRollbackRequestedAt(),
              b.getCompanyId(),
              link(b)));
    }
  }

  private static String link(MigBatch b) {
    return "/migration/batches/" + b.getBatchNo();
  }

  private void otherItems(ApprovalViewer viewer, List<PendingApproval> out) {
    if (viewer.can("MIG_RESUBMIT_APPROVE")) {
      for (Resubmission r :
          resubmissions.findByStatusOrderByPreparedAtAsc(Resubmission.Status.PREPARED)) {
        if (viewer.mayApproveItemOf(r.getPreparedBy())) {
          out.add(
              item(
                  "Corrected rows resubmission",
                  r.getResubmissionNo(),
                  r.getCorrectedRows() + " corrected rows of " + r.getObjectCode(),
                  r.getPreparedBy(),
                  r.getPreparedAt(),
                  r.getCompanyId(),
                  "/migration/batches"));
        }
      }
    }
    if (viewer.can("MIG_TRUEUP_APPROVE")) {
      for (MigTrueup t : trueups.findByStatusOrderByIdAsc(MigTrueup.Status.FOR_APPROVAL)) {
        if (viewer.mayApproveItemOf(t.getPreparedBy())) {
          out.add(
              item(
                  "Opening-balance adjustment",
                  t.getReference(),
                  "Year-end adjustment " + t.getTrueupNo(),
                  t.getPreparedBy(),
                  t.getPreparedAt(),
                  t.getCompanyId(),
                  "/migration/trueups"));
        }
      }
    }
  }

  private static PendingApproval item(
      String type,
      String reference,
      String description,
      String maker,
      Instant at,
      Long companyId,
      String link) {
    return new PendingApproval(
        MODULE, type, reference, description, null, null, maker, at, companyId, link);
  }
}
