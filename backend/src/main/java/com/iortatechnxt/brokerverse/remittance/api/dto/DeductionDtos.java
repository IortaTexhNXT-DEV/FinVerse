package com.iortatechnxt.brokerverse.remittance.api.dto;

import com.iortatechnxt.brokerverse.remittance.domain.DeductionApplication;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction.DeductionStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction.Terms;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Requests and responses of the remittance deduction endpoints (ACSL 2.9.2). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class DeductionDtos {

  private static final int MONEY_INTEGER = 17;
  private static final int MONEY_FRACTION = 2;

  private DeductionDtos() {}

  /**
   * A deduction.
   *
   * @param id id
   * @param deductionNo number
   * @param insurerCode insurer
   * @param currency currency
   * @param sourceType source (LOV REMIT_DEDUCTION_SOURCE)
   * @param sourceRef source reference
   * @param invoiceNo invoice, may be null
   * @param amount amount to deduct
   * @param appliedAmount amount applied to batches
   * @param remaining amount still to deduct
   * @param confirmationRef insurer's confirmation reference
   * @param confirmationDate date of the insurer's confirmation
   * @param remarks remarks
   * @param stage stage
   * @param createdBy preparer
   * @param createdAt created
   * @param submittedBy submitter
   * @param confirmedBy confirmer
   * @param confirmedAt confirmed at
   */
  public record DeductionResponse(
      Long id,
      String deductionNo,
      String insurerCode,
      String currency,
      String sourceType,
      String sourceRef,
      String invoiceNo,
      BigDecimal amount,
      BigDecimal appliedAmount,
      BigDecimal remaining,
      String confirmationRef,
      LocalDate confirmationDate,
      String remarks,
      DeductionStage stage,
      String createdBy,
      Instant createdAt,
      String submittedBy,
      String confirmedBy,
      Instant confirmedAt) {

    /**
     * Maps a deduction.
     *
     * @param d deduction
     * @return DTO
     */
    public static DeductionResponse from(RemittanceDeduction d) {
      return new DeductionResponse(
          d.getId(),
          d.getDeductionNo(),
          d.getInsurerCode(),
          d.getCurrency(),
          d.getSourceType(),
          d.getSourceRef(),
          d.getInvoiceNo(),
          d.getAmount(),
          d.getAppliedAmount(),
          d.remaining(),
          d.getConfirmationRef(),
          d.getConfirmationDate(),
          d.getRemarks(),
          d.getStage(),
          d.getCreatedBy(),
          d.getCreatedAt(),
          d.getSubmittedBy(),
          d.getConfirmedBy(),
          d.getConfirmedAt());
    }
  }

  /**
   * The part of a deduction consumed by a batch.
   *
   * @param id id
   * @param deductionId deduction
   * @param batchId batch
   * @param batchNo batch number
   * @param sendCycle send cycle of the batch
   * @param amount amount deducted
   * @param journalBatchNo journal of the posting
   * @param reversed reversed after a cancelled DV
   * @param createdAt applied at
   */
  public record ApplicationResponse(
      Long id,
      Long deductionId,
      Long batchId,
      String batchNo,
      int sendCycle,
      BigDecimal amount,
      String journalBatchNo,
      boolean reversed,
      Instant createdAt) {

    /**
     * Maps a consumption.
     *
     * @param a consumption
     * @return DTO
     */
    public static ApplicationResponse from(DeductionApplication a) {
      return new ApplicationResponse(
          a.getId(),
          a.getDeductionId(),
          a.getBatchId(),
          a.getBatchNo(),
          a.getSendCycle(),
          a.getAmount(),
          a.getJournalBatchNo(),
          a.isReversed(),
          a.getCreatedAt());
    }
  }

  /**
   * A deduction to prepare or change.
   *
   * @param companyId company (ignored on change)
   * @param insurerCode insurer party code
   * @param currency currency
   * @param sourceType source (LOV REMIT_DEDUCTION_SOURCE)
   * @param sourceRef source reference
   * @param invoiceNo invoice, may be null
   * @param amount amount
   * @param confirmationRef insurer's confirmation reference
   * @param confirmationDate date of the insurer's confirmation
   * @param remarks remarks
   */
  public record DeductionRequest(
      Long companyId,
      @NotBlank @Size(max = 30) String insurerCode,
      @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
      @NotBlank @Size(max = 30) String sourceType,
      @NotBlank @Size(max = 60) String sourceRef,
      @Size(max = 40) String invoiceNo,
      @NotNull
          @DecimalMin(value = "0.01")
          @Digits(integer = MONEY_INTEGER, fraction = MONEY_FRACTION)
          BigDecimal amount,
      @Size(max = 100) String confirmationRef,
      LocalDate confirmationDate,
      @Size(max = 1000) String remarks) {

    /**
     * The terms.
     *
     * @return terms
     */
    public Terms terms() {
      return new Terms(
          insurerCode.strip(),
          currency,
          sourceType,
          sourceRef.strip(),
          blankToNull(invoiceNo),
          amount,
          blankToNull(confirmationRef),
          confirmationDate,
          blankToNull(remarks));
    }

    private static String blankToNull(String value) {
      return value == null || value.isBlank() ? null : value.strip();
    }
  }
}
