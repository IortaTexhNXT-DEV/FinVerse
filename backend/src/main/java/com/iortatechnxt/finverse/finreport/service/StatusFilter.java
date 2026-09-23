package com.iortatechnxt.finverse.finreport.service;

import java.util.List;

/**
 * Transaction status selection of the finance reports (PREMIA "Posted / Unposted / Both").
 *
 * <p>Posted = journals that reached the ledger ({@code POSTED}, and {@code REVERSED} originals
 * which stay posted and are offset by their reversal). Unposted = saved but not yet posted journals
 * ({@code DRAFT}, {@code PENDING_APPROVAL}). Rejected and cancelled journals are never included.
 */
public enum StatusFilter {
  POSTED(List.of("POSTED", "REVERSED")),
  UNPOSTED(List.of("DRAFT", "PENDING_APPROVAL")),
  BOTH(List.of("POSTED", "REVERSED", "DRAFT", "PENDING_APPROVAL"));

  private final List<String> statuses;

  StatusFilter(List<String> statuses) {
    this.statuses = statuses;
  }

  /**
   * Journal statuses selected.
   *
   * @return status names
   */
  public List<String> statuses() {
    return statuses;
  }

  /**
   * Option names for report parameters.
   *
   * @return names
   */
  public static List<String> options() {
    return List.of(POSTED.name(), UNPOSTED.name(), BOTH.name());
  }

  /**
   * One-letter status flag printed on listings.
   *
   * @param journalStatus journal status name
   * @return "P" for posted, "U" for unposted
   */
  public static String flag(String journalStatus) {
    return POSTED.statuses.contains(journalStatus) ? "P" : "U";
  }
}
