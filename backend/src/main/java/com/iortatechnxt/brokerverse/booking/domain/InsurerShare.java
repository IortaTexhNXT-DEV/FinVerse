package com.iortatechnxt.brokerverse.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Share of one insurer in a booked invoice (co-insurance; 100 % for a single insurer).
 *
 * @param insurerCode insurer party code
 * @param sharePct share in percent
 */
@Embeddable
public record InsurerShare(
    @Column(name = "insurer_code", nullable = false, length = 30) String insurerCode,
    @Column(name = "share_pct", nullable = false, precision = 9, scale = 4) BigDecimal sharePct) {

  private static final int SCALE = 4;

  /** The share at the stored scale (4 decimals). */
  public InsurerShare {
    sharePct = sharePct == null ? null : sharePct.setScale(SCALE, RoundingMode.HALF_UP);
  }
}
