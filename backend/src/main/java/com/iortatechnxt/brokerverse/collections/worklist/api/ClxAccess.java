package com.iortatechnxt.brokerverse.collections.worklist.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Security expressions and paging of the Collections endpoints (COLLECTIONS_DESIGN 6.1). */
public final class ClxAccess {

  /** Worklist, account page, client view (read-only). */
  public static final String VIEW = "hasAuthority('CLX_VIEW')";

  /** Efforts, dispositions, remarks and the edit lock. */
  public static final String WORK = "hasAuthority('CLX_WORK')";

  /** Reassignment and assignment rules. */
  public static final String ASSIGN = "hasAuthority('CLX_ASSIGN')";

  /** Collections Setup. */
  public static final String SETUP = "hasAuthority('CLX_SETUP')";

  /** Exports. */
  public static final String EXPORT = "hasAuthority('CLX_EXPORT')";

  /** The Files screen. */
  public static final String FILES = "hasAnyAuthority('CLX_REPORT_VIEW', 'CLX_EXPORT')";

  /** The Collections home (every Collections persona). */
  public static final String HOME =
      "hasAnyAuthority('CLX_VIEW', 'CLX_UNAPPLIED_WORK', 'CLX_SETUP', 'CLX_REPORT_VIEW',"
          + " 'CLX_AUDIT_VIEW')";

  /** Pick lists of handlers (work, assignment, set-up). */
  public static final String HANDLERS = "hasAnyAuthority('CLX_WORK', 'CLX_ASSIGN', 'CLX_SETUP')";

  private static final int MAX_PAGE = 200;

  private ClxAccess() {}

  /**
   * A bounded page.
   *
   * @param page page
   * @param size size
   * @param sort sort
   * @return page request
   */
  public static PageRequest page(int page, int size, Sort sort) {
    return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE), sort);
  }
}
