package com.iortatechnxt.brokerverse.nonpackage.domain;

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
 * A Proposal Request Form (BRNB.005/006): a non-package risk that TSU prices with the insurers. It
 * carries its marketing reference (PRF number) and its Account Reference Number from creation
 * (BRNB.102), the quotation slip, the chosen insurer and the proposal slip. The status mirrors the
 * NB_PROPOSAL work case.
 */
@Entity
@Table(name = "npk_proposal")
public class ProposalRequest extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "prf_no", nullable = false, length = 30, updatable = false)
  private String prfNo;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "client_id", nullable = false)
  private Long clientId;

  @Column(name = "client_code", nullable = false, length = 30)
  private String clientCode;

  @Column(name = "client_name", nullable = false, length = 250)
  private String clientName;

  @Column(name = "period_from")
  private LocalDate periodFrom;

  @Column(name = "period_to")
  private LocalDate periodTo;

  @Column(name = "client_email", length = 120)
  private String clientEmail;

  @Column(name = "product_code", nullable = false, length = 20, updatable = false)
  private String productCode;

  @Column(name = "line_code", nullable = false, length = 30, updatable = false)
  private String lineCode;

  @Column(nullable = false, length = 3)
  private String currency = "PHP";

  @Column(name = "market_segment", length = 40)
  private String marketSegment;

  @Column(name = "source_channel", length = 40)
  private String sourceChannel;

  @Column(name = "risk_details", nullable = false, columnDefinition = "text")
  private String riskDetails;

  @Column(name = "total_sum_insured", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalSumInsured = BigDecimal.ZERO;

  @Column(name = "tsu_rule", length = 30)
  private String tsuRule;

  @Column(name = "tsu_reason", length = 300)
  private String tsuReason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ProposalStatus status = ProposalStatus.DRAFT;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "qs_no", length = 30)
  private String qsNo;

  @Column(name = "qs_template", length = 60)
  private String qsTemplate;

  @Column(name = "qs_reply_by")
  private LocalDate qsReplyBy;

  @Column(name = "qs_submitted_by", length = 50)
  private String qsSubmittedBy;

  @Column(name = "qs_approved_by", length = 50)
  private String qsApprovedBy;

  @Column(name = "qs_sent_at")
  private Instant qsSentAt;

  @Column(name = "terms_closed", nullable = false)
  private boolean termsClosed;

  @Column(name = "chosen_insurer", length = 30)
  private String chosenInsurer;

  @Column(name = "ps_no", length = 30)
  private String psNo;

  @Column(name = "ps_version", nullable = false)
  private int psVersion;

  @Column(name = "ps_submitted_by", length = 50)
  private String psSubmittedBy;

  @Column(name = "ps_approved_by", length = 50)
  private String psApprovedBy;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "accepted_at")
  private Instant acceptedAt;

  @Column(name = "accepted_groups", length = 200)
  private String acceptedGroups;

  @ElementCollection
  @CollectionTable(name = "npk_proposal_insurer", joinColumns = @JoinColumn(name = "proposal_id"))
  @OrderColumn(name = "insurer_index")
  @Column(name = "insurer_code", nullable = false, length = 30)
  private final List<String> insurers = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "npk_proposal_account", joinColumns = @JoinColumn(name = "proposal_id"))
  @OrderColumn(name = "account_index")
  @Column(name = "arn", nullable = false, length = 30)
  private final List<String> accountArns = new ArrayList<>();

  protected ProposalRequest() {}

  /**
   * Creates a PRF (not yet saved).
   *
   * @param companyId company
   * @param prfNo marketing reference
   * @param arn Account Reference Number
   * @param productCode product
   * @param lineCode product line
   */
  public ProposalRequest(
      Long companyId, String prfNo, String arn, String productCode, String lineCode) {
    this.companyId = companyId;
    this.prfNo = prfNo;
    this.arn = arn;
    this.productCode = productCode;
    this.lineCode = lineCode;
  }

  /**
   * Sets the client and coded header fields.
   *
   * @param client client facts
   * @param segment market segment
   * @param channel source channel
   * @param currencyCode currency
   */
  public void describe(ClientFacts client, String segment, String channel, String currencyCode) {
    this.clientId = client.id();
    this.clientCode = client.code();
    this.clientName = client.name();
    this.clientEmail = client.email();
    this.marketSegment = segment;
    this.sourceChannel = channel;
    this.currency = currencyCode == null ? "PHP" : currencyCode;
  }

  /**
   * Sets the period and the risk details.
   *
   * @param from period start
   * @param to period end
   * @param detailsJson risk details as JSON
   * @param sumInsured total sum insured
   */
  public void describeRisk(
      LocalDate from, LocalDate to, String detailsJson, BigDecimal sumInsured) {
    this.periodFrom = from;
    this.periodTo = to;
    this.riskDetails = detailsJson;
    this.totalSumInsured = sumInsured;
  }

  /**
   * Records the TSU routing decision (BRNB.098).
   *
   * @param rule matching rule, may be null when decided by the product
   * @param reason explanation
   */
  public void routeTsu(String rule, String reason) {
    this.tsuRule = rule;
    this.tsuReason = reason;
  }

  /**
   * Replaces the insurers requested / selected for the quotation slip.
   *
   * @param codes insurer party codes
   */
  public void selectInsurers(List<String> codes) {
    insurers.clear();
    insurers.addAll(codes);
  }

  /**
   * Records the submission for Marketing approval.
   *
   * @param user submitter
   * @param when time
   */
  public void markSubmitted(String user, Instant when) {
    this.submittedBy = user;
    this.submittedAt = when;
  }

  /**
   * Records the Marketing approval.
   *
   * @param user approver
   * @param when time
   */
  public void markApproved(String user, Instant when) {
    this.approvedBy = user;
    this.approvedAt = when;
  }

  /**
   * Records the quotation slip submitted for approval (BRNB.008).
   *
   * @param number QS number (kept when already given)
   * @param template template version used
   * @param replyBy reply date asked from the insurers
   * @param user TSU officer
   */
  public void prepareQuotationSlip(String number, String template, LocalDate replyBy, String user) {
    if (this.qsNo == null) {
      this.qsNo = number;
    }
    this.qsTemplate = template;
    this.qsReplyBy = replyBy;
    this.qsSubmittedBy = user;
  }

  /**
   * Records the quotation slip approved and sent to the insurers.
   *
   * @param user approver
   * @param when time
   */
  public void markQuotationSlipSent(String user, Instant when) {
    this.qsApprovedBy = user;
    this.qsSentAt = when;
  }

  /** Records that the user closed the request for terms with responses still pending. */
  public void closeTerms() {
    this.termsClosed = true;
  }

  /**
   * Records the proposal slip submitted for approval: the chosen insurer and a new PS version.
   *
   * @param number PS number (kept when already given)
   * @param insurer chosen insurer
   * @param user TSU officer
   * @return the new PS version
   */
  public int prepareProposalSlip(String number, String insurer, String user) {
    if (this.psNo == null) {
      this.psNo = number;
    }
    this.chosenInsurer = insurer;
    this.psSubmittedBy = user;
    this.psVersion++;
    return psVersion;
  }

  /**
   * Records the proposal slip approval (release to Marketing).
   *
   * @param user approver
   */
  public void markProposalSlipApproved(String user) {
    this.psApprovedBy = user;
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
   * Records the client's acceptance of risk groups.
   *
   * @param groups accepted groups
   * @param when time
   */
  public void markAccepted(List<Integer> groups, Instant when) {
    this.acceptedGroups = groups.stream().map(String::valueOf).collect(Collectors.joining(","));
    this.acceptedAt = when;
  }

  /**
   * Stores the ARNs of the accounts created.
   *
   * @param arns ARNs
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
  public void markStatus(ProposalStatus newStatus) {
    this.status = newStatus;
  }

  /**
   * Accepted risk groups.
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

  public String getPrfNo() {
    return prfNo;
  }

  public String getArn() {
    return arn;
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

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public String getCurrency() {
    return currency;
  }

  public String getRiskDetails() {
    return riskDetails;
  }

  public BigDecimal getTotalSumInsured() {
    return totalSumInsured;
  }

  public String getTsuRule() {
    return tsuRule;
  }

  public String getTsuReason() {
    return tsuReason;
  }

  public ProposalStatus getStatus() {
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

  public String getQsNo() {
    return qsNo;
  }

  public String getQsTemplate() {
    return qsTemplate;
  }

  public LocalDate getQsReplyBy() {
    return qsReplyBy;
  }

  public String getQsSubmittedBy() {
    return qsSubmittedBy;
  }

  public String getQsApprovedBy() {
    return qsApprovedBy;
  }

  public Instant getQsSentAt() {
    return qsSentAt;
  }

  public boolean isTermsClosed() {
    return termsClosed;
  }

  public String getChosenInsurer() {
    return chosenInsurer;
  }

  public String getPsNo() {
    return psNo;
  }

  public int getPsVersion() {
    return psVersion;
  }

  public String getPsSubmittedBy() {
    return psSubmittedBy;
  }

  public String getPsApprovedBy() {
    return psApprovedBy;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public Instant getAcceptedAt() {
    return acceptedAt;
  }

  public List<String> getInsurers() {
    return insurers;
  }

  public List<String> getAccountArns() {
    return accountArns;
  }

  /**
   * The client of a PRF.
   *
   * @param id crm client id
   * @param code client or prospect code
   * @param name display name
   * @param email e-mail
   */
  public record ClientFacts(Long id, String code, String name, String email) {}
}
