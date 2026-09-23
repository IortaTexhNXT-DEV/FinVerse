package com.iortatechnxt.finverse.reinsurance.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * Facultative placement of the part of a risk that exceeds the treaty capacity. Created PROVISIONAL
 * by the allocation; the reinsurance officer records the participants and submits it; a checker
 * approves it (PLACED: premium ceded to the participants); it is finally CLOSED. Endorsements ceded
 * before placement increase the provisional requirement.
 */
@Entity
@Table(name = "ri_fac_placement")
public class FacPlacement extends BaseEntity {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(name = "placement_no", nullable = false, updatable = false, length = 40)
  private String placementNo;

  @ManyToOne(optional = false)
  @JoinColumn(name = "cession_id", updatable = false)
  private Cession cession;

  @Column(name = "risk_id", nullable = false, updatable = false)
  private Long riskId;

  @Column(name = "risk_line_no", nullable = false, updatable = false)
  private int riskLineNo;

  @Column(name = "risk_description", nullable = false, updatable = false, length = 300)
  private String riskDescription;

  @Column(name = "risk_si", nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal riskSi;

  @Column(name = "risk_premium", nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal riskPremium;

  @Column(name = "fac_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal facPct;

  @Column(name = "fac_si", nullable = false, precision = 19, scale = 2)
  private BigDecimal facSi;

  @Column(name = "fac_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal facPremium;

  @Column(name = "placed_si", nullable = false, precision = 19, scale = 2)
  private BigDecimal placedSi = Money.zero();

  @Column(name = "placed_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal placedPremium = Money.zero();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FacStatus status = FacStatus.PROVISIONAL;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "placed_by", length = 50)
  private String placedBy;

  @Column(name = "placed_on")
  private LocalDate placedOn;

  @Column(name = "closed_on")
  private LocalDate closedOn;

  @Column(length = 300)
  private String remarks;

  @ElementCollection(fetch = FetchType.EAGER)
  @Fetch(FetchMode.SUBSELECT)
  @CollectionTable(name = "ri_fac_participant", joinColumns = @JoinColumn(name = "placement_id"))
  @OrderBy("lineNo")
  private final List<FacParticipant> participants = new ArrayList<>();

  /** For JPA. */
  protected FacPlacement() {}

  /**
   * Creates a provisional placement for the facultative part of a risk.
   *
   * @param placementNo placement number
   * @param cession cession that identified the requirement
   * @param risk risk
   * @param facSi facultative sum insured required
   * @param facPremium facultative premium
   */
  public FacPlacement(
      String placementNo, Cession cession, RiskRef risk, BigDecimal facSi, BigDecimal facPremium) {
    this.placementNo = placementNo;
    this.cession = cession;
    this.companyId = cession.getCompanyId();
    this.branchId = cession.getBranchId();
    this.riskId = risk.riskId();
    this.riskLineNo = risk.lineNo();
    this.riskDescription = risk.description();
    this.riskSi = Money.round(risk.ourSi());
    this.riskPremium = Money.round(risk.ourPremium());
    this.facSi = Money.round(facSi);
    this.facPremium = Money.round(facPremium);
    this.facPct =
        riskSi.signum() == 0
            ? BigDecimal.ZERO
            : facSi.multiply(HUNDRED).divide(riskSi, Money.RATE_SCALE, RoundingMode.HALF_EVEN);
  }

  /**
   * Adds the facultative share of an endorsement ceded before the placement was approved.
   *
   * @param si sum insured change
   * @param premium premium change
   */
  public void addProvisional(BigDecimal si, BigDecimal premium) {
    requireStatus(FacStatus.PROVISIONAL, FacStatus.PENDING_APPROVAL);
    this.facSi = facSi.add(Money.round(si));
    this.facPremium = facPremium.add(Money.round(premium));
  }

  /**
   * Replaces the participants (while provisional). Shares may total less than 100 %: the part not
   * placed stays with the company.
   *
   * @param lines participants
   * @param newRemarks slip remarks
   */
  public void assign(List<FacParticipant> lines, String newRemarks) {
    requireStatus(FacStatus.PROVISIONAL);
    BigDecimal total =
        lines.stream().map(FacParticipant::getSharePct).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(HUNDRED) > 0) {
      throw new BusinessRuleException("FAC_SHARES", "Facultative shares exceed 100 %");
    }
    participants.clear();
    participants.addAll(lines);
    this.remarks = newRemarks;
  }

  /**
   * Submits the slip for approval.
   *
   * @param maker submitting user
   * @param when submission time
   */
  public void submit(String maker, Instant when) {
    requireStatus(FacStatus.PROVISIONAL);
    if (participants.isEmpty()) {
      throw new BusinessRuleException("FAC_NO_PARTICIPANTS", "Record the reinsurers first");
    }
    this.status = FacStatus.PENDING_APPROVAL;
    this.submittedBy = maker;
    this.submittedAt = when;
  }

  /**
   * Returns a submitted slip to the maker.
   *
   * @param checker rejecting user
   */
  public void reject(String checker) {
    requireStatus(FacStatus.PENDING_APPROVAL);
    requireOtherUser(checker);
    this.status = FacStatus.PROVISIONAL;
  }

  /**
   * Approves the slip: fixes each participant's accepted sum insured, premium and commission.
   *
   * @param checker approving user (not the maker)
   * @param date placement (accounting) date
   * @param refOf accounting source reference of each participant
   */
  public void place(String checker, LocalDate date, Function<FacParticipant, String> refOf) {
    requireStatus(FacStatus.PENDING_APPROVAL);
    requireOtherUser(checker);
    BigDecimal si = Money.zero();
    BigDecimal premium = Money.zero();
    for (FacParticipant p : participants) {
      BigDecimal fraction = p.getSharePct().divide(HUNDRED, Money.RATE_SCALE, Money.ROUNDING);
      p.accept(facSi.multiply(fraction), facPremium.multiply(fraction), refOf.apply(p));
      si = si.add(p.getSumInsured());
      premium = premium.add(p.getPremium());
    }
    this.placedSi = si;
    this.placedPremium = premium;
    this.status = FacStatus.PLACED;
    this.placedBy = checker;
    this.placedOn = date;
  }

  /**
   * Closes the placement.
   *
   * @param date closing date
   */
  public void close(LocalDate date) {
    requireStatus(FacStatus.PLACED);
    this.status = FacStatus.CLOSED;
    this.closedOn = date;
  }

  /**
   * Placed share of the facultative requirement.
   *
   * @return placed SI / FAC SI x 100
   */
  public BigDecimal placementPct() {
    return facSi.signum() == 0
        ? BigDecimal.ZERO
        : placedSi.multiply(HUNDRED).divide(facSi, Money.SCALE, RoundingMode.HALF_EVEN);
  }

  /**
   * Total commission of the participants.
   *
   * @return commission
   */
  public BigDecimal commission() {
    return participants.stream()
        .map(FacParticipant::getCommission)
        .reduce(Money.zero(), BigDecimal::add);
  }

  private void requireOtherUser(String checker) {
    if (Objects.equals(submittedBy, checker)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A placement cannot be approved by the user who submitted it");
    }
  }

  private void requireStatus(FacStatus... allowed) {
    for (FacStatus s : allowed) {
      if (status == s) {
        return;
      }
    }
    throw new BusinessRuleException(
        "FAC_STATUS", "Placement " + placementNo + " is " + status + "; action not allowed");
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getPlacementNo() {
    return placementNo;
  }

  public Cession getCession() {
    return cession;
  }

  public Long getRiskId() {
    return riskId;
  }

  public int getRiskLineNo() {
    return riskLineNo;
  }

  public String getRiskDescription() {
    return riskDescription;
  }

  public BigDecimal getRiskSi() {
    return riskSi;
  }

  public BigDecimal getRiskPremium() {
    return riskPremium;
  }

  public BigDecimal getFacPct() {
    return facPct;
  }

  public BigDecimal getFacSi() {
    return facSi;
  }

  public BigDecimal getFacPremium() {
    return facPremium;
  }

  public BigDecimal getPlacedSi() {
    return placedSi;
  }

  public BigDecimal getPlacedPremium() {
    return placedPremium;
  }

  public FacStatus getStatus() {
    return status;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getPlacedBy() {
    return placedBy;
  }

  public LocalDate getPlacedOn() {
    return placedOn;
  }

  public LocalDate getClosedOn() {
    return closedOn;
  }

  public String getRemarks() {
    return remarks;
  }

  public List<FacParticipant> getParticipants() {
    return List.copyOf(participants);
  }
}
