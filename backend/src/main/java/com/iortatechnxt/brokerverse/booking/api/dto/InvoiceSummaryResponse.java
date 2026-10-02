package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A booked invoice in a list.
 *
 * @param id id
 * @param invoiceNo invoice number
 * @param arn account
 * @param accountId account id
 * @param kind kind
 * @param status status
 * @param endorsementNo endorsement number
 * @param policyYear policy year
 * @param policyNo policy number
 * @param clientCode client code
 * @param clientName client name
 * @param insurerCode lead insurer
 * @param riskCode risk code
 * @param lineCode product line
 * @param department sales department
 * @param costCenter cost center
 * @param currency currency
 * @param bookingDate booking date
 * @param inceptionDate period start
 * @param expiryDate period end
 * @param grossPremium gross premium
 * @param commission commission
 * @param directPayment direct payment
 * @param incentiveEligible incentive eligible
 */
public record InvoiceSummaryResponse(
    Long id,
    String invoiceNo,
    String arn,
    Long accountId,
    InvoiceKind kind,
    InvoiceStatus status,
    String endorsementNo,
    int policyYear,
    String policyNo,
    String clientCode,
    String clientName,
    String insurerCode,
    String riskCode,
    String lineCode,
    String department,
    String costCenter,
    String currency,
    LocalDate bookingDate,
    LocalDate inceptionDate,
    LocalDate expiryDate,
    BigDecimal grossPremium,
    BigDecimal commission,
    boolean directPayment,
    boolean incentiveEligible) {

  /**
   * Maps an invoice (no collections touched).
   *
   * @param i invoice
   * @return response
   */
  public static InvoiceSummaryResponse from(BookedInvoice i) {
    return new InvoiceSummaryResponse(
        i.getId(),
        i.getInvoiceNo(),
        i.getArn(),
        i.getAccountId(),
        i.getKind(),
        i.getStatus(),
        i.getEndorsementNo(),
        i.getPolicyYear(),
        i.getPolicyNo(),
        i.getFacts().clientCode(),
        i.getFacts().clientName(),
        i.getFacts().insurerCode(),
        i.getFacts().riskCode(),
        i.getFacts().lineCode(),
        i.getFacts().department(),
        i.getFacts().costCenter(),
        i.getCurrency(),
        i.getBookingDate(),
        i.getInceptionDate(),
        i.getExpiryDate(),
        i.getPremium().total(),
        i.getCommission().commission(),
        i.getFlags().directPayment(),
        i.getFlags().incentiveEligible());
  }
}
