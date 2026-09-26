package com.iortatechnxt.brokerverse.opsledger.domain;

import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Header facts of an Operations invoice, copied from the booked invoice (BRNB.027). Keys to other
 * modules (ARN, invoice, client, insurer, account) are plain values (OPERATIONS_DESIGN section 3).
 *
 * @param keys company, branch, invoice, ARN, account and endorsement keys
 * @param parties client, assured, payor and lead insurer
 * @param classification currency, dates, risk, line, segment and sales stamp
 * @param amounts gross premium and commission terms
 * @param flags direct payment, 2% CWT and incentive flags
 */
public record OpsInvoiceData(
    Keys keys, Parties parties, Classification classification, Amounts amounts, Flags flags) {

  /**
   * Identifiers.
   *
   * @param companyId company
   * @param branchId booking branch
   * @param invoiceNo invoice number
   * @param arn Account Reference Number
   * @param accountId account id (plain value)
   * @param kind booking, endorsement plus / minus or cancellation
   * @param endorsementNo endorsement number, null for an original booking
   * @param parentInvoiceNo original invoice of an endorsement or cancellation
   * @param rootInvoiceNo root of the invoice family: the invoice itself for an original booking,
   *     the root of the parent chain otherwise (DIS 3.27.2, ACSL 2.16.0)
   * @param policyNo policy number
   * @param policyYear policy year
   */
  public record Keys(
      Long companyId,
      Long branchId,
      String invoiceNo,
      String arn,
      Long accountId,
      InvoiceKind kind,
      String endorsementNo,
      String parentInvoiceNo,
      String rootInvoiceNo,
      String policyNo,
      int policyYear) {}

  /**
   * Parties.
   *
   * @param clientCode client code (party of the premium receivable)
   * @param assuredName assured name
   * @param payorName payor name
   * @param insurerCode lead insurer code
   */
  public record Parties(
      String clientCode, String assuredName, String payorName, String insurerCode) {}

  /**
   * Classification.
   *
   * @param currency currency
   * @param bookingDate booking date
   * @param inceptionDate period start
   * @param expiryDate period end
   * @param riskCode BDOI risk code
   * @param productLine product line
   * @param segment market segment
   * @param aoUsername account officer
   * @param salesUnit sales unit
   * @param costCenter cost center
   */
  @Embeddable
  public record Classification(
      @Column(nullable = false, length = 3, updatable = false) String currency,
      @Column(name = "booking_date", nullable = false, updatable = false) LocalDate bookingDate,
      @Column(name = "inception_date", nullable = false, updatable = false) LocalDate inceptionDate,
      @Column(name = "expiry_date", nullable = false, updatable = false) LocalDate expiryDate,
      @Column(name = "risk_code", length = 20, updatable = false) String riskCode,
      @Column(name = "product_line", length = 30, updatable = false) String productLine,
      @Column(length = 40, updatable = false) String segment,
      @Column(name = "ao_username", length = 50, updatable = false) String aoUsername,
      @Column(name = "sales_unit", length = 20, updatable = false) String salesUnit,
      @Column(name = "cost_center", length = 20, updatable = false) String costCenter) {}

  /**
   * Amounts.
   *
   * @param grossPremium gross premium (negative for return invoices)
   * @param commission commission
   * @param vatOnCommission VAT on the commission
   * @param wtaxRate withholding tax rate on the commission, percent
   */
  public record Amounts(
      BigDecimal grossPremium,
      BigDecimal commission,
      BigDecimal vatOnCommission,
      BigDecimal wtaxRate) {}

  /**
   * Flags.
   *
   * @param directPayment premium paid directly to the insurer (BRNB.114, MKTID.011)
   * @param cwt2Percent client withholds 2% creditable tax (CSHID.020/027)
   * @param incentiveEligible incentive eligible (BRNB.107)
   */
  public record Flags(boolean directPayment, boolean cwt2Percent, boolean incentiveEligible) {}
}
