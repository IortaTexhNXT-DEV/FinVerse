package com.iortatechnxt.brokerverse.crm.service;

import java.util.Set;

/**
 * A change of a client's risk profile requested by risk profiling (SNSRP-302, 304).
 *
 * @param riskRating new KYC risk rating (list of values KYC_RISK_RATING), null to keep it
 * @param addTags tags to add (list of values CLIENT_TAG, e.g. PEP, WATCHLIST_REVIEW)
 * @param removeTags tags to end (history is kept)
 * @param source who decided: RULE (risk rules) or MANUAL (investigator with justification)
 * @param reason justification or rule description
 * @param reference evidence reference (screening case or match number)
 */
public record RiskProfileChange(
    String riskRating,
    Set<String> addTags,
    Set<String> removeTags,
    String source,
    String reason,
    String reference) {

  /** Defensive copies; null tag sets are empty. */
  public RiskProfileChange {
    addTags = addTags == null ? Set.of() : Set.copyOf(addTags);
    removeTags = removeTags == null ? Set.of() : Set.copyOf(removeTags);
  }
}
