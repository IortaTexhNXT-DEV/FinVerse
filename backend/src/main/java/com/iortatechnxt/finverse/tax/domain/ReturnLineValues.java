package com.iortatechnxt.finverse.tax.domain;

import java.math.BigDecimal;

/**
 * A summary figure of a worksheet (and of the return computed from it).
 *
 * @param code stable line code, e.g. OUTPUT_VAT or ATC:WC160
 * @param description description
 * @param base tax base of the line, null when not applicable
 * @param amount amount (tax, sales or credit)
 */
public record ReturnLineValues(
    String code, String description, BigDecimal base, BigDecimal amount) {}
