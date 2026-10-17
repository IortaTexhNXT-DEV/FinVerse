package com.iortatechnxt.brokerverse.frbs.domain;

/** Enumerations of the FRBS module (ACCOUNTING_DISBURSEMENT_DESIGN 5.4, 7.3). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class FrbsEnums {

  private FrbsEnums() {}

  /** Stage of a service-fee run in workflow {@code FRBS_SERVICE_FEE} (FRBS 2.10.0-2.10.2). */
  public enum RunStage {
    /** Computed by the GL officer; can be recomputed. */
    COMPUTED,
    /** Waiting for the team lead. */
    FOR_APPROVAL,
    /** Approved: accrued and sent to Disbursement. */
    APPROVED,
    /** Every line released to its recipient. */
    RELEASED,
    /** Every line liquidated by its unit. */
    LIQUIDATED,
    /** Cancelled before approval; its invoices are free again. */
    CANCELLED;

    /**
     * Whether the run can still change (lines recomputed).
     *
     * @return true while computed
     */
    public boolean editable() {
      return this == COMPUTED;
    }
  }

  /** Status of a service-fee line (FRBS 2.10.2). */
  public enum LineStatus {
    /** Computed, not approved. */
    COMPUTED,
    /** Payout request sent to Disbursement. */
    SENT,
    /** Returned or cancelled by Disbursement; can be sent again. */
    RETURNED,
    /** Credited to the recipient (tagged or reported by Disbursement). */
    RELEASED,
    /** Liquidation report received. */
    LIQUIDATED,
    /** The run was cancelled. */
    CANCELLED
  }
}
