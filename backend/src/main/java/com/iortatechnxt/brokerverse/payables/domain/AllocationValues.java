package com.iortatechnxt.brokerverse.payables.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An open CREDIT item selected for payment.
 *
 * @param openItemId open item id
 * @param documentType document type of the item
 * @param documentNo document number of the item
 * @param documentDate document date of the item
 * @param dueDate due date of the item
 * @param amount amount paid against the item (item currency)
 */
public record AllocationValues(
    Long openItemId,
    String documentType,
    String documentNo,
    LocalDate documentDate,
    LocalDate dueDate,
    BigDecimal amount) {}
