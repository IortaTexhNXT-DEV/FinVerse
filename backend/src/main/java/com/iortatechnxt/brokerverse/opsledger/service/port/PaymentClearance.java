package com.iortatechnxt.brokerverse.opsledger.service.port;

/**
 * Whether the payment behind an acknowledgement receipt still clears: a check or a matured
 * post-dated check waits for the check holding period before remittance (RMTID.018); cash, bills
 * payment, trade, CLPC and direct credit payments are cleared when applied. An AR that Cashiering
 * does not know (migrated or test payments) is treated as a check, the safe reading.
 */
public interface PaymentClearance {

  /**
   * Tells whether the payment of an AR is a check that is still clearing.
   *
   * @param arNo AR number of the applied payment, may be null
   * @return true for a check or a matured post-dated check, or an unknown AR; false for a cleared
   *     payment
   */
  boolean clears(String arNo);
}
