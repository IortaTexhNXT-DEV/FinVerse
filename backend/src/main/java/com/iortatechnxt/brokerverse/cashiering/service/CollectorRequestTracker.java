package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequest;
import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequestRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Disposition;
import com.iortatechnxt.brokerverse.cashiering.domain.DispositionWords;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.UnappliedDispositionChanged;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the collector requests in step with the cashiering dispositions assigned from them
 * (BRCLXN.030-033, 040) and tells the requesting module through {@code
 * UnappliedDispositionChanged}: ACCEPTED when a disposition is assigned, APPLIED when it is
 * executed, REJECTED when a cashier refuses the request or withdraws its disposition. The event is
 * published inside the cashiering transaction, so the requester's record commits with it.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class CollectorRequestTracker {

  /** Status of an accepted request on the event. */
  public static final String ACCEPTED = "ACCEPTED";

  /** Status of a rejected request on the event. */
  public static final String REJECTED = "REJECTED";

  /** Status of an executed request on the event. */
  public static final String APPLIED = "APPLIED";

  private final CollectorRequestRepository requests;
  private final ApplicationEventPublisher events;
  private final Clock clock;
  private final UserDirectory directory;

  /**
   * Creates the tracker.
   *
   * @param requests collector requests
   * @param events event publisher
   * @param clock clock
   * @param directory user directory (display names in texts)
   */
  public CollectorRequestTracker(
      CollectorRequestRepository requests,
      ApplicationEventPublisher events,
      Clock clock,
      UserDirectory directory) {
    this.requests = requests;
    this.events = events;
    this.clock = clock;
    this.directory = directory;
  }

  /**
   * A disposition was executed: its request, if any, is applied.
   *
   * @param item unapplied item
   * @param d executed disposition
   */
  public void executed(Unapplied item, Disposition d) {
    requests
        .findByDispositionId(d.getId())
        .ifPresent(
            r -> {
              String note =
                  DispositionWords.of(d.getDispositionType())
                      + " "
                      + item.getCurrency()
                      + " "
                      + DisplayFormat.amount(d.getAmount())
                      + " executed"
                      + (d.getDisbursementRequestNo() == null
                          ? ""
                          : " (refund request " + d.getDisbursementRequestNo() + ")")
                      + (d.getOrNo() == null ? "" : " (official receipt " + d.getOrNo() + ")");
              r.applied(clock.instant(), note);
              publish(item, r, APPLIED, note, d.getOrNo());
            });
  }

  /**
   * A disposition was withdrawn before processing: its request, if any, is rejected.
   *
   * @param item unapplied item
   * @param d withdrawn disposition
   * @param by cashier
   */
  public void withdrawn(Unapplied item, Disposition d, String by) {
    requests
        .findByDispositionId(d.getId())
        .ifPresent(
            r -> {
              String note =
                  "Disposition withdrawn by Cashiering (" + directory.displayName(by) + ")";
              r.reject(by, clock.instant(), note);
              publish(item, r, REJECTED, note);
            });
  }

  /**
   * Publishes the change of a request.
   *
   * @param item unapplied item
   * @param r request
   * @param status ACCEPTED, REJECTED or APPLIED
   * @param message reason or remarks
   */
  void publish(Unapplied item, CollectorRequest r, String status, String message) {
    publish(item, r, status, message, null);
  }

  private void publish(
      Unapplied item, CollectorRequest r, String status, String message, String documentNo) {
    events.publishEvent(
        new UnappliedDispositionChanged(
            r.getCompanyId(),
            item.getReference(),
            r.getSource(),
            r.getSourceRef(),
            status,
            r.getRequestNo(),
            message,
            documentNo));
  }
}
