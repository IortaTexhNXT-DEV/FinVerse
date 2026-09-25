package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycle;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycleRepository;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.UploadStatus;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItemRepository;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconUpload;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconUpload.FileKey;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconUpload.Period;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconUpload.RowCounts;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconUploadRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Upload attempts of insurer production reports (PRCID.009/010/031/032): the duplicate block with
 * its attempt and alert, the start of an attempt per insurer and month (the insurer side of the
 * cycle's earlier upload is replaced), and its outcome, which moves the cycle to reconciling and
 * notifies the reconciliation handlers ({@code RECON_FEEDBACK_UPLOADED}). Each method is its own
 * transaction: the feed handler runs outside one.
 */
@Service
@Transactional
public class ReconUploadRecorder {

  /** Alert and error code of a refused duplicate. */
  public static final String DUPLICATE = "RECON_UPLOAD_DUPLICATE";

  private static final String ENTITY = "ReconUpload";
  private static final List<UploadStatus> TAKEN_IN =
      List.of(UploadStatus.PROCESSED, UploadStatus.PARTIAL);

  private final ReconUploadRepository uploads;
  private final ReconCycleRepository cycleRows;
  private final ReconCycleService cycles;
  private final ReconItemRepository items;
  private final AlertService alerts;
  private final NotificationService notifications;
  private final AuditTrailService audit;

  /**
   * Creates the recorder.
   *
   * @param uploads upload attempts
   * @param cycleRows cycles (company look-up)
   * @param cycles cycle service
   * @param items reconciliation items
   * @param alerts alerts
   * @param notifications notifications
   * @param audit audit trail
   */
  public ReconUploadRecorder(
      ReconUploadRepository uploads,
      ReconCycleRepository cycleRows,
      ReconCycleService cycles,
      ReconItemRepository items,
      AlertService alerts,
      NotificationService notifications,
      AuditTrailService audit) {
    this.uploads = uploads;
    this.cycleRows = cycleRows;
    this.cycles = cycles;
    this.items = items;
    this.alerts = alerts;
    this.notifications = notifications;
    this.audit = audit;
  }

  /**
   * Refuses a file identical to an earlier upload (PRCID.010): records the blocked attempt and
   * raises {@code RECON_UPLOAD_DUPLICATE}.
   *
   * @param file file name and checksum
   * @param runNo flow-in run, may be null
   * @return the refusal message, empty when the file is new
   */
  public Optional<String> blockIfDuplicate(FileKey file, String runNo) {
    Optional<ReconUpload> earlier =
        uploads.findFirstBySha256AndStatusInOrderByIdAsc(file.sha256(), TAKEN_IN);
    if (earlier.isEmpty()) {
      return Optional.empty();
    }
    ReconUpload first = earlier.get();
    String message =
        file.fileName()
            + " is identical to "
            + first.getFileName()
            + " uploaded on "
            + first.getCreatedAt()
            + " by "
            + first.getCreatedBy();
    ReconUpload blocked =
        uploads.save(
            new ReconUpload(
                first.getCompanyId(),
                file,
                new Period(first.getInsurerCode(), first.getProductionMonth(), first.getCycleId()),
                attempt(first.getCompanyId(), first.getInsurerCode(), first.getProductionMonth()),
                UploadStatus.DUPLICATE_BLOCKED));
    blocked.finish(UploadStatus.DUPLICATE_BLOCKED, runNo, RowCounts.NONE, message);
    alerts.raise(
        DUPLICATE,
        new AlertFacts(
            first.getCompanyId(),
            null,
            ENTITY,
            String.valueOf(blocked.getId()),
            "Duplicate insurer production upload refused: " + message,
            null,
            DUPLICATE + ":" + blocked.getId()));
    audit.record(ENTITY, blocked.getId(), AuditAction.CREATE, "Blocked duplicate: " + message);
    return Optional.of(message);
  }

  /**
   * Records an attempt that could not be read.
   *
   * @param companyId company, null when unknown (nothing recorded)
   * @param file file
   * @param runNo flow-in run
   * @param message error
   */
  public void failed(Long companyId, FileKey file, String runNo, String message) {
    if (companyId == null) {
      return;
    }
    ReconUpload upload =
        uploads.save(new ReconUpload(companyId, file, Period.UNKNOWN, 1, UploadStatus.FAILED));
    upload.finish(UploadStatus.FAILED, runNo, RowCounts.NONE, message);
    audit.record(ENTITY, upload.getId(), AuditAction.CREATE, "Failed upload: " + message);
  }

  /**
   * Starts the attempt of one insurer and month: the open cycle (opened when needed), the attempt
   * number, and the removal of the insurer side of earlier uploads of the cycle.
   *
   * @param companyId company, null to find it from the open cycle of the insurer and month
   * @param file file
   * @param period insurer and month
   * @param runNo flow-in run
   * @return the attempt
   */
  public ReconUpload begin(Long companyId, FileKey file, Period period, String runNo) {
    Long company = companyId == null ? companyOf(period) : companyId;
    ReconCycle cycle = cycles.openOrGet(company, period.insurerCode(), period.productionMonth());
    for (ReconItem item : items.findByCycleIdOrderByIdAsc(cycle.getId())) {
      if (item.getUploadId() == null) {
        continue;
      }
      if (item.getInvoiceNo() == null) {
        items.delete(item);
      } else {
        item.detachInsurer();
      }
    }
    ReconUpload upload =
        uploads.save(
            new ReconUpload(
                company,
                file,
                new Period(period.insurerCode(), cycle.getProductionMonth(), cycle.getId()),
                attempt(company, period.insurerCode(), cycle.getProductionMonth()),
                UploadStatus.RECEIVED));
    upload.finish(UploadStatus.RECEIVED, runNo, RowCounts.NONE, null);
    return upload;
  }

  private Long companyOf(Period period) {
    List<ReconCycle> open =
        cycleRows.findByInsurerCodeAndProductionMonthAndClosedFalse(
            period.insurerCode(), period.productionMonth());
    if (open.size() != 1) {
      throw new BusinessRuleException(
          "RECON_COMPANY_UNKNOWN",
          "Upload the production of "
              + period.insurerCode()
              + " from Production Reconciliation - Uploads (company not known)");
    }
    return open.get(0).getCompanyId();
  }

  private int attempt(Long companyId, String insurerCode, LocalDate month) {
    return (int)
            uploads.countByCompanyIdAndInsurerCodeAndProductionMonth(companyId, insurerCode, month)
        + 1;
  }

  /**
   * Records the outcome of an attempt, moves its cycle to reconciling and notifies the handlers.
   *
   * @param uploadId attempt
   * @param counts rows read, accepted and failed
   * @return the attempt
   */
  public ReconUpload finish(Long uploadId, RowCounts counts) {
    ReconUpload upload =
        uploads
            .findById(uploadId)
            .orElseThrow(() -> new ResourceNotFoundException(ENTITY, uploadId));
    UploadStatus status;
    if (counts.failed() == 0) {
      status = UploadStatus.PROCESSED;
    } else {
      status = counts.accepted() == 0 ? UploadStatus.FAILED : UploadStatus.PARTIAL;
    }
    upload.finish(
        status,
        upload.getRunNo(),
        counts,
        counts.accepted() + " of " + counts.read() + " row(s) taken in");
    ReconCycle cycle = cycles.require(upload.getCycleId());
    if (counts.accepted() > 0) {
      cycles.feedbackUploaded(cycle);
      cycles.closeWhenSettled(cycle);
      notifications.notifyPermission(
          "RECON_PROCESS",
          new Notice(
              "Insurer feedback uploaded: " + cycle.getCycleNo(),
              upload.getFileName() + ": " + upload.getMessage(),
              "/prodrecon/cycles/" + cycle.getId(),
              ReconCycleService.ENTITY,
              String.valueOf(cycle.getId())),
          "RECON_FEEDBACK_UPLOADED");
    }
    audit.record(
        ENTITY,
        upload.getId(),
        AuditAction.UPDATE,
        status + " for " + cycle.getCycleNo() + ": " + upload.getMessage());
    return upload;
  }
}
