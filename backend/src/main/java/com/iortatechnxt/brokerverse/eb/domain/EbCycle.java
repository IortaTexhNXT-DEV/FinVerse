package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
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
import java.util.Objects;

/**
 * One policy year of a programme (design 4.2, 7.1): numbered {@code EBC-<yyyy>-nnnnnn}, with its
 * business type NEW_BUSINESS or RENEWAL (BRID-022.01; carried to the accounts created at placement,
 * shared work item BT0), the stage mirrored from its {@code EB_CYCLE} work case, the outcome and
 * the ARNs created for it. At most one open cycle per programme and policy year.
 */
@Entity
@Table(name = "eb_cycle")
public class EbCycle extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "cycle_no", nullable = false, length = 30, updatable = false)
  private String cycleNo;

  @Column(name = "programme_id", nullable = false, updatable = false)
  private Long programmeId;

  @Enumerated(EnumType.STRING)
  @Column(name = "business_type", nullable = false, length = 20, updatable = false)
  private BusinessType businessType;

  @Column(name = "policy_year", nullable = false, updatable = false)
  private int policyYear;

  @Column(name = "target_inception")
  private LocalDate targetInception;

  @Column(nullable = false)
  private boolean remarketing;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EbCycleStage stage = EbCycleStage.OPEN;

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  private EbCycleOutcome outcome;

  @Column(name = "outcome_reason", length = 40)
  private String outcomeReason;

  @Column(name = "outcome_remarks", length = 500)
  private String outcomeRemarks;

  @Column(name = "closed_at")
  private Instant closedAt;

  @ElementCollection
  @CollectionTable(name = "eb_cycle_account", joinColumns = @JoinColumn(name = "cycle_id"))
  @OrderColumn(name = "account_index")
  @Column(name = "arn", nullable = false, length = 30)
  private final List<String> accountArns = new ArrayList<>();

  protected EbCycle() {}

  /**
   * Opens a cycle in stage OPEN.
   *
   * @param companyId company
   * @param cycleNo cycle number (immutable)
   * @param programmeId programme
   * @param businessType NEW_BUSINESS or RENEWAL, required (FR-EB-021 R2)
   * @param policyYear policy year
   * @param targetInception target inception date
   */
  public EbCycle(
      Long companyId,
      String cycleNo,
      Long programmeId,
      BusinessType businessType,
      int policyYear,
      LocalDate targetInception) {
    if (businessType == null) {
      throw new BusinessRuleException(
          "EB_BUSINESS_TYPE_REQUIRED", "Select the business type of the cycle");
    }
    this.companyId = companyId;
    this.cycleNo = cycleNo;
    this.programmeId = programmeId;
    this.businessType = businessType;
    this.policyYear = policyYear;
    this.targetInception = targetInception;
  }

  /**
   * Mirrors the stage of the work case; a terminal stage closes the cycle.
   *
   * @param newStage stage
   * @param when time of the transition
   */
  public void mirror(EbCycleStage newStage, Instant when) {
    this.stage = newStage;
    if (newStage.terminal() && closedAt == null) {
      this.closedAt = when;
    }
  }

  /**
   * Records how the cycle ended.
   *
   * @param result outcome
   * @param reason reason code (list EB_LOST_REASON) for LOST / NOT_RENEWED, else null
   * @param remarks remarks
   */
  public void recordOutcome(EbCycleOutcome result, String reason, String remarks) {
    this.outcome = Objects.requireNonNull(result, "outcome");
    this.outcomeReason = reason;
    this.outcomeRemarks = remarks;
  }

  /** The client asked to remarket (franchise and proposals from other insurers). */
  public void markRemarketing() {
    this.remarketing = true;
  }

  /**
   * Records an account created for the cycle at placement (BRID-017).
   *
   * @param arn Account Reference Number
   */
  public void recordAccount(String arn) {
    if (!accountArns.contains(arn)) {
      accountArns.add(arn);
    }
  }

  /**
   * Whether the cycle is still open.
   *
   * @return true until a terminal stage
   */
  public boolean isOpen() {
    return closedAt == null;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getCycleNo() {
    return cycleNo;
  }

  public Long getProgrammeId() {
    return programmeId;
  }

  public BusinessType getBusinessType() {
    return businessType;
  }

  public int getPolicyYear() {
    return policyYear;
  }

  public LocalDate getTargetInception() {
    return targetInception;
  }

  public boolean isRemarketing() {
    return remarketing;
  }

  public EbCycleStage getStage() {
    return stage;
  }

  public EbCycleOutcome getOutcome() {
    return outcome;
  }

  public String getOutcomeReason() {
    return outcomeReason;
  }

  public String getOutcomeRemarks() {
    return outcomeRemarks;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public List<String> getAccountArns() {
    return List.copyOf(accountArns);
  }
}
