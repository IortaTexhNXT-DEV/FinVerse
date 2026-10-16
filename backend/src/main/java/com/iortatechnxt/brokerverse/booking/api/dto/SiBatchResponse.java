package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.SiBatch;
import com.iortatechnxt.brokerverse.booking.domain.SiBatchStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A daily service-invoice batch of an insurer (G09).
 *
 * @param id id
 * @param companyId company
 * @param insurerCode insurer
 * @param insurerName insurer name
 * @param batchDate business date of the service invoices
 * @param itemCount number of service invoices
 * @param currency currency, null when the invoices are in several currencies
 * @param commissionTotal total commission
 * @param netTotal total net amount
 * @param status delivery status
 * @param attempts e-mails sent
 * @param recipientEmail billing address used
 * @param lastError reason of the last failure
 * @param generatedAt generation time
 * @param sentAt delivery time
 */
public record SiBatchResponse(
    Long id,
    Long companyId,
    String insurerCode,
    String insurerName,
    LocalDate batchDate,
    int itemCount,
    String currency,
    BigDecimal commissionTotal,
    BigDecimal netTotal,
    SiBatchStatus status,
    int attempts,
    String recipientEmail,
    String lastError,
    Instant generatedAt,
    Instant sentAt) {

  /**
   * Maps a batch.
   *
   * @param b batch
   * @return response
   */
  public static SiBatchResponse from(SiBatch b) {
    return new SiBatchResponse(
        b.getId(),
        b.getCompanyId(),
        b.getInsurerCode(),
        b.getInsurerName(),
        b.getBatchDate(),
        b.getItemCount(),
        b.getCurrency(),
        b.getCommissionTotal(),
        b.getNetTotal(),
        b.getStatus(),
        b.getAttempts(),
        b.getRecipientEmail(),
        b.getLastError(),
        b.getCreatedAt(),
        b.getSentAt());
  }
}
