package com.iortatechnxt.brokerverse.disbursement.api.dto;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.CwtDirection;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.EventSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.StatusEditStage;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.TagKind;
import com.iortatechnxt.brokerverse.disbursement.domain.Instrument;
import com.iortatechnxt.brokerverse.disbursement.domain.InstrumentEvent;
import com.iortatechnxt.brokerverse.disbursement.domain.StatusEdit;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherTag;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherTag.CwtTag;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherTag.ReceiptTag;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherViewService.VoucherView;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Requests and responses of the instrument, status edit and tag endpoints (DIS 2.8-2.11). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class InstrumentDtos {

  private InstrumentDtos() {}

  /**
   * An instrument with its history.
   *
   * @param id id
   * @param mode mode
   * @param instrumentNo number
   * @param status status
   * @param amount amount
   * @param currency currency
   * @param reference bank reference
   * @param printedOn print date
   * @param releasedTo released to
   * @param history status history
   * @param edits status edits
   * @param statuses statuses of the mode
   */
  public record InstrumentResponse(
      Long id,
      DisbursementMode mode,
      String instrumentNo,
      InstrumentStatus status,
      BigDecimal amount,
      String currency,
      String reference,
      LocalDate printedOn,
      String releasedTo,
      List<EventResponse> history,
      List<StatusEditResponse> edits,
      List<InstrumentStatus> statuses) {

    /**
     * Maps the instrument of a voucher page.
     *
     * @param view voucher page
     * @return DTO
     */
    public static InstrumentResponse from(VoucherView view) {
      Instrument i = view.instrument();
      return new InstrumentResponse(
          i.getId(),
          i.getMode(),
          i.getInstrumentNo(),
          i.getStatus(),
          i.getAmount(),
          i.getCurrency(),
          i.getReference(),
          i.getPrintedOn(),
          i.getReleasedTo(),
          view.history().stream().map(EventResponse::from).toList(),
          view.edits().stream().map(StatusEditResponse::from).toList(),
          view.statuses());
    }
  }

  /**
   * One status change.
   *
   * @param fromStatus previous status
   * @param toStatus new status
   * @param source what changed it
   * @param note note
   * @param fileRef upload
   * @param at when
   * @param by who
   */
  public record EventResponse(
      InstrumentStatus fromStatus,
      InstrumentStatus toStatus,
      EventSource source,
      String note,
      String fileRef,
      Instant at,
      String by) {

    /**
     * Maps an event.
     *
     * @param e event
     * @return DTO
     */
    public static EventResponse from(InstrumentEvent e) {
      return new EventResponse(
          e.getFromStatus(),
          e.getToStatus(),
          e.getSource(),
          e.getNote(),
          e.getFileRef(),
          e.getCreatedAt(),
          e.getCreatedBy());
    }
  }

  /**
   * A status edit.
   *
   * @param id id
   * @param fromStatus current status
   * @param toStatus requested status
   * @param reason reason
   * @param stage stage
   * @param requestedBy requestor
   * @param requestedAt requested
   * @param decidedBy decided by
   */
  public record StatusEditResponse(
      Long id,
      InstrumentStatus fromStatus,
      InstrumentStatus toStatus,
      String reason,
      StatusEditStage stage,
      String requestedBy,
      Instant requestedAt,
      String decidedBy) {

    /**
     * Maps an edit.
     *
     * @param e edit
     * @return DTO
     */
    public static StatusEditResponse from(StatusEdit e) {
      return new StatusEditResponse(
          e.getId(),
          e.getFromStatus(),
          e.getToStatus(),
          e.getReason(),
          e.getStage(),
          e.getCreatedBy(),
          e.getCreatedAt(),
          e.getDecidedBy());
    }
  }

  /**
   * A tag.
   *
   * @param id id
   * @param kind OR / AR or CWT
   * @param direction CWT direction
   * @param docNo receipt or certificate number
   * @param docDate receipt date
   * @param receivedOn received
   * @param releasedOn released
   * @param periodFrom period from
   * @param periodTo period to
   * @param amount amount
   * @param certificateRef register reference
   * @param remarks remarks
   * @param taggedBy user
   */
  public record TagResponse(
      Long id,
      TagKind kind,
      CwtDirection direction,
      String docNo,
      LocalDate docDate,
      LocalDate receivedOn,
      LocalDate releasedOn,
      LocalDate periodFrom,
      LocalDate periodTo,
      BigDecimal amount,
      String certificateRef,
      String remarks,
      String taggedBy) {

    /**
     * Maps a tag.
     *
     * @param t tag
     * @return DTO
     */
    public static TagResponse from(VoucherTag t) {
      return new TagResponse(
          t.getId(),
          t.getKind(),
          t.getDirection(),
          t.getDocNo(),
          t.getDocDate(),
          t.getReceivedOn(),
          t.getReleasedOn(),
          t.getPeriodFrom(),
          t.getPeriodTo(),
          t.getAmount(),
          t.getCertificateRef(),
          t.getRemarks(),
          t.getCreatedBy());
    }
  }

  /**
   * An OR / AR received (DIS 2.10.2).
   *
   * @param receiptNo number
   * @param receiptDate date
   * @param receivedOn date received
   * @param amount amount
   * @param remarks remarks
   */
  public record ReceiptTagRequest(
      @NotBlank @Size(max = 40) String receiptNo,
      @NotNull LocalDate receiptDate,
      @NotNull LocalDate receivedOn,
      @Digits(integer = 17, fraction = 2) BigDecimal amount,
      @Size(max = 500) String remarks) {

    /**
     * The tag values.
     *
     * @return receipt tag
     */
    public ReceiptTag tag() {
      return new ReceiptTag(receiptNo.strip(), receiptDate, receivedOn, amount, remarks);
    }
  }

  /**
   * A CWT certificate (DIS 2.11.2).
   *
   * @param direction received or released
   * @param certificateNo number
   * @param periodFrom period from
   * @param periodTo period to
   * @param on date received or released
   * @param amount amount
   * @param certificateRef register reference
   * @param remarks remarks
   */
  public record CwtTagRequest(
      @NotNull CwtDirection direction,
      @NotBlank @Size(max = 40) String certificateNo,
      @NotNull LocalDate periodFrom,
      @NotNull LocalDate periodTo,
      @NotNull LocalDate on,
      @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
      @Size(max = 60) String certificateRef,
      @Size(max = 500) String remarks) {

    /**
     * The tag values.
     *
     * @return CWT tag
     */
    public CwtTag tag() {
      return new CwtTag(
          direction,
          certificateNo.strip(),
          periodFrom,
          periodTo,
          on,
          amount,
          certificateRef,
          remarks);
    }
  }

  /**
   * To whom a check or MC / DD was released.
   *
   * @param releasedTo recipient
   */
  public record ReleaseRequest(@NotBlank @Size(max = 250) String releasedTo) {}

  /**
   * A bank reference (branch confirmation, BOB reference, MC / DD number).
   *
   * @param reference reference
   */
  public record ReferenceRequest(@Size(max = 80) String reference) {}

  /**
   * The ATD e-mail recipients (DIS 2.7.7).
   *
   * @param to branch mailboxes
   * @param cc copies (requestor)
   */
  public record EmailRequest(
      @NotEmpty @Size(max = 10) List<@Email @NotBlank String> to,
      @Size(max = 10) List<@Email @NotBlank String> cc) {}

  /**
   * A status edit request (DIS 2.8.5).
   *
   * @param toStatus requested status
   * @param reason reason
   */
  public record StatusEditRequest(
      @NotNull InstrumentStatus toStatus, @NotBlank @Size(max = 500) String reason) {}
}
