package com.iortatechnxt.brokerverse.placement.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row of a payment report as read from the file.
 *
 * @param rowNo row number in the file
 * @param reference PN / loan application number (CLPC) or ARN
 * @param paid reported paid
 * @param amount amount paid, may be null
 * @param paidOn payment date, may be null
 */
public record ReportedPayment(
    int rowNo, String reference, boolean paid, BigDecimal amount, LocalDate paidOn) {}
