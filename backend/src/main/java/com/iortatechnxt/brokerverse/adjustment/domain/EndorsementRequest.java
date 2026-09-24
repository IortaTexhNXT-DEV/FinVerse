package com.iortatechnxt.brokerverse.adjustment.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.Hibernate;

/**
 * An endorsement or cancellation request on a booked invoice (OPERATIONS_DESIGN 4.5; ADJID.001-025,
 * MKTID.008): what is asked, the recompute per component and per insurer, the processing trail and
 * what the posting produced. The stage mirrors the {@code OPS_ENDORSEMENT} work case.
 */
@Entity
@Table(name = "adj_request")
public class EndorsementRequest extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(name = "request_no", nullable = false, length = 40, updatable = false)
  private String requestNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "request_class", nullable = false, length = 20)
  private RequestClass requestClass;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Computation computation;

  @Embedded private RequestSubject subject;

  @Embedded private RequestTerms terms;

  @Embedded private AmountInput amounts;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private RequestStage stage;

  @Column(name = "needs_approval", nullable = false)
  private boolean needsApproval;

  @Column(nullable = false)
  private boolean negative;

  @Column(name = "duplicate_override", length = 500)
  private String duplicateOverride;

  @Column(name = "baseline_override", length = 500)
  private String baselineOverride;

  @Column(name = "quotation_required", nullable = false)
  private boolean quotationRequired;

  @Column(name = "quotation_ref", length = 40)
  private String quotationRef;

  @Column(name = "handoff_ref", length = 40)
  private String handoffRef;

  @Column(name = "return_reason", length = 40)
  private String returnReason;

  @Column(name = "return_comment", length = 1000)
  private String returnComment;

  @Embedded private ProcessingTrail trail;

  @Embedded private PostingOutcome outcome;

  @Column(name = "slip_no", length = 40)
  private String slipNo;

  @ElementCollection
  @CollectionTable(name = "adj_request_component", joinColumns = @JoinColumn(name = "request_id"))
  @OrderColumn(name = "line_index")
  private final List<ComponentChange> changes = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "adj_request_share", joinColumns = @JoinColumn(name = "request_id"))
  @OrderColumn(name = "share_index")
  private final List<ShareChange> shares = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "adj_request_journal", joinColumns = @JoinColumn(name = "request_id"))
  @OrderColumn(name = "journal_index")
  @Column(name = "batch_no", nullable = false, length = 40)
  private final List<String> journals = new ArrayList<>();

  protected EndorsementRequest() {}

  /**
   * A new draft request.
   *
   * @param companyId company
   * @param branchId branch of the invoice
   * @param requestNo request number ({@code ENR-<yyyy>})
   * @param subject invoice and account
   * @param content class, computation, terms and amounts
   */
  public EndorsementRequest(
      Long companyId, Long branchId, String requestNo, RequestSubject subject, Content content) {
    this.companyId = companyId;
    this.branchId = branchId;
    this.requestNo = requestNo;
    this.subject = subject;
    this.stage = RequestStage.DRAFT;
    this.trail = ProcessingTrail.NONE;
    this.outcome = PostingOutcome.NONE;
    apply(content);
  }

  private void apply(Content content) {
    this.requestClass = content.requestClass();
    this.computation = content.computation();
    this.terms = content.terms();
    this.amounts = content.amounts();
    this.needsApproval = content.needsApproval();
    this.negative = content.negative();
  }

  /**
   * Changes a draft or returned request.
   *
   * @param content new content
   */
  public void revise(Content content) {
    if (!stage.isEditable()) {
      throw new BusinessRuleException(
          "ADJ_REQUEST_NOT_EDITABLE", requestNo + " is " + stage + " and can no longer be changed");
    }
    apply(content);
  }

  /**
   * Records the recompute (before / after per component and per insurer share).
   *
   * @param components component changes
   * @param insurers insurer share changes
   */
  public void recordRecompute(List<ComponentChange> components, List<ShareChange> insurers) {
    changes.clear();
    changes.addAll(components);
    shares.clear();
    shares.addAll(insurers);
  }

  /**
   * Records the justifications of a duplicate or an over-adjustment (ADJID.023/028).
   *
   * @param duplicate why a duplicate request proceeds, may be null
   * @param baseline why the over-adjustment baseline is exceeded, may be null
   */
  public void overrides(String duplicate, String baseline) {
    this.duplicateOverride = duplicate;
    this.baselineOverride = baseline;
  }

  /**
   * Flags a TSI increase above the package limit (ADJID.008): Marketing prepares a quotation.
   *
   * @param required whether a quotation is required
   * @param handoff reference of the hand-off raised for Marketing, may be null
   */
  public void quotationRequired(boolean required, String handoff) {
    this.quotationRequired = required;
    this.handoffRef = handoff;
  }

  /**
   * Links the quotation prepared for a TSI increase, by reference only (ADJID.008).
   *
   * @param reference quotation number
   */
  public void linkQuotation(String reference) {
    this.quotationRef = reference;
  }

  /**
   * Mirrors the stage of the work case.
   *
   * @param next new stage
   */
  public void moveTo(RequestStage next) {
    this.stage = next;
  }

  /**
   * Records a return with its reason (ADJID.005/007).
   *
   * @param reason return reason code
   * @param comment comment
   */
  public void returned(String reason, String comment) {
    this.returnReason = reason;
    this.returnComment = comment;
  }

  /**
   * Records the submission.
   *
   * @param by user
   * @param at time
   */
  public void submitted(String by, Instant at) {
    trail = trail().submitted(by, at);
  }

  /**
   * Records the validation.
   *
   * @param by user
   * @param at time
   */
  public void validated(String by, Instant at) {
    trail = trail().validated(by, at);
  }

  /**
   * Records the approval.
   *
   * @param by user
   * @param at time
   */
  public void approved(String by, Instant at) {
    trail = trail().approved(by, at);
  }

  /**
   * Records the posting.
   *
   * @param result what was posted
   * @param journalBatches journal batches written
   * @param by user
   * @param at time
   */
  public void posted(PostingOutcome result, List<String> journalBatches, String by, Instant at) {
    this.outcome = result;
    journalBatches.stream().filter(b -> !journals.contains(b)).forEach(journals::add);
    trail = trail().posted(by, at, stage != RequestStage.AWAITING_REAPPLICATION);
  }

  /**
   * Records the re-application of the invoice's payments.
   *
   * @param result posting outcome with the excess
   * @param at time
   */
  public void reapplied(PostingOutcome result, Instant at) {
    this.outcome = result;
    trail = trail().completed(at);
  }

  /**
   * Records the posting batch of the request.
   *
   * @param batchNo validation batch number
   */
  public void inBatch(String batchNo) {
    this.outcome = outcome().inBatch(batchNo);
  }

  /**
   * Records the end of a cancelled request.
   *
   * @param at time
   */
  public void closed(Instant at) {
    trail = trail().completed(at);
  }

  /**
   * Numbers the endorsement slip once (ADJID.015).
   *
   * @param number slip number ({@code ES-<yyyy>})
   */
  public void assignSlip(String number) {
    if (slipNo == null) {
      this.slipNo = number;
    }
  }

  /** Loads the collections (reads outside the persistence context). */
  public void loadCollections() {
    Hibernate.initialize(changes);
    Hibernate.initialize(shares);
    Hibernate.initialize(journals);
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

  public RequestClass getRequestClass() {
    return requestClass;
  }

  public Computation getComputation() {
    return computation;
  }

  public RequestSubject getSubject() {
    return subject;
  }

  public RequestTerms getTerms() {
    return terms;
  }

  public AmountInput getAmounts() {
    return amounts == null ? AmountInput.NONE : amounts;
  }

  public RequestStage getStage() {
    return stage;
  }

  public boolean isNeedsApproval() {
    return needsApproval;
  }

  public boolean isNegative() {
    return negative;
  }

  public String getDuplicateOverride() {
    return duplicateOverride;
  }

  public String getBaselineOverride() {
    return baselineOverride;
  }

  public boolean isQuotationRequired() {
    return quotationRequired;
  }

  public String getQuotationRef() {
    return quotationRef;
  }

  public String getHandoffRef() {
    return handoffRef;
  }

  public String getReturnReason() {
    return returnReason;
  }

  public String getReturnComment() {
    return returnComment;
  }

  /**
   * The processing trail.
   *
   * @return trail, never null
   */
  public ProcessingTrail trail() {
    return trail == null ? ProcessingTrail.NONE : trail;
  }

  /**
   * The posting outcome.
   *
   * @return outcome, never null
   */
  public PostingOutcome outcome() {
    return outcome == null ? PostingOutcome.NONE : outcome;
  }

  public String getSlipNo() {
    return slipNo;
  }

  public List<ComponentChange> getChanges() {
    return Collections.unmodifiableList(changes);
  }

  public List<ShareChange> getShares() {
    return Collections.unmodifiableList(shares);
  }

  public List<String> getJournals() {
    return Collections.unmodifiableList(journals);
  }

  /**
   * The editable content of a request.
   *
   * @param requestClass financial, non-financial or internal
   * @param computation how the amounts are computed
   * @param terms type, reason, dates and inputs
   * @param amounts amounts entered (amount changes)
   * @param needsApproval whether the team leader approves it (ADJID.010)
   * @param negative whether the request reduces the invoice (PENDING_NEG_ADJ)
   */
  public record Content(
      RequestClass requestClass,
      Computation computation,
      RequestTerms terms,
      AmountInput amounts,
      boolean needsApproval,
      boolean negative) {}
}
