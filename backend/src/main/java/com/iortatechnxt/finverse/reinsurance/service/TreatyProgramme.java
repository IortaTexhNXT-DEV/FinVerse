package com.iortatechnxt.finverse.reinsurance.service;

import com.iortatechnxt.finverse.reinsurance.domain.Treaty;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyType;
import com.iortatechnxt.finverse.reinsurance.service.AllocationMath.Capacity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * The authorized treaties of one line of business and underwriting year: at most one of each type.
 *
 * @param quotaShare quota share treaty, null when none
 * @param surplus surplus treaty, null when none
 * @param excessOfLoss excess of loss treaty, null when none
 */
public record TreatyProgramme(Treaty quotaShare, Treaty surplus, Treaty excessOfLoss) {

  /** A programme without treaties (everything retained). */
  public static final TreatyProgramme EMPTY = new TreatyProgramme(null, null, null);

  /**
   * Builds a programme from its treaties.
   *
   * @param treaties authorized treaties of the programme
   * @return programme
   */
  public static TreatyProgramme of(List<Treaty> treaties) {
    return new TreatyProgramme(
        find(treaties, TreatyType.QUOTA_SHARE).orElse(null),
        find(treaties, TreatyType.SURPLUS).orElse(null),
        find(treaties, TreatyType.XOL).orElse(null));
  }

  /**
   * Proportional capacity per risk (see {@link AllocationMath}).
   *
   * @return capacity
   */
  public Capacity capacity() {
    if (surplus != null) {
      return new Capacity(
          quotaShare == null ? BigDecimal.ZERO : quotaShare.quotaShareFraction(),
          surplus.getRetentionLimit(),
          surplus.getLines());
    }
    if (quotaShare != null) {
      return new Capacity(quotaShare.quotaShareFraction(), quotaShare.getTreatyLimit(), 0);
    }
    return Capacity.NONE;
  }

  /**
   * Whether a proportional treaty applies.
   *
   * @return true when a quota share or surplus exists
   */
  public boolean isProportional() {
    return quotaShare != null || surplus != null;
  }

  private static Optional<Treaty> find(List<Treaty> treaties, TreatyType type) {
    return treaties.stream().filter(t -> t.getTreatyType() == type).findFirst();
  }
}
