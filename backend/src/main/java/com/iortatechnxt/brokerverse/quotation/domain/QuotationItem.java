package com.iortatechnxt.brokerverse.quotation.domain;

import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import java.math.BigDecimal;

/**
 * A risk item of a quotation version. Items sharing a risk group become one account when the client
 * accepts them (BRNB.045).
 *
 * @param riskGroup risk group, 1 for a single account
 * @param data risk data (vehicle, location, person or generic)
 * @param premium annual premium of the item, null until rated
 * @param ratePercent rate applied in percent, null until rated
 */
public record QuotationItem(
    int riskGroup, RiskItemData data, BigDecimal premium, BigDecimal ratePercent) {}
