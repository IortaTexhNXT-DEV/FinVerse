package com.iortatechnxt.brokerverse.payrequest.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A Marketing request for a payment or a check cancellation (MKT 1.2.0-2.26.0;
 * ACCOUNTING_DISBURSEMENT_DESIGN 5.2): a Refund Request Form with its lines, a Request for Payment
 * of an employee cash advance, or the cancellation of a disbursed check. Its stage mirrors the work
 * case of the kind's workflow; approved requests are paid through the Operations {@code
 * DisbursementGateway} and the Disbursement statuses are tracked on it.
 */
@Entity
@Table(name = "prq_request")
public class PaymentRequest extends BaseEntity {

  private static final String SEND_SEPARATOR = "/";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private RequestKind kind;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private RequestStage stage;

  @Column(name = "request_date", nullable = false)
  private LocalDate requestDate;

  @Embedded private RequestContent content;

  @Embedded private Payee payee;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "validation_required", nullable = false)
  private boolean validationRequired;

  @Column(name = "validation_round", nullable = false)
  private int validationRound;

  @Embedded private CancellationTarget target;

  @Embedded private RequestTrail trail;

  @Embedded private DisbursementTrack track;

  @Column(name = "payout_recorded", nullable = false)
  private boolean payoutRecorded;

  @Column(name = "send_count", nullable = false)
  private int sendCount;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "request_id", nullable = false)
  @OrderBy("lineNo")
  private final List<RefundLine> lines = new ArrayList<>();

  protected PaymentRequest() {}

  /**
   * Opens a request.
   *
   * @param companyId company
   * @param branchId branch
   * @param requestNo request number
   * @param kind refund, cash advance or check cancellation
   * @param requestDate request date
   */
  public PaymentRequest(
      Long companyId, Long branchId, String requestNo, RequestKind kind, LocalDate requestDate) {
    this.companyId = companyId;
    this.branchId = branchId;
    this.requestNo = requestNo;
    this.kind = kind;
    this.requestDate = requestDate;
    this.stage =
        kind == RequestKind.CHECK_CANCELLATION ? RequestStage.REQUESTED : RequestStage.DRAFT;
    this.amount = BigDecimal.ZERO;
    this.target = CancellationTarget.NONE;
    this.trail = RequestTrail.NONE;
    this.track = DisbursementTrack.NONE;
  }

  /**
   * Sets the form (creation and changes while editable).
   *
   * @param newContent header fields
   * @param newPayee payee and mode
   * @param newAmount amount (the sum of the lines for refunds)
   */
  public void fill(RequestContent newContent, Payee newPayee, BigDecimal newAmount) {
    this.content = newContent;
    this.payee = newPayee;
    this.amount = newAmount;
  }

  /**
   * Replaces the refund lines.
   *
   * @param newLines lines
   * @param needsValidation whether a line concerns a cancelled policy (MKT 1.11.0)
   */
  public void replaceLines(List<RefundLine> newLines, boolean needsValidation) {
    lines.clear();
    lines.addAll(newLines);
    this.validationRequired = needsValidation;
  }

  /** Removes the lines (the caller flushes before adding new ones, for the live AR index). */
  public void clearLines() {
    lines.clear();
  }

  /**
   * Sets the check a cancellation request is about.
   *
   * @param newTarget target
   */
  public void aimAt(CancellationTarget newTarget) {
    this.target = newTarget;
  }

  /**
   * Mirrors the work case stage.
   *
   * @param newStage stage
   */
  public void moveTo(RequestStage newStage) {
    this.stage = newStage;
    if (newStage == RequestStage.CANCELLED) {
      lines.forEach(RefundLine::release);
    }
  }

  /**
   * Starts a new validation round (first submission or resubmission after a rejection).
   *
   * @return round number
   */
  public int nextValidationRound() {
    this.validationRound++;
    return validationRound;
  }

  /**
   * Records who did the last step.
   *
   * @param newTrail trail
   */
  public void record(RequestTrail newTrail) {
    this.trail = newTrail;
  }

  /**
   * Records where the payment stands.
   *
   * @param newTrack track
   */
  public void track(DisbursementTrack newTrack) {
    this.track = newTrack;
  }

  /**
   * The source reference of the next sending to Disbursement: the request number, then {@code
   * <request>/<n>} when a returned request is sent again (the gateway is idempotent on it).
   *
   * @return source reference
   */
  public String nextSendRef() {
    this.sendCount++;
    return currentSendRef();
  }

  /**
   * The source reference of the latest sending to Disbursement.
   *
   * @return source reference
   */
  public String currentSendRef() {
    return sendCount <= 1 ? requestNo : requestNo + SEND_SEPARATOR + sendCount;
  }

  /**
   * The request number of a gateway source reference.
   *
   * @param sourceRef source reference of a sending
   * @return request number
   */
  public static String requestNoOf(String sourceRef) {
    int at = sourceRef.indexOf(SEND_SEPARATOR);
    return at < 0 ? sourceRef : sourceRef.substring(0, at);
  }

  /** The CA / SA information was added to the client record (MKT 2.25.0). */
  public void payoutRecorded() {
    this.payoutRecorded = true;
  }

  /**
   * When the request was paid.
   *
   * @param at time
   */
  public void disbursed(Instant at) {
    this.track = trackOrNone().disbursed(at);
  }

  /**
   * The payment track, never null.
   *
   * @return track
   */
  public DisbursementTrack trackOrNone() {
    return track == null ? DisbursementTrack.NONE : track;
  }

  /**
   * The trail, never null.
   *
   * @return trail
   */
  public RequestTrail trailOrNone() {
    return trail == null ? RequestTrail.NONE : trail;
  }

  /**
   * The cancellation target, never null.
   *
   * @return target
   */
  public CancellationTarget targetOrNone() {
    return target == null ? CancellationTarget.NONE : target;
  }

  /**
   * The first root invoice of the lines (the family the refund belongs to).
   *
   * @return root invoice, or null
   */
  public String rootInvoiceNo() {
    return lines.stream()
        .map(RefundLine::getRootInvoiceNo)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public RequestKind getKind() {
    return kind;
  }

  public RequestStage getStage() {
    return stage;
  }

  public LocalDate getRequestDate() {
    return requestDate;
  }

  public RequestContent getContent() {
    return content;
  }

  public Payee getPayee() {
    return payee;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public boolean isValidationRequired() {
    return validationRequired;
  }

  public int getValidationRound() {
    return validationRound;
  }

  public boolean isPayoutRecorded() {
    return payoutRecorded;
  }

  public List<RefundLine> getLines() {
    return List.copyOf(lines);
  }
}
