package com.iortatechnxt.finverse.claims.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.party.domain.Party;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * An insurance claim at 100 % under an approved policy in force at the date of loss.
 *
 * <p>The claim holds the policy facts at registration ({@link ClaimPolicy}), the loss details
 * ({@link LossDetails}), the involved parties and the running estimate / paid totals ({@link
 * ClaimTotals}). Financial changes are made only through approved documents (reserve changes,
 * settlements, recoveries) which call the {@code apply...} methods; status rules are enforced here.
 */
@Entity
@Table(name = "clm_claim")
public class Claim extends BaseEntity {

  private static final Set<ClaimStatus> SETTLEABLE =
      EnumSet.of(ClaimStatus.OPEN, ClaimStatus.PARTIALLY_SETTLED, ClaimStatus.REOPENED);
  private static final Set<ClaimStatus> DECLINABLE =
      EnumSet.of(ClaimStatus.REGISTERED, ClaimStatus.OPEN, ClaimStatus.REOPENED);

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "claim_no", nullable = false, length = 40)
  private String claimNo;

  @Embedded private ClaimPolicy policy;

  @Embedded private LossDetails loss;

  @Column(nullable = false, length = 3)
  private String currency;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "claimant_party_id")
  private Party claimant;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ClaimStatus status = ClaimStatus.REGISTERED;

  @Column(name = "status_reason", length = 200)
  private String statusReason;

  @Column(name = "closed_on")
  private LocalDate closedOn;

  @Embedded private final ClaimTotals totals = new ClaimTotals();

  @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private final List<ClaimParty> parties = new ArrayList<>();

  protected Claim() {}

  /**
   * Registers a claim (status REGISTERED). The caller also lists the claimant among the involved
   * parties ({@link #addParty} with role CLAIMANT).
   *
   * @param claimNo allocated claim number
   * @param branchId branch owning the claim
   * @param policy policy facts
   * @param loss loss details
   * @param currency claim currency (the policy currency)
   * @param claimant main claimant
   * @param companyId company
   */
  public Claim(
      String claimNo,
      Long branchId,
      ClaimPolicy policy,
      LossDetails loss,
      String currency,
      Party claimant,
      Long companyId) {
    this.claimNo = claimNo;
    this.branchId = branchId;
    this.policy = policy;
    this.loss = loss;
    this.currency = currency;
    this.claimant = claimant;
    this.companyId = companyId;
  }

  /**
   * Adds an involved party.
   *
   * @param party party
   * @param role role
   * @throws BusinessRuleException when the party type does not fit the role or it is already listed
   */
  public void addParty(Party party, ClaimPartyRole role) {
    if (involves(party, role)) {
      throw new BusinessRuleException(
          "CLAIM_PARTY_EXISTS", party.getCode() + " is already listed as " + role);
    }
    parties.add(ClaimParty.of(this, party, role));
  }

  /**
   * Whether a party is already involved in a role.
   *
   * @param party party
   * @param role role
   * @return true when listed
   */
  public boolean involves(Party party, ClaimPartyRole role) {
    return parties.stream().anyMatch(p -> p.matches(party, role));
  }

  /**
   * Fails unless the claim is still being handled.
   *
   * @param action attempted action, for the message
   */
  public void requireActive(String action) {
    if (!status.isActive()) {
      throw invalidStatus(action);
    }
  }

  /**
   * Fails unless settlements may be made (a reserve was approved and the claim is not finished).
   *
   * @param action attempted action, for the message
   */
  public void requireSettleable(String action) {
    if (!SETTLEABLE.contains(status)) {
      throw invalidStatus(action);
    }
  }

  /**
   * Fails unless recoveries may be received: active or closed claims (salvage often comes late).
   *
   * @param action attempted action, for the message
   */
  public void requireRecoverable(String action) {
    if (!status.isActive() && status != ClaimStatus.CLOSED) {
      throw invalidStatus(action);
    }
  }

  /**
   * Applies an approved estimate. The first approved payment estimate opens the claim.
   *
   * @param side side
   * @param cost cost type
   * @param newEstimate new estimate at 100 %
   */
  public void applyEstimate(EstimateSide side, CostType cost, BigDecimal newEstimate) {
    totals.setEstimate(side, cost, newEstimate);
    if (status == ClaimStatus.REGISTERED && side == EstimateSide.PAYMENT) {
      status = ClaimStatus.OPEN;
    }
  }

  /**
   * Applies an approved settlement (a final settlement is closed by the caller once the remaining
   * reserve has been released, see {@link #close}).
   *
   * @param cost cost type
   * @param amount net amount at 100 %
   */
  public void applySettlement(CostType cost, BigDecimal amount) {
    requireSettleable("settle");
    totals.addPaid(EstimateSide.PAYMENT, cost, amount);
    status = ClaimStatus.PARTIALLY_SETTLED;
  }

  /**
   * Applies an approved recovery.
   *
   * @param amount amount recovered at 100 %
   */
  public void applyRecovery(BigDecimal amount) {
    requireRecoverable("record a recovery on");
    totals.addPaid(EstimateSide.RECOVERY, CostType.LOSS, amount);
  }

  /**
   * Closes the claim once its payment outstanding has been released.
   *
   * @param date closing date
   * @param reason reason
   */
  public void close(LocalDate date, String reason) {
    requireSettleable("close");
    closeOn(date, reason);
  }

  /**
   * Reopens a closed claim.
   *
   * @param reason reason
   */
  public void reopen(String reason) {
    if (status != ClaimStatus.CLOSED) {
      throw invalidStatus("reopen");
    }
    status = ClaimStatus.REOPENED;
    statusReason = reason;
    closedOn = null;
  }

  /**
   * Repudiates (REJECTED) or withdraws (WITHDRAWN) a claim on which nothing was paid, once its
   * reserve has been released.
   *
   * @param target REJECTED or WITHDRAWN
   * @param date decision date
   * @param reason reason
   */
  public void decline(ClaimStatus target, LocalDate date, String reason) {
    if (target != ClaimStatus.REJECTED && target != ClaimStatus.WITHDRAWN) {
      throw new IllegalArgumentException("Not a declining status: " + target);
    }
    requireDeclinable();
    requireNoOutstanding();
    status = target;
    statusReason = reason;
    closedOn = date;
  }

  /** Fails unless the claim may be repudiated or withdrawn: not finished and nothing paid yet. */
  public void requireDeclinable() {
    if (!DECLINABLE.contains(status)) {
      throw invalidStatus("decline");
    }
    if (totals.totalPaid().signum() != 0) {
      throw new BusinessRuleException(
          "CLAIM_ALREADY_PAID", "Claim " + claimNo + " has settlements and cannot be declined");
    }
  }

  private void closeOn(LocalDate date, String reason) {
    requireNoOutstanding();
    status = ClaimStatus.CLOSED;
    statusReason = reason;
    closedOn = date;
  }

  private void requireNoOutstanding() {
    if (totals.paymentOutstanding().signum() != 0) {
      throw new BusinessRuleException(
          "CLAIM_HAS_OUTSTANDING",
          "Claim " + claimNo + " still has an outstanding reserve to release");
    }
  }

  /**
   * Company share of the claim's totals (each cost type rounded on its own total, exactly as the
   * movement lines are, so the figures equal the sums of the lines).
   *
   * @return company-share figures
   */
  public OurShare ourShare() {
    return totals.ourShare(policy);
  }

  private BusinessRuleException invalidStatus(String action) {
    return new BusinessRuleException(
        "INVALID_CLAIM_STATUS", "Cannot " + action + " claim " + claimNo + " in status " + status);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getClaimNo() {
    return claimNo;
  }

  public ClaimPolicy getPolicy() {
    return policy;
  }

  public LossDetails getLoss() {
    return loss;
  }

  public String getCurrency() {
    return currency;
  }

  public Party getClaimant() {
    return claimant;
  }

  public ClaimStatus getStatus() {
    return status;
  }

  public String getStatusReason() {
    return statusReason;
  }

  public LocalDate getClosedOn() {
    return closedOn;
  }

  public ClaimTotals getTotals() {
    return totals;
  }

  public List<ClaimParty> getParties() {
    return List.copyOf(parties);
  }
}
