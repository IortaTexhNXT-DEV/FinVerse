package com.iortatechnxt.brokerverse.nonpackage.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Terms quoted by an insurer (BRNB.009).
 *
 * @param status received or declined (pending while nothing is keyed in)
 * @param premium premium quoted
 * @param rate rate in percent
 * @param deductibles deductibles
 * @param conditions conditions, warranties, exclusions
 * @param validUntil validity of the offer
 * @param remarks remarks
 */
public record ResponseTerms(
    ResponseStatus status,
    BigDecimal premium,
    BigDecimal rate,
    String deductibles,
    String conditions,
    LocalDate validUntil,
    String remarks) {}
