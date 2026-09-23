package com.iortatechnxt.finverse.reserves.service;

import com.iortatechnxt.finverse.reserves.domain.EarningUnits;
import com.iortatechnxt.finverse.reserves.domain.PremiumAmounts;
import com.iortatechnxt.finverse.reserves.domain.ReserveKey;
import com.iortatechnxt.finverse.reserves.domain.UprItem;
import com.iortatechnxt.finverse.underwriting.domain.UprBasis;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The approved premium transactions of a company up to a date, with their cover periods, earning
 * basis and written amounts (base currency): the input of every premium-based reserve.
 */
public final class Portfolio {

  private final List<Entry> entries;

  /**
   * Creates a portfolio.
   *
   * @param entries premium transactions
   */
  public Portfolio(List<Entry> entries) {
    this.entries = List.copyOf(entries);
  }

  /**
   * Premium transactions.
   *
   * @return entries
   */
  public List<Entry> entries() {
    return entries;
  }

  /**
   * UPR of every transaction approved on or before a date.
   *
   * @param date valuation date
   * @return items in portfolio order
   */
  public List<UprItem> uprAt(LocalDate date) {
    return entries.stream()
        .filter(e -> !e.approvalDate().isAfter(date))
        .map(e -> e.upr(date))
        .toList();
  }

  /**
   * Unearned amounts by reporting unit at a date.
   *
   * @param date date
   * @return unearned amounts
   */
  public Map<ReserveKey, PremiumAmounts> unearnedByKey(LocalDate date) {
    Map<ReserveKey, PremiumAmounts> out = new HashMap<>();
    uprAt(date).forEach(i -> out.merge(i.key(), i.unearned(), PremiumAmounts::plus));
    return out;
  }

  /**
   * Earned amounts by reporting unit between two dates: written in (open, close] + unearned at open
   * − unearned at close.
   *
   * @param open opening date (exclusive)
   * @param close closing date (inclusive)
   * @return earned amounts
   */
  public Map<ReserveKey, PremiumAmounts> earnedByKey(LocalDate open, LocalDate close) {
    Map<ReserveKey, PremiumAmounts> out = new HashMap<>();
    for (Entry e : entries) {
      if (e.approvalDate().isAfter(open) && !e.approvalDate().isAfter(close)) {
        out.merge(e.key(), e.written(), PremiumAmounts::plus);
      }
    }
    unearnedByKey(open).forEach((k, v) -> out.merge(k, v, PremiumAmounts::plus));
    unearnedByKey(close).forEach((k, v) -> out.merge(k, v, PremiumAmounts::minus));
    return out;
  }

  /**
   * One premium transaction of the portfolio.
   *
   * @param policyId policy id
   * @param endorsementNo endorsement number (0 = original issue)
   * @param documentNo document number
   * @param kind NEW or endorsement type
   * @param key reporting unit
   * @param basis earning basis of the product
   * @param coverFrom first day of cover
   * @param coverTo last day of cover
   * @param approvalDate approval (accounting) date
   * @param written written amounts (base currency)
   */
  public record Entry(
      Long policyId,
      int endorsementNo,
      String documentNo,
      String kind,
      ReserveKey key,
      UprBasis basis,
      LocalDate coverFrom,
      LocalDate coverTo,
      LocalDate approvalDate,
      PremiumAmounts written) {

    /**
     * UPR of this transaction at a date.
     *
     * @param date valuation date
     * @return item
     */
    public UprItem upr(LocalDate date) {
      EarningUnits units = UprMath.units(basis, coverFrom, coverTo, date);
      return new UprItem(
          policyId,
          endorsementNo,
          documentNo,
          kind,
          key,
          basis.name(),
          coverFrom,
          coverTo,
          approvalDate,
          units,
          written,
          written.times(units.unearnedFraction()));
    }
  }
}
