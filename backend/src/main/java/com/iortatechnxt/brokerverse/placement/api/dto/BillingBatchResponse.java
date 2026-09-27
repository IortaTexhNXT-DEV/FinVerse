package com.iortatechnxt.brokerverse.placement.api.dto;

import com.iortatechnxt.brokerverse.placement.domain.BillingBatch;
import com.iortatechnxt.brokerverse.placement.domain.BillingItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A CLPC billing batch (BRNB.067).
 *
 * @param id id
 * @param batchNo batch number
 * @param billingDate billing date
 * @param status status
 * @param itemCount accounts billed
 * @param totalPremium total premium
 * @param createdAt created at
 * @param createdBy created by
 * @param items billed accounts (detail only)
 */
public record BillingBatchResponse(
    Long id,
    String batchNo,
    LocalDate billingDate,
    String status,
    int itemCount,
    BigDecimal totalPremium,
    Instant createdAt,
    String createdBy,
    List<Item> items) {

  /**
   * Maps a batch without its items.
   *
   * @param b batch
   * @return response
   */
  public static BillingBatchResponse from(BillingBatch b) {
    return of(b, List.of());
  }

  /**
   * Maps a batch with its items.
   *
   * @param b batch
   * @return response
   */
  public static BillingBatchResponse detail(BillingBatch b) {
    return of(b, b.getItems().stream().map(Item::from).toList());
  }

  private static BillingBatchResponse of(BillingBatch b, List<Item> items) {
    return new BillingBatchResponse(
        b.getId(),
        b.getBatchNo(),
        b.getBillingDate(),
        b.getStatus().name(),
        b.getItemCount(),
        b.getTotalPremium(),
        b.getCreatedAt(),
        b.getCreatedBy(),
        items);
  }

  /**
   * A billed account with the CLPC fields.
   *
   * @param lineNo line
   * @param accountId account
   * @param arn reference
   * @param pnNumbers PN numbers
   * @param loanApplicationNo loan application number
   * @param bookingDate booking date
   * @param borrower borrower
   * @param originatingUnit originating unit
   * @param premium premium
   * @param bdoiLocation BDOI location
   * @param amortised amortised
   * @param paymentStatus BILLED, PAID or UNPAID
   */
  public record Item(
      int lineNo,
      Long accountId,
      String arn,
      String pnNumbers,
      String loanApplicationNo,
      LocalDate bookingDate,
      String borrower,
      String originatingUnit,
      BigDecimal premium,
      String bdoiLocation,
      boolean amortised,
      String paymentStatus) {

    /**
     * Maps an item.
     *
     * @param i item
     * @return response
     */
    public static Item from(BillingItem i) {
      return new Item(
          i.getLineNo(),
          i.getAccountId(),
          i.getArn(),
          i.getPnNumbers(),
          i.getLoanApplicationNo(),
          i.getBookingDate(),
          i.getBorrower(),
          i.getOriginatingUnit(),
          i.getPremium(),
          i.getBdoiLocation(),
          i.isAmortised(),
          i.getPaymentStatus().name());
    }
  }
}
