package com.iortatechnxt.brokerverse.reserves.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveKey.PostingKey;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveLineValues;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveType;
import com.iortatechnxt.brokerverse.reserves.domain.RunLine;
import com.iortatechnxt.brokerverse.reserves.domain.TakafulItem;
import com.iortatechnxt.brokerverse.reserves.domain.ValuationRun;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/**
 * Books the reserve movements of a valuation run through the accounting engine: one event per
 * {@link ReserveEvent}, branch and line of business, with the signed movement = this run's closing
 * balance − the previous posted run's balance, dated the valuation date. Takaful surplus payable is
 * a flow of the month, posted as {@code TAKAFUL_SURPLUS}. Source references are unique per run,
 * event and unit, so a replay never double-posts; a reversal posts the same amounts with the
 * opposite sign under its own references.
 */
@Component
public class ReservePosting {

  /** Source module of reserve journals. */
  public static final String MODULE = "RESERVES";

  /** Event type of the takaful surplus. */
  public static final String TAKAFUL_EVENT = "TAKAFUL_SURPLUS";

  private static final String SURPLUS = "SURPLUS";
  private static final String SEPARATOR = ":";

  private final AccountingEventPublisher publisher;

  /**
   * Creates the poster.
   *
   * @param publisher accounting engine
   */
  public ReservePosting(AccountingEventPublisher publisher) {
    this.publisher = publisher;
  }

  /**
   * Posts the movements of a run.
   *
   * @param run run being posted
   * @param previous previous posted run, null for the first one
   * @param takaful takaful surplus lines of the run with their cost centre
   * @return number of journals posted
   */
  public int post(ValuationRun run, ValuationRun previous, Surplus takaful) {
    return publish(run, previous, takaful, run.getValuationDate(), BigDecimal.ONE, "P");
  }

  /**
   * Reverses the movements a run posted.
   *
   * @param run posted run being cancelled
   * @param previous run its movements were measured against, null for the first one
   * @param takaful takaful surplus lines of the run with their cost centre
   * @param date value date of the reversal
   * @return number of journals posted
   */
  public int reverse(ValuationRun run, ValuationRun previous, Surplus takaful, LocalDate date) {
    return publish(run, previous, takaful, date, BigDecimal.ONE.negate(), "R");
  }

  /**
   * Movements per posting unit and event between two runs.
   *
   * @param closing lines of the run
   * @param opening lines of the previous posted run
   * @return components with their signed movement, by unit and event
   */
  public static Map<PostingKey, Map<ReserveEvent, Map<String, BigDecimal>>> movements(
      List<ReserveLineValues> closing, List<ReserveLineValues> opening) {
    Map<PostingKey, Map<ReserveType, GrossRi>> balances = new HashMap<>();
    closing.forEach(l -> add(balances, l, BigDecimal.ONE));
    opening.forEach(l -> add(balances, l, BigDecimal.ONE.negate()));
    Map<PostingKey, Map<ReserveEvent, Map<String, BigDecimal>>> out =
        new TreeMap<>(PostingKey.ORDER);
    balances.forEach(
        (key, byType) -> {
          Map<ReserveEvent, Map<String, BigDecimal>> events = new EnumMap<>(ReserveEvent.class);
          for (ReserveEvent event : ReserveEvent.values()) {
            Map<String, BigDecimal> amounts = amounts(event, byType);
            if (!amounts.isEmpty()) {
              events.put(event, amounts);
            }
          }
          if (!events.isEmpty()) {
            out.put(key, events);
          }
        });
    return out;
  }

  private int publish(
      ValuationRun run,
      ValuationRun previous,
      Surplus takaful,
      LocalDate date,
      BigDecimal sign,
      String kind) {
    List<ReserveLineValues> opening =
        previous == null ? List.of() : previous.getLines().stream().map(RunLine::values).toList();
    List<ReserveLineValues> closing = run.getLines().stream().map(RunLine::values).toList();
    int journals = 0;
    for (var unit : movements(closing, opening).entrySet()) {
      for (var event : unit.getValue().entrySet()) {
        Map<String, BigDecimal> signed = new LinkedHashMap<>();
        event.getValue().forEach((c, v) -> signed.put(c, v.multiply(sign)));
        send(run, new Unit(unit.getKey(), null), event.getKey().name(), signed, date, kind);
        journals++;
      }
    }
    Map<PostingKey, BigDecimal> surplus = new TreeMap<>(PostingKey.ORDER);
    takaful.items().forEach(t -> surplus.merge(t.key().postingKey(), t.payable(), BigDecimal::add));
    for (var unit : surplus.entrySet()) {
      if (unit.getValue().signum() != 0) {
        send(
            run,
            new Unit(unit.getKey(), takaful.costCenter()),
            TAKAFUL_EVENT,
            Map.of(SURPLUS, unit.getValue().multiply(sign)),
            date,
            kind);
        journals++;
      }
    }
    return journals;
  }

  private void send(
      ValuationRun run,
      Unit unit,
      String eventType,
      Map<String, BigDecimal> amounts,
      LocalDate date,
      String kind) {
    String reference = "RSV-" + run.getPeriodName();
    publisher.publish(
        new BusinessEvent(
            eventType,
            run.getCompanyId(),
            unit.key().branchId(),
            date,
            run.getBaseCurrency(),
            MODULE,
            String.join(
                SEPARATOR,
                "RSV",
                run.getId().toString(),
                kind,
                eventType,
                unit.key().branchId().toString(),
                unit.key().businessLine()),
            reference,
            null,
            unit.key().businessLine(),
            unit.costCenter(),
            ("R".equals(kind) ? "Reversal of " : "Reserve movement ")
                + eventType
                + " "
                + run.getPeriodName()
                + " "
                + unit.key().businessLine(),
            amounts,
            null));
  }

  private static void add(
      Map<PostingKey, Map<ReserveType, GrossRi>> balances, ReserveLineValues l, BigDecimal sign) {
    balances
        .computeIfAbsent(l.key().postingKey(), k -> new EnumMap<>(ReserveType.class))
        .merge(
            l.type(), new GrossRi(l.gross().multiply(sign), l.ri().multiply(sign)), GrossRi::plus);
  }

  private static Map<String, BigDecimal> amounts(
      ReserveEvent event, Map<ReserveType, GrossRi> byType) {
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    for (ReserveEvent.Component c : event.components()) {
      GrossRi movement = byType.getOrDefault(c.type(), GrossRi.zero());
      BigDecimal value = Money.round(c.reinsurance() ? movement.ri() : movement.gross());
      if (value.signum() != 0) {
        amounts.put(c.name(), value);
      }
    }
    return amounts;
  }

  /**
   * Takaful surplus of a run and the cost centre of its expense journal.
   *
   * @param items surplus lines
   * @param costCenter cost centre (null when not configured)
   */
  public record Surplus(List<TakafulItem> items, String costCenter) {

    /** Canonical constructor copying the lines. */
    public Surplus {
      items = List.copyOf(items);
    }
  }

  /** Posting unit with the cost centre of the journal. */
  private record Unit(PostingKey key, String costCenter) {}
}
