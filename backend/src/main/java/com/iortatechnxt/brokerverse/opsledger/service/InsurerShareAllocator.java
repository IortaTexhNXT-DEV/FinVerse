package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceShare;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits an amount of a co-insured invoice by the insurers' shares (ADJID.027: one invoice with an
 * internal distribution; payments and adjustments follow the original shares). Amounts are rounded
 * half-up to centavos and the last share takes the rounding difference, as booking does.
 */
public final class InsurerShareAllocator {

  private static final int SCALE = 2;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private InsurerShareAllocator() {}

  /**
   * The part of an amount for each insurer.
   *
   * @param amount signed amount
   * @param shares insurer shares (percent, adding up to 100)
   * @return amount per insurer code, in share order, adding up to the amount
   */
  public static Map<String, BigDecimal> allocate(BigDecimal amount, List<OpsInvoiceShare> shares) {
    Map<String, BigDecimal> parts = new LinkedHashMap<>();
    BigDecimal total = amount.setScale(SCALE, RoundingMode.HALF_UP);
    BigDecimal left = total;
    for (int i = 0; i < shares.size(); i++) {
      OpsInvoiceShare share = shares.get(i);
      BigDecimal part =
          i == shares.size() - 1
              ? left
              : total.multiply(share.sharePct()).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
      parts.merge(share.insurerCode(), part, BigDecimal::add);
      left = left.subtract(part);
    }
    return parts;
  }
}
