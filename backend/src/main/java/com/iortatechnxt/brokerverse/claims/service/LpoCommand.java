package com.iortatechnxt.brokerverse.claims.service;

import com.iortatechnxt.brokerverse.claims.domain.LpoCover;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input of a local purchase order.
 *
 * @param garageCode garage party code
 * @param cover own damage or third party
 * @param issueDate issue date, null for today
 * @param gross gross repair amount
 * @param discount garage discount, may be null
 * @param description repair description
 */
public record LpoCommand(
    String garageCode,
    LpoCover cover,
    LocalDate issueDate,
    BigDecimal gross,
    BigDecimal discount,
    String description) {}
