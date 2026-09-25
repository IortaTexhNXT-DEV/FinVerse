package com.iortatechnxt.brokerverse.frbs.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.LineStatus;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine.Ticket;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLineRepository;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRun;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRunRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest.Status;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.DisbursementStatusChanged;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Follows the payouts of the service-fee lines in Disbursement (FRBS 2.10.2): every {@code
 * DisbursementStatusChanged} of an frbs request is kept on its line (gateway status, DV number);
 * PAID tags the line released on that day; RETURNED or CANCELLED before release puts it back for a
 * new sending. Events of an earlier sending are ignored.
 */
@Component
public class ServiceFeeFeedback {

  private final ServiceFeeRunRepository runs;
  private final ServiceFeeLineRepository lines;
  private final ServiceFeeTagService tags;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the listener.
   *
   * @param runs runs
   * @param lines lines
   * @param tags run progress
   * @param audit audit trail
   * @param clock clock
   */
  public ServiceFeeFeedback(
      ServiceFeeRunRepository runs,
      ServiceFeeLineRepository lines,
      ServiceFeeTagService tags,
      AuditTrailService audit,
      Clock clock) {
    this.runs = runs;
    this.lines = lines;
    this.tags = tags;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * A service-fee payout changed status in Disbursement.
   *
   * @param event status change
   */
  @EventListener
  public void on(DisbursementStatusChanged event) {
    String[] parts =
        ServiceFees.MODULE.equals(event.sourceModule()) && event.sourceRef() != null
            ? event.sourceRef().split(":")
            : new String[0];
    if (parts.length >= 2 && parts[1].chars().allMatch(Character::isDigit)) {
      runs.findByRunNo(parts[0])
          .ifPresent(
              run ->
                  line(run, Integer.parseInt(parts[1]))
                      .filter(l -> l.sourceRef(run.getRunNo()).equals(event.sourceRef()))
                      .ifPresent(l -> follow(run, l, event)));
    }
  }

  private Optional<ServiceFeeLine> line(ServiceFeeRun run, int lineNo) {
    return lines.findByRunIdAndLineNo(run.getId(), lineNo);
  }

  private void follow(ServiceFeeRun run, ServiceFeeLine line, DisbursementStatusChanged event) {
    Ticket ticket =
        new Ticket(event.requestNo(), event.status().name(), event.dvNo(), event.reason());
    if (event.status() == Status.PAID && line.getStatus() == LineStatus.SENT) {
      line.track(ticket);
      line.release(LocalDate.now(clock), ServiceFees.SYSTEM);
    } else if (event.status() == Status.RETURNED || event.status() == Status.CANCELLED) {
      line.returned(ticket);
    } else {
      line.track(ticket);
    }
    audit.record(
        ServiceFees.ENTITY,
        run.getRunNo(),
        AuditAction.UPDATE,
        "Line "
            + line.getLineNo()
            + ": Disbursement "
            + event.status()
            + (event.dvNo() == null ? "" : ", DV " + event.dvNo()));
    tags.progress(run);
  }
}
