package com.iortatechnxt.brokerverse.collections.bulk.service;

import java.util.List;

/**
 * Port of the Collections bulk update (BRCLXN.051) for the fields the worklist owns (wave C1-A):
 * the PR disposition, a collection effort and remarks of a collection account, recorded with the
 * bulk reference in the field-change log. The worklist implements it as a component-scanned bean;
 * until then {@link PendingWorklistUpdates} refuses those columns, so a bulk update can still set
 * promises and escalations.
 */
public interface WorklistUpdates {

  /**
   * Checks an update without changing anything (no exception: the errors are returned).
   *
   * @param companyId company
   * @param update the fields to change
   * @return error messages; empty when valid
   */
  List<String> validate(Long companyId, ItemUpdate update);

  /**
   * Applies an update in the caller's transaction.
   *
   * @param companyId company
   * @param update the fields to change
   * @param bulkRef bulk upload or bulk action reference
   * @return reference of what was recorded
   */
  String apply(Long companyId, ItemUpdate update, String bulkRef);

  /**
   * Worklist fields of one collection account.
   *
   * @param invoiceNo invoice
   * @param dispositionCode PR disposition (LOV CLX_PR_DISPOSITION), may be null
   * @param effortCode collection effort (LOV CLX_EFFORT_CODE), may be null
   * @param remarks remarks, may be null
   */
  record ItemUpdate(String invoiceNo, String dispositionCode, String effortCode, String remarks) {

    /**
     * Whether the update changes anything.
     *
     * @return true when a field is given
     */
    public boolean isEmpty() {
      return dispositionCode == null && effortCode == null && remarks == null;
    }
  }
}
