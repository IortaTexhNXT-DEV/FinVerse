package com.iortatechnxt.brokerverse.prodrecon.api.dto;

import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ExtractTrigger;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.Frequency;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtract;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtractLine;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconSchedule;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Extract, sending and schedule records of the production reconciliation API (PRCID.001-011). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class ReconExtractDtos {

  private ReconExtractDtos() {}

  /**
   * A production register extract.
   *
   * @param id id
   * @param cycleId cycle
   * @param extractNo extract number
   * @param trigger scheduled or manual
   * @param bookingFrom first booking date
   * @param bookingTo last booking date
   * @param fileName file name
   * @param fileId repository file
   * @param rowCount lines
   * @param newCount lines new to the cycle
   * @param createdAt extracted at
   * @param createdBy extracted by
   * @param sentAt sent at
   * @param sentBy sent by
   * @param recipients recipients
   */
  public record ExtractResponse(
      Long id,
      Long cycleId,
      String extractNo,
      ExtractTrigger trigger,
      LocalDate bookingFrom,
      LocalDate bookingTo,
      String fileName,
      Long fileId,
      int rowCount,
      int newCount,
      Instant createdAt,
      String createdBy,
      Instant sentAt,
      String sentBy,
      String recipients) {

    /**
     * Maps an extract.
     *
     * @param e extract
     * @return response
     */
    public static ExtractResponse from(ReconExtract e) {
      return new ExtractResponse(
          e.getId(),
          e.getCycleId(),
          e.getExtractNo(),
          e.getTrigger(),
          e.getBookingFrom(),
          e.getBookingTo(),
          e.getFileName(),
          e.getFileId(),
          e.getRowCount(),
          e.getNewCount(),
          e.getCreatedAt(),
          e.getCreatedBy(),
          e.getSentAt(),
          e.getSentBy(),
          e.getRecipients());
    }
  }

  /**
   * A register line (Annex IV #5).
   *
   * @param lineNo line
   * @param invoiceNo invoice
   * @param kind booking, endorsement or cancellation
   * @param bookingDate booking date
   * @param inceptionDate inception
   * @param expiryDate expiry
   * @param policyNo policy
   * @param endorsementNo endorsement
   * @param pnNos PN numbers
   * @param assuredName assured
   * @param riskCode risk code
   * @param aoUsername account officer
   * @param basicPremium basic premium
   * @param grossCommission commission
   * @param grossPremium gross premium
   * @param bookedVat VAT on commission
   * @param amountPaid premium paid
   * @param datePaid last payment
   * @param remittanceStatus remittance status
   * @param estimated estimated item (RMTID.037)
   */
  public record ExtractLineResponse(
      int lineNo,
      String invoiceNo,
      String kind,
      LocalDate bookingDate,
      LocalDate inceptionDate,
      LocalDate expiryDate,
      String policyNo,
      String endorsementNo,
      String pnNos,
      String assuredName,
      String riskCode,
      String aoUsername,
      BigDecimal basicPremium,
      BigDecimal grossCommission,
      BigDecimal grossPremium,
      BigDecimal bookedVat,
      BigDecimal amountPaid,
      LocalDate datePaid,
      String remittanceStatus,
      boolean estimated) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return response
     */
    public static ExtractLineResponse from(ReconExtractLine l) {
      return new ExtractLineResponse(
          l.getLineNo(),
          l.getInvoiceNo(),
          l.getKind(),
          l.getBookingDate(),
          l.getInceptionDate(),
          l.getExpiryDate(),
          l.getPolicyNo(),
          l.getEndorsementNo(),
          l.getPnNos(),
          l.getAssuredName(),
          l.getRiskCode(),
          l.getAoUsername(),
          l.getBasicPremium(),
          l.getGrossCommission(),
          l.getGrossPremium(),
          l.getBookedVat(),
          l.getAmountPaid(),
          l.getDatePaid(),
          l.getRemittanceStatus(),
          l.isEstimated());
    }
  }

  /**
   * A manual extraction (PRCID.011).
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param from first booking date
   * @param to last booking date
   */
  public record ExtractRequest(
      @NotNull Long companyId,
      @NotBlank @Size(max = 30) String insurerCode,
      @NotNull LocalDate from,
      @NotNull LocalDate to) {}

  /**
   * Recipients of a register.
   *
   * @param to recipients; the insurer's addresses when empty
   * @param cc copy recipients
   */
  public record SendRequest(List<@NotBlank String> to, List<@NotBlank String> cc) {}

  /**
   * An extraction schedule.
   *
   * @param id id
   * @param insurerCode insurer
   * @param frequency frequency
   * @param runDay run day
   * @param nextRunDate next run
   * @param autoSend send right away
   * @param recipients recipients
   * @param active active
   * @param lastRunAt last run
   * @param lastExtractNo last extract
   */
  public record ScheduleResponse(
      Long id,
      String insurerCode,
      Frequency frequency,
      int runDay,
      LocalDate nextRunDate,
      boolean autoSend,
      String recipients,
      boolean active,
      Instant lastRunAt,
      String lastExtractNo) {

    /**
     * Maps a schedule.
     *
     * @param s schedule
     * @return response
     */
    public static ScheduleResponse from(ReconSchedule s) {
      return new ScheduleResponse(
          s.getId(),
          s.getInsurerCode(),
          s.getFrequency(),
          s.getRunDay(),
          s.getNextRunDate(),
          s.isAutoSend(),
          s.getRecipients(),
          s.isActive(),
          s.getLastRunAt(),
          s.getLastExtractNo());
    }
  }

  /**
   * A schedule to save.
   *
   * @param companyId company (new schedules)
   * @param insurerCode insurer (new schedules)
   * @param frequency frequency
   * @param runDay day of the month (1-28) or of the week (1-7)
   * @param autoSend send right away
   * @param recipients recipients, comma separated
   * @param active active
   */
  public record ScheduleRequest(
      Long companyId,
      @Size(max = 30) String insurerCode,
      @NotNull Frequency frequency,
      @Min(1) @Max(28) int runDay,
      boolean autoSend,
      @Size(max = 500) String recipients,
      boolean active) {

    /**
     * The domain terms.
     *
     * @return terms
     */
    public ReconSchedule.Terms terms() {
      return new ReconSchedule.Terms(frequency, runDay, autoSend, recipients, active);
    }
  }

  /**
   * Reconciliation parameters shown on the workbench.
   *
   * @param tolerance amount tolerance (RECON_TOLERANCE)
   * @param keys pairing keys (RECON_MATCH_KEYS)
   */
  public record SettingsResponse(BigDecimal tolerance, List<String> keys) {}
}
