package com.iortatechnxt.brokerverse.closing.api.dto;

import com.iortatechnxt.brokerverse.period.domain.PeriodModuleLock;
import java.time.Instant;

/**
 * Cut-off of the broking books of a period (FRBS 3.4.0).
 *
 * @param periodId period
 * @param periodName period name
 * @param module group of books
 * @param locked whether closed
 * @param lockedBy closed by
 * @param lockedAt closed at
 * @param unlockedBy reopened by
 * @param unlockedAt reopened at
 * @param note pending items at cut-off, or the reason of the reopening
 */
public record BooksCutoffResponse(
    Long periodId,
    String periodName,
    String module,
    boolean locked,
    String lockedBy,
    Instant lockedAt,
    String unlockedBy,
    Instant unlockedAt,
    String note) {

  /**
   * Maps an entity.
   *
   * @param l cut-off record
   * @param periodName name of its period
   * @return response
   */
  public static BooksCutoffResponse from(PeriodModuleLock l, String periodName) {
    return new BooksCutoffResponse(
        l.getPeriodId(),
        periodName,
        l.getModule(),
        l.isLocked(),
        l.getLockedBy(),
        l.getLockedAt(),
        l.getUnlockedBy(),
        l.getUnlockedAt(),
        l.getNote());
  }
}
