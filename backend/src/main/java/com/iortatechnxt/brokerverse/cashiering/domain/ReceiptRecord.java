package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordStage;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * BDOI's record of an AR or OR to create, cancel or reinstate (FRS.CSH.02.01, 02.02, 02.05, 03.01,
 * 04.01): the creator saves it (Created), may edit or cancel it, and submits it for posting; the
 * Approver/Poster, who is not the creator, posts it (the AR / OR number is issued, cancelled or
 * reinstated) or returns it with a reason. Its number follows {@code <Prefix>-<Receipt
 * Type>-<Sequence>} and is never reused.
 */
@Entity
@Table(name = "csh_receipt_record")
@SuppressWarnings("PMD.GodClass") // aggregate root of the record: details, accounts, lifecycle
public class ReceiptRecord extends BaseEntity {

  private static final Set<RecordStage> EDITABLE =
      Set.of(RecordStage.CREATED, RecordStage.RETURNED);

  private static final int RESULT_LENGTH = 500;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "record_no", nullable = false, length = 40, updatable = false)
  private String recordNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "record_kind", nullable = false, length = 15, updatable = false)
  private RecordKind recordKind;

  @Enumerated(EnumType.STRING)
  @Column(name = "receipt_kind", nullable = false, length = 2, updatable = false)
  private ReceiptKind receiptKind;

  @Column(name = "receipt_type", nullable = false, length = 30)
  private String receiptType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RecordStage stage = RecordStage.CREATED;

  @Column(name = "branch_id")
  private Long branchId;

  @Embedded private RecordParty party;

  @Embedded private RecordTender tender;

  @Embedded private RecordReason reason;

  @Column(name = "receipt_id")
  private Long receiptId;

  @Column(name = "receipt_no", length = 40)
  private String receiptNo;

  @Column(name = "return_reason", length = 250)
  private String returnReason;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "posted_by", length = 50)
  private String postedBy;

  @Column(name = "posted_at")
  private Instant postedAt;

  @Column(name = "posting_result", length = 500)
  private String postingResult;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @ElementCollection
  @CollectionTable(
      name = "csh_receipt_record_account",
      joinColumns = @JoinColumn(name = "record_id"))
  @OrderColumn(name = "line_no")
  private final List<RecordAccount> accounts = new ArrayList<>();

  protected ReceiptRecord() {}

  /**
   * Creates a saved record (status Created).
   *
   * @param companyId company
   * @param recordNo CR-, CN- or RE- number
   * @param recordKind creation, cancellation or reinstatement
   * @param receiptKind AR or OR
   */
  public ReceiptRecord(
      Long companyId, String recordNo, RecordKind recordKind, ReceiptKind receiptKind) {
    this.companyId = companyId;
    this.recordNo = recordNo;
    this.recordKind = recordKind;
    this.receiptKind = receiptKind;
  }

  /**
   * Sets the details of a creation record (save or edit).
   *
   * @param type AR type or OR type
   * @param branch receipting branch
   * @param payor entry type and payor
   * @param money payment, bank account, check and remarks
   * @param selected accounts with their paid amounts
   */
  public void describe(
      String type,
      Long branch,
      RecordParty payor,
      RecordTender money,
      List<RecordAccount> selected) {
    requireEditable();
    this.receiptType = type;
    this.branchId = branch;
    this.party = payor;
    this.tender = money;
    replaceAccounts(selected);
  }

  /**
   * Sets the details of a cancellation or reinstatement record.
   *
   * @param receipt receipt cancelled or reinstated
   * @param why reason, type and the information of CSHID.005
   * @param selected accounts with the amounts reversed
   */
  public void target(Receipt receipt, RecordReason why, List<RecordAccount> selected) {
    requireEditable();
    this.receiptId = receipt.getId();
    this.receiptNo = receipt.getReceiptNo();
    this.receiptType = receipt.getReceiptClass();
    this.branchId = receipt.getBranchId();
    this.reason = why;
    replaceAccounts(selected);
  }

  /**
   * Sets the money of a cancellation or reinstatement record (currency and amount of the receipt).
   *
   * @param money currency, amount and payment type
   */
  public void money(RecordTender money) {
    requireEditable();
    this.tender = money;
  }

  private void replaceAccounts(List<RecordAccount> selected) {
    accounts.clear();
    if (selected != null) {
      accounts.addAll(selected);
    }
  }

  /**
   * Submits the record for posting (FRS.CSH.02.01.10).
   *
   * @param by creator
   * @param at time
   */
  public void submit(String by, Instant at) {
    requireEditable();
    this.stage = RecordStage.FOR_POSTING;
    this.submittedBy = by;
    this.submittedAt = at;
    this.returnReason = null;
  }

  /**
   * Returns the record to its creator (FRS.CSH.02.05.07).
   *
   * @param why reason of the return
   */
  public void returnToCreator(String why) {
    requireForPosting();
    this.stage = RecordStage.RETURNED;
    this.returnReason = why;
  }

  /** Cancels a saved record (FRS.CSH.02.01.12): only in status Created. */
  public void cancelRecord() {
    if (stage != RecordStage.CREATED) {
      throw new BusinessRuleException(
          "RECORD_NOT_CREATED",
          "Only a record in status Created can be cancelled; " + recordNo + " is " + label());
    }
    this.stage = RecordStage.RECORD_CANCELLED;
  }

  /**
   * Records the posting: the receipt issued, cancelled or reinstated and its journal.
   *
   * @param receipt receipt
   * @param by Approver/Poster
   * @param at time
   * @param batch journal batch, may be null
   */
  public void posted(Receipt receipt, String by, Instant at, String batch) {
    requireForPosting();
    this.stage = RecordStage.POSTED;
    this.receiptId = receipt.getId();
    this.receiptNo = receipt.getReceiptNo();
    this.postedBy = by;
    this.postedAt = at;
    this.journalBatchNo = batch;
    this.postingResult = null;
  }

  /**
   * Keeps the reason why a posting of several records failed for this one (FRS.CSH.02.05.06).
   *
   * @param message reason
   */
  public void postingFailed(String message) {
    this.postingResult =
        message == null ? null : message.substring(0, Math.min(RESULT_LENGTH, message.length()));
  }

  /**
   * Whether the creator may still change the record.
   *
   * @return true in status Created or Returned
   */
  public boolean isEditable() {
    return EDITABLE.contains(stage);
  }

  private void requireEditable() {
    if (!isEditable()) {
      throw new BusinessRuleException(
          "RECORD_NOT_EDITABLE", recordNo + " is " + label() + " and cannot be changed");
    }
  }

  private void requireForPosting() {
    if (stage != RecordStage.FOR_POSTING) {
      throw new BusinessRuleException(
          "RECORD_NOT_FOR_POSTING", recordNo + " is " + label() + ", not For Posting");
    }
  }

  /**
   * The status as BDOI's FRS names it.
   *
   * @return label, for example "AR/OR Issued"
   */
  public String label() {
    return switch (stage) {
      case CREATED -> "Created";
      case FOR_POSTING -> "For Posting";
      case RETURNED -> "Returned";
      case RECORD_CANCELLED -> "Record Cancelled";
      case POSTED -> postedLabel();
    };
  }

  private String postedLabel() {
    return switch (recordKind) {
      case CREATION -> "AR/OR Issued";
      case CANCELLATION -> "AR/OR Cancelled";
      case REINSTATEMENT -> "AR/OR Reinstated";
    };
  }

  /**
   * Total of the accounts, or the paid amount when no account is selected.
   *
   * @return amount
   */
  public BigDecimal total() {
    if (accounts.isEmpty()) {
      return tender == null || tender.amount() == null ? BigDecimal.ZERO : tender.amount();
    }
    return accounts.stream().map(RecordAccount::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRecordNo() {
    return recordNo;
  }

  public RecordKind getRecordKind() {
    return recordKind;
  }

  public ReceiptKind getReceiptKind() {
    return receiptKind;
  }

  public String getReceiptType() {
    return receiptType;
  }

  public RecordStage getStage() {
    return stage;
  }

  public Long getBranchId() {
    return branchId;
  }

  public RecordParty getParty() {
    return party;
  }

  public RecordTender getTender() {
    return tender;
  }

  public RecordReason getReason() {
    return reason;
  }

  public Long getReceiptId() {
    return receiptId;
  }

  public String getReceiptNo() {
    return receiptNo;
  }

  public String getReturnReason() {
    return returnReason;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getPostedBy() {
    return postedBy;
  }

  public Instant getPostedAt() {
    return postedAt;
  }

  public String getPostingResult() {
    return postingResult;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public List<RecordAccount> getAccounts() {
    return List.copyOf(accounts);
  }
}
