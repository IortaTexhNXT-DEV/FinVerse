package com.iortatechnxt.brokerverse.underwriting.service;

import java.util.Collection;
import java.util.Map;

/**
 * PORT implemented by the reinsurance module: how the company's net premium of each premium
 * transaction (policy issue or endorsement) was ceded.
 *
 * <p>Underwriting does not depend on reinsurance: it injects this interface optionally. When no
 * implementation is deployed, or a transaction has no entry in the returned map, underwriting
 * reports show zero treaty and FAC premium and the whole net premium as retention.
 *
 * <p>Implementation contract: read-only, no side effects, may be called with up to a few thousand
 * references per report run (query in bulk), amounts in the policy currency, negative for return
 * premium transactions.
 */
public interface PolicyReinsuranceView {

  /**
   * Reinsurance figures of premium transactions.
   *
   * @param transactions transactions (see {@link TransactionRef})
   * @return figures by transaction; transactions without cession may be omitted
   */
  Map<TransactionRef, ReinsuranceFigures> figures(Collection<TransactionRef> transactions);
}
