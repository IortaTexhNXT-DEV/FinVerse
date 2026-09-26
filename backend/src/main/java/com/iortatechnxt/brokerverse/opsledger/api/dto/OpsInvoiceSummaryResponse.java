package com.iortatechnxt.brokerverse.opsledger.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An invoice in a search result (RMTID.026, ADJID.024).
 *
 * @param invoiceNo invoice number
 * @param arn Account Reference Number
 * @param kind booking, endorsement plus / minus or cancellation
 * @param policyNo policy number
 * @param clientCode client
 * @param assuredName assured
 * @param insurerCode lead insurer
 * @param currency currency
 * @param bookingDate booking date
 * @param grossPremium gross premium
 * @param premiumBalance outstanding premium receivable
 * @param paymentStatus payment status
 * @param remittanceStatus remittance status
 * @param flags flags and lock
 * @param inceptionDate period start
 * @param aoUsername account officer
 * @param rootInvoiceNo root of the invoice family (DIS 3.27.2)
 */
public record OpsInvoiceSummaryResponse(
    String invoiceNo,
    String arn,
    InvoiceKind kind,
    String policyNo,
    String clientCode,
    String assuredName,
    String insurerCode,
    String currency,
    LocalDate bookingDate,
    BigDecimal grossPremium,
    BigDecimal premiumBalance,
    PaymentStatus paymentStatus,
    RemittanceStatus remittanceStatus,
    FlagsResponse flags,
    LocalDate inceptionDate,
    String aoUsername,
    String rootInvoiceNo) {

  /**
   * Maps an invoice (components loaded).
   *
   * @param i invoice
   * @return response
   */
  public static OpsInvoiceSummaryResponse from(OpsInvoice i) {
    return new OpsInvoiceSummaryResponse(
        i.getInvoiceNo(),
        i.getArn(),
        i.getKind(),
        i.getPolicyNo(),
        i.getClientCode(),
        i.getAssuredName(),
        i.getInsurerCode(),
        i.getCurrency(),
        i.getBookingDate(),
        i.getGrossPremium(),
        i.premiumBalance(),
        i.getPaymentStatus(),
        i.getRemittanceStatus(),
        FlagsResponse.from(i),
        i.getClassification().inceptionDate(),
        i.getClassification().aoUsername(),
        i.getRootInvoiceNo());
  }
}
