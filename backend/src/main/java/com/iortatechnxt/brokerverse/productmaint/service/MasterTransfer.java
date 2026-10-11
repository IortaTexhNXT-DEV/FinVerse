package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProductRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.productmaint.domain.MasterChange;
import com.iortatechnxt.brokerverse.productmaint.domain.MasterChangeRepository;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The transfer of the product master changes to the other BDOI systems (BDOI FRS FRPM.029.01): the
 * pending changes are written in one file with a header, one detail record per change and a trailer
 * with the record count, named ProductMaster_yyyyMMddHHmmss.csv, and delivered to the target; a
 * failed delivery marks the changes FAILED with the error and raises the alert
 * PM_MASTER_TRANSFER_FAILED; a failed change is sent again on reprocessing. Without a connected
 * target the changes wait.
 */
@Service
@Transactional
public class MasterTransfer {

  /** Exception code of a failed transfer. */
  public static final String ALERT = "PM_MASTER_TRANSFER_FAILED";

  private static final DateTimeFormatter STAMP =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);
  private static final String SEPARATOR = ",";
  private static final int BUFFER = 512;

  private final MasterChangeRepository changes;
  private final RiskProductRepository products;
  private final MasterTarget target;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the transfer.
   *
   * @param changes product master changes
   * @param products products (names and lines)
   * @param target delivery target
   * @param alerts exception alerts
   * @param audit audit trail
   * @param clock clock
   */
  public MasterTransfer(
      MasterChangeRepository changes,
      RiskProductRepository products,
      MasterTarget target,
      AlertService alerts,
      AuditTrailService audit,
      Clock clock) {
    this.changes = changes;
    this.products = products;
    this.target = target;
    this.alerts = alerts;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Sends the pending changes in one file.
   *
   * @return the result
   */
  public TransferRun transfer() {
    List<MasterChange> pending = changes.findByStatusOrderByIdAsc(MasterChange.PENDING);
    if (pending.isEmpty() || !target.connected()) {
      return new TransferRun(null, 0, pending.size(), target.name());
    }
    String file = "ProductMaster_" + STAMP.format(BusinessClock.now(clock)) + ".csv";
    String content = render(file, pending);
    try {
      target.deliver(file, content, pending.size());
      pending.forEach(c -> c.sent(file, clock.instant()));
      audit.record(
          "ProductMasterTransfer",
          file,
          AuditAction.RUN,
          pending.size() + " product master change(s) sent to " + target.name());
      return new TransferRun(file, pending.size(), 0, target.name());
    } catch (MasterTarget.MasterTransferException e) {
      pending.forEach(c -> c.failed(file, e.getMessage()));
      alerts.raise(
          ALERT,
          new AlertFacts(
              null,
              null,
              "ProductMasterTransfer",
              file,
              "File " + file + " was not delivered: " + e.getMessage(),
              null,
              ALERT + ":" + file));
      return new TransferRun(file, 0, pending.size(), target.name());
    }
  }

  /**
   * Puts a failed change back in the queue and transfers at once.
   *
   * @param id change
   * @return the result
   */
  public TransferRun reprocess(Long id) {
    MasterChange c =
        changes
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product master change", id));
    if (!MasterChange.FAILED.equals(c.getStatus())) {
      throw new BusinessRuleException(
          "PM_MASTER_NOT_FAILED", "Only a failed product master change is processed again");
    }
    c.retry();
    changes.flush();
    return transfer();
  }

  /**
   * The file of a set of changes: header H, detail D per change, trailer T with the count.
   *
   * @param file file name
   * @param list changes
   * @return content
   */
  String render(String file, List<MasterChange> list) {
    StringBuilder out = new StringBuilder(BUFFER * (list.size() + 2));
    out.append(String.join(SEPARATOR, "H", file, BusinessClock.today(clock).toString(), "BIBS"))
        .append('\n');
    for (MasterChange c : list) {
      RiskProduct p = products.findByCode(c.getProductCode()).orElse(null);
      out.append(
              String.join(
                  SEPARATOR,
                  "D",
                  c.getProductCode(),
                  csv(p == null ? "" : p.getName()),
                  p == null ? "" : p.getLineCode(),
                  Objects.toString(c.getVersionNo(), ""),
                  c.getChangeKind(),
                  Objects.toString(c.getEffectiveDate(), ""),
                  Objects.toString(c.getSourceRequestNo(), "")))
          .append('\n');
    }
    out.append(String.join(SEPARATOR, "T", String.valueOf(list.size()))).append('\n');
    return out.toString();
  }

  private static String csv(String text) {
    return text.contains(SEPARATOR) || text.contains("\"")
        ? "\"" + text.replace("\"", "\"\"") + "\""
        : text;
  }

  /**
   * The result of a transfer.
   *
   * @param fileName file written, null when nothing was sent
   * @param sent changes sent
   * @param waiting changes still waiting or failed
   * @param target delivery target
   */
  public record TransferRun(String fileName, int sent, int waiting, String target) {}
}
