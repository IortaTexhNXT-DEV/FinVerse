package com.iortatechnxt.brokerverse.booking.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything a booked invoice is created with, before it is numbered and posted.
 *
 * @param companyId company
 * @param branchId booking branch
 * @param arn Account Reference Number
 * @param accountId account id
 * @param transactionNo transaction of the account (booking key with the ARN, BRNB.076)
 * @param kind booking, endorsement or cancellation
 * @param policyYear policy year (1 for annual accounts, BRNB.112)
 * @param policyNo policy number of the year
 * @param facts client, insurer, risk and sales facts
 * @param currency currency
 * @param inceptionDate start of the period invoiced
 * @param expiryDate end of the period invoiced
 * @param premium premium by component (negative for returns)
 * @param commission commission terms (negative for returns)
 * @param flags direct payment, CWT 2 %, incentive, business type
 * @param shares insurer shares (sum 100 %)
 * @param endorsementNo endorsement number, null for the original booking
 * @param parentInvoiceNo invoice the endorsement relates to, null for the original booking
 */
public record InvoiceDraft(
    Long companyId,
    Long branchId,
    String arn,
    Long accountId,
    String transactionNo,
    InvoiceKind kind,
    int policyYear,
    String policyNo,
    InvoiceFacts facts,
    String currency,
    LocalDate inceptionDate,
    LocalDate expiryDate,
    PremiumComponents premium,
    CommissionTerms commission,
    InvoiceFlags flags,
    List<InsurerShare> shares,
    String endorsementNo,
    String parentInvoiceNo) {

  /** Defensive copy. */
  public InvoiceDraft {
    shares = List.copyOf(shares);
  }
}
