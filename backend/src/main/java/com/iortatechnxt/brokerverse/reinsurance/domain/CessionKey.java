package com.iortatechnxt.brokerverse.reinsurance.domain;

/**
 * Transaction key of a cession.
 *
 * @param policyId policy
 * @param endorsementNo endorsement number (0 = original issue)
 */
public record CessionKey(Long policyId, int endorsementNo) {}
