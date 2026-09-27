package com.iortatechnxt.brokerverse.collections.worklist.service;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import java.math.BigDecimal;
import java.util.List;

/**
 * Filters of the PR worklist (COLLECTIONS_DESIGN 11; BRCLXN.001-012, 050-052): null criteria are
 * ignored. Also the selection of a reassignment by criteria (client, unit, aging, amount).
 *
 * @param text part of the invoice no., ARN, policy no., client code or assured name
 * @param statuses statuses (empty = every status)
 * @param scope segment, unit, Unit Head, AO and client
 * @param work handler and collection work filters
 * @param ranges amount and aging ranges
 */
public record WorklistFilter(
    String text, List<ItemStatus> statuses, Scope scope, Work work, Ranges ranges) {

  /** Defensive copy and empty parts. */
  public WorklistFilter {
    statuses = statuses == null ? List.of() : List.copyOf(statuses);
    scope = scope == null ? Scope.ANY : scope;
    work = work == null ? Work.ANY : work;
    ranges = ranges == null ? Ranges.ANY : ranges;
  }

  /**
   * The items in some statuses.
   *
   * @param statuses statuses
   * @return filter
   */
  public static WorklistFilter of(ItemStatus... statuses) {
    return new WorklistFilter(null, List.of(statuses), Scope.ANY, Work.ANY, Ranges.ANY);
  }

  /**
   * The same filter with a scope.
   *
   * @param newScope scope
   * @return filter
   */
  public WorklistFilter with(Scope newScope) {
    return new WorklistFilter(text, statuses, newScope, work, ranges);
  }

  /**
   * The same filter with work criteria.
   *
   * @param newWork work criteria
   * @return filter
   */
  public WorklistFilter with(Work newWork) {
    return new WorklistFilter(text, statuses, scope, newWork, ranges);
  }

  /**
   * The same filter with ranges.
   *
   * @param newRanges ranges
   * @return filter
   */
  public WorklistFilter with(Ranges newRanges) {
    return new WorklistFilter(text, statuses, scope, work, newRanges);
  }

  /**
   * Who the account belongs to (BRCLXN.011/012).
   *
   * @param segment market segment
   * @param salesUnit sales unit
   * @param unitHead Unit Head
   * @param aoUsername account officer
   * @param clientCode client
   */
  public record Scope(
      String segment, String salesUnit, String unitHead, String aoUsername, String clientCode) {

    /** No scope criterion. */
    public static final Scope ANY = new Scope(null, null, null, null, null);
  }

  /**
   * The collection work on the account.
   *
   * @param handler current handler
   * @param unassigned true for accounts without a handler
   * @param agingBracket aging bracket
   * @param category tagging category A / B / C
   * @param dispositionCode current disposition
   * @param promiseStatus promise flag
   * @param escalated true for escalated accounts only, false for accounts not escalated
   */
  public record Work(
      String handler,
      Boolean unassigned,
      String agingBracket,
      String category,
      String dispositionCode,
      String promiseStatus,
      Boolean escalated) {

    /** No work criterion. */
    public static final Work ANY = new Work(null, null, null, null, null, null, null);
  }

  /**
   * Amount and aging ranges.
   *
   * @param amountFrom lowest net outstanding
   * @param amountTo highest net outstanding
   * @param agingFrom lowest age in days
   * @param agingTo highest age in days
   */
  public record Ranges(
      BigDecimal amountFrom, BigDecimal amountTo, Integer agingFrom, Integer agingTo) {

    /** No range. */
    public static final Ranges ANY = new Ranges(null, null, null, null);
  }
}
