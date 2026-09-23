package com.iortatechnxt.finverse.reinsurance.domain;

import java.math.BigDecimal;

/**
 * An insured risk as allocated in one cession.
 *
 * @param riskId underwriting risk id
 * @param lineNo risk line number within the policy
 * @param description risk description
 * @param ourSi company sum insured of the risk in the transaction (base of the share %)
 * @param ourPremium company net premium of the risk in the transaction
 */
public record RiskRef(
    Long riskId, int lineNo, String description, BigDecimal ourSi, BigDecimal ourPremium) {}
