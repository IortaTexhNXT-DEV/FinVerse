package com.iortatechnxt.brokerverse.commission.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Security expressions and paging of the commission receivables endpoints (section 6.1). */
final class CommissionAccess {

  /** Reading DP lists, accounts and billings. */
  static final String READ = "hasAnyAuthority('COMMREC_PROCESS', 'COMMREC_APPROVE')";

  /** Working DP lists, accounts, billings and collections. */
  static final String PROCESS = "hasAuthority('COMMREC_PROCESS')";

  /** Reading incentive schemes and runs. */
  static final String INCENTIVE_READ =
      "hasAnyAuthority('COMMREC_PROCESS', 'COMMREC_APPROVE', 'INCENTIVE_MANAGE')";

  /** Maintaining schemes and computing runs. */
  static final String INCENTIVE = "hasAuthority('INCENTIVE_MANAGE')";

  /** Posting incentive runs (team leader). */
  static final String APPROVE = "hasAuthority('COMMREC_APPROVE')";

  /** Reading certificate submissions. */
  static final String CERT_READ = "hasAnyAuthority('BIR_CERT_SUBMIT', 'BIR_CERT_ACK')";

  /** Submitting certificates. */
  static final String CERT_SUBMIT = "hasAuthority('BIR_CERT_SUBMIT')";

  /** Comptrollership decisions. */
  static final String CERT_ACK = "hasAuthority('BIR_CERT_ACK')";

  /** Largest page served. */
  static final int MAX_PAGE = 200;

  private CommissionAccess() {}

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
