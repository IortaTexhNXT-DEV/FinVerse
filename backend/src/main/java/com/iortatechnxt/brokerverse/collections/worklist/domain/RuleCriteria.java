package com.iortatechnxt.brokerverse.collections.worklist.domain;

import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * Criteria of an assignment rule or a reassignment by criteria (BRCLXN.052): null criteria are
 * ignored; codes compare case-insensitively.
 *
 * @param segment market segment
 * @param salesUnit sales unit
 * @param clientCode client
 * @param amountFrom lowest net outstanding
 * @param amountTo highest net outstanding
 * @param agingFrom lowest age in days
 * @param agingTo highest age in days
 */
@Embeddable
public record RuleCriteria(
    @Column(length = 40) String segment,
    @Column(name = "sales_unit", length = 20) String salesUnit,
    @Column(name = "client_code", length = 30) String clientCode,
    @Column(name = "amount_from", precision = 19, scale = 2) BigDecimal amountFrom,
    @Column(name = "amount_to", precision = 19, scale = 2) BigDecimal amountTo,
    @Column(name = "aging_from") Integer agingFrom,
    @Column(name = "aging_to") Integer agingTo) {

  /** No criterion: every item matches. */
  public static final RuleCriteria NONE =
      new RuleCriteria(null, null, null, null, null, null, null);

  /**
   * Whether an item matches.
   *
   * @param item item
   * @return true when every criterion given matches
   */
  public boolean matches(CollectionItem item) {
    return same(segment, item.getClassification().segment())
        && same(salesUnit, item.getClassification().salesUnit())
        && same(clientCode, item.getParties().clientCode())
        && amountInside(item.getFigures().total())
        && agingInside(item.getFigures().agingDays());
  }

  private boolean amountInside(BigDecimal amount) {
    return (amountFrom == null || amount.compareTo(amountFrom) >= 0)
        && (amountTo == null || amount.compareTo(amountTo) <= 0);
  }

  private boolean agingInside(int days) {
    return (agingFrom == null || days >= agingFrom) && (agingTo == null || days <= agingTo);
  }

  private static boolean same(String criterion, String value) {
    return criterion == null
        || criterion.isBlank()
        || value != null && String.CASE_INSENSITIVE_ORDER.compare(criterion, value) == 0;
  }
}
