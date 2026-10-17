package com.iortatechnxt.brokerverse.journal.domain;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Journal batch (voucher): a balanced set of debit and credit lines posted as one unit.
 *
 * <p>Supports every batch shape (1Dr-1Cr, 1Dr-nCr, nDr-1Cr, nDr-nCr). The aggregate owns its lines
 * and enforces the lifecycle in {@link JournalStatus}, including maker-checker segregation.
 */
@Entity
@Table(name = "jnl_batch")
@SuppressWarnings("PMD.GodClass") // aggregate root of the voucher: header, lines, lifecycle, links
public class JournalBatch extends BaseEntity {

  /** Largest base-currency rounding difference that is auto-corrected, per line. */
  private static final BigDecimal ROUNDING_TOLERANCE_PER_LINE = new BigDecimal("0.01");

  private static final Set<JournalStatus> EDITABLE =
      EnumSet.of(JournalStatus.DRAFT, JournalStatus.REJECTED);
  private static final Set<JournalStatus> UNPOSTED =
      EnumSet.of(JournalStatus.DRAFT, JournalStatus.REJECTED, JournalStatus.PENDING_APPROVAL);

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "batch_no", nullable = false, length = 40)
  private String batchNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "journal_type", nullable = false, length = 20)
  private JournalType journalType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private JournalStatus status = JournalStatus.DRAFT;

  @Column(name = "transaction_date", nullable = false)
  private LocalDate transactionDate;

  @Column(name = "value_date", nullable = false)
  private LocalDate valueDate;

  @Column(name = "period_id")
  private Long periodId;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, length = 500)
  private String narration;

  @Column(length = 60)
  private String reference;

  @Column(name = "source_module", length = 30)
  private String sourceModule;

  @Column(name = "source_reference", length = 80)
  private String sourceReference;

  @Column(name = "reversal_of_id")
  private Long reversalOfId;

  @Column(name = "reversed_by_id")
  private Long reversedById;

  @Column(name = "total_debit", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalDebit = BigDecimal.ZERO;

  @Column(name = "total_credit", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalCredit = BigDecimal.ZERO;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "authorized_by", length = 50)
  private String authorizedBy;

  @Column(name = "authorized_at")
  private Instant authorizedAt;

  @Column(name = "rejected_by", length = 50)
  private String rejectedBy;

  @Column(name = "rejection_reason", length = 200)
  private String rejectionReason;

  @Column(name = "posted_at")
  private Instant postedAt;

  @Column(name = "assigned_to", length = 50)
  private String assignedTo;

  @Column(name = "assigned_by", length = 50)
  private String assignedBy;

  @Column(name = "assigned_at")
  private Instant assignedAt;

  @Column(name = "reverse_on")
  private LocalDate reverseOn;

  @Column(name = "corrects_batch_id")
  private Long correctsBatchId;

  @Column(name = "related_invoice_no", length = 40)
  private String relatedInvoiceNo;

  @Column(name = "root_invoice_no", length = 40)
  private String rootInvoiceNo;

  @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("lineNo")
  private final List<JournalLine> lines = new ArrayList<>();

  protected JournalBatch() {}

  /**
   * Creates a draft batch.
   *
   * @param header header values
   * @param batchNo allocated batch number
   */
  public JournalBatch(JournalHeader header, String batchNo) {
    this.companyId = header.companyId();
    this.branchId = header.branchId();
    this.batchNo = batchNo;
    this.journalType = header.journalType();
    this.transactionDate = header.transactionDate();
    this.valueDate = header.valueDate();
    this.currency = header.currency();
    this.narration = header.narration();
    this.reference = header.reference();
    this.sourceModule = header.sourceModule();
    this.sourceReference = header.sourceReference();
    this.reversalOfId = header.reversalOfId();
  }

  /**
   * Replaces all lines (only while editable) and recomputes totals.
   *
   * @param specs line values
   */
  public void replaceLines(List<JournalLineSpec> specs) {
    requireStatus(EDITABLE, "edit");
    lines.clear();
    int number = 1;
    for (JournalLineSpec spec : specs) {
      JournalLine line = new JournalLine(spec);
      line.attach(this, number++);
      lines.add(line);
    }
    correctBaseRounding();
    recalculateTotals();
  }

  /**
   * Updates editable header fields.
   *
   * @param header new header values
   */
  public void updateHeader(JournalHeader header) {
    requireStatus(EDITABLE, "edit");
    this.branchId = header.branchId();
    this.transactionDate = header.transactionDate();
    this.valueDate = header.valueDate();
    this.currency = header.currency();
    this.narration = header.narration();
    this.reference = header.reference();
  }

  /**
   * Checks debit/credit equality in base currency.
   *
   * @return true when balanced and non-empty
   */
  public boolean isBalanced() {
    return totalDebit.signum() > 0 && totalDebit.compareTo(totalCredit) == 0;
  }

  /**
   * Maker submits the batch for authorization.
   *
   * @param user maker
   * @param when timestamp
   */
  public void submit(String user, Instant when) {
    requireStatus(EDITABLE, "submit");
    if (!isBalanced()) {
      throw new BusinessRuleException(
          "UNBALANCED_JOURNAL",
          "Journal is not balanced: debit " + totalDebit + " vs credit " + totalCredit);
    }
    this.status = JournalStatus.PENDING_APPROVAL;
    this.submittedBy = user;
    this.submittedAt = when;
    this.rejectedBy = null;
    this.rejectionReason = null;
  }

  /**
   * Checker authorizes the batch. Posting is performed by the posting engine right after.
   *
   * @param checker authorizing user
   * @param when timestamp
   * @param allowSelfApproval true only for system generated journals
   */
  public void authorize(String checker, Instant when, boolean allowSelfApproval) {
    requireStatus(EnumSet.of(JournalStatus.PENDING_APPROVAL), "authorize");
    if (!allowSelfApproval && Objects.equals(submittedBy, checker)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A journal cannot be authorized by the user who submitted it");
    }
    this.authorizedBy = checker;
    this.authorizedAt = when;
  }

  /**
   * Records successful posting.
   *
   * @param period posting period
   * @param when timestamp
   */
  public void markPosted(Long period, Instant when) {
    requireStatus(EnumSet.of(JournalStatus.PENDING_APPROVAL), "post");
    this.status = JournalStatus.POSTED;
    this.periodId = period;
    this.postedAt = when;
  }

  /**
   * Checker returns the batch to the maker.
   *
   * @param checker user
   * @param reason reason
   */
  public void reject(String checker, String reason) {
    requireStatus(EnumSet.of(JournalStatus.PENDING_APPROVAL), "reject");
    this.status = JournalStatus.REJECTED;
    this.rejectedBy = checker;
    this.rejectionReason = reason;
  }

  /** Cancels an unposted batch. Cancelled batches are retained for audit. */
  public void cancel() {
    requireStatus(EDITABLE, "cancel");
    this.status = JournalStatus.CANCELLED;
  }

  /**
   * Assigns the batch to the user who will post it (FRBS 2.5.1); allowed until it is posted.
   *
   * @param assignee user, null to clear the assignment
   * @param by assigning user (TL)
   * @param when timestamp
   */
  public void assign(String assignee, String by, Instant when) {
    requireStatus(UNPOSTED, "assign");
    this.assignedTo = assignee;
    this.assignedBy = assignee == null ? null : by;
    this.assignedAt = assignee == null ? null : when;
  }

  /**
   * Sets the date on which the posted batch is reversed automatically (FRBS 2.8.1). Only manual,
   * adjustment and accrual journals that are still editable take a reversal date, which must fall
   * after the value date.
   *
   * @param date reversal date, null for none
   */
  public void scheduleReversal(LocalDate date) {
    if (date == null) {
      this.reverseOn = null;
      return;
    }
    requireStatus(EDITABLE, "set a reversal date on");
    if (journalType.isSystemGenerated() || journalType == JournalType.REVERSAL) {
      throw new BusinessRuleException(
          "REVERSAL_DATE_NOT_ALLOWED", "Only manual journals take an automatic reversal date");
    }
    if (!date.isAfter(valueDate)) {
      throw new BusinessRuleException(
          "REVERSAL_DATE_INVALID", "The reversal date must be after the value date " + valueDate);
    }
    this.reverseOn = date;
  }

  /**
   * Links a correcting journal to the journal it corrects and to its invoice family (ACSL 2.9.1,
   * 2.16.0).
   *
   * @param corrects id of the corrected journal, may be null
   * @param relatedInvoice invoice the correction concerns, may be null
   * @param rootInvoice root of the invoice family, may be null
   */
  public void link(Long corrects, String relatedInvoice, String rootInvoice) {
    this.correctsBatchId = corrects;
    this.relatedInvoiceNo = relatedInvoice;
    this.rootInvoiceNo = rootInvoice;
  }

  /**
   * Whether the automatic reversal of the batch is due on a date.
   *
   * @param date business date
   * @return true when posted, not yet reversed and the reversal date has come
   */
  public boolean isReversalDue(LocalDate date) {
    return status == JournalStatus.POSTED
        && reversedById == null
        && reverseOn != null
        && !reverseOn.isAfter(date);
  }

  /**
   * Marks a posted batch as reversed by another batch.
   *
   * @param reversalBatchId reversing batch
   */
  public void markReversed(Long reversalBatchId) {
    requireStatus(EnumSet.of(JournalStatus.POSTED), "reverse");
    this.status = JournalStatus.REVERSED;
    this.reversedById = reversalBatchId;
  }

  /** Ensures the batch can still be reversed. */
  public void requireReversible() {
    requireStatus(EnumSet.of(JournalStatus.POSTED), "reverse");
    if (reversedById != null) {
      throw new BusinessRuleException(
          "ALREADY_REVERSED", "Journal " + batchNo + " is already reversed");
    }
  }

  private void requireStatus(Set<JournalStatus> allowed, String action) {
    if (!allowed.contains(status)) {
      throw new BusinessRuleException(
          "INVALID_JOURNAL_STATUS",
          "Cannot " + action + " journal " + batchNo + " in status " + status);
    }
  }

  /**
   * Line-level conversion to base currency can leave a sub-cent difference even when the
   * transaction currency amounts balance. Such a difference (within tolerance) is absorbed by the
   * largest line on the short side so the base totals balance exactly.
   */
  private void correctBaseRounding() {
    correctableDifference()
        .ifPresent(
            diff -> {
              BalanceSide shortSide = diff.signum() > 0 ? BalanceSide.CREDIT : BalanceSide.DEBIT;
              lines.stream()
                  .filter(l -> l.getSide() == shortSide)
                  .max(Comparator.comparing(JournalLine::getBaseAmount))
                  .ifPresent(l -> l.adjustBaseAmount(diff.abs()));
            });
  }

  private Optional<BigDecimal> correctableDifference() {
    if (lines.isEmpty() || !balancedInTransactionCurrencies()) {
      return Optional.empty();
    }
    BigDecimal diff = sum(BalanceSide.DEBIT).subtract(sum(BalanceSide.CREDIT));
    BigDecimal tolerance = ROUNDING_TOLERANCE_PER_LINE.multiply(BigDecimal.valueOf(lines.size()));
    boolean correctable = diff.signum() != 0 && diff.abs().compareTo(tolerance) <= 0;
    return correctable ? Optional.of(diff) : Optional.empty();
  }

  private boolean balancedInTransactionCurrencies() {
    return lines.stream()
        .map(JournalLine::getCurrency)
        .distinct()
        .allMatch(c -> sumFc(c, BalanceSide.DEBIT).compareTo(sumFc(c, BalanceSide.CREDIT)) == 0);
  }

  private BigDecimal sumFc(String ccy, BalanceSide side) {
    return lines.stream()
        .filter(l -> l.getSide() == side && l.getCurrency().equals(ccy))
        .map(JournalLine::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal sum(BalanceSide side) {
    return lines.stream()
        .filter(l -> l.getSide() == side)
        .map(JournalLine::getBaseAmount)
        .reduce(Money.zero(), BigDecimal::add);
  }

  private void recalculateTotals() {
    this.totalDebit = sum(BalanceSide.DEBIT);
    this.totalCredit = sum(BalanceSide.CREDIT);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public JournalType getJournalType() {
    return journalType;
  }

  public JournalStatus getStatus() {
    return status;
  }

  public LocalDate getTransactionDate() {
    return transactionDate;
  }

  public LocalDate getValueDate() {
    return valueDate;
  }

  public Long getPeriodId() {
    return periodId;
  }

  public String getCurrency() {
    return currency;
  }

  public String getNarration() {
    return narration;
  }

  public String getReference() {
    return reference;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceReference() {
    return sourceReference;
  }

  public Long getReversalOfId() {
    return reversalOfId;
  }

  public Long getReversedById() {
    return reversedById;
  }

  public BigDecimal getTotalDebit() {
    return totalDebit;
  }

  public BigDecimal getTotalCredit() {
    return totalCredit;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getAuthorizedBy() {
    return authorizedBy;
  }

  public Instant getAuthorizedAt() {
    return authorizedAt;
  }

  public String getRejectedBy() {
    return rejectedBy;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public Instant getPostedAt() {
    return postedAt;
  }

  public String getAssignedTo() {
    return assignedTo;
  }

  public String getAssignedBy() {
    return assignedBy;
  }

  public Instant getAssignedAt() {
    return assignedAt;
  }

  public LocalDate getReverseOn() {
    return reverseOn;
  }

  public Long getCorrectsBatchId() {
    return correctsBatchId;
  }

  public String getRelatedInvoiceNo() {
    return relatedInvoiceNo;
  }

  public String getRootInvoiceNo() {
    return rootInvoiceNo;
  }

  public List<JournalLine> getLines() {
    return List.copyOf(lines);
  }
}
