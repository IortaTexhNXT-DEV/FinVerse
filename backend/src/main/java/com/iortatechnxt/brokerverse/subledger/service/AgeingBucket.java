package com.iortatechnxt.brokerverse.subledger.service;

/**
 * Ageing bucket by days past due, inclusive bounds; {@code toDays} null = open ended.
 *
 * @param label column label
 * @param fromDays lower bound (days)
 * @param toDays upper bound (days) or null
 */
public record AgeingBucket(String label, int fromDays, Integer toDays) {

  /**
   * Checks whether a number of days falls in the bucket.
   *
   * @param days days past due (negative = not yet due)
   * @return true when inside
   */
  public boolean contains(long days) {
    return days >= fromDays && (toDays == null || days <= toDays);
  }
}
