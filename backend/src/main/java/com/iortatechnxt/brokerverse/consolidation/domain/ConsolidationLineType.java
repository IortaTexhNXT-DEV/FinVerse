package com.iortatechnxt.brokerverse.consolidation.domain;

/**
 * Kind of consolidated ledger line.
 *
 * <ul>
 *   <li>TRANSLATED: a member's account balance translated into the consolidation currency.
 *   <li>CTA: currency translation adjustment that keeps a translated trial balance in balance.
 *   <li>ELIMINATION: consolidation adjustment (inter-company balances, investment/equity).
 * </ul>
 */
public enum ConsolidationLineType {
  TRANSLATED,
  CTA,
  ELIMINATION
}
