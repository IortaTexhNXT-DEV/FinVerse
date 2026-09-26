package com.iortatechnxt.brokerverse.brokerclaims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The cover a claim is made under, as copied from the account when the claim is recorded, and the
 * premium check of that cover with the claims authorization code (BRCLM.001/003/007/009/016/039;
 * CLAIMS_BROKING_DESIGN 5.1). The snapshot lets the reports run without a join to the account
 * tables; policy data is never re-keyed and never edited by Claims (BRCLM.039 AC3).
 *
 * <p>Owned by wave CL1-A, which adds the factory from {@code AccountQueryService} / {@code
 * BookingQueryService} and the premium check / authorization behaviour. Mapped by CL0 to the V1021
 * columns of {@code bcl_claim}.
 */
@Embeddable
public class CoverSnapshot {

  @Column(nullable = false, length = 30)
  private String arn;

  @Column(name = "account_id")
  private Long accountId;

  @Column(name = "policy_year", nullable = false)
  private int policyYear;

  @Column(name = "policy_no", length = 60)
  private String policyNo;

  @Column(name = "cover_version_no")
  private Integer coverVersionNo;

  @Column(name = "cover_version_ref", length = 40)
  private String coverVersionRef;

  @Column(name = "cover_version_at")
  private LocalDate coverVersionAt;

  @Column(name = "product_code", length = 30)
  private String productCode;

  @Column(name = "line_code", length = 30)
  private String lineCode;

  @Column(name = "client_code", length = 30)
  private String clientCode;

  @Column(name = "assured_name", length = 250)
  private String assuredName;

  @Column(name = "lead_insurer_code", length = 30)
  private String leadInsurerCode;

  @Column(name = "period_from")
  private LocalDate periodFrom;

  @Column(name = "period_to")
  private LocalDate periodTo;

  @Column(name = "sum_insured", precision = 19, scale = 2)
  private BigDecimal sumInsured;

  @Column(name = "sales_region", length = 40)
  private String salesRegion;

  @Column(name = "sales_department", length = 40)
  private String salesDepartment;

  @Column(name = "sales_team", length = 40)
  private String salesTeam;

  @Column(name = "account_officer", length = 50)
  private String accountOfficer;

  @Column(name = "cost_center", length = 40)
  private String costCenter;

  @Column(name = "invoicing_branch_id")
  private Long invoicingBranchId;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(name = "premium_status", length = 20)
  private ClaimPremiumStatus premiumStatus;

  @Column(name = "premium_checked_at")
  private Instant premiumCheckedAt;

  @Column(name = "authorization_code", length = 30)
  private String authorizationCode;

  @Column(name = "authorized_by", length = 50)
  private String authorizedBy;

  @Column(name = "authorized_at")
  private Instant authorizedAt;

  @Column(name = "dp_evidence_attachment_id")
  private Long dpEvidenceAttachmentId;

  protected CoverSnapshot() {}

  public String getArn() {
    return arn;
  }

  public Long getAccountId() {
    return accountId;
  }

  public int getPolicyYear() {
    return policyYear;
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public Integer getCoverVersionNo() {
    return coverVersionNo;
  }

  public String getCoverVersionRef() {
    return coverVersionRef;
  }

  public LocalDate getCoverVersionAt() {
    return coverVersionAt;
  }

  public String getProductCode() {
    return productCode;
  }

  public String getLineCode() {
    return lineCode;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public String getLeadInsurerCode() {
    return leadInsurerCode;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public BigDecimal getSumInsured() {
    return sumInsured;
  }

  public String getSalesRegion() {
    return salesRegion;
  }

  public String getSalesDepartment() {
    return salesDepartment;
  }

  public String getSalesTeam() {
    return salesTeam;
  }

  public String getAccountOfficer() {
    return accountOfficer;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public Long getInvoicingBranchId() {
    return invoicingBranchId;
  }

  public String getCurrency() {
    return currency;
  }

  public ClaimPremiumStatus getPremiumStatus() {
    return premiumStatus;
  }

  public Instant getPremiumCheckedAt() {
    return premiumCheckedAt;
  }

  public String getAuthorizationCode() {
    return authorizationCode;
  }

  public String getAuthorizedBy() {
    return authorizedBy;
  }

  public Instant getAuthorizedAt() {
    return authorizedAt;
  }

  public Long getDpEvidenceAttachmentId() {
    return dpEvidenceAttachmentId;
  }
}
