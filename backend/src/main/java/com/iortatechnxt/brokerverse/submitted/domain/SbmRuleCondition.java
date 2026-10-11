package com.iortatechnxt.brokerverse.submitted.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A condition of a rule (BRIDSP-08): a fact of the record, an operator and a value (a comma list
 * for IN and NOT_IN).
 *
 * @param field fact name
 * @param operator EQ, NE, IN, NOT_IN, GT, GTE, LT, LTE, EMPTY, NOT_EMPTY
 * @param value value, may be null for EMPTY and NOT_EMPTY
 */
@Embeddable
public record SbmRuleCondition(
    @Column(name = "field", nullable = false, length = 40) String field,
    @Column(name = "operator", nullable = false, length = 10) String operator,
    @Column(name = "value", length = 500) String value) {}
