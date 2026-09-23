package com.iortatechnxt.finverse.tax.api.dto;

import com.iortatechnxt.finverse.tax.domain.Certificate2307;
import com.iortatechnxt.finverse.tax.domain.CertificateStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * BIR Form 2307 certificate (register row).
 *
 * @param id id
 * @param certificateNo certificate number
 * @param batchNo issuing batch
 * @param partyCode payee party code
 * @param payeeTin payee TIN as printed
 * @param payeeName payee name
 * @param periodStart quarter start
 * @param periodEnd quarter end
 * @param totalIncome total income payments
 * @param totalTax total tax withheld
 * @param status status
 * @param statusReason cancellation reason
 */
public record CertificateResponse(
    Long id,
    String certificateNo,
    String batchNo,
    String partyCode,
    String payeeTin,
    String payeeName,
    LocalDate periodStart,
    LocalDate periodEnd,
    BigDecimal totalIncome,
    BigDecimal totalTax,
    CertificateStatus status,
    String statusReason) {

  /**
   * Maps a certificate (batch loaded).
   *
   * @param c certificate
   * @return response
   */
  public static CertificateResponse from(Certificate2307 c) {
    return new CertificateResponse(
        c.getId(),
        c.getCertificateNo(),
        c.getBatch().getBatchNo(),
        c.getPartyCode(),
        c.payee().formattedTin(),
        c.payee().name(),
        c.getPeriodStart(),
        c.getPeriodEnd(),
        c.getTotalIncome(),
        c.getTotalTax(),
        c.getStatus(),
        c.getStatusReason());
  }
}
