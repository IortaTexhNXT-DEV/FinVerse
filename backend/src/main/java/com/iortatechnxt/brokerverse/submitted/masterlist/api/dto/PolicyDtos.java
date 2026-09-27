package com.iortatechnxt.brokerverse.submitted.masterlist.api.dto;

import com.iortatechnxt.brokerverse.submitted.domain.SbmAssured;
import com.iortatechnxt.brokerverse.submitted.domain.SbmBusinessType;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoan;
import com.iortatechnxt.brokerverse.submitted.domain.SbmMarks;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyData;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyHistory;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRisk;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTerms;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTracking;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response records of the masterlist API (FRS FR-SP-003, 010-012). */
public final class PolicyDtos {

  private PolicyDtos() {}

  /**
   * A row of the masterlist.
   *
   * @param id record id
   * @param sbmNo masterlist number
   * @param segment segment code
   * @param businessType NB or RB
   * @param pnNo PN
   * @param policyNo policy number
   * @param assuredName assured
   * @param insurerCode insurer code
   * @param inceptionDate inception
   * @param expiryDate expiry
   * @param sumInsured sum insured
   * @param currency currency
   * @param classification classification
   * @param bucket bucket code
   * @param renewalTag renewal tag
   * @param status status
   * @param handlerUsername handler
   * @param conversionStatus conversion status code
   * @param flags flag chips (RENEWABLE, NON_RENEWABLE, FFY, NO_TOUCH, MIGRATED, INSURER_APPROVAL,
   *     FALLOUT)
   * @param falloutReason fallout reason code
   */
  public record PolicyRow(
      Long id,
      String sbmNo,
      String segment,
      SbmBusinessType businessType,
      String pnNo,
      String policyNo,
      String assuredName,
      String insurerCode,
      LocalDate inceptionDate,
      LocalDate expiryDate,
      BigDecimal sumInsured,
      String currency,
      String classification,
      String bucket,
      String renewalTag,
      String status,
      String handlerUsername,
      String conversionStatus,
      List<String> flags,
      String falloutReason) {

    /**
     * Maps a record.
     *
     * @param p record
     * @return row
     */
    public static PolicyRow from(SbmPolicy p) {
      return new PolicyRow(
          p.getId(),
          p.getSbmNo(),
          p.getSegment(),
          p.getBusinessType(),
          p.getLoan().pnNo(),
          p.getTerms().policyNo(),
          p.getAssured().assuredName(),
          p.getTerms().insurerCode(),
          p.getTerms().inceptionDate(),
          p.getTerms().expiryDate(),
          p.getTerms().sumInsured(),
          p.getTerms().currency(),
          p.getClassification() == null ? null : p.getClassification().name(),
          p.getBucket(),
          p.getRenewalTag(),
          p.getStatus().name(),
          p.getHandlerUsername(),
          p.getConversionStatus(),
          flags(p),
          p.getFalloutReason());
    }
  }

  /**
   * The flag chips of a record (FR-SP-010).
   *
   * @param p record
   * @return flags
   */
  public static List<String> flags(SbmPolicy p) {
    List<String> flags = new java.util.ArrayList<>();
    if (p.getRenewalTag() != null) {
      flags.add(p.getRenewalTag());
    }
    if (p.getMarks().ffy()) {
      flags.add("FFY");
    }
    if (p.getMarks().noTouch()) {
      flags.add("NO_TOUCH");
    }
    if (p.getMarks().employeeAccount()) {
      flags.add("EMPLOYEE");
    }
    if (p.isMigrated()) {
      flags.add("MIGRATED");
    }
    if (p.isInsurerApprovalRequired()) {
      flags.add("INSURER_APPROVAL");
    }
    if (p.isFallout()) {
      flags.add("FALLOUT");
    }
    return List.copyOf(flags);
  }

  /**
   * A record with every field (Policy record page).
   *
   * @param row list fields and flags
   * @param data loan, assured, terms, risk and lists
   * @param outcome processing outcome
   * @param tracking tracking fields
   * @param origin source and dates
   * @param renewal renewal followed
   */
  public record PolicyDetail(
      PolicyRow row,
      SbmPolicyData data,
      Outcome outcome,
      SbmTracking tracking,
      Origin origin,
      Renewal renewal) {

    /**
     * Maps a record.
     *
     * @param p record
     * @return detail
     */
    public static PolicyDetail from(SbmPolicy p) {
      return new PolicyDetail(
          PolicyRow.from(p),
          p.data(),
          new Outcome(
              p.getLoanStatus(),
              p.isAmortised(),
              p.getBucketReason(),
              p.getRaTemplate(),
              p.getRenewalTagSource(),
              p.getRenewalTagReason(),
              p.getRenewalTaggedBy(),
              p.getRenewalTaggedAt(),
              p.getLastRunNo(),
              p.getAdequacyStatus(),
              p.getStatusReason()),
          p.tracking(),
          new Origin(
              p.getSourceCode(),
              p.getDateReceived(),
              p.isMigrated(),
              p.getLegacyRef(),
              p.isHasDocuments(),
              p.getCreatedBy(),
              p.getCreatedAt()),
          new Renewal(p.getRenewalRef(), p.getRenewalArn(), p.getBookedInvoiceNo(), p.getBookedOn()));
    }
  }

