package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Movements an Operations module posts on one invoice for one business transaction (RMTID.038). The
 * pair (source module, source reference) is the idempotency key: posting the same request twice
 * returns the first movements. Record the movement in the same transaction as the business event
 * and its journal.
 *
 * @param invoiceNo invoice number
 * @param type movement type
 * @param sourceModule module posting (e.g. CASHIERING)
 * @param sourceRef business transaction (e.g. {@code APP:123}), at most 80 characters
 * @param valueDate value date
 * @param amounts signed amount per component; zero amounts are ignored
 * @param refs AR, OR, batch and journal numbers
 * @param remarks remarks, may be null
 */
public record MovementRequest(
    String invoiceNo,
    MovementType type,
    String sourceModule,
    String sourceRef,
    LocalDate valueDate,
    Map<LedgerComponent, BigDecimal> amounts,
    DocumentRefs refs,
    String remarks) {

  /** Copies the amounts in component order; no references means none. */
  public MovementRequest {
    Map<LedgerComponent, BigDecimal> ordered = new EnumMap<>(LedgerComponent.class);
    if (amounts != null) {
      amounts.forEach(
          (component, amount) -> {
            if (amount != null && amount.signum() != 0) {
              ordered.put(component, amount);
            }
          });
    }
    amounts = Collections.unmodifiableMap(ordered);
    refs = refs == null ? DocumentRefs.NONE : refs;
  }

  /**
   * Document numbers carried by a movement.
   *
   * @param arNo acknowledgement receipt number
   * @param orNo official receipt number
   * @param batchNo remittance, adjustment or other batch
   * @param journalBatchNo GL journal batch
   */
  public record DocumentRefs(String arNo, String orNo, String batchNo, String journalBatchNo) {

    /** No document numbers. */
    public static final DocumentRefs NONE = new DocumentRefs(null, null, null, null);
  }
}
