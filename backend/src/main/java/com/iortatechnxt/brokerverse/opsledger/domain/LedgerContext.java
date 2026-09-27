package com.iortatechnxt.brokerverse.opsledger.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The ledger an invoice posts to (DATA_MIGRATION_DESIGN 14.2): NEW for invoices booked in BIBS,
 * LEGACY for open legacy invoices and every invoice of their family. Postings of a LEGACY invoice
 * use the {@code LG_} amount components, which the accounting rules route to the legacy control
 * accounts so the legacy positions run off visibly.
 */
public enum LedgerContext {
  /** Invoices booked in BIBS. */
  NEW(""),
  /** Legacy invoices and their family. */
  LEGACY("LG_");

  /**
   * The event components that sit on a control account with a legacy twin: premium receivable, PR
   * 2307, due to insurer, commission receivable, unrealised commission and deferred output VAT (and
   * their realisation). Income, expense, bank and tax components keep their accounts.
   */
  public static final Set<String> CONTROL_COMPONENTS =
      Set.of(
          "PR_BASIC",
          "PR_DST",
          "PR_PTX_VAT",
          "PR_LGT",
          "PR_FST",
          "PR_OTHER",
          "PR2307",
          "DTIP",
          "COMMISSION_RECEIVABLE",
          "UNREALIZED_COMMISSION",
          "DEFERRED_OUTPUT_VAT",
          "REALIZED_COMMISSION",
          "REALIZED_VAT");

  private final String prefix;

  LedgerContext(String prefix) {
    this.prefix = prefix;
  }

  /**
   * The accounting amount component of this context.
   *
   * @param base component of the NEW context (e.g. PR_BASIC, DTIP, APPLIED)
   * @return the component itself for NEW, {@code LG_} and the component for LEGACY
   */
  public String component(String base) {
    return prefix + base;
  }

  /**
   * Routes the amounts of an event: in the legacy context the control components take the {@code
   * LG_} prefix, every other component is kept.
   *
   * @param amounts event amounts
   * @return routed amounts, in the same order
   */
  public Map<String, BigDecimal> route(Map<String, BigDecimal> amounts) {
    return routed(amounts);
  }

  /**
   * Routes the component parties of an event like its amounts.
   *
   * @param parties party per component
   * @return routed parties
   */
  public Map<String, String> routeParties(Map<String, String> parties) {
    return routed(parties);
  }

  private <V> Map<String, V> routed(Map<String, V> values) {
    if (this == NEW) {
      return values;
    }
    Map<String, V> out = new LinkedHashMap<>();
    values.forEach((k, v) -> out.put(CONTROL_COMPONENTS.contains(k) ? prefix + k : k, v));
    return out;
  }
}
