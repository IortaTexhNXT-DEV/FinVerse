package com.iortatechnxt.brokerverse.brokerclaims.cover.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPremiumStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;

/**
 * The premium check of a cover from the payment status of its invoices (BRCLM.001;
 * CLAIMS_BROKING_DESIGN 8.3): cancelled invoices are excluded (FR-CM-016 R3); none left gives
 * NO_INVOICE; any unpaid invoice gives UNPAID; else any partly paid gives PARTIALLY_PAID; when
 * every invoice is a direct payment to the insurer the result is DIRECT_PAYMENT; otherwise PAID
 * (return invoices without a client receivable do not count against it).
 */
public final class PremiumRule {

  private PremiumRule() {}

  /**
   * Evaluates the invoices of one ARN and policy year.
   *
   * @param invoices invoices of the cover, cancelled ones included
   * @return the result with the invoices that are not fully paid
   */
  public static PremiumCheck evaluate(List<InvoiceState> invoices) {
    List<InvoiceState> live = invoices.stream().filter(i -> !i.cancelled()).toList();
    List<InvoiceState> open =
        live.stream()
            .filter(
                i ->
                    i.paymentStatus() == PaymentStatus.UNPAID
                        || i.paymentStatus() == PaymentStatus.PARTIALLY_PAID)
            .toList();
    return new PremiumCheck(status(live, open), open);
  }

  private static ClaimPremiumStatus status(List<InvoiceState> live, List<InvoiceState> open) {
    if (live.isEmpty()) {
      return ClaimPremiumStatus.NO_INVOICE;
    }
    if (open.stream().anyMatch(i -> i.paymentStatus() == PaymentStatus.UNPAID)) {
      return ClaimPremiumStatus.UNPAID;
    }
    if (!open.isEmpty()) {
      return ClaimPremiumStatus.PARTIALLY_PAID;
    }
    return live.stream().allMatch(InvoiceState::directPayment)
        ? ClaimPremiumStatus.DIRECT_PAYMENT
        : ClaimPremiumStatus.PAID;
  }

  /**
   * An invoice of the cover as the check needs it.
   *
   * @param invoiceNo invoice number
   * @param kind booking, endorsement or cancellation
   * @param paymentStatus payment status of the ledger
   * @param directPayment premium paid directly to the insurer (BRNB.114)
   * @param cancelled cancelled invoice
   * @param balance outstanding premium of the client
   * @param currency invoice currency
   */
  public record InvoiceState(
      String invoiceNo,
      String kind,
      PaymentStatus paymentStatus,
      boolean directPayment,
      boolean cancelled,
      BigDecimal balance,
      String currency) {}

  /**
   * Result of a premium check.
   *
   * @param status PAID, UNPAID, PARTIALLY_PAID, DIRECT_PAYMENT or NO_INVOICE
   * @param unpaid invoices not fully paid, with their balance
   */
  public record PremiumCheck(ClaimPremiumStatus status, List<InvoiceState> unpaid) {

    /** Defensive copy. */
    public PremiumCheck {
      unpaid = List.copyOf(unpaid);
    }

    /**
     * Whether the premium is unpaid or partly paid (flag "Unpaid premium", alert).
     *
     * @return true when an invoice is not fully paid
     */
    public boolean blocking() {
      return status == ClaimPremiumStatus.UNPAID || status == ClaimPremiumStatus.PARTIALLY_PAID;
    }
  }
}
