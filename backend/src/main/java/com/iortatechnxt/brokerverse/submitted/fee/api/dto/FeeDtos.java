package com.iortatechnxt.brokerverse.submitted.fee.api.dto;

import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedView;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHandlingFee;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNoTouchBatch;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNoTouchLine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Records of the handling fee and No Touch billing API (FR-SP-070 to 075). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class FeeDtos {

  private FeeDtos() {}

  /**
   * A handling fee record.
   *
   * @param id id
   * @param feeNo number
   * @param policyId record
   * @param pnNo PN number
   * @param locationRef location reference
   * @param amount amount
   * @param currency currency
   * @param billingDate billing date
   * @param status status
   * @param unappliedRef tagged payment
   * @param channel channel of the payment
   * @param ticketRef income request
   * @param ticketMessage answer of Cashiering
   * @param orNo official receipt
   * @param taggedBy tagger
   * @param taggedAt tagging
   * @param appliedAt application
   * @param cancelReason reason of cancellation
   * @param bulkJobNo billing upload
   */
  public record FeeView(
      Long id,
      String feeNo,
      Long policyId,
      String pnNo,
      String locationRef,
      BigDecimal amount,
      String currency,
      LocalDate billingDate,
      String status,
      String unappliedRef,
      String channel,
      String ticketRef,
      String ticketMessage,
      String orNo,
      String taggedBy,
      Instant taggedAt,
      Instant appliedAt,
      String cancelReason,
      String bulkJobNo) {

    /**
     * Maps a record.
     *
     * @param f record
     * @return view
     */
    public static FeeView from(SbmHandlingFee f) {
      return new FeeView(
          f.getId(),
          f.getFeeNo(),
          f.getPolicyId(),
          f.getPnNo(),
          f.getLocationRef(),
          f.getAmount(),
          f.getCurrency(),
          f.getBillingDate(),
          f.getStatus(),
          f.getUnappliedRef(),
          f.getChannel(),
          f.getTicketRef(),
          f.getTicketMessage(),
          f.getOrNo(),
          f.getTaggedBy(),
          f.getTaggedAt(),
          f.getAppliedAt(),
          f.getCancelReason(),
          f.getBulkJobNo());
    }
  }

  /**
   * A payment matching several records.
   *
   * @param unappliedRef payment
   * @param paymentDate date
   * @param amount amount
   * @param currency currency
   * @param reference reference
   * @param channel channel
   * @param fees candidate records
   */
  public record AmbiguousView(
      String unappliedRef,
      LocalDate paymentDate,
      BigDecimal amount,
      String currency,
      String reference,
      String channel,
      List<FeeView> fees) {

    /**
     * Maps a payment and its candidates.
     *
     * @param payment payment
     * @param fees candidates
     * @return view
     */
    public static AmbiguousView from(UnappliedView payment, List<SbmHandlingFee> fees) {
      return new AmbiguousView(
          payment.unappliedRef(),
          payment.paymentDate(),
          payment.amount(),
          payment.currency(),
          payment.reference(),
          payment.channel(),
          fees.stream().map(FeeView::from).toList());
    }
  }

  /**
   * A tagging by hand.
   *
   * @param unappliedRef payment
   */
  public record TagRequest(@NotBlank String unappliedRef) {}

  /**
   * A cancellation.
   *
   * @param reason reason
   */
  public record CancelRequest(@NotBlank String reason) {}

  /**
   * What the tagger did.
   *
   * @param tagged payments tagged
   * @param ambiguous payments matching several records
   */
  public record TaggingView(int tagged, List<String> ambiguous) {}

  /**
   * A No Touch batch.
   *
   * @param id id
   * @param batchNo number
   * @param insurerCode insurer
   * @param period month
   * @param status status
   * @param lineCount lines
   * @param grossFee gross service fee
   * @param vat VAT
   * @param wtax withholding tax
   * @param exportedFileId export sent to the insurer
   * @param statementFileId statement
   * @param siNo service invoice
   * @param journalBatchNo journal batch
   * @param returnedAt insurer return
   * @param billedAt billing
   */
  public record BatchView(
      Long id,
      String batchNo,
      String insurerCode,
      String period,
      String status,
      int lineCount,
      BigDecimal grossFee,
      BigDecimal vat,
      BigDecimal wtax,
      Long exportedFileId,
      Long statementFileId,
      String siNo,
      String journalBatchNo,
      Instant returnedAt,
      Instant billedAt) {

    /**
     * Maps a batch.
     *
     * @param b batch
     * @return view
     */
    public static BatchView from(SbmNoTouchBatch b) {
      return new BatchView(
          b.getId(),
          b.getBatchNo(),
          b.getInsurerCode(),
          b.getPeriod(),
          b.getStatus(),
          b.getLineCount(),
          b.getGrossFee(),
          b.getVat(),
          b.getWtax(),
          b.getExportedFileId(),
          b.getStatementFileId(),
          b.getSiNo(),
          b.getJournalBatchNo(),
          b.getReturnedAt(),
          b.getBilledAt());
    }
  }

  /**
   * A line of a No Touch batch.
   *
   * @param policyId record
   * @param sbmNo masterlist number
   * @param pnNo PN number
   * @param assuredName assured
   * @param policyNo policy number
   * @param plateNo plate
   * @param sumInsured sum insured
   * @param basicPremium basic premium
   * @param grossFee gross service fee
   * @param vat VAT
   * @param wtax withholding tax
   */
  public record LineView(
      Long policyId,
      String sbmNo,
      String pnNo,
      String assuredName,
      String policyNo,
      String plateNo,
      BigDecimal sumInsured,
      BigDecimal basicPremium,
      BigDecimal grossFee,
      BigDecimal vat,
      BigDecimal wtax) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return view
     */
    public static LineView from(SbmNoTouchLine l) {
      return new LineView(
          l.getPolicyId(),
          l.getSbmNo(),
          l.getPnNo(),
          l.getAssuredName(),
          l.getPolicyNo(),
          l.getPlateNo(),
          l.getSumInsured(),
          l.getBasicPremium(),
          l.getGrossFee(),
          l.getVat(),
          l.getWtax());
    }
  }

  /**
   * An export of a month.
   *
   * @param insurerCode insurer
   * @param period month
   */
  public record ExportRequest(@NotBlank String insurerCode, @NotNull YearMonth period) {}
}
