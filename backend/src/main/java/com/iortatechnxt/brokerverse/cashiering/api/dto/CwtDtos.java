package com.iortatechnxt.brokerverse.cashiering.api.dto;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.CwtPath;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtBatch;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response records of BIR 2307 (CSHID.026/027, MKTID.010/013, DBMID.001). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class CwtDtos {

  private CwtDtos() {}

  /**
   * A 2307 tag.
   *
   * @param id id
   * @param reference CWT- reference
   * @param invoiceNo invoice
   * @param arn account
   * @param clientCode client
   * @param insurerCode insurer
   * @param amount 2% amount
   * @param path cash or certificate
   * @param certificateNo certificate
   * @param periodFrom period from
   * @param periodTo period to
   * @param cwtCopyReceived CWT copy received
   * @param remitted invoice already remitted when tagged
   * @param stage workflow stage
   * @param batchId batch
   * @param receiptNo AR of the cash path
   * @param reclassJournalNo reclass journal
   * @param offsetJournalNo DTIP offset journal
   * @param remarks remarks
   * @param taggedBy tagged by
   * @param taggedAt tagged at
   */
  public record CwtTagResponse(
      Long id,
      String reference,
      String invoiceNo,
      String arn,
      String clientCode,
      String insurerCode,
      BigDecimal amount,
      String path,
      String certificateNo,
      LocalDate periodFrom,
      LocalDate periodTo,
      boolean cwtCopyReceived,
      boolean remitted,
      String stage,
      Long batchId,
      String receiptNo,
      String reclassJournalNo,
      String offsetJournalNo,
      String remarks,
      String taggedBy,
      Instant taggedAt) {

    /**
     * Maps a tag.
     *
     * @param t tag
     * @return response
     */
    public static CwtTagResponse from(CwtTag t) {
      return new CwtTagResponse(
          t.getId(),
          t.getReference(),
          t.getInvoiceNo(),
          t.getArn(),
          t.getClientCode(),
          t.getInsurerCode(),
          t.getAmount(),
          t.getPath().name(),
          t.getCertificateNo(),
          t.getPeriodFrom(),
          t.getPeriodTo(),
          t.isCwtCopyReceived(),
          t.isRemitted(),
          t.getStage(),
          t.getBatchId(),
          t.getReceiptNo(),
          t.getReclassJournalNo(),
          t.getOffsetJournalNo(),
          t.getRemarks(),
          t.getCreatedBy(),
          t.getCreatedAt());
    }
  }

  /**
   * A 2307 batch.
   *
   * @param id id
   * @param batchNo CWB- number
   * @param insurerCode insurer
   * @param tagCount certificates
   * @param totalAmount total
   * @param status status
   * @param disbursementRequestNo Disbursement request
   * @param routedAt routed
   * @param releasedAt released
   * @param createdBy validated by
   * @param createdAt validated at
   */
  public record CwtBatchResponse(
      Long id,
      String batchNo,
      String insurerCode,
      int tagCount,
      BigDecimal totalAmount,
      String status,
      String disbursementRequestNo,
      Instant routedAt,
      Instant releasedAt,
      String createdBy,
      Instant createdAt) {

    /**
     * Maps a batch.
     *
     * @param b batch
     * @return response
     */
    public static CwtBatchResponse from(CwtBatch b) {
      return new CwtBatchResponse(
          b.getId(),
          b.getBatchNo(),
          b.getInsurerCode(),
          b.getTagCount(),
          b.getTotalAmount(),
          b.getStatus(),
          b.getDisbursementRequestNo(),
          b.getRoutedAt(),
          b.getReleasedAt(),
          b.getCreatedBy(),
          b.getCreatedAt());
    }
  }

  /**
   * A tag to create (MKTID.013).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param amount 2% amount, null for the expected 2%
   * @param path cash or certificate
   * @param certificateNo certificate number
   * @param periodFrom period from
   * @param periodTo period to
   * @param remarks remarks
   */
  public record CwtTagRequest(
      @NotNull Long companyId,
      @NotBlank @Size(max = 40) String invoiceNo,
      BigDecimal amount,
      @NotNull CwtPath path,
      @Size(max = 60) String certificateNo,
      LocalDate periodFrom,
      LocalDate periodTo,
      @Size(max = 250) String remarks) {}

  /**
   * The expected 2% of an invoice.
   *
   * @param invoiceNo invoice
   * @param arn account
   * @param assuredName assured
   * @param insurerCode insurer
   * @param cwt 2% CWT account
   * @param expected 2% still expected
   * @param remittanceStatus remittance status (MKTID.010 "already remitted?")
   */
  public record ExpectedResponse(
      String invoiceNo,
      String arn,
      String assuredName,
      String insurerCode,
      boolean cwt,
      BigDecimal expected,
      String remittanceStatus) {}

  /**
   * A batch to validate.
   *
   * @param companyId company
   * @param tagIds tags
   */
  public record BatchRequest(@NotNull Long companyId, @NotEmpty List<Long> tagIds) {}
}
