package com.iortatechnxt.finverse.tax.api.dto;

import com.iortatechnxt.finverse.tax.domain.Certificate2307Batch;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * BIR Form 2307 batch.
 *
 * @param id id
 * @param batchNo batch number
 * @param periodStart quarter start
 * @param periodEnd quarter end
 * @param certificateCount certificates issued
 * @param totalIncome total income payments
 * @param totalTax total tax withheld
 * @param createdBy user
 * @param createdAt time
 * @param skippedPayees payees with lines under an unmapped ATC (generation response only)
 */
public record CertificateBatchResponse(
    Long id,
    String batchNo,
    LocalDate periodStart,
    LocalDate periodEnd,
    int certificateCount,
    BigDecimal totalIncome,
    BigDecimal totalTax,
    String createdBy,
    Instant createdAt,
    List<String> skippedPayees) {

  /**
   * Maps a batch.
   *
   * @param b batch
   * @param skipped skipped payees
   * @return response
   */
  public static CertificateBatchResponse from(Certificate2307Batch b, List<String> skipped) {
    return new CertificateBatchResponse(
        b.getId(),
        b.getBatchNo(),
        b.getPeriodStart(),
        b.getPeriodEnd(),
        b.getCertificateCount(),
        b.getTotalIncome(),
        b.getTotalTax(),
        b.getCreatedBy(),
        b.getCreatedAt(),
        List.copyOf(skipped));
  }
}
