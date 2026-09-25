package com.iortatechnxt.brokerverse.collections.unapplied.api.dto;

import com.iortatechnxt.brokerverse.collections.unapplied.domain.ApplicationRequest;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.UnappliedDisposition;
import com.iortatechnxt.brokerverse.collections.unapplied.service.AccountFacts;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedDispositionService.DispositionInput;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedRules.Rule;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedWorklistService.CollectorRow;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedEvent;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedView;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response records of the collector view of unapplied payments (BRCLXN.030-048). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class UnappliedCollectorDtos {

  private UnappliedCollectorDtos() {}

  /**
   * An unapplied payment in the collector list (BRCLXN.034-036).
   *
   * @param unappliedRef Cashiering reference
   * @param paymentDate payment date
   * @param ageDays days since the payment date
   * @param paymentFileName payment file
   * @param transactionNo transaction no.
   * @param currency currency
   * @param amount paid amount
   * @param balance unapplied balance
   * @param paymentType payment type
   * @param payor payor
   * @param bankCode bank
   * @param checkNo check no.
   * @param reference payor's reference
   * @param clientCode matched client
   * @param invoiceNo matched invoice
   * @param salesUnit marketing unit
   * @param cashieringTab Cashiering tab (processing stage)
   * @param cashieringStatus status of the Cashiering disposition or request
   * @param account account fields of the matched invoice, may be null
   * @param dispositionCode latest collector disposition, may be null
   * @param dispositionInvoiceNo its invoice, may be null
   * @param dispositionBy its author, may be null
   * @param dispositionAt its time, may be null
   */
  public record UnappliedRowResponse(
      String unappliedRef,
      LocalDate paymentDate,
      int ageDays,
      String paymentFileName,
      String transactionNo,
      String currency,
      BigDecimal amount,
      BigDecimal balance,
      String paymentType,
      String payor,
      String bankCode,
      String checkNo,
      String reference,
      String clientCode,
      String invoiceNo,
      String salesUnit,
      String cashieringTab,
      String cashieringStatus,
      AccountFacts account,
      String dispositionCode,
      String dispositionInvoiceNo,
      String dispositionBy,
      Instant dispositionAt) {

    /**
     * Maps a row.
     *
     * @param row row
     * @return response
     */
    public static UnappliedRowResponse from(CollectorRow row) {
      UnappliedView v = row.item();
      UnappliedDisposition d = row.disposition();
      return new UnappliedRowResponse(
          v.unappliedRef(),
          v.paymentDate(),
          row.ageDays(),
          v.paymentFileName(),
          v.transactionNo(),
          v.currency(),
          v.amount(),
          v.balance(),
          v.paymentType(),
          v.payor(),
          v.bankCode(),
          v.checkNo(),
          v.reference(),
          v.matchedClientCode(),
          v.matchedInvoiceNo(),
          v.salesUnit(),
          v.cashieringTab(),
          v.dispositionStatus(),
          row.account(),
          d == null ? null : d.getDispositionCode(),
          d == null ? null : d.getInvoiceNo(),
          d == null ? null : d.getCreatedBy(),
          d == null ? null : d.getCreatedAt());
    }
  }

  /**
   * An unapplied payment with its collector dispositions and requests.
   *
   * @param row the item
   * @param dispositions collector dispositions, newest first
   * @param requests requests to Cashiering, newest first
   */
  public record UnappliedDetailResponse(
      UnappliedRowResponse row,
      List<DispositionResponse> dispositions,
      List<RequestResponse> requests) {}

  /**
   * A collector disposition.
   *
   * @param id id
   * @param unappliedRef item
   * @param dispositionCode value of CLX_UPP_DISPOSITION
   * @param cashieringAction Cashiering action
   * @param invoiceNo target invoice
   * @param amount amount, null for the whole balance
   * @param remarks remarks
   * @param requestId request sent to Cashiering, may be null
   * @param createdBy author
   * @param createdAt time
   */
  public record DispositionResponse(
      Long id,
      String unappliedRef,
      String dispositionCode,
      String cashieringAction,
      String invoiceNo,
      BigDecimal amount,
      String remarks,
      Long requestId,
      String createdBy,
      Instant createdAt) {

    /**
     * Maps a disposition.
     *
     * @param d disposition
     * @return response
     */
    public static DispositionResponse from(UnappliedDisposition d) {
      return new DispositionResponse(
          d.getId(),
          d.getUnappliedRef(),
          d.getDispositionCode(),
          d.getCashieringAction(),
          d.getInvoiceNo(),
          d.getAmount(),
          d.getRemarks(),
          d.getRequestId(),
          d.getCreatedBy(),
          d.getCreatedAt());
    }
  }

  /**
   * A request sent to Cashiering (status view).
   *
   * @param id id
   * @param unappliedRef item
   * @param action Cashiering action
   * @param invoiceNo target invoice
   * @param amount amount, null for the whole balance
   * @param requestedBy collector
   * @param requestedAt time
   * @param sourceRef reference sent to Cashiering
   * @param status SENT, DEFERRED, ACCEPTED, REJECTED or APPLIED
   * @param cashieringRef Cashiering (or hand-off) reference
   * @param statusMessage last message
   * @param statusAt time of the last answer
   * @param payor payor
   * @param currency currency
   * @param paidAmount paid amount
   * @param fileRunNo file that listed it (application requests)
   */
  public record RequestResponse(
      Long id,
      String unappliedRef,
      String action,
      String invoiceNo,
      BigDecimal amount,
      String requestedBy,
      Instant requestedAt,
      String sourceRef,
      String status,
      String cashieringRef,
      String statusMessage,
      Instant statusAt,
      String payor,
      String currency,
      BigDecimal paidAmount,
      String fileRunNo) {

    /**
     * Maps a request.
     *
     * @param r request
     * @return response
     */
    public static RequestResponse from(ApplicationRequest r) {
      return new RequestResponse(
          r.getId(),
          r.getUnappliedRef(),
          r.getAction(),
          r.getInvoiceNo(),
          r.getAmount(),
          r.getRequestedBy(),
          r.getRequestedAt(),
          r.getSourceRef(),
          r.getStatus().name(),
          r.getCashieringRef(),
          r.getStatusMessage(),
          r.getStatusAt(),
          r.getPayment().payorName(),
          r.getPayment().currency(),
          r.getPayment().paidAmount(),
          r.getFileRunNo());
    }
  }

  /**
   * A collector disposition to record.
   *
   * @param companyId company
   * @param dispositionCode value of CLX_UPP_DISPOSITION
   * @param invoiceNo target invoice (mandatory when the value requires one)
   * @param amount amount, empty for the whole balance
   * @param remarks remarks
   */
  public record DisposeRequest(
      @NotNull Long companyId,
      @NotBlank @Size(max = 40) String dispositionCode,
      @Size(max = 40) String invoiceNo,
      @DecimalMin(value = "0.01") BigDecimal amount,
      @Size(max = 1000) String remarks) {

    /**
     * The service input.
     *
     * @return input
     */
    public DispositionInput toInput() {
      return new DispositionInput(dispositionCode, invoiceNo, amount, remarks);
    }
  }

  /**
   * The disposition values of the form with their rule, and the invoice number pattern.
   *
   * @param rules active values in LOV order
   * @param invoicePattern CLX_INVOICE_NO_PATTERN
   */
  public record RulesResponse(List<RuleResponse> rules, String invoicePattern) {}

  /**
   * A disposition value with its rule.
   *
   * @param code code
   * @param label label
   * @param requiresInvoice whether the invoice number is mandatory
   * @param cashieringAction Cashiering action (NONE = documentation only)
   */
  public record RuleResponse(
      String code, String label, boolean requiresInvoice, String cashieringAction) {

    /**
     * Maps a rule.
     *
     * @param r rule
     * @return response
     */
    public static RuleResponse from(Rule r) {
      return new RuleResponse(r.code(), r.label(), r.requiresInvoice(), r.cashieringAction());
    }
  }

  /**
   * An event of an item's history (BRCLXN.040).
   *
   * @param at time
   * @param event event code
   * @param description what happened
   * @param amount amount, may be null
   * @param by user or SYSTEM
   * @param reference related document, may be null
   */
  public record EventResponse(
      Instant at,
      String event,
      String description,
      BigDecimal amount,
      String by,
      String reference) {

    /**
     * Maps an event.
     *
     * @param e event
     * @return response
     */
    public static EventResponse from(UnappliedEvent e) {
      return new EventResponse(
          e.at(), e.event(), e.description(), e.amount(), e.by(), e.reference());
    }
  }

  /**
   * The file written by a manual run.
   *
   * @param path folder and file name, null when nothing was due
   * @param message what happened
   */
  public record FileResponse(String path, String message) {}
}
