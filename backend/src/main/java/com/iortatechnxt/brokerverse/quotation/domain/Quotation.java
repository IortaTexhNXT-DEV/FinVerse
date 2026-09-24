package com.iortatechnxt.brokerverse.quotation.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A package quotation (BRNB.043): one client (a prospect is allowed, BRNB.063), one product, its
 * Account Reference Number from creation (BRNB.102) and numbered versions of its content
 * (BRNB.020). The header carries the figures of the current version for lists and search; the
 * status mirrors the NB_QUOTATION work case.
 */
@Entity(name = "BrokingQuotation")
@Table(name = "quo_quotation")
public class Quotation extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "quotation_no", nullable = false, length = 30, updatable = false)
  private String quotationNo;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "request_id", updatable = false)
  private Long requestId;

  @Column(name = "client_id", nullable = false)
  private Long clientId;

  @Column(name = "client_code", nullable = false, length = 30)
  private String clientCode;

  @Column(name = "client_name", nullable = false, length = 250)
  private String clientName;

  @Column(name = "client_email", length = 120)
  private String clientEmail;

  @Column(name = "product_code", nullable = false, length = 20, updatable = false)
  private String productCode;

  @Column(name = "line_code", nullable = false, length = 30, updatable = false)
  private String lineCode;

  @Column(name = "market_segment", length = 40)
  private String marketSegment;

  @Column(name = "source_channel", length = 40)
  private String sourceChannel;

  @Column(name = "insurer_code", length = 30)
  private String insurerCode;

  @Column(name = "insurer_branch", length = 20)
  private String insurerBranch;

  @Column(name = "period_from")
  private LocalDate periodFrom;

  @Column(name = "period_to")
  private LocalDate periodTo;

  @Column(name = "valid_until", nullable = false)
  private LocalDate validUntil;

  @Column(nullable = false, length = 3)
  private String currency = "PHP";

  @Column(name = "direct_payment", nullable = false)
  private boolean directPayment;

  @Column(name = "template_version", nullable = false, length = 60, updatable = false)
  private String templateVersion;

  @Column(name = "current_version", nullable = false)
  private int currentVersion = 1;

  @Column(name = "version_open", nullable = false)
  private boolean versionOpen = true;

  @Column(name = "risk_groups", nullable = false)
  private int riskGroups = 1;

  @Column(name = "total_sum_insured", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalSumInsured = BigDecimal.ZERO;

  @Column(name = "net_premium", precision = 19, scale = 2)
  private BigDecimal netPremium;

  @Column(name = "gross_premium", precision = 19, scale = 2)
  private BigDecimal grossPremium;

  @Column(name = "commission", precision = 19, scale = 2)
  private BigDecimal commission;

  @Column(name = "tsu_required", nullable = false)
  private boolean tsuRequired;

  @Column(name = "tsu_reason", length = 300)
  private String tsuReason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private QuotationStatus status = QuotationStatus.DRAFT;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "accepted_at")
  private Instant acceptedAt;

  @Column(name = "accepted_groups", length = 200)
  private String acceptedGroups;

  @ElementCollection
  @CollectionTable(name = "quo_quotation_account", joinColumns = @JoinColumn(name = "quotation_id"))
  @OrderColumn(name = "account_index")
  @Column(name = "arn", nullable = false, length = 30)
  private final List<String> accountArns = new ArrayList<>();

  protected Quotation() {}

  /**
   * Creates a quotation (not yet saved).
   *
   * @param header numbers, client, product and origin
   * @return quotation in DRAFT, version 1 open
   */
  public static Quotation create(QuotationHeader header) {
    Quotation q = new Quotation();
    q.companyId = header.companyId();
    q.quotationNo = header.quotationNo();
    q.arn = header.arn();
    q.requestId = header.requestId();
    q.productCode = header.productCode();
    q.lineCode = header.lineCode();
    q.templateVersion = header.templateVersion();
    q.currency = header.currency() == null ? "PHP" : header.currency();
    q.describe(header.client(), header.marketSegment(), header.sourceChannel());
    return q;
  }

  /**
   * Updates the client facts and the coded header fields.
   *
   * @param client client id, code, name and e-mail
   * @param segment market segment
   * @param channel source channel
   */
  public void describe(QuotationHeader.ClientFacts client, String segment, String channel) {
    this.clientId = client.id();
    this.clientCode = client.code();
    this.clientName = client.name();
    this.clientEmail = client.email();
    this.marketSegment = segment;
    this.sourceChannel = channel;
  }

  /**
   * Copies the figures of the current version onto the header.
   *
   * @param content current content
   * @param totalSumInsured total sum insured
   */
  public void showContent(QuotationContent content, BigDecimal totalSumInsured) {
    this.insurerCode = content.insurerCode();
    this.insurerBranch = content.insurerBranch();
    this.periodFrom = content.periodFrom();
    this.periodTo = content.periodTo();
    this.validUntil = content.validUntil();
    this.directPayment = content.directPayment();
    this.riskGroups = Math.max(1, content.groups().size());
    this.totalSumInsured = totalSumInsured;
    this.netPremium = content.premium().netPremium();
    this.grossPremium = content.premium().grossPremium();
    this.commission = content.premium().commission();
  }

  /**
   * Records the TSU routing decision of the current content (BRNB.098, information only).
   *
   * @param required whether a TSU rule applies
   * @param reason rule description
   */
  public void routeTsu(boolean required, String reason) {
    this.tsuRequired = required;
    this.tsuReason = required ? reason : null;
  }

  /**
   * Opens version n+1 after the current one was submitted (BRNB.020).
   *
   * @return the new version number
   */
  public int openNextVersion() {
    currentVersion++;
    versionOpen = true;
    return currentVersion;
  }

  /**
   * Freezes the current version (submission).
   *
   * @param user submitter
   * @param when time
   */
  public void markSubmitted(String user, Instant when) {
    this.versionOpen = false;
    this.submittedBy = user;
    this.submittedAt = when;
  }

  /**
   * Records the approval.
   *
   * @param user approver
   * @param when time
   */
  public void markApproved(String user, Instant when) {
    this.approvedBy = user;
    this.approvedAt = when;
  }

  /**
   * Records the dispatch to the client.
   *
   * @param when time
   */
  public void markSent(Instant when) {
    this.sentAt = when;
  }

  /**
   * Records the client's acceptance of risk groups (BRNB.045).
   *
   * @param groups accepted risk groups
   * @param when time
   */
  public void markAccepted(List<Integer> groups, Instant when) {
    this.acceptedGroups = groups.stream().map(String::valueOf).collect(Collectors.joining(","));
    this.acceptedAt = when;
  }

  /**
   * Stores the ARNs of the accounts created from the quotation.
   *
   * @param arns account references
   */
  public void linkAccounts(List<String> arns) {
    accountArns.clear();
    accountArns.addAll(arns);
  }

  /**
   * Mirrors the work case stage.
   *
   * @param newStatus status
   */
  public void markStatus(QuotationStatus newStatus) {
    this.status = newStatus;
  }

  /**
   * Risk groups accepted by the client.
   *
   * @return groups, empty before acceptance
   */
  public List<Integer> getAcceptedGroupList() {
    if (acceptedGroups == null || acceptedGroups.isBlank()) {
      return List.of();
    }
    return Arrays.stream(acceptedGroups.split(",")).map(Integer::valueOf).toList();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getQuotationNo() {
    return quotationNo;
  }

  public String getArn() {
    return arn;
  }

  public Long getRequestId() {
    return requestId;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getClientName() {
    return clientName;
  }

  public String getClientEmail() {
    return clientEmail;
  }

  public String getProductCode() {
    return productCode;
  }

  public String getLineCode() {
    return lineCode;
  }

  public String getMarketSegment() {
    return marketSegment;
  }

  public String getSourceChannel() {
    return sourceChannel;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getInsurerBranch() {
    return insurerBranch;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public LocalDate getValidUntil() {
    return validUntil;
  }

  public String getCurrency() {
    return currency;
  }

  public boolean isDirectPayment() {
    return directPayment;
  }

  public String getTemplateVersion() {
    return templateVersion;
  }

  public int getCurrentVersion() {
    return currentVersion;
  }

  public boolean isVersionOpen() {
    return versionOpen;
  }

  public int getRiskGroups() {
    return riskGroups;
  }

  public BigDecimal getTotalSumInsured() {
    return totalSumInsured;
  }

  public BigDecimal getNetPremium() {
    return netPremium;
  }

  public BigDecimal getGrossPremium() {
    return grossPremium;
  }

  public BigDecimal getCommission() {
    return commission;
  }

  public boolean isTsuRequired() {
    return tsuRequired;
  }

  public String getTsuReason() {
    return tsuReason;
  }

  public QuotationStatus getStatus() {
    return status;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public Instant getAcceptedAt() {
    return acceptedAt;
  }

  public List<String> getAccountArns() {
    return accountArns;
  }
}
