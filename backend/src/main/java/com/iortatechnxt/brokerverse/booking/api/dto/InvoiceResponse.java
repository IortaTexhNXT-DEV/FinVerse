package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceFacts;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceFlags;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A booked invoice with its components, commission, shares and journals.
 *
 * @param id id
 * @param invoiceNo invoice number
 * @param arn account
 * @param accountId account id
 * @param transactionNo transaction of the account (booking key)
 * @param kind kind
 * @param status status
 * @param endorsementNo endorsement number
 * @param parentInvoiceNo invoice an endorsement relates to
 * @param rootInvoiceNo root of the invoice family (DIS 3.27.2)
 * @param policyYear policy year
 * @param policyNo policy number
 * @param facts client, insurer, risk, sales and cost center
 * @param currency currency
 * @param bookingDate booking date
 * @param inceptionDate period start
 * @param expiryDate period end
 * @param premium premium by component
 * @param commission commission terms
 * @param flags direct payment, CWT 2 %, incentive, business type
 * @param source how it was booked
 * @param serviceInvoiceNo first service invoice
 * @param bookedBy user
 * @param bookedAt time
 * @param shares insurer shares
 * @param journalBatches journal batches
 * @param insurerBillingNo insurer billing number (BRID-020), null when none
 */
public record InvoiceResponse(
    Long id,
    String invoiceNo,
    String arn,
    Long accountId,
    String transactionNo,
    InvoiceKind kind,
    InvoiceStatus status,
    String endorsementNo,
    String parentInvoiceNo,
    String rootInvoiceNo,
    int policyYear,
    String policyNo,
    InvoiceFacts facts,
    String currency,
    LocalDate bookingDate,
    LocalDate inceptionDate,
    LocalDate expiryDate,
    PremiumDto premium,
    CommissionDto commission,
    InvoiceFlags flags,
    BookingSource source,
    String serviceInvoiceNo,
    String bookedBy,
    Instant bookedAt,
    List<ShareDto> shares,
    List<String> journalBatches,
    String insurerBillingNo) {

  /**
   * Maps an invoice (collections loaded).
   *
   * @param i invoice
   * @return response
   */
  public static InvoiceResponse from(BookedInvoice i) {
    return new InvoiceResponse(
        i.getId(),
        i.getInvoiceNo(),
        i.getArn(),
        i.getAccountId(),
        i.getTransactionNo(),
        i.getKind(),
        i.getStatus(),
        i.getEndorsementNo(),
        i.getParentInvoiceNo(),
        i.getRootInvoiceNo(),
        i.getPolicyYear(),
        i.getPolicyNo(),
        i.getFacts(),
        i.getCurrency(),
        i.getBookingDate(),
        i.getInceptionDate(),
        i.getExpiryDate(),
        PremiumDto.from(i.getPremium()),
        CommissionDto.from(i.getCommission()),
        i.getFlags(),
        i.getSource(),
        i.getServiceInvoiceNo(),
        i.getBookedBy(),
        i.getBookedAt(),
        i.getShares().stream().map(ShareDto::from).toList(),
        i.getJournalBatches(),
        i.getInsurerBillingNo());
  }
}
