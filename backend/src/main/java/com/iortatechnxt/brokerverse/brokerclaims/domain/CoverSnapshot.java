package com.iortatechnxt.brokerverse.brokerclaims.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The cover a claim is made under, as copied from the account when the claim is recorded, and the
 * premium check of that cover with the claims authorization code (BRCLM.001/003/007/009/016/039;
 * CLAIMS_BROKING_DESIGN 5.1). The snapshot lets the reports run without a join to the account
 * tables; policy data is never re-keyed and never edited by Claims (BRCLM.039 AC3): it only changes
 * by a refresh from the account ("Refresh Cover Data", BRCLM.016) or by switching to the latest
 * cover version (BRCLM.039).
 *
 * <p>Owned by wave CL1-A. The authorization code is issued only on a PAID premium check, or on a
 * DIRECT_PAYMENT check under parameter {@code BCL_AUTH_DP_POLICY} (the rule is applied by the
 * service; the snapshot keeps the result, who and when). Its meaning waits for CLQ01.
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

  /**
   * Snapshot of a cover at recording (BRCLM.003/007/009/016).
   *
   * @param policy account, policy year and policy facts
   * @param version cover version at the loss date
   * @param sales Marketing unit, account officer and invoicing branch
   * @return snapshot, not yet premium-checked
   */
  public static CoverSnapshot of(Policy policy, Version version, Sales sales) {
    CoverSnapshot cover = new CoverSnapshot();
    cover.arn = policy.arn();
    cover.accountId = policy.accountId();
    cover.policyYear = policy.policyYear();
    cover.applyPolicy(policy);
    cover.applyVersion(version);
    cover.applySales(sales);
    return cover;
  }

  /**
   * Reloads the policy number, sum insured and sales data from the account (BRCLM.016 "Refresh
   * Cover Data"; FR-CL-013 policy number pending until issued). ARN, policy year and currency never
   * change.
   *
   * @param policy current policy facts of the same cover
   * @param sales current sales stamp and invoicing branch
   * @return the changed fields as "field: old -&gt; new", empty when nothing changed
   */
  public List<String> refresh(Policy policy, Sales sales) {
    if (!arn.equals(policy.arn()) || policyYear != policy.policyYear()) {
      throw new BusinessRuleException(
          "BCL_COVER_MISMATCH", "The cover data of another account cannot be applied");
    }
    List<String> changes = new ArrayList<>();
    diff(changes, "Policy no.", policyNo, policy.policyNo());
    diff(changes, "Sum insured", sumInsured, policy.sumInsured());
    diff(changes, "Marketing team", salesTeam, sales.team());
    diff(changes, "Account officer", accountOfficer, sales.accountOfficer());
    diff(changes, "Department", salesDepartment, sales.department());
    diff(changes, "Region", salesRegion, sales.region());
    diff(changes, "Cost center", costCenter, sales.costCenter());
    diff(changes, "Invoicing branch", invoicingBranchId, sales.invoicingBranchId());
    applyPolicy(policy);
    applySales(sales);
    return changes;
  }

  /**
   * Switches the claim to another cover version, e.g. the latest one (BRCLM.039 "Use Latest
   * Version").
   *
   * @param version version
   * @return "v&lt;old&gt; -&gt; v&lt;new&gt;"
   */
  public String useVersion(Version version) {
    String change = label(coverVersionNo, coverVersionRef) + " -> " + label(version.no(), version.ref());
    applyVersion(version);
    return change;
  }

  /**
   * Records the result of a premium check (BRCLM.001).
   *
   * @param status result
   * @param at time of the check
   */
  public void premiumChecked(ClaimPremiumStatus status, Instant at) {
    this.premiumStatus = status;
    this.premiumCheckedAt = at;
  }

  /**
   * Records the claims authorization code (BRCLM.001). The premium rule is checked by the service
   * before; the code is issued once.
   *
   * @param code code {@code CAC-<yyyy>-nnnnnn}
   * @param by user
   * @param at time
   * @param evidenceAttachmentId insurer payment evidence of a direct-payment cover, may be null
   */
  public void authorize(String code, String by, Instant at, Long evidenceAttachmentId) {
    if (isAuthorized()) {
      throw new BusinessRuleException(
          "BCL_ALREADY_AUTHORIZED", "The claim already has authorization code " + authorizationCode);
    }
    this.authorizationCode = code;
    this.authorizedBy = by;
    this.authorizedAt = at;
    this.dpEvidenceAttachmentId = evidenceAttachmentId;
  }

  /**
   * Whether the claims authorization code was issued.
   *
   * @return true once authorized
   */
  public boolean isAuthorized() {
    return authorizationCode != null;
  }

  /**
   * Whether a date falls within the cover period of the policy year (CLQ02: outside is a warning).
   *
   * @param date date
   * @return true when unknown period or inside it
   */
  public boolean covers(LocalDate date) {
    boolean afterStart = periodFrom == null || !date.isBefore(periodFrom);
    boolean beforeEnd = periodTo == null || !date.isAfter(periodTo);
    return afterStart && beforeEnd;
  }

  /**
   * Cover label "Cover v&lt;n&gt; (&lt;endorsement no.&gt;)" (BRCLM.039).
   *
   * @return label
   */
  public String versionLabel() {
    return label(coverVersionNo, coverVersionRef);
  }

  private static String label(Integer no, String ref) {
    String base = "Cover v" + (no == null ? 0 : no);
    return ref == null ? base : base + " (" + ref + ")";
  }

  private static void diff(List<String> changes, String field, Object before, Object after) {
    boolean same =
        before instanceof BigDecimal b && after instanceof BigDecimal a
            ? b.compareTo(a) == 0
            : Objects.equals(before, after);
    if (!same) {
      changes.add(field + ": " + before + " -> " + after);
    }
  }

  private void applyPolicy(Policy policy) {
    this.policyNo = policy.policyNo();
    this.productCode = policy.productCode();
    this.lineCode = policy.lineCode();
    this.clientCode = policy.clientCode();
    this.assuredName = policy.assuredName();
    this.leadInsurerCode = policy.leadInsurerCode();
    this.periodFrom = policy.periodFrom();
    this.periodTo = policy.periodTo();
    this.sumInsured = policy.sumInsured();
    if (currency == null) {
      this.currency = policy.currency();
    }
  }

  private void applyVersion(Version version) {
    this.coverVersionNo = version.no();
    this.coverVersionRef = version.ref();
    this.coverVersionAt = version.at();
  }

  private void applySales(Sales sales) {
    this.salesRegion = sales.region();
    this.salesDepartment = sales.department();
    this.salesTeam = sales.team();
    this.accountOfficer = sales.accountOfficer();
    this.costCenter = sales.costCenter();
    this.invoicingBranchId = sales.invoicingBranchId();
  }

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

  /**
   * Account, policy year and policy facts of a cover (BRCLM.003/007/009).
   *
   * @param arn account reference number
   * @param accountId account id
   * @param policyYear policy year of the term (1 = first year)
   * @param policyNo policy number of the year, null while not issued
   * @param productCode product
   * @param lineCode product line
   * @param clientCode client
   * @param assuredName assured
   * @param leadInsurerCode lead insurer
   * @param periodFrom start of the policy year
   * @param periodTo end of the policy year
   * @param sumInsured total sum insured
   * @param currency claim currency (cover currency, else the default)
   */
  public record Policy(
      String arn,
      Long accountId,
      int policyYear,
      String policyNo,
      String productCode,
      String lineCode,
      String clientCode,
      String assuredName,
      String leadInsurerCode,
      LocalDate periodFrom,
      LocalDate periodTo,
      BigDecimal sumInsured,
      String currency) {}

  /**
   * Cover version (BRCLM.039): number of booked endorsements of the policy year effective on or
   * before the loss date, with the last one's number and effective date.
   *
   * @param no version number (0 = the original cover)
   * @param ref last endorsement number, null for the original cover
   * @param at effective date of that endorsement, null for the original cover
   */
  public record Version(int no, String ref, LocalDate at) {}

  /**
   * Sales stamp and invoicing branch of the cover (BRCLM.016).
   *
   * @param region region
   * @param department department
   * @param team Marketing team
   * @param accountOfficer account officer
   * @param costCenter cost center
   * @param invoicingBranchId invoicing branch
   */
  public record Sales(
      String region,
      String department,
      String team,
      String accountOfficer,
      String costCenter,
      Long invoicingBranchId) {}
}
