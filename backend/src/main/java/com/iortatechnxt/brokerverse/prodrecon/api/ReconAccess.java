package com.iortatechnxt.brokerverse.prodrecon.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Security expressions and paging of the production reconciliation endpoints (section 6.1). */
final class ReconAccess {

  /** Reading cycles, extracts, items and uploads: handlers and senders. */
  static final String READ = "hasAnyAuthority('RECON_PROCESS', 'RECON_SEND')";

  /** Extracting, uploading, matching, feedback, schedules and closing. */
  static final String PROCESS = "hasAuthority('RECON_PROCESS')";

  /** Sending registers to insurers. */
  static final String SEND = "hasAuthority('RECON_SEND')";

  /** Largest page served. */
  static final int MAX_PAGE = 200;

  private ReconAccess() {}

  /**
   * A bounded page request.
   *
   * @param page page
   * @param size size
   * @return page request
   */
  static Pageable page(int page, int size) {
    return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE));
  }
}
