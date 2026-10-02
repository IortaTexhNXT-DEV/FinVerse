package com.iortatechnxt.brokerverse.productmaint.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * One negotiation round of a package request (BRPM.010/012, PMADD04): its quotation slip (number,
 * template version, notes, reply date), the insurers approached, the four-eyes approval and the
 * send time. A round is locked at the ManCom sign-off (QS editable until then, BRPM.012).
 */
@Entity
@Table(name = "pm_negotiation_round")
public class NegotiationRound extends BaseEntity {

  @Column(name = "request_id", nullable = false, updatable = false)
  private Long requestId;

  @Column(name = "round_no", nullable = false, updatable = false)
  private int roundNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RoundStatus status = RoundStatus.PREPARATION;

  @Column(name = "qs_no", length = 30)
  private String qsNo;

  @Column(name = "qs_template", length = 60)
  private String qsTemplate;

  @Column(name = "qs_notes", length = 4000)
  private String qsNotes;

  @Column(name = "reply_due")
  private LocalDate replyDue;

  @Column(name = "prepared_by", length = 50)
  private String preparedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(nullable = false)
  private boolean locked;

  @ElementCollection
  @CollectionTable(name = "pm_round_insurer", joinColumns = @JoinColumn(name = "round_id"))
  @OrderColumn(name = "insurer_index")
  @Column(name = "insurer_code", nullable = false, length = 30)
  private final List<String> insurers = new ArrayList<>();

  protected NegotiationRound() {}

  /**
   * Opens a round in preparation.
   *
   * @param requestId request
   * @param roundNo round number (1..n)
   * @param insurerCodes insurers to approach
   * @param notes quotation slip notes (terms asked, changes of this round)
   */
  public NegotiationRound(Long requestId, int roundNo, List<String> insurerCodes, String notes) {
    this.requestId = requestId;
    this.roundNo = roundNo;
    this.insurers.addAll(insurerCodes);
    this.qsNotes = notes;
  }

  /**
   * Changes the insurers and notes while the slip is prepared.
   *
   * @param insurerCodes insurers
   * @param notes notes
   */
  public void prepare(List<String> insurerCodes, String notes) {
    insurers.clear();
    insurers.addAll(insurerCodes);
    this.qsNotes = notes;
    this.status = RoundStatus.PREPARATION;
  }

  /**
   * Submits the slip for approval with its number and template version.
   *
   * @param number QS number
   * @param template template version tag
   * @param reply reply due date
   * @param user preparer
   * @param when time
   */
  public void submit(String number, String template, LocalDate reply, String user, Instant when) {
    this.qsNo = number;
    this.qsTemplate = template;
    this.replyDue = reply;
    this.preparedBy = user;
    this.submittedAt = when;
    this.status = RoundStatus.FOR_APPROVAL;
  }

  /**
   * Records the approval and the send to the insurers.
   *
   * @param user approver
   * @param when time
   */
  public void markSent(String user, Instant when) {
    this.approvedBy = user;
    this.approvedAt = when;
    this.sentAt = when;
    this.status = RoundStatus.SENT;
  }

  /** Closes the round (next round opened, or terms final). */
  public void close() {
    this.status = RoundStatus.CLOSED;
  }

  /** Locks the round at the ManCom sign-off. */
  public void lock() {
    this.locked = true;
  }

  public Long getRequestId() {
    return requestId;
  }

  public int getRoundNo() {
    return roundNo;
  }

  public RoundStatus getStatus() {
    return status;
  }

  public String getQsNo() {
    return qsNo;
  }

  public String getQsTemplate() {
    return qsTemplate;
  }

  public String getQsNotes() {
    return qsNotes;
  }

  public LocalDate getReplyDue() {
    return replyDue;
  }

  public String getPreparedBy() {
    return preparedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public boolean isLocked() {
    return locked;
  }

  public List<String> getInsurers() {
    return List.copyOf(insurers);
  }
}
