package com.iortatechnxt.finverse.reserves.domain;

import java.util.Comparator;

/**
 * Reporting unit of a reserve: branch, line of business, product and channel (source type).
 *
 * @param branchId branch
 * @param businessLine line of business
 * @param productCode product code
 * @param sourceType channel (DIRECT, AGENT, BROKER)
 */
public record ReserveKey(
    Long branchId, String businessLine, String productCode, String sourceType) {

  /** Display order: branch, line of business, product, channel. */
  public static final Comparator<ReserveKey> ORDER =
      Comparator.comparing(ReserveKey::branchId)
          .thenComparing(ReserveKey::businessLine)
          .thenComparing(ReserveKey::productCode)
          .thenComparing(ReserveKey::sourceType);

  /**
   * Posting unit of this key: branch and line of business.
   *
   * @return key with product and channel blanked out
   */
  public PostingKey postingKey() {
    return new PostingKey(branchId, businessLine);
  }

  /**
   * Unit at which reserve movements are posted: branch and line of business.
   *
   * @param branchId branch
   * @param businessLine line of business
   */
  public record PostingKey(Long branchId, String businessLine) {

    /** Posting order: branch, then line of business. */
    public static final Comparator<PostingKey> ORDER =
        Comparator.comparing(PostingKey::branchId).thenComparing(PostingKey::businessLine);
  }
}
