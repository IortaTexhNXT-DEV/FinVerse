package com.iortatechnxt.brokerverse.remittance.api.dto;

import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.BatchSettlement;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceAmounts;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.OrStatus;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RemittanceType;
import com.iortatechnxt.brokerverse.remittance.service.BatchService.Preview;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Requests and responses of the remittance batch endpoints (RMTID.002/007-011/019/029). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class BatchDtos {

  private BatchDtos() {}

  /**
   * Amounts of a line or batch.
   *
   * @param paidAr paid AR
   * @param commission realized commission
   * @param commissionVat VAT on commission
   * @param wtax withholding tax
   * @param dtip DTIP reduced
   * @param incentive incentive
   * @param incentiveVat VAT on the incentive
   * @param netDue net due before the incentive
   * @param cpc2 CPC2 incentive (DIS 3.29.2)
   * @param cpc2Vat VAT on the CPC2 incentive
   * @param payable net due less the early and CPC2 incentives
   */
  public record Amounts(
      BigDecimal paidAr,
      BigDecimal commission,
      BigDecimal commissionVat,
      BigDecimal wtax,
      BigDecimal dtip,
      BigDecimal incentive,
      BigDecimal incentiveVat,
      BigDecimal netDue,
      BigDecimal cpc2,
      BigDecimal cpc2Vat,
      BigDecimal payable) {

    /**
     * Maps amounts.
     *
     * @param a amounts
     * @return DTO
     */
    public static Amounts from(RemittanceAmounts a) {
      return new Amounts(
          a.paidAr(),
          a.commission(),
          a.commissionVat(),
          a.wtax(),
          a.dtip(),
          a.incentive(),
          a.incentiveVat(),
          a.netDue(),
          a.cpc2(),
          a.cpc2Vat(),
          a.payable());
    }
  }

  /**
   * A batch in a list.
   *
   * @param id id
   * @param batchNo number
   * @param insurerCode insurer
   * @param type remittance type
   * @param currency currency
   * @param stage stage
   * @param processor processor
   * @param lineCount accounts kept
   * @param totals totals of the accounts kept
   * @param createdAt extracted at
   * @param submittedAt submitted at
   * @param approvedAt approved at
   * @param dvNo DV number
   * @param specialRequestNo special remittance request
   */
  public record BatchSummary(
      Long id,
      String batchNo,
      String insurerCode,
      RemittanceType type,
      String currency,
      BatchStage stage,
      String processor,
      int lineCount,
      Amounts totals,
      Instant createdAt,
      Instant submittedAt,
      Instant approvedAt,
      String dvNo,
      String specialRequestNo) {

    /**
     * Maps a batch.
     *
     * @param b batch
     * @return DTO
     */
    public static BatchSummary from(RemittanceBatch b) {
      return new BatchSummary(
          b.getId(),
          b.getBatchNo(),
          b.getInsurerCode(),
          b.getRemittanceType(),
          b.getCurrency(),
          b.getStage(),
          b.getProcessor(),
          b.getLineCount(),
          Amounts.from(b.getTotals()),
          b.getCreatedAt(),
          b.getSubmittedAt(),
          b.getApprovedAt(),
          b.getDvNo(),
          b.getSpecialRequestNo());
    }
  }

  /**
   * One account of a batch.
   *
   * @param invoiceNo invoice
   * @param arn ARN
   * @param endorsementNo endorsement
   * @param policyNo policy
   * @param clientCode client
   * @param assuredName assured
   * @param riskCode risk code
   * @param inceptionDate inception
   * @param bookingDate booking date
   * @param lastPaidOn last payment
   * @param basicPremium basic premium part
   * @param cpc2Code CPC2 criterion applied, null when none
   * @param cpc2Rate CPC2 rate in percent
   * @param amounts amounts (read-only)
   * @param exclusion exclusion, null when kept
   * @param insurerOr insurer OR, null until uploaded
   * @param journalBatchNo remittance journal
   * @param remittedStatus remittance status after the DV
   */
  public record Line(
      String invoiceNo,
      String arn,
      String endorsementNo,
      String policyNo,
      String clientCode,
      String assuredName,
      String riskCode,
      LocalDate inceptionDate,
      LocalDate bookingDate,
      LocalDate lastPaidOn,
      BigDecimal basicPremium,
      String cpc2Code,
      BigDecimal cpc2Rate,
      Amounts amounts,
      Exclusion exclusion,
      InsurerOr insurerOr,
      String journalBatchNo,
      String remittedStatus) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return DTO
     */
    public static Line from(BatchLine l) {
      return new Line(
          l.getInvoiceNo(),
          l.getArn(),
          l.getEndorsementNo(),
          l.getPolicyNo(),
          l.getClientCode(),
          l.getAssuredName(),
          l.getRiskCode(),
          l.getInceptionDate(),
          l.getBookingDate(),
          l.getLastPaidOn(),
          l.getBasicPremium(),
          l.getCpc2Code(),
          l.getCpc2Rate(),
          Amounts.from(l.getAmounts()),
          Exclusion.from(l),
          InsurerOr.from(l),
          l.getJournalBatchNo(),
          l.getRemittedStatus() == null ? null : l.getRemittedStatus().name());
    }
  }

  /**
   * The exclusion of a line (RMTID.002 addendum).
   *
   * @param excluded whether excluded now
   * @param reason reason code
   * @param comment comment
   * @param excludedBy user
   * @param excludedAt time
   * @param restoredBy user who restored it last
   * @param restoredAt time
   */
  public record Exclusion(
      boolean excluded,
      String reason,
      String comment,
      String excludedBy,
      Instant excludedAt,
      String restoredBy,
      Instant restoredAt) {

    /**
     * Maps the exclusion facts, null when never excluded.
     *
     * @param l line
     * @return DTO
     */
    public static Exclusion from(BatchLine l) {
      if (l.getExcludedBy() == null) {
        return null;
      }
      return new Exclusion(
          l.isExcluded(),
          l.getExclusionReason(),
          l.getExclusionComment(),
          l.getExcludedBy(),
          l.getExcludedAt(),
          l.getRestoredBy(),
          l.getRestoredAt());
    }
  }

  /**
   * The insurer's OR of a line (RMTID.013/016).
   *
   * @param orNo OR number
   * @param orDate OR date
   * @param amount OR amount
   * @param status matched or mismatch
   * @param runNo upload run
   */
  public record InsurerOr(
      String orNo, LocalDate orDate, BigDecimal amount, OrStatus status, String runNo) {

    /**
     * Maps the OR, null until uploaded.
     *
     * @param l line
     * @return DTO
     */
    public static InsurerOr from(BatchLine l) {
      return l.getInsurerOrNo() == null
          ? null
          : new InsurerOr(
              l.getInsurerOrNo(),
              l.getInsurerOrDate(),
              l.getInsurerOrAmount(),
              l.getOrStatus(),
              l.getOrRunNo());
    }
  }

  /**
   * A batch with its accounts and the outcome of its approval.
   *
   * @param summary header and totals
   * @param insurerName insurer name
   * @param submittedBy submitter
   * @param approvedBy approver
   * @param returnReason return reason
   * @param disbursement Disbursement request, status and amount
   * @param receipts commission and incentive ORs
   * @param settlement deductions, amount due, send cycle, early-incentive SI and cancelled DV
   * @param scheduleSentAt when the schedule was sent to the insurer
   * @param extractFileId extract file in the repository
   * @param lines accounts
   */
  public record BatchResponse(
      BatchSummary summary,
      String insurerName,
      String submittedBy,
      String approvedBy,
      String returnReason,
      Disbursement disbursement,
      Receipts receipts,
      Settlement settlement,
      Instant scheduleSentAt,
      Long extractFileId,
      List<Line> lines) {

    /**
     * Maps a batch with its lines.
     *
     * @param b batch
     * @param insurerName insurer name
     * @return DTO
     */
    public static BatchResponse from(RemittanceBatch b, String insurerName) {
      return new BatchResponse(
          BatchSummary.from(b),
          insurerName,
          b.getSubmittedBy(),
          b.getApprovedBy(),
          b.getReturnReason(),
          new Disbursement(
              b.getDisbursementRequestNo(), b.getDisbursementStatus(), b.getDisbursedAmount()),
          new Receipts(
              b.getCommissionOrNo(),
              b.getCommissionOrStatus(),
              b.getIncentiveOrNo(),
              b.getIncentiveOrStatus(),
              b.getOrMessage()),
          Settlement.from(b),
          b.getScheduleSentAt(),
          b.getExtractFileId(),
          b.getLines().stream().map(Line::from).toList());
    }
  }

  /**
   * The payment request of a batch.
   *
   * @param requestNo request number
   * @param status status
   * @param amount amount
   */
  public record Disbursement(String requestNo, String status, BigDecimal amount) {}

  /**
   * How the batch is settled beyond its lines (ACSL 2.9.2, DIS 2.20.0, 3.29.1).
   *
   * @param deductionAmount deductions applied in the current cycle
   * @param amountDue amount payable less the deductions
   * @param sendCycle send cycle (1, then +1 after each cancelled DV)
   * @param reference reference of the current payment request
   * @param earlySiNo early-incentive service invoice
   * @param earlySiWtax withholding tax on it
   * @param cancelledDvNo last cancelled DV
   * @param cancelReason its cancellation reason
   * @param cancelledAt when it was cancelled
   */
  public record Settlement(
      BigDecimal deductionAmount,
      BigDecimal amountDue,
      int sendCycle,
      String reference,
      String earlySiNo,
      BigDecimal earlySiWtax,
      String cancelledDvNo,
      String cancelReason,
      Instant cancelledAt) {

    /**
     * Maps the settlement of a batch.
     *
     * @param b batch
     * @return DTO
     */
    public static Settlement from(RemittanceBatch b) {
      BatchSettlement s = b.getSettlement();
      return new Settlement(
          s.getDeductionAmount(),
          b.amountDue(),
          s.getSendCycle(),
          b.cycleReference(),
          s.getEarlySiNo(),
          s.getEarlySiWtax(),
          s.getCancelledDvNo(),
          s.getCancelReason(),
          s.getCancelledAt());
    }
  }

  /**
   * ORs issued for a batch.
   *
   * @param commissionOrNo commission OR
   * @param commissionOrStatus issued or deferred
   * @param incentiveOrNo incentive OR
   * @param incentiveOrStatus issued or deferred
   * @param message outcome message
   */
  public record Receipts(
      String commissionOrNo,
      String commissionOrStatus,
      String incentiveOrNo,
      String incentiveOrStatus,
      String message) {}

  /**
   * A submission preview (RMTID.002/019).
   *
   * @param totals totals of the accounts kept
   * @param lineCount accounts kept
   * @param excludedCount accounts excluded
   * @param lines accounts kept
   * @param problems problems blocking the submission
   */
  public record PreviewResponse(
      Amounts totals, int lineCount, long excludedCount, List<Line> lines, List<String> problems) {

    /**
     * Maps a preview.
     *
     * @param p preview
     * @return DTO
     */
    public static PreviewResponse from(Preview p) {
      return new PreviewResponse(
          Amounts.from(p.batch().getTotals()),
          p.lines().size(),
          p.batch().getLines().stream().filter(BatchLine::isExcluded).count(),
          p.lines().stream().map(Line::from).toList(),
          p.problems());
    }
  }

  /**
   * Exclusion of invoices.
   *
   * @param invoiceNos invoices
   * @param reasonCode reason (LOV REMIT_EXCLUSION_REASON)
   * @param comment comment
   */
  public record ExcludeRequest(
      @NotEmpty List<@NotBlank String> invoiceNos,
      @NotBlank String reasonCode,
      @Size(max = 500) String comment) {}

  /**
   * Restore of an invoice.
   *
   * @param invoiceNo invoice
   */
  public record RestoreRequest(@NotBlank String invoiceNo) {}

  /**
   * An optional comment.
   *
   * @param comment comment
   */
  public record CommentRequest(@Size(max = 1000) String comment) {}

  /**
   * A return with a reason.
   *
   * @param reasonCode reason (LOV REMIT_RETURN_REASON)
   * @param comment comment
   */
  public record ReturnRequest(@NotBlank String reasonCode, @Size(max = 1000) String comment) {}

  /**
   * An assignment.
   *
   * @param username processor
   */
  public record AssignRequest(@NotBlank String username) {}

  /**
   * The schedule e-mail to the insurer (MKTID.001).
   *
   * @param to recipients
   * @param cc copy recipients
   * @param subject subject
   * @param body body
   */
  public record SendScheduleRequest(
      @NotEmpty List<@Email String> to,
      List<@Email String> cc,
      @NotBlank @Size(max = 250) String subject,
      @NotBlank @Size(max = 4000) String body) {}

  /**
   * A queued e-mail.
   *
   * @param messageId message
   * @param passwordMessageId password e-mail
   */
  public record QueuedResponse(Long messageId, Long passwordMessageId) {}
}
