package com.iortatechnxt.brokerverse.opsledger.api.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceMasterFacts;
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
 * @param master Invoice Master List values of the account (BR-175), null when not read
 * @param origin BIBS or MIGRATED with the legacy facts (unwrapped)
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
    String rootInvoiceNo,
    InvoiceMasterFacts.Facts master,
    @JsonUnwrapped InvoiceOriginResponse origin) {

  /**
   * Maps an invoice (components loaded).
   *
   * @param i invoice
   * @return response
   */
  public static OpsInvoiceSummaryResponse from(OpsInvoice i) {
    return from(i, null);
  }

  /**
   * Maps an invoice of the Invoice Master List (components loaded) with its account values.
   *
   * @param i invoice
   * @param master account values, null when the invoice has no account
   * @return response
   */
  public static OpsInvoiceSummaryResponse from(OpsInvoice i, InvoiceMasterFacts.Facts master) {
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
        i.getRootInvoiceNo(),
        master,
        InvoiceOriginResponse.from(i));
  }
}
