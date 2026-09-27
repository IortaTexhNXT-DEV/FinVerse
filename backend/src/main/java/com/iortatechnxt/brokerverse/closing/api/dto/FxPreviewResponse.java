package com.iortatechnxt.brokerverse.closing.api.dto;

import com.iortatechnxt.brokerverse.closing.domain.RevaluationItem;
import com.iortatechnxt.brokerverse.closing.service.FxRevaluationService.Preview;
import com.iortatechnxt.brokerverse.closing.service.OpenItemRevaluation.OpenItemLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * FX revaluation preview.
 *
 * @param periodId period
 * @param periodName period name
 * @param revaluationDate revaluation date
 * @param items GL balances with their revaluation
 * @param missingRates currencies without a CLOSING rate
 * @param openItems foreign currency open items (information only)
 * @param existingRunId run already posted for the period
 * @param totalDifference net unrealized difference of postable items
 */
public record FxPreviewResponse(
    Long periodId,
    String periodName,
    LocalDate revaluationDate,
    List<Item> items,
    List<String> missingRates,
    List<OpenItemLine> openItems,
    Long existingRunId,
    BigDecimal totalDifference) {

  /**
   * Maps a preview.
   *
   * @param p preview
   * @return view
   */
  public static FxPreviewResponse from(Preview p) {
    List<Item> items = p.items().stream().map(Item::from).toList();
    BigDecimal total =
        items.stream()
            .filter(Item::postable)
            .map(Item::difference)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new FxPreviewResponse(
        p.periodId(),
        p.periodName(),
        p.revaluationDate(),
        items,
        p.missingRates(),
        p.openItems(),
        p.existingRunId(),
        total);
  }

  /**
   * Revalued GL balance.
   *
   * @param branchId branch
   * @param accountCode account
   * @param accountName account name
   * @param currency currency
   * @param fcBalance foreign currency balance
   * @param bookedBase booked base
   * @param closingRate closing rate
   * @param revaluedBase revalued base
   * @param difference unrealized difference
   * @param postable whether a journal can be posted
   */
  public record Item(
      Long branchId,
      String accountCode,
      String accountName,
      String currency,
      BigDecimal fcBalance,
      BigDecimal bookedBase,
      BigDecimal closingRate,
      BigDecimal revaluedBase,
      BigDecimal difference,
      boolean postable) {

    /**
     * Maps an item.
     *
     * @param i item
     * @return view
     */
    public static Item from(RevaluationItem i) {
      return new Item(
          i.branchId(),
          i.accountCode(),
          i.accountName(),
          i.currency(),
          i.fcBalance(),
          i.bookedBase(),
          i.closingRate(),
          i.revaluedBase(),
          i.difference(),
          i.postable());
    }
  }
}
