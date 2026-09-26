package com.iortatechnxt.brokerverse.collections.feed.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertCheck;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.OutboxStatus;
import com.iortatechnxt.brokerverse.collections.feed.domain.OutboxItemRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code CLX_OUTBOX_STALE} (COLLECTIONS_DESIGN 8, risk 1): an item Collections handed to Cashiering
 * or Commission (check pick-up, 2307 tag, DP account) that is still pending after a day, so the
 * consumer's intake is followed up.
 */
@Component
public class OutboxStaleCheck implements AlertCheck {

  /** Exception code. */
  public static final String CODE = "CLX_OUTBOX_STALE";

  private final OutboxItemRepository outbox;
  private final Clock clock;

  /**
   * Creates the check.
   *
   * @param outbox outbox rows
   * @param clock clock
   */
  public OutboxStaleCheck(OutboxItemRepository outbox, Clock clock) {
    this.outbox = outbox;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AlertSignal> evaluate(LocalDate asOf) {
    return outbox
        .findByStatusAndCreatedAtBeforeOrderByIdAsc(
            OutboxStatus.PENDING, clock.instant().minus(Duration.ofDays(1)))
        .stream()
        .map(
            o ->
                new AlertSignal(
                    CODE,
                    new AlertFacts(
                        o.getCompanyId(),
                        null,
                        "CollectionOutbox",
                        o.getIdempotencyKey(),
                        o.getFeedCode()
                            + " item of "
                            + o.getInvoiceNo()
                            + " not taken by Operations since "
                            + o.getCreatedAt(),
                        null,
                        CODE + ":" + o.getId())))
        .toList();
  }
}
