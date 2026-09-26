package com.iortatechnxt.brokerverse.remittance.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Security expressions and paging of the remittance endpoints (OPERATIONS_DESIGN 6.1). */
final class RemittanceAccess {

  /** Remittance team reads (processor, team leader). */
  static final String TEAM =
      "hasAnyAuthority('REMIT_PROCESS', 'REMIT_EXTRACT', 'REMIT_APPROVE', 'REMIT_OR_UPLOAD')";

  /** Readers of the batches, including the approvers. */
  static final String BATCH_READ = TEAM;

  /** Extraction runs (RMTID.001/004). */
  static final String EXTRACT = "hasAuthority('REMIT_EXTRACT')";

  /** Exclusion and restore (RMTID.002 addendum). */
  static final String EXCLUDE = "hasAuthority('REMIT_EXCLUDE')";

  /** Processor actions. */
  static final String PROCESS = "hasAuthority('REMIT_PROCESS')";

  /** Team leader approval. */
  static final String APPROVE = "hasAuthority('REMIT_APPROVE')";

  /** Return of a batch. */
  static final String RETURN = "hasAnyAuthority('REMIT_PROCESS', 'REMIT_APPROVE')";

  /** Re-assignment of a batch. */
  static final String ASSIGN = "hasAnyAuthority('WORK_ASSIGN', 'REMIT_APPROVE')";

  /** Insurer OR upload (RMTID.012/013). */
  static final String OR_UPLOAD = "hasAuthority('REMIT_OR_UPLOAD')";

  /** Hold readers: Marketing, approvers and the remittance team. */
  static final String HOLD_READ =
      "hasAnyAuthority('HOLD_REQUEST', 'HOLD_APPROVE', 'REMIT_PROCESS', 'REMIT_APPROVE')";

  /** Hold requestor (Marketing). */
  static final String HOLD_REQUEST = "hasAuthority('HOLD_REQUEST')";

  /** Hold approver. */
  static final String HOLD_APPROVE = "hasAuthority('HOLD_APPROVE')";

  /** Special remittance readers. */
  static final String SPECIAL_READ =
      "hasAnyAuthority('SPECIAL_REMIT_REQUEST', 'SPECIAL_REMIT_APPROVE', 'REMIT_PROCESS')";

  /** Special remittance requestor. */
  static final String SPECIAL_REQUEST = "hasAuthority('SPECIAL_REMIT_REQUEST')";

  /** Special remittance approver. */
  static final String SPECIAL_APPROVE = "hasAuthority('SPECIAL_REMIT_APPROVE')";

  /** Deduction readers: ACSL, the confirmers and the remittance team (ACSL 2.9.2). */
  static final String DEDUCTION_READ =
      "hasAnyAuthority('ACSL_PROCESS', 'ACSL_VIEW', 'REMIT_DEDUCTION_CONFIRM', 'REMIT_PROCESS',"
          + " 'REMIT_APPROVE')";

  /** Deduction preparer (ACSL processor). */
  static final String DEDUCTION_PREPARE = "hasAuthority('ACSL_PROCESS')";

  /** Deduction confirmer. */
  static final String DEDUCTION_CONFIRM = "hasAuthority('REMIT_DEDUCTION_CONFIRM')";

  private static final int MAX_PAGE = 200;

  private RemittanceAccess() {}

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
