package com.iortatechnxt.brokerverse.issuance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * An e-policy received from the insurer (BRNB.073), stored as an EPOLICY document of the account,
 * with the data extracted from it (BRNB.104), the review that updates the policy number (BRNB.074)
 * and its dispatch to the client (BRNB.077).
 */
@Entity
@Table(name = "iss_epolicy")
public class Epolicy extends BaseEntity {

  private static final String SEPARATOR = ", ";
  private static final int MAX_NOTE = 500;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "account_id", nullable = false, updatable = false)
  private Long accountId;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "attachment_id", nullable = false, updatable = false)
  private Long attachmentId;

  @Column(name = "file_name", nullable = false, length = 255, updatable = false)
  private String fileName;

  @Enumerated(EnumType.STRING)
  @Column(name = "match_method", nullable = false, length = 20, updatable = false)
  private MatchMethod matchMethod;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EpolicyStatus status = EpolicyStatus.RECEIVED;

  @Column(name = "extracted_policy_numbers", length = 500)
  private String extractedPolicyNumbers;

  @Column(name = "extracted_period_from")
  private LocalDate extractedPeriodFrom;

  @Column(name = "extracted_period_to")
  private LocalDate extractedPeriodTo;

  @Column(name = "extracted_premium", precision = 19, scale = 2)
  private BigDecimal extractedPremium;

  @Column(name = "extraction_note", length = MAX_NOTE)
  private String extractionNote;

  @Column(name = "policy_numbers", length = 500)
  private String policyNumbers;

  @Column(name = "issue_date")
  private LocalDate issueDate;

  @Column(name = "reviewed_by", length = 50)
  private String reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "reject_reason", length = 40)
  private String rejectReason;

  @Column(name = "dispatch_count", nullable = false)
  private int dispatchCount;

  @Column(name = "dispatched_at")
  private Instant dispatchedAt;

  @Column(name = "dispatched_to", length = 500)
  private String dispatchedTo;

  protected Epolicy() {}

  /**
   * Records a received e-policy.
   *
   * @param companyId company
   * @param accountId account
   * @param arn Account Reference Number
   * @param document stored document and how it was matched to the account
   */
  public Epolicy(Long companyId, Long accountId, String arn, ReceivedDocument document) {
    this.companyId = companyId;
    this.accountId = accountId;
    this.arn = arn;
    this.attachmentId = document.attachmentId();
    this.fileName = document.fileName();
    this.matchMethod = document.matchMethod();
  }

  /**
   * Stores the extracted data and opens the review (BRNB.104).
   *
   * @param numbers policy numbers found
   * @param periodFrom period start found
   * @param periodTo period end found
   * @param premium premium found
   * @param note what could not be read
   */
  public void extracted(
      List<String> numbers,
      LocalDate periodFrom,
      LocalDate periodTo,
      BigDecimal premium,
      String note) {
    requireOpen();
    this.extractedPolicyNumbers = numbers.isEmpty() ? null : String.join(SEPARATOR, numbers);
    this.extractedPeriodFrom = periodFrom;
    this.extractedPeriodTo = periodTo;
    this.extractedPremium = premium;
    this.extractionNote =
        note == null || note.length() <= MAX_NOTE ? note : note.substring(0, MAX_NOTE);
    this.status = EpolicyStatus.REVIEW;
  }

  /**
   * Confirms the policy numbers after review (BRNB.074).
   *
   * @param numbers confirmed policy numbers
   * @param date issue date
   * @param user reviewer
   * @param when time
   */
  public void confirm(List<String> numbers, LocalDate date, String user, Instant when) {
    requireOpen();
    this.policyNumbers = String.join(SEPARATOR, numbers);
    this.issueDate = date;
    this.reviewedBy = user;
    this.reviewedAt = when;
    this.status = EpolicyStatus.CONFIRMED;
  }

  /**
   * Rejects the document at review.
   *
   * @param reason reason (list EPOLICY_REJECT_REASON)
   * @param user reviewer
   * @param when time
   */
  public void reject(String reason, String user, Instant when) {
    requireOpen();
    this.rejectReason = reason;
    this.reviewedBy = user;
    this.reviewedAt = when;
    this.status = EpolicyStatus.REJECTED;
  }

  /**
   * Records a dispatch to the client (BRNB.077).
   *
   * @param to recipients
   * @param when time
   */
  public void dispatched(String to, Instant when) {
    if (status != EpolicyStatus.CONFIRMED) {
      throw new BusinessRuleException(
          "EPOLICY_NOT_CONFIRMED",
          "Confirm the policy data of " + arn + " before sending the e-policy");
    }
    this.dispatchCount++;
    this.dispatchedAt = when;
    this.dispatchedTo = to;
  }

  /**
   * Whether the e-policy waits for its review.
   *
   * @return true when received or in review
   */
  public boolean isOpen() {
    return status == EpolicyStatus.RECEIVED || status == EpolicyStatus.REVIEW;
  }

  private void requireOpen() {
    if (!isOpen()) {
      throw new BusinessRuleException(
          "EPOLICY_REVIEWED", "The e-policy " + fileName + " of " + arn + " is already " + status);
    }
  }

  /**
   * Policy numbers found by the extraction, one by one.
   *
   * @return numbers
   */
  public List<String> getExtractedPolicyNumberList() {
    return split(extractedPolicyNumbers);
  }

  /**
   * Confirmed policy numbers, one by one.
   *
   * @return numbers
   */
  public List<String> getPolicyNumberList() {
    return split(policyNumbers);
  }

  private static List<String> split(String joined) {
    return joined == null ? List.of() : Arrays.asList(joined.split(SEPARATOR));
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getArn() {
    return arn;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public String getFileName() {
    return fileName;
  }

  public MatchMethod getMatchMethod() {
    return matchMethod;
  }

  public EpolicyStatus getStatus() {
    return status;
  }

  public LocalDate getExtractedPeriodFrom() {
    return extractedPeriodFrom;
  }

  public LocalDate getExtractedPeriodTo() {
    return extractedPeriodTo;
  }

  public BigDecimal getExtractedPremium() {
    return extractedPremium;
  }

  public String getExtractionNote() {
    return extractionNote;
  }

  public LocalDate getIssueDate() {
    return issueDate;
  }

  public String getReviewedBy() {
    return reviewedBy;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public String getRejectReason() {
    return rejectReason;
  }

  public int getDispatchCount() {
    return dispatchCount;
  }

  public Instant getDispatchedAt() {
    return dispatchedAt;
  }

  public String getDispatchedTo() {
    return dispatchedTo;
  }

  /**
   * The stored document of a received e-policy.
   *
   * @param attachmentId EPOLICY document of the account
   * @param fileName file name
   * @param matchMethod how the file was matched to the account
   */
  public record ReceivedDocument(Long attachmentId, String fileName, MatchMethod matchMethod) {}
}