  /**
   * Outcome of the processing.
   *
   * @param loanStatus LAMD loan status
   * @param amortised amortised
   * @param bucketReason reason of the bucket
   * @param raTemplate RA template
   * @param renewalTagSource RULE or MANUAL
   * @param renewalTagReason reason of the tag
   * @param renewalTaggedBy user of a manual tag
   * @param renewalTaggedAt time of a manual tag
   * @param lastRunNo last processing run
   * @param adequacyStatus adequacy of the last review
   * @param statusReason reason of the status
   */
  public record Outcome(
      String loanStatus,
      boolean amortised,
      String bucketReason,
      String raTemplate,
      String renewalTagSource,
      String renewalTagReason,
      String renewalTaggedBy,
      Instant renewalTaggedAt,
      String lastRunNo,
      String adequacyStatus,
      String statusReason) {}

  /**
   * Where the record comes from.
   *
   * @param sourceCode source
   * @param dateReceived date received
   * @param migrated migrated
   * @param legacyRef legacy reference
   * @param hasDocuments documents attached
   * @param createdBy creator
   * @param createdAt creation time
   */
  public record Origin(
      String sourceCode,
      LocalDate dateReceived,
      boolean migrated,
      String legacyRef,
      boolean hasDocuments,
      String createdBy,
      Instant createdAt) {}

  /**
   * The renewal followed from the Renewal module and the booking.
   *
   * @param renewalRef renewal reference
   * @param arn renewal account
   * @param bookedInvoiceNo booked invoice
   * @param bookedOn booking date
   */
  public record Renewal(String renewalRef, String arn, String bookedInvoiceNo, LocalDate bookedOn) {}

  /**
   * A new or edited record (FR-SP-003).
   *
   * @param segment segment
   * @param businessType NB or RB
   * @param loan loan
   * @param assured assured
   * @param terms policy terms
   * @param risk risk
   * @param marks lists
   * @param tracking tracking fields of a new record, may be null
   */
  public record PolicyRequest(
      @NotBlank String segment,
      @NotNull SbmBusinessType businessType,
      SbmLoan loan,
      @NotNull SbmAssured assured,
      @NotNull SbmTerms terms,
      SbmRisk risk,
      SbmMarks marks,
      SbmTracking tracking) {

    /**
     * The policy data.
     *
     * @return data
     */
    public SbmPolicyData data() {
      SbmTerms t =
          new SbmTerms(
              terms.insurerCode(),
              terms.policyNo(),
              terms.inceptionDate(),
              terms.expiryDate(),
              terms.coverageDays(),
              terms.sumInsured(),
              terms.totalPremium(),
              terms.currency() == null ? "PHP" : terms.currency());
      return new SbmPolicyData(segment, businessType, loan, assured, t, risk, marks);
    }
  }

  /**
   * A renewal tag (FR-SP-003).
   *
   * @param tag RENEWABLE or NON_RENEWABLE
   * @param reason reason, required for NON_RENEWABLE
   */
  public record TagRequest(@NotBlank String tag, String reason) {}

  /**
   * An action with its reason (dispose, exclude, reinstate, close).
   *
   * @param reasonCode reason
   * @param comment comment
   */
  public record ActionRequest(String reasonCode, String comment) {}

  /**
   * Assign a handler to records (FR-SP-012).
   *
   * @param companyId company
   * @param ids records
   * @param handler handler
   */
  public record AssignRequest(
      @NotNull Long companyId, @NotEmpty List<Long> ids, @NotBlank String handler) {}

  /**
   * A field change of the history.
   *
   * @param field field
   * @param oldValue before
   * @param newValue after
   * @param source what changed it
   * @param reference reference
   * @param by user
   * @param at time
   */
  public record HistoryRow(
      String field,
      String oldValue,
      String newValue,
      String source,
      String reference,
      String by,
      Instant at) {

    /**
     * Maps a history row.
     *
     * @param h change
     * @return row
     */
    public static HistoryRow from(SbmPolicyHistory h) {
      return new HistoryRow(
          h.getField(),
          h.getOldValue(),
          h.getNewValue(),
          h.getSource().name(),
          h.getReference(),
          h.getCreatedBy(),
          h.getCreatedAt());
    }
  }
}
