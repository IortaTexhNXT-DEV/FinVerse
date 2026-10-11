package com.iortatechnxt.brokerverse.productmaint.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.productmaint.domain.MasterChange;
import com.iortatechnxt.brokerverse.productmaint.domain.MasterChangeRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.MasterInboxFile;
import com.iortatechnxt.brokerverse.productmaint.domain.MasterInboxFileRepository;
import com.iortatechnxt.brokerverse.productmaint.service.MasterTarget;
import com.iortatechnxt.brokerverse.productmaint.service.MasterTransfer;
import com.iortatechnxt.brokerverse.productmaint.service.MasterTransfer.TransferRun;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Monitoring of the product master changes sent to the other BDOI systems (BDOI FRS FRPM.029.01):
 * the changes with their status, file and error, the target, Send Now, reprocessing of a failed
 * change and, in SIT and UAT, the files received by the simulated receiving system.
 */
@RestController
@RequestMapping("/api/v1/product-maintenance/master-changes")
public class MasterChangeController {

  private static final String VIEW = "hasAnyAuthority('PRODUCT_MAINTAIN', 'PKG_REPORT_VIEW')";
  private static final String ACT = "hasAuthority('PRODUCT_MAINTAIN')";
  private static final int MAX_PAGE = 100;

  private final MasterChangeRepository changes;
  private final MasterInboxFileRepository inbox;
  private final MasterTransfer transfer;
  private final MasterTarget target;

  /**
   * Creates the controller.
   *
   * @param changes product master changes
   * @param inbox files of the simulated receiving system
   * @param transfer the transfer
   * @param target delivery target
   */
  public MasterChangeController(
      MasterChangeRepository changes,
      MasterInboxFileRepository inbox,
      MasterTransfer transfer,
      MasterTarget target) {
    this.changes = changes;
    this.inbox = inbox;
    this.transfer = transfer;
    this.target = target;
  }

  /**
   * The changes, newest first.
   *
   * @param status PENDING, SENT or FAILED; all when absent
   * @param page page
   * @param size size
   * @return the monitoring view
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public MonitorView list(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE),
            Sort.by(Sort.Direction.DESC, "id"));
    Page<MasterChange> found =
        status == null || status.isBlank()
            ? changes.findAll(pageable)
            : changes.findByStatus(status.strip(), pageable);
    return new MonitorView(
        target.name(),
        target.connected(),
        changes.countByStatus(MasterChange.PENDING),
        changes.countByStatus(MasterChange.FAILED),
        PageResponse.of(found, ChangeView::from));
  }

  /**
   * Sends the pending changes now.
   *
   * @return the result
   */
  @PostMapping("/transfer")
  @PreAuthorize(ACT)
  public TransferRun sendNow() {
    return transfer.transfer();
  }

  /**
   * Sends a failed change again.
   *
   * @param id change
   * @return the result
   */
  @PostMapping("/{id}/reprocess")
  @PreAuthorize(ACT)
  public TransferRun reprocess(@PathVariable Long id) {
    return transfer.reprocess(id);
  }

  /**
   * The files received by the simulated receiving system (SIT and UAT).
   *
   * @return files, newest first
   */
  @GetMapping("/simulator/files")
  @PreAuthorize(VIEW)
  public List<InboxView> received() {
    return inbox.findTop50ByOrderByIdDesc().stream().map(InboxView::from).toList();
  }

  /**
   * The monitoring view.
   *
   * @param target delivery target
   * @param connected whether the target can receive files
   * @param pending changes waiting
   * @param failed changes failed
   * @param changes page of changes
   */
  public record MonitorView(
      String target,
      boolean connected,
      long pending,
      long failed,
      PageResponse<ChangeView> changes) {}

  /**
   * A product master change.
   *
   * @param id id
   * @param productCode product
   * @param versionNo version
   * @param changeKind RELEASED, RETIRED or EXPIRED
   * @param effectiveDate effective date
   * @param sourceRequestNo package request
   * @param status PENDING, SENT or FAILED
   * @param attempts attempts
   * @param fileName transfer file
   * @param sentAt when sent
   * @param error last error
   * @param recordedAt when recorded
   */
  public record ChangeView(
      Long id,
      String productCode,
      Integer versionNo,
      String changeKind,
      LocalDate effectiveDate,
      String sourceRequestNo,
      String status,
      int attempts,
      String fileName,
      Instant sentAt,
      String error,
      Instant recordedAt) {

    static ChangeView from(MasterChange c) {
      return new ChangeView(
          c.getId(),
          c.getProductCode(),
          c.getVersionNo(),
          c.getChangeKind(),
          c.getEffectiveDate(),
          c.getSourceRequestNo(),
          c.getStatus(),
          c.getAttempts(),
          c.getFileName(),
          c.getSentAt(),
          c.getError(),
          c.getCreatedAt());
    }
  }

  /**
   * A file received by the simulated receiving system.
   *
   * @param id id
   * @param fileName file name
   * @param recordCount detail records
   * @param receivedAt when
   * @param content content
   */
  public record InboxView(
      Long id, String fileName, int recordCount, Instant receivedAt, String content) {

    static InboxView from(MasterInboxFile f) {
      return new InboxView(
          f.getId(), f.getFileName(), f.getRecordCount(), f.getReceivedAt(), f.getContent());
    }
  }
}
