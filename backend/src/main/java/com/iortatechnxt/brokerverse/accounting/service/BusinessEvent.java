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
 * @param componentParties sub-ledger party per amount component, for events whose party lines
 *     concern more than one party (e.g. a broker booking: premium receivable from the client and
 *     due to the insurer, BRNB.027); components not listed use {@code partyCode}
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
    Map<String, String> accounts,
    Map<String, String> componentParties) {

  /** Canonical constructor copying the maps. */
  public BusinessEvent {
    amounts = Map.copyOf(amounts);
    accounts = accounts == null ? Map.of() : Map.copyOf(accounts);
    componentParties = componentParties == null ? Map.of() : Map.copyOf(componentParties);
  }

  /**
   * An event whose party lines all concern {@code partyCode}.
   *
   * @param eventType event type code
   * @param companyId company
   * @param branchId branch
   * @param valueDate accounting date
   * @param currency currency
   * @param sourceModule publishing module
   * @param sourceReference unique key of the business transaction
   * @param reference business reference
   * @param partyCode sub-ledger party
   * @param businessLine line of business
   * @param costCenter cost centre
   * @param narration narration
   * @param amounts amount components
   * @param accounts account role overrides
   */
  public BusinessEvent(
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
    this(
        eventType,
        companyId,
        branchId,
        valueDate,
        currency,
        sourceModule,
        sourceReference,
        reference,
        partyCode,
        businessLine,
        costCenter,
        narration,
        amounts,
        accounts,
        Map.of());
  }

  /**
   * The sub-ledger party of the lines of an amount component.
   *
   * @param component component name
   * @return the component's own party, else the event party
   */
  public String partyFor(String component) {
    return componentParties.getOrDefault(component, partyCode);
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
