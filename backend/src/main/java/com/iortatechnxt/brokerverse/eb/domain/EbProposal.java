package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An insurer proposal on a cycle (BRID-005.02, 005.03, 010, 015; FR-EB-040, 045), numbered {@code
 * EBPR-<yyyy>-nnnnnn}: the incumbent's indicative terms, a proposal answering a request, or a
 * revised proposal answering a revision request. Without the portal the AO enters it with the
 * insurer's document attached; only a VALIDATED proposal counts in the comparative. A new version
 * of the same insurer supersedes the earlier one.
 */
@Entity
@Table(name = "eb_proposal")
public class EbProposal extends EbCycleRecord {

  /** Kind of proposal. */
  public enum Kind {
    /** Indicative renewal terms of the incumbent. */
    INCUMBENT_INDICATIVE,
    /** Answer to a request for proposal. */
    PROPOSAL,
    /** Answer to a revision request. */
    REVISED
  }

  /** Status of a proposal. */
  public enum Status {
    /** Entered, to be checked against the TOR. */
    SUBMITTED,
    /** Checked: counts in the comparative. */
    VALIDATED,
    /** Rejected with a reason. */
    REJECTED,
    /** Replaced by a later version of the insurer. */
    SUPERSEDED
  }

  @Column(name = "proposal_no", nullable = false, length = 30, updatable = false)
  private String proposalNo;

  @Column(name = "request_id", updatable = false)
  private Long requestId;

  @Column(name = "revision_id", updatable = false)
  private Long revisionId;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30, updatable = false)
  private Kind kind;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.SUBMITTED;

  @Column(nullable = false, length = 20, updatable = false)
  private String source;

  @Column(name = "received_on", nullable = false)
  private LocalDate receivedOn;

  @Column(name = "valid_until")
  private LocalDate validUntil;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(length = 4000)
  private String terms;

  @Column(length = 4000)
  private String exclusions;

  @Column(name = "attachment_id", nullable = false)
  private Long attachmentId;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "reject_reason", length = 500)
  private String rejectReason;

  @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder")
  private final List<EbProposalLine> lines = new ArrayList<>();

  @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private final List<EbProposalItem> items = new ArrayList<>();

  @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private final List<EbProposalFactor> factors = new ArrayList<>();

  protected EbProposal() {}

  /**
   * Records a proposal.
   *
   * @param cycle cycle
   * @param proposalNo number
   * @param origin insurer, kind, version, request and revision
   * @param content dates, currency, terms, exclusions and the insurer's document
   */
  public EbProposal(EbCycle cycle, String proposalNo, Origin origin, Content content) {
    super(cycle);
    this.proposalNo = proposalNo;
    this.insurerCode = origin.insurerCode();
    this.kind = origin.kind();
    this.versionNo = origin.versionNo();
    this.requestId = origin.requestId();
    this.revisionId = origin.revisionId();
    this.source = "AO";
    this.receivedOn = content.receivedOn();
    this.validUntil = content.validUntil();
    this.currency = content.currency();
    this.terms = content.terms();
    this.exclusions = content.exclusions();
    this.attachmentId = content.attachmentId();
  }

  /**
   * Adds the premium of a plan of a benefit line.
   *
   * @param data line data
   */
  public void addLine(EbProposalLine.Data data) {
    lines.add(new EbProposalLine(this, lines.size() + 1, data));
  }

  /**
   * Adds the answer to a TOR item.
   *
   * @param data item data
   */
  public void addItem(EbProposalItem.Data data) {
    items.add(new EbProposalItem(this, data));
  }

  /**
   * Adds a capability factor.
   *
   * @param data factor data
   */
  public void addFactor(EbProposalFactor.Data data) {
    factors.add(new EbProposalFactor(this, data));
  }

  /**
   * Validates the proposal.
   *
   * @param by user
   * @param at time
   */
  public void validate(String by, Instant at) {
    requireSubmitted();
    decide(Status.VALIDATED, by, at);
  }

  /**
   * Rejects the proposal with a reason.
   *
   * @param reason why
   * @param by user
   * @param at time
   */
  public void reject(String reason, String by, Instant at) {
    requireSubmitted();
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException(
          "EB_PROPOSAL_REASON_REQUIRED", "Enter the reason for rejecting the proposal");
    }
    this.rejectReason = reason.strip();
    decide(Status.REJECTED, by, at);
  }

  /** A later version of the insurer replaces this one. */
  public void supersede() {
    if (status == Status.SUBMITTED || status == Status.VALIDATED) {
      this.status = Status.SUPERSEDED;
    }
  }

  private void decide(Status result, String by, Instant at) {
    this.status = result;
    this.decidedBy = by;
    this.decidedAt = at;
  }

  private void requireSubmitted() {
    if (status != Status.SUBMITTED) {
      throw new BusinessRuleException(
          "EB_PROPOSAL_DECIDED", "Proposal " + proposalNo + " is already decided");
    }
  }

  /**
   * Annual premium of a benefit line (sum of its plans).
   *
   * @param benefitLine benefit line
   * @return premium, zero when the line is not offered
   */
  public BigDecimal premiumOf(String benefitLine) {
    return lines.stream()
        .filter(l -> l.getBenefitLine().equals(benefitLine))
        .map(EbProposalLine::getAnnualPremium)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Total sum insured of a benefit line (sum of its plans).
   *
   * @param benefitLine benefit line
   * @return TSI, zero when none is given
   */
  public BigDecimal sumInsuredOf(String benefitLine) {
    return lines.stream()
        .filter(l -> l.getBenefitLine().equals(benefitLine) && l.getSumInsured() != null)
        .map(EbProposalLine::getSumInsured)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Whether the proposal offers a benefit line.
   *
   * @param benefitLine benefit line
   * @return true when a plan of the line is priced
   */
  public boolean offers(String benefitLine) {
    return lines.stream().anyMatch(l -> l.getBenefitLine().equals(benefitLine));
  }

  public String getProposalNo() {
    return proposalNo;
  }

  public Long getRequestId() {
    return requestId;
  }

  public Long getRevisionId() {
    return revisionId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public Kind getKind() {
    return kind;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public Status getStatus() {
    return status;
  }

  public String getSource() {
    return source;
  }

  public LocalDate getReceivedOn() {
    return receivedOn;
  }

  public LocalDate getValidUntil() {
    return validUntil;
  }

  public String getCurrency() {
    return currency;
  }

  public String getTerms() {
    return terms;
  }

  public String getExclusions() {
    return exclusions;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getRejectReason() {
    return rejectReason;
  }

  public List<EbProposalLine> getLines() {
    return Collections.unmodifiableList(lines);
  }

  public List<EbProposalItem> getItems() {
    return Collections.unmodifiableList(items);
  }

  public List<EbProposalFactor> getFactors() {
    return Collections.unmodifiableList(factors);
  }

  /**
   * Where a proposal comes from.
   *
   * @param insurerCode insurer
   * @param kind kind
   * @param versionNo version of the insurer's proposals on the cycle
   * @param requestId the request answered, may be null
   * @param revisionId the revision answered, may be null
   */
  public record Origin(
      String insurerCode, Kind kind, int versionNo, Long requestId, Long revisionId) {}

  /**
   * What the insurer offers besides the premium lines.
   *
   * @param receivedOn date received
   * @param validUntil validity, may be null
   * @param currency currency
   * @param terms terms and additional benefits, may be null
   * @param exclusions exclusions, may be null
   * @param attachmentId the insurer's document
   */
  public record Content(
      LocalDate receivedOn,
      LocalDate validUntil,
      String currency,
      String terms,
      String exclusions,
      Long attachmentId) {}
}
