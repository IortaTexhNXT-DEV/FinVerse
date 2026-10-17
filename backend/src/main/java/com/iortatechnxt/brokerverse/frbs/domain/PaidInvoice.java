package com.iortatechnxt.brokerverse.frbs.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An invoice of the Operations ledger fully paid in a period, candidate for the service fee (FRBS
 * 2.10.0).
 *
 * @param invoiceNo invoice
 * @param rootInvoiceNo root of the invoice family
 * @param clientCode client
 * @param assuredName assured
 * @param insurerCode insurer
 * @param marketSegment market segment (LOV {@code MARKET_SEGMENT})
 * @param salesUnit sales unit
 * @param costCenter cost centre of the invoice
 * @param branchId branch
 * @param currency currency
 * @param commission commission (without VAT)
 * @param wtax withholding tax of the insurer on the commission
 * @param paidOn date the invoice became fully paid
 */
public record PaidInvoice(
    String invoiceNo,
    String rootInvoiceNo,
    String clientCode,
    String assuredName,
    String insurerCode,
    String marketSegment,
    String salesUnit,
    String costCenter,
    Long branchId,
    String currency,
    BigDecimal commission,
    BigDecimal wtax,
    LocalDate paidOn) {}
