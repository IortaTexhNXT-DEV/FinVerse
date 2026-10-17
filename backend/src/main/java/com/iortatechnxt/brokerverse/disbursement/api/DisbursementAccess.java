package com.iortatechnxt.brokerverse.disbursement.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Security expressions and paging of the Disbursement endpoints (design 8.1). */
final class DisbursementAccess {

  /** Readers of requests and vouchers. */
  static final String READ =
      "hasAnyAuthority('DISB_VIEW', 'DISB_PROCESS', 'DISB_REVIEW', 'DISB_APPROVE')";

  /** Processor actions: encode, process, submit, instruments. */
  static final String PROCESS = "hasAuthority('DISB_PROCESS')";

  /** Team leader: submit for approval, bank accounts and check series. */
  static final String REVIEW = "hasAuthority('DISB_REVIEW')";

  /** Approver: approve, reject, authorise bank account status. */
  static final String APPROVE = "hasAuthority('DISB_APPROVE')";

  /** Cancellation: the workflow checks the permission of the voucher's stage. */
  static final String CANCEL = "hasAnyAuthority('DISB_PROCESS', 'DISB_REVIEW', 'DISB_APPROVE')";

  /** OR / AR and CWT tagging. */
  static final String TAG = "hasAuthority('DISB_TAG')";

  /** End of day. */
  static final String EOD = "hasAuthority('DISB_EOD')";

  /** End-of-day readers. */
  static final String EOD_READ = "hasAnyAuthority('DISB_EOD', 'DISB_VIEW')";

  /** Status edit approval. */
  static final String STATUS_APPROVE = "hasAuthority('DISB_STATUS_APPROVE')";

  /** Payee readers. */
  static final String PAYEE_READ =
      "hasAnyAuthority('DISB_VIEW', 'DISB_PAYEE_MAINTAIN', 'DISB_PAYEE_AUTHORIZE')";

  /** Payee maintenance. */
  static final String PAYEE_MAINTAIN = "hasAuthority('DISB_PAYEE_MAINTAIN')";

  /** Payee authorisation. */
  static final String PAYEE_AUTHORIZE = "hasAuthority('DISB_PAYEE_AUTHORIZE')";

  /** Funding readers. */
  static final String FUNDING_READ =
      "hasAnyAuthority('DISB_VIEW', 'DISB_FUNDING_REQUEST', 'DISB_FUNDING_VERIFY',"
          + " 'DISB_FUNDING_APPROVE')";

  /** Funding maker. */
  static final String FUNDING_REQUEST = "hasAuthority('DISB_FUNDING_REQUEST')";

  /** Funding verifier. */
  static final String FUNDING_VERIFY = "hasAuthority('DISB_FUNDING_VERIFY')";

  /** Funding approvers. */
  static final String FUNDING_APPROVE = "hasAuthority('DISB_FUNDING_APPROVE')";

  /** Bank account readers. */
  static final String BANK_READ = "hasAnyAuthority('DISB_VIEW', 'DISB_REVIEW', 'DISB_APPROVE')";

  /** Full payee account numbers. */
  static final String VIEW_FULL = "DISB_PAYEE_VIEW_FULL";

  private static final int MAX_PAGE = 200;

  private DisbursementAccess() {}

  /**
   * A bounded page, newest first.
   *
   * @param page page
   * @param size size
   * @return page request
   */
  static PageRequest newestFirst(int page, int size) {
    return PageRequest.of(
        Math.max(page, 0),
        Math.min(Math.max(size, 1), MAX_PAGE),
        Sort.by(Sort.Direction.DESC, "id"));
  }

  /**
   * A bounded page without sort.
   *
   * @param page page
   * @param size size
   * @return page request
   */
  static PageRequest plain(int page, int size) {
    return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE));
  }
}
