package com.iortatechnxt.finverse.investment.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Coupon / interest received. Interest is first accrued up to the receipt date; the whole accrued
 * interest receivable is then relieved and any difference goes to interest income.
 *
 * @param receiptDate value date
 * @param cashAmount cash received (net of final tax)
 * @param finalTax final tax withheld at source
 * @param remarks remarks
 */
public record CouponReceiptRequest(
    @NotNull LocalDate receiptDate,
    @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal cashAmount,
    @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal finalTax,
    @Size(max = 250) String remarks) {}
