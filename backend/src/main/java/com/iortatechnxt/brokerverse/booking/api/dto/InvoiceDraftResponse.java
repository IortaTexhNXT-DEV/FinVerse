package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.InvoiceDraft;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceFacts;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceFlags;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import java.time.LocalDate;
import java.util.List;

/**
 * An invoice as it would be booked (pre-booking confirmation, endorsement preview).
 *
 * @param arn account
 * @param transactionNo transaction (booking key)
 * @param kind kind
 * @param policyYear policy year
 * @param policyNo policy number
 * @param facts client, insurer, risk, sales and cost center
 * @param currency currency
 * @param inceptionDate period start
 * @param expiryDate period end
 * @param premium premium by component
 * @param commission commission
 * @param flags flags
 * @param shares insurer shares
 */
public record InvoiceDraftResponse(
    String arn,
    String transactionNo,
    InvoiceKind kind,
    int policyYear,
    String policyNo,
    InvoiceFacts facts,
    String currency,
    LocalDate inceptionDate,
    LocalDate expiryDate,
    PremiumDto premium,
    CommissionDto commission,
    InvoiceFlags flags,
    List<ShareDto> shares) {

  /**
   * Maps a draft.
   *
   * @param d draft
   * @return response
   */
  public static InvoiceDraftResponse from(InvoiceDraft d) {
    return new InvoiceDraftResponse(
        d.arn(),
        d.transactionNo(),
        d.kind(),
        d.policyYear(),
        d.policyNo(),
        d.facts(),
        d.currency(),
        d.inceptionDate(),
        d.expiryDate(),
        PremiumDto.from(d.premium()),
        CommissionDto.from(d.commission()),
        d.flags(),
        d.shares().stream().map(ShareDto::from).toList());
  }
}
