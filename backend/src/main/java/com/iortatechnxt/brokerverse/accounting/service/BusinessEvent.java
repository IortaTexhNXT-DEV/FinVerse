package com.iortatechnxt.brokerverse.accounting.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A business transaction that has financial consequences, published by an operational module
 * (underwriting, claims, reinsurance, receipts, payments...) to the accounting engine.
 *
 * <p>The engine turns it into a balanced journal using the configured accounting rule. The module
 * never chooses GL accounts itself; it only states what happened and the amounts involved.
 *
 * @param eventType event type code, e.g. POLICY_ISSUE (see acc_event_type)
 * @param companyId company
 * @param branchId branch that owns the transaction
 * @param valueDate accounting date
 * @param currency transaction currency
 * @param sourceModule publishing module, e.g. UNDERWRITING
 * @param sourceReference unique key of the business transaction (idempotency)
 * @param reference business reference shown on the ledger (policy, claim, receipt no.)
 * @param partyCode sub-ledger party (client, intermediary, reinsurer, supplier)
 * @param businessLine line of business dimension
 * @param costCenter cost centre dimension
 * @param narration narration
 * @param amounts amount components by name (e.g. GROSS_PREMIUM, DST, TOTAL_DUE)
 * @param accounts account role overrides for {@code @ROLE} rule lines (e.g. BANK -> 1111)
 */
public record BusinessEvent(
    String eventType,
    Long companyId,
    Long branchId,
    LocalDate valueDate,
    String currency,
    String sourceModule,
    String sourceReference,
    String reference,
    String partyCode,
    String businessLine,
    String costCenter,
    String narration,
    Map<String, BigDecimal> amounts,
    Map<String, String> accounts) {

  /** Canonical constructor copying the maps. */
  public BusinessEvent {
    amounts = Map.copyOf(amounts);
    accounts = accounts == null ? Map.of() : Map.copyOf(accounts);
  }

  /**
   * Returns an amount component, zero when absent.
   *
   * @param component component name
   * @return amount
   */
  public BigDecimal amount(String component) {
    return amounts.getOrDefault(component, BigDecimal.ZERO);
  }

  /**
   * Compact text of the amount components (for the event log).
   *
   * @return "NAME=value, ..." sorted by name
   */
  public String amountsText() {
    return amounts.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(e -> e.getKey() + "=" + e.getValue().toPlainString())
        .collect(Collectors.joining(", "));
  }
}
