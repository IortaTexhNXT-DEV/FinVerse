package com.iortatechnxt.brokerverse.reserves.domain;

import java.math.BigDecimal;

/**
 * Maintainable takaful settings.
 *
 * @param enabled whether valuation runs compute the takaful surplus
 * @param productCodes comma separated takaful product codes (blank = every product)
 * @param participantSharePct participants' share % of the surplus (Mudharabah ratio)
 * @param taxPct tax % withheld on the participants' share
 * @param costCenter cost centre of the surplus expense journal (mandatory on expense accounts)
 */
public record TakafulTerms(
    boolean enabled,
    String productCodes,
    BigDecimal participantSharePct,
    BigDecimal taxPct,
    String costCenter) {}
