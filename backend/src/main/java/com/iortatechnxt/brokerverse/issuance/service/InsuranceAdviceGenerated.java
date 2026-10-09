package com.iortatechnxt.brokerverse.issuance.service;

/**
 * An Insurance Advice was generated; it is sent at once when its mortgagee bank is enrolled for
 * automatic sending (FR-NB-107).
 *
 * @param adviceId advice
 * @param marketSegment market segment of the account, may be null
 */
public record InsuranceAdviceGenerated(Long adviceId, String marketSegment) {}
