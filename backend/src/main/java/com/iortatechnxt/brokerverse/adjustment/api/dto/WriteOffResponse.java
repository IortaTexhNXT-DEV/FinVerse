package com.iortatechnxt.brokerverse.adjustment.api.dto;

import com.iortatechnxt.brokerverse.adjustment.domain.MinBalanceItem;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A balance written off or credited (ADJID.026).
 *
 * @param invoiceNo invoice
 * @param arn ARN
 * @param clientCode client
 * @param currency currency
 * @param balance balance cleared
 * @param action WRITE_OFF or CREDIT
 * @param fileRef upload or request
 * @param journalBatchNo journal
 * @param createdBy user
 * @param createdAt time
 */
public record WriteOffResponse(
    String invoiceNo,
    String arn,
    String clientCode,
    String currency,
    BigDecimal balance,
    String action,
    String fileRef,
    String journalBatchNo,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps a write-off.
   *
   * @param i write-off
   * @return response
   */
  public static WriteOffResponse from(MinBalanceItem i) {
    return new WriteOffResponse(
        i.getInvoiceNo(),
        i.getArn(),
        i.getClientCode(),
        i.getCurrency(),
        i.getBalance(),
        i.getAction().name(),
        i.getFileRef(),
        i.getJournalBatchNo(),
        i.getCreatedBy(),
        i.getCreatedAt());
  }
}
