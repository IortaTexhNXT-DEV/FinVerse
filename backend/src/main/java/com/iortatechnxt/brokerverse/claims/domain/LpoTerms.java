package com.iortatechnxt.brokerverse.claims.domain;

import com.iortatechnxt.brokerverse.party.domain.Party;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Terms of a new local purchase order.
 *
 * @param garage garage party
 * @param cover own damage or third party
 * @param issueDate issue date
 * @param gross gross repair amount
 * @param discount garage discount
 * @param description repair description
 */
public record LpoTerms(
    Party garage,
    LpoCover cover,
    LocalDate issueDate,
    BigDecimal gross,
    BigDecimal discount,
    String description) {}
