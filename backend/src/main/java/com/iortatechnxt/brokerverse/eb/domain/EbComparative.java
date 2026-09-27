package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A comparative analysis of the validated proposals of a cycle (BRID-010, 011, 016; FR-EB-041 to
 * 043), numbered {@code EBCA-<yyyy>-nnnnnn}: a snapshot of the rows (JSON) and, per benefit line,
 * the lowest premium and the proposal the AO recommends. It is signed off by someone other than its
 * maker, approved by Management when a threshold rule matches, then presented to the client. A
 * rebuild supersedes the earlier version.
 */
@Entity
@Table(name = "eb_comparative")
public class EbComparative extends EbCycleRecord {

  /** Status of a comparative version. */
  public enum Status {
    /** Built, being reviewed by the AO. */
    DRAFT,
    /** Submitted for sign-off. */
    FOR_APPROVAL,
    /** Waiting for the threshold approval. */
    THRESHOLD_APPROVAL,
    /** Signed off (and approved), ready to present. */
    APPROVED,
    /** Presented to the client. */
    PRESENTED,
    /** Replaced by a later version. */
    SUPERSEDED
  }

  @Column(name = "comparative_no", nullable = false, length = 30, updatable = false)
  private String comparativeNo;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.DRAFT;

  @Column(nullable = false, columnDefinition = "text")
  private String snapshot;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(length = 2000)
  private String summary;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "threshold_rules", length = 500)
  private String thresholdRules;

  @Column(name = "approver_permission", length = 60)
  private String approverPermission;

  @Column(name = "presented_at")
  private Instant presentedAt;

  @Column(name = "presented_by", length = 50)
  private String presentedBy;

  @Column(name = "attachment_id")
  private Long attachmentId;

  @OneToMany(mappedBy = "comparative", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private final List<Line> lines = new ArrayList<>();

  protected EbComparative() {}

  /**
   * Creates a draft version.
   *
   * @param cycle cycle
   * @param comparativeNo number
   * @param versionNo version, from 1
   * @param snapshot JSON of the rows
   * @param dueDate date the comparative is due to the client, may be null
   */
  public EbComparative(
      EbCycle cycle, String comparativeNo, int versionNo, String snapshot, LocalDate dueDate) {
    super(cycle);
    this.comparativeNo = comparativeNo;
    this.versionNo = versionNo;
    this.snapshot = snapshot;
    this.dueDate = dueDate;
  }

  /**
   * Adds a benefit line with its lowest premium and the proposal offering it (the default
   * recommendation).
   *
   * @param benefitLine benefit line
   * @param lowestPremium lowest annual premium, may be null
   * @param proposalId proposal of the lowest premium, may be null
   */
  public void addLine(String benefitLine, BigDecimal lowestPremium, Long proposalId) {
    lines.add(new Line(this, benefitLine, lowestPremium, proposalId));
  }

  /**
   * Marks the recommended proposal of a line.
   *
   * @param benefitLine benefit line
   * @param proposalId validated proposal of the line
   */
  public void recommend(String benefitLine, Long proposalId) {
    requireStatus(Status.DRAFT);
    line(benefitLine)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "EB_COMPARATIVE_LINE", "Benefit line " + benefitLine + " is not compared"))
        .recommendedProposalId = proposalId;
  }

  /**
   * Keeps the AO's summary.
   *
   * @param text summary, may be null
   */
  public void summarise(String text) {
    requireStatus(Status.DRAFT);
    this.summary = text;
  }

  /**
   * Submits the draft for sign-off.
   *
   * @param at time
   * @param by maker
   */
  public void submit(Instant at, String by) {
    requireStatus(Status.DRAFT);
    if (lines.stream().anyMatch(l -> l.recommendedProposalId == null)) {
      throw new BusinessRuleException(
          "EB_RECOMMENDATION_REQUIRED", "Mark the recommended proposal of each line");
    }
    this.status = Status.FOR_APPROVAL;
    this.submittedAt = at;
    this.submittedBy = by;
  }

  /**
   * Signed off: waits for the threshold approval when rules matched, else approved.
   *
   * @param rules matched rules (description), null when none
   * @param approver permission of the threshold approver, null when none
   */
  public void signedOff(String rules, String approver) {
    requireStatus(Status.FOR_APPROVAL);
    this.thresholdRules = rules;
    this.approverPermission = approver;
    this.status = rules == null ? Status.APPROVED : Status.THRESHOLD_APPROVAL;
  }

  /** Approved above the threshold. */
  public void thresholdApproved() {
    requireStatus(Status.THRESHOLD_APPROVAL);
    this.status = Status.APPROVED;
  }

  /**
   * The client confirmation matched a threshold rule the sign-off did not: back to approval.
   *
   * @param rules matched rules
   * @param approver approver permission
   */
  public void needsThresholdApproval(String rules, String approver) {
    this.thresholdRules = rules;
    this.approverPermission = approver;
    this.status = Status.THRESHOLD_APPROVAL;
  }

  /** Returned to the AO. */
  public void returned() {
    if (status != Status.FOR_APPROVAL && status != Status.THRESHOLD_APPROVAL) {
      throw new BusinessRuleException(
          "EB_COMPARATIVE_STATUS", "Comparative " + comparativeNo + " is not waiting for approval");
    }
    this.status = Status.DRAFT;
  }

  /**
   * Presented to the client.
   *
   * @param at time
   * @param by user
   * @param attachment the stored PDF sent
   */
  public void presented(Instant at, String by, Long attachment) {
    requireStatus(Status.APPROVED);
    this.status = Status.PRESENTED;
    this.presentedAt = at;
    this.presentedBy = by;
    this.attachmentId = attachment;
  }

  /** A later version replaces this one. */
  public void supersede() {
    this.status = Status.SUPERSEDED;
  }

  /**
   * The line of a benefit line.
   *
   * @param benefitLine benefit line
   * @return line
   */
  public Optional<Line> line(String benefitLine) {
    return lines.stream().filter(l -> l.benefitLine.equals(benefitLine)).findFirst();
  }

  /**
   * The maker: who submitted the comparative (else who built it).
   *
   * @return username
   */
  public String maker() {
    return submittedBy != null ? submittedBy : getCreatedBy();
  }

  private void requireStatus(Status expected) {
    if (status != expected) {
      throw new BusinessRuleException(
          "EB_COMPARATIVE_STATUS",
          "Comparative " + comparativeNo + " is " + status.name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }
  }

  public String getComparativeNo() {
    return comparativeNo;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public Status getStatus() {
    return status;
  }

  public String getSnapshot() {
    return snapshot;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public String getSummary() {
    return summary;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public String getThresholdRules() {
    return thresholdRules;
  }

  public String getApproverPermission() {
    return approverPermission;
  }

  public Instant getPresentedAt() {
    return presentedAt;
  }

  public String getPresentedBy() {
    return presentedBy;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public List<Line> getLines() {
    return Collections.unmodifiableList(lines);
  }

  /** A benefit line of the comparative with its recommendation. */
  @Entity(name = "EbComparativeLine")
  @Table(name = "eb_comparative_line")
  public static class Line extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comparative_id", nullable = false, updatable = false)
    private EbComparative comparative;

    @Column(name = "benefit_line", nullable = false, length = 30)
    private String benefitLine;

    @Column(name = "recommended_proposal_id")
    private Long recommendedProposalId;

    @Column(name = "lowest_premium", precision = 19, scale = 2)
    private BigDecimal lowestPremium;

    protected Line() {}

    Line(EbComparative comparative, String benefitLine, BigDecimal lowest, Long proposalId) {
      this.comparative = comparative;
      this.benefitLine = benefitLine;
      this.lowestPremium = lowest;
      this.recommendedProposalId = proposalId;
    }

    public String getBenefitLine() {
      return benefitLine;
    }

    public Long getRecommendedProposalId() {
      return recommendedProposalId;
    }

    public BigDecimal getLowestPremium() {
      return lowestPremium;
    }
  }
}
