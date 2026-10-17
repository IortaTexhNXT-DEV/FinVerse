package com.iortatechnxt.brokerverse.cashiering.api.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CommissionLine;
import com.iortatechnxt.brokerverse.cashiering.domain.MinimalBalanceRule;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentFileLayout;
import com.iortatechnxt.brokerverse.cashiering.domain.PrintBatch;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptSeries;
import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Request and response records of the receipt series, file layouts, minimal balance rules, batch
 * printing and commission payment lines (CSHID.006/007/008/015/016/019).
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class SetupDtos {

  private SetupDtos() {}

  /**
   * A receipt series.
   *
   * @param id id
   * @param branchId branch
   * @param kind AR or OR
   * @param atpNo BIR ATP
   * @param prefix prefix
   * @param fromNo first number
   * @param toNo last number
   * @param nextNo next number
   * @param remaining numbers left
   * @param warnAt warning threshold
   * @param low at or below the threshold
   * @param recordStatus maker-checker status
   * @param maker maker
   * @param authorizedBy checker
   * @param seriesYear year the series serves, null for every year
   * @param numberFormat number format (prefix, year and branch indicator resolved), null for prefix
   *     and sequence
   * @param nextNumber the number the series gives next, null when depleted
   * @param origin BIBS or the legacy origin (source system, legacy reference, batch)
   */
  public record SeriesResponse(
      Long id,
      Long branchId,
      String kind,
      String atpNo,
      String prefix,
      long fromNo,
      long toNo,
      long nextNo,
      long remaining,
      int warnAt,
      boolean low,
      String recordStatus,
      String maker,
      String authorizedBy,
      Integer seriesYear,
      String numberFormat,
      String nextNumber,
      @JsonUnwrapped RecordOrigin origin) {

    /**
     * Maps a series.
     *
     * @param s series
     * @return response
     */
    public static SeriesResponse from(ReceiptSeries s) {
      return new SeriesResponse(
          s.getId(),
          s.getBranchId(),
          s.getKind().name(),
          s.getAtpNo(),
          s.getPrefix(),
          s.getFromNo(),
          s.getToNo(),
          s.getNextNo(),
          s.remaining(),
          s.getWarnAt(),
          s.isLow(),
          s.getRecordStatus().name(),
          s.getMaker(),
          s.getAuthorizedBy(),
          s.getSeriesYear(),
          s.getNumberFormat(),
          s.preview(),
          s.getRecordOrigin());
    }
  }

  /**
   * A series to create.
   *
   * @param companyId company
   * @param branchId branch
   * @param kind AR or OR
   * @param prefix prefix
   * @param fromNo first number
   * @param toNo last number
   * @param atpNo BIR ATP
   * @param warnAt warning threshold
   * @param seriesYear year the series serves, null for every year
   * @param numberFormat number format, null for the format of the setting, blank for prefix and
   *     sequence
   */
  public record SeriesRequest(
      @NotNull Long companyId,
      @NotNull Long branchId,
      @NotNull ReceiptKind kind,
      @NotBlank @Size(max = 20) String prefix,
      @Min(1) long fromNo,
      @Min(1) long toNo,
      @Size(max = 40) String atpNo,
      @Min(0) int warnAt,
      @Min(2000) @Max(2999) Integer seriesYear,
      @Size(max = 60) String numberFormat) {}

  /**
   * A series change.
   *
   * @param atpNo BIR ATP
   * @param toNo last number
   * @param warnAt warning threshold
   */
  public record SeriesUpdate(@Size(max = 40) String atpNo, @Min(1) long toNo, @Min(0) int warnAt) {}

  /**
   * A payment file layout.
   *
   * @param handlerCode handler
   * @param kind AUTO, DELIMITED or FIXED_WIDTH
   * @param delimiter separator
   * @param fields fixed-width fields
   * @param description description
   * @param updatedBy changed by
   * @param updatedAt changed at
   */
  public record LayoutResponse(
      String handlerCode,
      String kind,
      String delimiter,
      String fields,
      String description,
      String updatedBy,
      Instant updatedAt) {

    /**
     * Maps a layout.
     *
     * @param l layout
     * @return response
     */
    public static LayoutResponse from(PaymentFileLayout l) {
      return new LayoutResponse(
          l.getHandlerCode(),
          l.getKind(),
          l.getDelimiter(),
          l.getFields(),
          l.getDescription(),
          l.getUpdatedBy(),
          l.getUpdatedAt());
    }
  }

  /**
   * A layout change.
   *
   * @param kind AUTO, DELIMITED or FIXED_WIDTH
   * @param delimiter separator
   * @param fields fixed-width fields
   */
  public record LayoutRequest(
      @NotBlank String kind, @Size(max = 1) String delimiter, @Size(max = 1000) String fields) {}

  /**
   * A minimal balance rule.
   *
   * @param kind PREMIUM, EXCESS or COMMISSION
   * @param maxAmount maximum balance
   * @param excludeCwt exclude the 2% CWT
   * @param excludeDst exclude the DST
   * @param excludeWholePremium exclude the whole premium
   * @param action REVERSE or OVERAGES
   * @param active active
   * @param description description
   */
  public record RuleResponse(
      String kind,
      BigDecimal maxAmount,
      boolean excludeCwt,
      boolean excludeDst,
      boolean excludeWholePremium,
      String action,
      boolean active,
      String description) {

    /**
     * Maps a rule.
     *
     * @param r rule
     * @return response
     */
    public static RuleResponse from(MinimalBalanceRule r) {
      return new RuleResponse(
          r.getKind(),
          r.getMaxAmount(),
          r.isExcludeCwt(),
          r.isExcludeDst(),
          r.isExcludeWholePremium(),
          r.getAction(),
          r.isActive(),
          r.getDescription());
    }
  }

  /**
   * A print batch.
   *
   * @param id id
   * @param batchNo PRB- number
   * @param criteria selection
   * @param requestedCount receipts selected
   * @param printedCount printed
   * @param failedCount failed
   * @param status status
   * @param fileName file
   * @param createdBy printed by
   * @param createdAt printed at
   * @param skippedCount failures skipped
   * @param copyLabel copy printed (CLIENT, COMPANY or BOTH)
   * @param zipped whether the ZIP of the receipts is kept
   * @param lines receipts
   */
  public record PrintBatchResponse(
      Long id,
      String batchNo,
      String criteria,
      int requestedCount,
      int printedCount,
      int failedCount,
      String status,
      String fileName,
      String createdBy,
      Instant createdAt,
      int skippedCount,
      String copyLabel,
      boolean zipped,
      List<PrintLineResponse> lines) {

    /**
     * Maps a batch.
     *
     * @param b batch (lines loaded when wanted)
     * @param withLines include the lines
     * @return response
     */
    public static PrintBatchResponse from(PrintBatch b, boolean withLines) {
      return new PrintBatchResponse(
          b.getId(),
          b.getBatchNo(),
          b.getCriteria(),
          b.getRequestedCount(),
          b.getPrintedCount(),
          b.getFailedCount(),
          b.getStatus(),
          b.getFileName(),
          b.getCreatedBy(),
          b.getCreatedAt(),
          b.getSkippedCount(),
          b.getCopyLabel(),
          b.getZipFileId() != null,
          withLines ? b.getLines().stream().map(PrintLineResponse::from).toList() : List.of());
    }
  }

  /**
   * A receipt of a print batch.
   *
   * @param receiptId receipt
   * @param receiptNo number
   * @param status PRINTED, FAILED or SKIPPED
   * @param message message
   * @param reprint whether the receipt was printed before (REPRINT mark)
   * @param certificateNo certificate number of the receipt
   */
  public record PrintLineResponse(
      Long receiptId,
      String receiptNo,
      String status,
      String message,
      boolean reprint,
      String certificateNo) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return response
     */
    public static PrintLineResponse from(PrintBatch.Line l) {
      return new PrintLineResponse(
          l.getReceiptId(),
          l.getReceiptNo(),
          l.getStatus(),
          l.getMessage(),
          l.isReprint(),
          l.getCertificateNo());
    }
  }

  /**
   * A commission payment line.
   *
   * @param id id
   * @param jobNo upload job
   * @param rowNo row
   * @param insurerCode insurer
   * @param payeeName payee
   * @param certificateRef certificate
   * @param paymentRef payment
   * @param invoiceNo invoice
   * @param gross basic commission
   * @param vat VAT
   * @param wtax withholding tax
   * @param paymentDate date
   * @param status status
   * @param receiptNo OR
   * @param message message
   */
  public record CommissionLineResponse(
      Long id,
      String jobNo,
      int rowNo,
      String insurerCode,
      String payeeName,
      String certificateRef,
      String paymentRef,
      String invoiceNo,
      BigDecimal gross,
      BigDecimal vat,
      BigDecimal wtax,
      LocalDate paymentDate,
      String status,
      String receiptNo,
      String message) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return response
     */
    public static CommissionLineResponse from(CommissionLine l) {
      return new CommissionLineResponse(
          l.getId(),
          l.getJobNo(),
          l.getRowNo(),
          l.getInsurerCode(),
          l.getPayeeName(),
          l.getCertificateRef(),
          l.getPaymentRef(),
          l.getInvoiceNo(),
          l.getGross(),
          l.getVat(),
          l.getWtax(),
          l.getPaymentDate(),
          l.getStatus(),
          l.getReceiptNo(),
          l.getMessage());
    }
  }
}
