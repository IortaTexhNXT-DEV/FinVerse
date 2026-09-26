package com.iortatechnxt.brokerverse.brokerclaims.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A BDOI claim case file (BRCLM.001-043; CLAIMS_BROKING_DESIGN 5.1): one claim reference per loss
 * incident whatever the number of locations and insurers (037 AC4, 043 AC3), numbered {@code
 * BCL-<yyyy>-nnnnnn} (CLQ13). It holds the identity (company, number, handling branch and unit,
 * handler, source) and three parts owned by the build waves: the {@link CoverSnapshot} and the
 * {@link LossDetails} (CL1-A) and the {@link ClaimProgress} (CL1-B). A business method on the claim
 * delegates to the part that owns the rule. Attachments, workflow cases and audit entries use the
 * entity type {@link ClaimCodes#ENTITY_TYPE}, which is also the JPA entity name (JPQL {@code from
 * BrokerClaim}), since the insurer-side {@code claims.domain.Claim} keeps the name {@code Claim}.
 */
@Entity(name = "BrokerClaim")
@Table(name = "bcl_claim")
public class Claim extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "claim_no", nullable = false, length = 30, updatable = false)
  private String claimNo;

  @Column(name = "branch_id")
  private Long branchId;

  @Column(name = "unit_code", length = 40)
  private String unitCode;

  @Column(nullable = false, length = 50)
  private String handler;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private ClaimSource source;

  @Column(name = "legacy_ref", length = 60, updatable = false)
  private String legacyRef;

  @Embedded private CoverSnapshot cover;

  @Embedded private LossDetails loss;

  @Embedded private ClaimProgress progress;

  protected Claim() {}

  /**
   * Records a claim in phase NEW.
   *
   * @param companyId company
   * @param claimNo claim number (immutable)
   * @param origin source, handler, unit, branch and legacy reference
   * @param cover cover snapshot and premium check
   * @param loss loss details and claimant
   */
  public Claim(
      Long companyId, String claimNo, Origin origin, CoverSnapshot cover, LossDetails loss) {
    this.companyId = companyId;
    this.claimNo = claimNo;
    this.source = origin.source();
    this.handler = origin.handler();
    this.unitCode = origin.unitCode();
    this.branchId = origin.branchId();
    this.legacyRef = origin.legacyRef();
    this.cover = cover;
    this.loss = loss;
    this.progress = ClaimProgress.newClaim();
  }

  /**
   * Hands the claim to another handler (reassignment through {@code WORK_ASSIGN}, NFR p.37).
   *
   * @param newHandler username of the new handler
   * @param newUnitCode unit of the new handler ({@code BCL_UNIT}), null when not registered
   */
  public void assignTo(String newHandler, String newUnitCode) {
    this.handler = newHandler;
    this.unitCode = newUnitCode;
  }

  /**
   * Whether the claim is permanently closed: it then accepts only diary entries, insurer updates
   * and reopen (design 5.1).
   *
   * @return true in phase CLOSED
   */
  public boolean isClosed() {
    return progress.getPhase() == ClaimPhase.CLOSED;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getClaimNo() {
    return claimNo;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getUnitCode() {
    return unitCode;
  }

  public String getHandler() {
    return handler;
  }

  public ClaimSource getSource() {
    return source;
  }

  public String getLegacyRef() {
    return legacyRef;
  }

  public CoverSnapshot getCover() {
    return cover;
  }

  public LossDetails getLoss() {
    return loss;
  }

  public ClaimProgress getProgress() {
    return progress;
  }

  /**
   * Where a claim comes from and who handles it.
   *
   * @param source how the claim reached BDOI
   * @param handler username of the claims handler
   * @param unitCode handling unit ({@code BCL_UNIT}), null when the handler has none
   * @param branchId handling branch, null for head office claims without a branch
   * @param legacyRef legacy EBIX / ISYS reference of a migrated claim (CLQ14)
   */
  public record Origin(
      ClaimSource source, String handler, String unitCode, Long branchId, String legacyRef) {}
}
