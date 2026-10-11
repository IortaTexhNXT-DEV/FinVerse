package com.iortatechnxt.brokerverse.commission.service;

/**
 * Columns of the insurer's answer to a commission billing (CMRID.009, format parked OQ38): the
 * billing file returned with a decision per account.
 */
public final class DpResponseLayout {

  /** Billing number. */
  public static final String BILLING = "Billing No.";

  /** Invoice number. */
  public static final String INVOICE = "Invoice No.";

  /** APPROVED or REJECTED. */
  public static final String DECISION = "Decision";

  /** Reason of a rejection (LOV DP_FEEDBACK_REASON). */
  public static final String REASON = "Reason";

  /** Comment. */
  public static final String COMMENT = "Comment";

  private DpResponseLayout() {}
}
