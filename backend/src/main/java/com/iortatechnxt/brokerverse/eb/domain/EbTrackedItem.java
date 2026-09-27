package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A deliverable BDOI waits for (BRID-030; FR-EB-057): the contract after placement, an HMO card, a
 * card replacement or a billing, owed by the insurer, the client or BDOI, with its due date. The
 * job {@code EB_ITEM_FOLLOWUP} sends the follow-ups of a pending item past due and escalates it
 * after the last one; receiving, releasing or closing it stops them.
 */
@Entity
@Table(name = "eb_tracked_item")
public class EbTrackedItem extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "programme_id", nullable = false, updatable = false)
  private Long programmeId;

  @Column(name = "cycle_id", updatable = false)
  private Long cycleId;

  @Column(name = "item_type", nullable = false, length = 30, updatable = false)
  private String itemType;

  @Column(nullable = false, length = 200)
  private String subject;

  @Column(name = "member_ref", length = 60)
  private String memberRef;

  @Column(name = "member_change_ref", length = 30)
  private String memberChangeRef;

  @Column(name = "account_arn", length = 30)
  private String accountArn;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private EbResponsibleParty responsible;

  @Column(name = "party_code", length = 30)
  private String partyCode;

  @Column(name = "recipient_email", length = 500)
  private String recipientEmail;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EbItemStatus status = EbItemStatus.PENDING;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "follow_ups_sent", nullable = false)
  private int followUpsSent;

  @Column(name = "last_follow_up_at")
  private Instant lastFollowUpAt;

  @Column(name = "escalated_at")
  private Instant escalatedAt;

  @Column(name = "received_on")
  private LocalDate receivedOn;

  @Column(name = "released_on")
  private LocalDate releasedOn;

  @Column(name = "closed_on")
  private LocalDate closedOn;

  @Column(length = 1000)
  private String remarks;

  @Column(name = "member_id")
  private Long memberId;

  @Column(name = "member_change_id")
  private Long memberChangeId;

  protected EbTrackedItem() {}

  /**
   * Opens a pending item.
   *
   * @param companyId company
   * @param programmeId programme
   * @param cycleId cycle, may be null
   * @param itemType type (list EB_TRACKED_ITEM_TYPE)
   * @param details what is expected, from whom and by when
   */
  public EbTrackedItem(
      Long companyId, Long programmeId, Long cycleId, String itemType, Details details) {
    this.companyId = companyId;
    this.programmeId = programmeId;
    this.cycleId = cycleId;
    this.itemType = itemType;
    apply(details);
  }

  /**
   * Changes what is expected while the item is pending.
   *
   * @param details subject, references, party and due date
   */
  public void update(Details details) {
    requirePending();
    apply(details);
  }

  private void apply(Details details) {
    this.subject = details.subject();
    this.memberRef = details.memberRef();
    this.memberChangeRef = details.memberChangeRef();
    this.accountArn = details.accountArn();
    this.responsible = details.responsible();
    this.partyCode = details.partyCode();
    this.recipientEmail = details.recipientEmail();
    this.dueDate = details.dueDate();
    this.remarks = details.remarks();
  }

  /**
   * Links the item to the roster member and the member change it is expected for (FR-EB-057: an HMO
   * card for each added member, the billing of each member change).
   *
   * @param member roster member, may be null
   * @param change member change, may be null
   */
  public void linkMember(Long member, Long change) {
    this.memberId = member;
    this.memberChangeId = change;
  }

  public Long getMemberId() {
    return memberId;
  }

  public Long getMemberChangeId() {
    return memberChangeId;
  }

  /**
   * The item was received.
   *
   * @param on date received (required)
   * @param note remarks, may be null
   */
  public void receive(LocalDate on, String note) {
    requirePending();
    if (on == null) {
      throw new BusinessRuleException("EB_ITEM_RECEIVED_DATE", "Enter the date received");
    }
    this.receivedOn = on;
    this.status = EbItemStatus.RECEIVED;
    note(note);
  }

  /**
   * The item was released to the client or member.
   *
   * @param on date released
   * @param note remarks, may be null
   */
  public void release(LocalDate on, String note) {
    if (status != EbItemStatus.RECEIVED) {
      throw new BusinessRuleException(
          "EB_ITEM_NOT_RECEIVED", "Only a received item can be released");
    }
    this.releasedOn = on;
    this.status = EbItemStatus.RELEASED;
    note(note);
  }

  /**
   * Closes the item; a pending item needs its date received.
   *
   * @param on date closed
   * @param received date received of a pending item, ignored otherwise
   * @param note remarks, may be null
   */
  public void close(LocalDate on, LocalDate received, String note) {
    if (status == EbItemStatus.CLOSED) {
      throw new BusinessRuleException("EB_ITEM_CLOSED", "The item is already closed");
    }
    if (status == EbItemStatus.PENDING) {
      if (received == null) {
        throw new BusinessRuleException("EB_ITEM_RECEIVED_DATE", "Enter the date received");
      }
      this.receivedOn = received;
    }
    this.closedOn = on;
    this.status = EbItemStatus.CLOSED;
    note(note);
  }

  /**
   * Counts a follow-up sent to the responsible party.
   *
   * @param when time sent
   */
  public void followedUp(Instant when) {
    this.followUpsSent++;
    this.lastFollowUpAt = when;
  }

  /**
   * Marks the item escalated to the AO (once).
   *
   * @param when time
   */
  public void escalate(Instant when) {
    if (escalatedAt == null) {
      this.escalatedAt = when;
    }
  }

  private void note(String note) {
    if (note != null && !note.isBlank()) {
      this.remarks = note.strip();
    }
  }

  private void requirePending() {
    if (status != EbItemStatus.PENDING) {
      throw new BusinessRuleException(
          "EB_ITEM_NOT_PENDING", "The item is no longer pending (" + status.name() + ")");
    }
  }

  /**
   * Whether the item is past due on a date.
   *
   * @param today business date
   * @return true when pending after its due date
   */
  public boolean overdueOn(LocalDate today) {
    return status == EbItemStatus.PENDING && today.isAfter(dueDate);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getProgrammeId() {
    return programmeId;
  }

  public Long getCycleId() {
    return cycleId;
  }

  public String getItemType() {
    return itemType;
  }

  public String getSubject() {
    return subject;
  }

  public String getMemberRef() {
    return memberRef;
  }

  public String getMemberChangeRef() {
    return memberChangeRef;
  }

  public String getAccountArn() {
    return accountArn;
  }

  public EbResponsibleParty getResponsible() {
    return responsible;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public String getRecipientEmail() {
    return recipientEmail;
  }

  public EbItemStatus getStatus() {
    return status;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public int getFollowUpsSent() {
    return followUpsSent;
  }

  public Instant getLastFollowUpAt() {
    return lastFollowUpAt;
  }

  public Instant getEscalatedAt() {
    return escalatedAt;
  }

  public LocalDate getReceivedOn() {
    return receivedOn;
  }

  public LocalDate getReleasedOn() {
    return releasedOn;
  }

  public LocalDate getClosedOn() {
    return closedOn;
  }

  public String getRemarks() {
    return remarks;
  }

  /**
   * What is expected, from whom and by when.
   *
   * @param subject what is expected (e.g. "HMO card of Juan Dela Cruz")
   * @param memberRef employee number or member name, may be null
   * @param memberChangeRef member change number, may be null
   * @param accountArn account (contract after placement), may be null
   * @param responsible who owes it
   * @param partyCode insurer party code when the insurer owes it
   * @param recipientEmail follow-up recipients (comma separated), may be null
   * @param dueDate due date
   * @param remarks remarks, may be null
   */
  public record Details(
      String subject,
      String memberRef,
      String memberChangeRef,
      String accountArn,
      EbResponsibleParty responsible,
      String partyCode,
      String recipientEmail,
      LocalDate dueDate,
      String remarks) {}
}
