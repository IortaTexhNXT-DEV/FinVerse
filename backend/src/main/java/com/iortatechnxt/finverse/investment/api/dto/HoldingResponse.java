package com.iortatechnxt.finverse.investment.api.dto;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import com.iortatechnxt.finverse.investment.domain.AmortizationMethod;
import com.iortatechnxt.finverse.investment.domain.Classification;
import com.iortatechnxt.finverse.investment.domain.CouponFrequency;
import com.iortatechnxt.finverse.investment.domain.DayCountConvention;
import com.iortatechnxt.finverse.investment.domain.HoldingStatus;
import com.iortatechnxt.finverse.investment.domain.HoldingTerms;
import com.iortatechnxt.finverse.investment.domain.InstrumentType;
import com.iortatechnxt.finverse.investment.domain.InvestmentHolding;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Investment holding view.
 *
 * @param id id
 * @param companyId company
 * @param branchId branch
 * @param portfolioId portfolio
 * @param portfolioCode portfolio code
 * @param classification classification
 * @param holdingNo holding number
 * @param instrumentType instrument
 * @param securityCode security code
 * @param description description
 * @param issuerCode issuer / bank
 * @param custodian custodian
 * @param currency currency
 * @param faceValue face value
 * @param purchasePrice clean purchase price
 * @param purchasedInterest purchased accrued interest
 * @param tradeDate trade date
 * @param settlementDate settlement date
 * @param maturityDate maturity date
 * @param couponRate coupon rate %
 * @param couponFrequency coupon frequency
 * @param dayCount day count
 * @param amortizationMethod amortization method
 * @param effectiveRate effective annual rate %
 * @param securityDeposit security deposit flag
 * @param bankAccount settlement bank account
 * @param takeOn take-on flag
 * @param amortizedCost amortized cost
 * @param accruedInterest accrued interest receivable
 * @param fairValueAdjustment cumulative fair value adjustment
 * @param carryingAmount carrying amount
 * @param fairValue last fair value
 * @param fairValueDate last valuation date
 * @param lastAccrualDate interest accrued up to
 * @param lastAmortizationDate amortized up to
 * @param status life-cycle status
 * @param closedDate maturity / sale date
 * @param purchaseBatchNo purchase journal
 * @param recordStatus maker-checker status
 * @param createdBy creator
 * @param maker user who created or last maintained the record (unchanged by authorization)
 * @param authorizedBy checker
 */
public record HoldingResponse(
    Long id,
    Long companyId,
    Long branchId,
    Long portfolioId,
    String portfolioCode,
    Classification classification,
    String holdingNo,
    InstrumentType instrumentType,
    String securityCode,
    String description,
    String issuerCode,
    String custodian,
    String currency,
    BigDecimal faceValue,
    BigDecimal purchasePrice,
    BigDecimal purchasedInterest,
    LocalDate tradeDate,
    LocalDate settlementDate,
    LocalDate maturityDate,
    BigDecimal couponRate,
    CouponFrequency couponFrequency,
    DayCountConvention dayCount,
    AmortizationMethod amortizationMethod,
    BigDecimal effectiveRate,
    boolean securityDeposit,
    String bankAccount,
    boolean takeOn,
    BigDecimal amortizedCost,
    BigDecimal accruedInterest,
    BigDecimal fairValueAdjustment,
    BigDecimal carryingAmount,
    BigDecimal fairValue,
    LocalDate fairValueDate,
    LocalDate lastAccrualDate,
    LocalDate lastAmortizationDate,
    HoldingStatus status,
    LocalDate closedDate,
    String purchaseBatchNo,
    RecordStatus recordStatus,
    String createdBy,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param h holding
   * @return response
   */
  public static HoldingResponse from(InvestmentHolding h) {
    HoldingTerms t = h.terms();
    return new HoldingResponse(
        h.getId(),
        h.getCompanyId(),
        h.getBranchId(),
        h.getPortfolio().getId(),
        h.getPortfolio().getCode(),
        h.getPortfolio().getClassification(),
        h.getHoldingNo(),
        h.getInstrumentType(),
        h.getSecurityCode(),
        h.getDescription(),
        h.getIssuerCode(),
        h.getCustodian(),
        h.getCurrency(),
        t.faceValue(),
        t.purchasePrice(),
        t.purchasedInterest(),
        t.tradeDate(),
        t.settlementDate(),
        t.maturityDate(),
        t.couponRate(),
        t.couponFrequency(),
        t.dayCount(),
        t.amortizationMethod(),
        h.getEffectiveRate(),
        h.isSecurityDeposit(),
        h.getBankAccount(),
        h.isTakeOn(),
        h.getAmortizedCost(),
        h.getAccruedInterest(),
        h.getFairValueAdjustment(),
        h.carryingAmount(),
        h.getFairValue(),
        h.getFairValueDate(),
        h.getLastAccrualDate(),
        h.getLastAmortizationDate(),
        h.getStatus(),
        h.getClosedDate(),
        h.getPurchaseBatchNo(),
        h.getRecordStatus(),
        h.getCreatedBy(),
        h.getMaker(),
        h.getAuthorizedBy());
  }
}
