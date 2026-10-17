package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The client's changes relayed to insurers (BRID-012, 015; FR-EB-044, 045): requested changes on
 * TOR items or in free text, and the insurers asked to revise their proposals, each OPEN until its
 * revised proposal is recorded. The request is ANSWERED when every target answered.
 */
@Entity
@Table(name = "eb_revision_request")
public class EbRevisionRequest extends EbCycleRecord {

  /** Status of a revision request or of one of its targets. */
  public enum Status {
    /** Waiting for the revised proposal(s). */
    OPEN,
    /** Answered. */
    ANSWERED
  }

  @Column(name = "revision_no", nullable = false, updatable = false)
  private int revisionNo;

  @Column(length = 1000)
  private String description;

  @Column(name = "relayed_at", nullable = false)
  private Instant relayedAt;

  @Column(name = "relayed_by", nullable = false, length = 50)
  private String relayedBy;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.OPEN;

  @OneToMany(mappedBy = "revision", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder")
  private final List<Item> items = new ArrayList<>();

  @OneToMany(mappedBy = "revision", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private final List<Target> targets = new ArrayList<>();

  protected EbRevisionRequest() {}

  /**
   * Records a revision request.
   *
   * @param cycle cycle
   * @param revisionNo number within the cycle
   * @param description description, may be null
   * @param sending time, user and due date
   */
  public EbRevisionRequest(
      EbCycle cycle, int revisionNo, String description, EbInsurerRequest.Sending sending) {
    super(cycle);
    this.revisionNo = revisionNo;
    this.description = description;
    this.relayedAt = sending.at();
    this.relayedBy = sending.by();
    this.dueDate = sending.dueDate();
  }

  /**
   * Adds a requested change.
   *
   * @param torItemId TOR item, may be null (free text)
   * @param change the requested change
   */
  public void addItem(Long torItemId, String change) {
    items.add(new Item(this, items.size() + 1, torItemId, change));
  }

  /**
   * Adds an insurer asked to revise.
   *
   * @param insurerCode insurer
   * @return the target
   */
  public Target addTarget(String insurerCode) {
    Target target = new Target(this, insurerCode);
    targets.add(target);
    return target;
  }

  /**
   * The open target of an insurer.
   *
   * @param insurerCode insurer
   * @return target
   */
  public Optional<Target> openTarget(String insurerCode) {
    return targets.stream()
        .filter(t -> t.status == Status.OPEN && t.insurerCode.equals(insurerCode))
        .findFirst();
  }

  /**
   * Marks the target of an insurer answered by a proposal; the request is answered with its last
   * target.
   *
   * @param target target
   * @param proposalId revised proposal
   */
  public void answer(Target target, Long proposalId) {
    target.status = Status.ANSWERED;
    target.answeredProposalId = proposalId;
    if (targets.stream().allMatch(t -> t.status == Status.ANSWERED)) {
      this.status = Status.ANSWERED;
    }
  }

  public int getRevisionNo() {
    return revisionNo;
  }

  public String getDescription() {
    return description;
  }

  public Instant getRelayedAt() {
    return relayedAt;
  }

  public String getRelayedBy() {
    return relayedBy;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public Status getStatus() {
    return status;
  }

  public List<Item> getItems() {
    return Collections.unmodifiableList(items);
  }

  public List<Target> getTargets() {
    return Collections.unmodifiableList(targets);
  }

  /** A requested change of a revision. */
  @Entity(name = "EbRevisionItem")
  @Table(name = "eb_revision_item")
  public static class Item extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false, updatable = false)
    private EbRevisionRequest revision;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "tor_item_id")
    private Long torItemId;

    @Column(name = "requested_change", nullable = false, length = 1000)
    private String requestedChange;

    protected Item() {}

    Item(EbRevisionRequest revision, int sortOrder, Long torItemId, String requestedChange) {
      this.revision = revision;
      this.sortOrder = sortOrder;
      this.torItemId = torItemId;
      this.requestedChange = requestedChange;
    }

    public int getSortOrder() {
      return sortOrder;
    }

    public Long getTorItemId() {
      return torItemId;
    }

    public String getRequestedChange() {
      return requestedChange;
    }
  }

  /** An insurer asked to revise its proposal. */
  @Entity(name = "EbRevisionTarget")
  @Table(name = "eb_revision_target")
  public static class Target extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false, updatable = false)
    private EbRevisionRequest revision;

    @Column(name = "insurer_code", nullable = false, length = 30)
    private String insurerCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "answered_proposal_id")
    private Long answeredProposalId;

    protected Target() {}

    Target(EbRevisionRequest revision, String insurerCode) {
      this.revision = revision;
      this.insurerCode = insurerCode;
    }

    /**
     * Keeps the outbox message of the relay.
     *
     * @param message message id
     */
    public void sentAs(Long message) {
      this.messageId = message;
    }

    public String getInsurerCode() {
      return insurerCode;
    }

    public Status getStatus() {
      return status;
    }

    public Long getMessageId() {
      return messageId;
    }

    public Long getAnsweredProposalId() {
      return answeredProposalId;
    }
  }
}
