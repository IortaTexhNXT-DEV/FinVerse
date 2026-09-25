package com.iortatechnxt.brokerverse.payrequest.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Expenses of one fieldwork day as entered (Appendix D Liquidation Form).
 *
 * @param fieldworkDate date
 * @param particulars particulars
 * @param perDiem per diem
 * @param representation representation
 * @param transport transportation
 * @param lodging lodging
 * @param others other expenses
 */
public record ExpenseValues(
    LocalDate fieldworkDate,
    String particulars,
    BigDecimal perDiem,
    BigDecimal representation,
    BigDecimal transport,
    BigDecimal lodging,
    BigDecimal others) {}
