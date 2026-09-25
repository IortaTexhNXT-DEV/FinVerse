package com.iortatechnxt.brokerverse.acsl.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A correction entry (ACSL 2.7.0-2.15.0, 2.9.1; ACCOUNTING_DISBURSEMENT_DESIGN 5.3): assigned by
 * the team leader, prepared, reviewed and approved, then posted as a system journal whose lines are
 * the correction's. Posted journals are never changed: a wrong account is corrected by reversing
 * the original line and re-posting it to the right account, linked to the invoice family.
 */
@Entity
@Table(name = "acsl_correction")
public class Correction extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "correction_no", nullable = false, length = 30, updatable = false)
  private String correctionNo;

  @Column(name = "case_id", updatable = false)
  private Long caseId;

  @Column(nullable = false, length = 20)
  private String kind;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CorrectionStage stage = CorrectionStage.ASSIGNED;

  @Column(name = "invoice_no", length = 40)
  private String invoiceNo;

  @Column(name = "root_invoice_no", length = 40)
  private String rootInvoiceNo;

  @Column(name = "original_batch_no", length = 40)
  private String originalBatchNo;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, length = 500)
  private String description;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "reviewed_by", length = 50)
  private String reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "return_comment", length = 500)
  private String returnComment;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Column(name = "posted_at")
  private Instant postedAt;

  @Column(name = "open_items", nullable = false)
  private int openItems;

  @Column(name = "ledger_movements", nullable = false)
  private int ledgerMovements;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "correction_id", nullable = false)
  @OrderBy("lineNo")
  private List<CorrectionLine> lines = new ArrayList<>();

  protected Correction() {}

  /**
   * Opens a correction.
   *
   * @param companyId company
   * @param branchId branch of the journal
   * @param correctionNo number
   * @param header kind, invoice, original journal, currency and description
   * @param caseId case it was raised from, may be null
   */
  public Correction(
      Long companyId, Long branchId, String correctionNo, Header header, Long caseId) {
    this.companyId = companyId;
    this.branchId = branchId;
    this.correctionNo = correctionNo;
    this.caseId = caseId;
    this.kind = header.kind();
    this.invoiceNo = header.invoiceNo();
    this.rootInvoiceNo = header.rootInvoiceNo();
    this.originalBatchNo = header.originalBatchNo();
    this.currency = header.currency();
    this.description = header.description();
  }

  /**
   * Replaces the lines while in draft.
   *
   * @param values lines
   */
  public void replaceLines(List<CorrectionLineValues> values) {
    lines.clear();
    int no = 1;
    for (CorrectionLineValues v : values) {
      lines.add(new CorrectionLine(no++, v));
    }
  }

  /**
   * Mirrors the work case stage.
   *
   * @param newStage stage
   */
  public void moveTo(CorrectionStage newStage) {
    this.stage = newStage;
  }

  /**
   * Records the submission.
   *
   * @param user preparer
   * @param at when
   */
  public void submitted(String user, Instant at) {
    this.submittedBy = user;
    this.submittedAt = at;
  }

  /**
   * Records the review.
   *
   * @param user reviewer
   * @param at when
   */
  public void reviewed(String user, Instant at) {
    this.reviewedBy = user;
    this.reviewedAt = at;
  }

  /**
   * Records a return with its comment (ACSL 2.12.0).
   *
   * @param comment comment
   */
  public void returned(String comment) {
    this.returnComment = comment;
  }

  /**
   * Records the approval and the posting (ACSL 2.11.0, 2.15.0).
   *
   * @param user approver
   * @param at when
   * @param batchNo journal batch
   * @param items open items recorded
   * @param movements invoice ledger movements recorded
   */
  public void posted(String user, Instant at, String batchNo, int items, int movements) {
    this.approvedBy = user;
    this.approvedAt = at;
    this.journalBatchNo = batchNo;
    this.postedAt = at;
    this.openItems = items;
    this.ledgerMovements = movements;
  }

  /**
   * Total debits.
   *
   * @return debits
   */
  public BigDecimal totalDebit() {
    return lines.stream()
        .map(CorrectionLine::debitAmount)
        .filter(a -> a.signum() > 0)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Total credits.
   *
   * @return credits
   */
  public BigDecimal totalCredit() {
    return lines.stream()
        .map(CorrectionLine::debitAmount)
        .filter(a -> a.signum() < 0)
        .map(BigDecimal::negate)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Whether debits equal credits.
   *
   * @return true when balanced
   */
  public boolean isBalanced() {
    return totalDebit().compareTo(totalCredit()) == 0;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getCorrectionNo() {
    return correctionNo;
  }

  public Long getCaseId() {
    return caseId;
  }

  public String getKind() {
    return kind;
  }

  public CorrectionStage getStage() {
    return stage;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getRootInvoiceNo() {
    return rootInvoiceNo;
  }

  public String getOriginalBatchNo() {
    return originalBatchNo;
  }

  public String getCurrency() {
    return currency;
  }

  public String getDescription() {
    return description;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getReviewedBy() {
    return reviewedBy;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public String getReturnComment() {
    return returnComment;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public Instant getPostedAt() {
    return postedAt;
  }

  public int getOpenItems() {
    return openItems;
  }

  public int getLedgerMovements() {
    return ledgerMovements;
  }

  public List<CorrectionLine> getLines() {
    return List.copyOf(lines);
  }

  /**
   * Header of a correction.
   *
   * @param kind kind (LOV {@code ACSL_CORRECTION_KIND})
   * @param invoiceNo invoice corrected, may be null
   * @param rootInvoiceNo its family root, may be null
   * @param originalBatchNo journal corrected, may be null
   * @param currency currency
   * @param description description
   */
  public record Header(
      String kind,
      String invoiceNo,
      String rootInvoiceNo,
      String originalBatchNo,
      String currency,
      String description) {}
}
