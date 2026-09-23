package com.iortatechnxt.finverse.investment.api.dto;

import com.iortatechnxt.finverse.investment.domain.AmortizationMethod;
import com.iortatechnxt.finverse.investment.domain.CouponFrequency;
import com.iortatechnxt.finverse.investment.domain.DayCountConvention;
import com.iortatechnxt.finverse.investment.domain.InstrumentType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Capture (or, while pending approval, edit) an investment holding.
 *
 * @param companyId company
 * @param branchId owning branch
 * @param portfolioId portfolio (classification and GL accounts)
 * @param instrumentType instrument
 * @param securityCode ISIN / series / certificate number
 * @param description description
 * @param issuerCode issuer or depository bank party code
 * @param custodian custodian (e.g. Bureau of the Treasury RoSS, trust bank)
 * @param currency currency
 * @param faceValue face value
 * @param purchasePrice clean consideration (excluding accrued interest)
 * @param purchasedInterest accrued interest bought (computed from the coupon terms when blank)
 * @param tradeDate trade date
 * @param settlementDate settlement date (value date of the purchase)
 * @param maturityDate maturity date (blank for equities)
 * @param couponRate coupon rate, % per annum
 * @param couponFrequency coupon frequency
 * @param dayCount day count convention
 * @param amortizationMethod amortization method (defaults to effective interest, or none when
 *     bought at par)
 * @param securityDeposit deposited with the Insurance Commission as security
 * @param bankAccount GL bank account used for settlement, coupons and redemption
 * @param takeOn true for a holding brought over from a previous system
 * @param takeOnDate take-on date (value date of the opening balance)
 */
public record HoldingRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotNull Long portfolioId,
    @NotNull InstrumentType instrumentType,
    @Size(max = 40) String securityCode,
    @NotBlank @Size(max = 200) String description,
    @NotBlank @Size(max = 30) String issuerCode,
    @Size(max = 120) String custodian,
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal faceValue,
    @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal purchasePrice,
    @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal purchasedInterest,
    @NotNull LocalDate tradeDate,
    @NotNull LocalDate settlementDate,
    LocalDate maturityDate,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal couponRate,
    @NotNull CouponFrequency couponFrequency,
    @NotNull DayCountConvention dayCount,
    AmortizationMethod amortizationMethod,
    boolean securityDeposit,
    @NotBlank @Size(max = 30) String bankAccount,
    boolean takeOn,
    LocalDate takeOnDate) {}
