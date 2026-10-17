package com.iortatechnxt.brokerverse.approval.service;

/**
 * Port through which a module lets the approval inbox approve several of its items at once (FRBS
 * 2.5.6, BASAU 2.5.3). Implement it next to the module's {@link PendingApprovalSource}; the
 * implementation checks the approving permission and runs every approval in its own transaction
 * with exactly the controls of the single approval (maker-checker, limits, re-validation).
 */
public interface BulkApprovalAction {

  /**
   * Whether this action approves items of a module and type, as listed in the inbox.
   *
   * @param module module of the {@link PendingApproval}
   * @param type type of the {@link PendingApproval}
   * @return true when supported
   */
  boolean supports(String module, String type);

  /**
   * Approves one item.
   *
   * @param companyId company of the item (may be null)
   * @param reference reference of the item
   * @return short outcome text, e.g. "Posted"
   */
  String approve(Long companyId, String reference);
}
