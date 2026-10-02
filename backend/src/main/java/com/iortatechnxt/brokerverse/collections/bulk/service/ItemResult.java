package com.iortatechnxt.brokerverse.collections.bulk.service;

/**
 * Outcome of a bulk action on one collection account (BRCLXN.050/051): done, or why it was refused.
 *
 * @param reference invoice number
 * @param ok whether it was done
 * @param message what was done or the reason of the refusal
 */
public record ItemResult(String reference, boolean ok, String message) {

  /**
   * A success.
   *
   * @param reference invoice
   * @param message what was done
   * @return result
   */
  public static ItemResult done(String reference, String message) {
    return new ItemResult(reference, true, message);
  }

  /**
   * A refusal.
   *
   * @param reference invoice
   * @param message reason
   * @return result
   */
  public static ItemResult refused(String reference, String message) {
    return new ItemResult(reference, false, message);
  }
}
