package com.iortatechnxt.brokerverse.issuance.service;

import java.math.BigDecimal;

/**
 * One proposed value of an extraction (BRIDSP-02): the text found and the confidence of the pattern
 * that found it (an insurer's own pattern is more certain than a default one).
 *
 * @param value text found
 * @param confidence 0 to 1
 */
public record ExtractedValue(String value, BigDecimal confidence) {}
