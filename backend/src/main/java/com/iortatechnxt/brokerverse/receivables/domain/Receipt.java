package com.iortatechnxt.brokerverse.receivables.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
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
import java.util.List;
import java.util.Objects;

/**
 * Official receipt (collection) of money from a policyholder, intermediary, reinsurer or other
 * payer.
 *
 * <p>Maker-checker: a receipt is entered PENDING_APPROVAL and accounted for only when a different
 * user approves it. The approved amount is split into an applied part (matched to the payer's debit
 * notes) and an unapplied part kept on account.
 */
@Entity
@Table(name = "rcv_receipt")
public class Receipt extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "receipt_no", nullable = false, length = 40)
  private String receiptNo;

  @Column(name = "receipt_date", nullable = false)
  private LocalDate receiptDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "payer_type", nullable = false, length = 20)
  private PayerType payerType;

  @Column(name = "party_id")
  private Long partyId;

  @Column(name = "party_code", length = 30)
  private String partyCode;

  @Column(name = "payer_name", nullable = false, length = 200)
  private String payerName;

  @Column(length = 20)
  private String department;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReceiptMode mode;

  @Column(name = "instrument_no", length = 40)
  private String instrumentNo;

  @Column(name = "instrument_date")
  private LocalDate instrumentDate;

  @Column(name = "drawee_bank", length = 120)
  private String draweeBank;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal exchangeRate;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "base_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal baseAmount;

  @Column(name = "applied_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal appliedAmount = BigDecimal.ZERO;

  @Column(name = "bank_account_code", nullable = false, length = 30)
  private String bankAccountCode;

  @Column(name = "income_account_code", length = 30)
  private String incomeAccountCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "allocation_method", nullable = false, length = 10)
  private AllocationMethod allocationMethod;

  @Column(length = 250)
  private String narration;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReceiptStatus status = ReceiptStatus.PENDING_APPROVAL;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Column(name = "credit_item_id")
  private Long creditItemId;

  @Column(name = "reversal_date")
  private LocalDate reversalDate;

  @Column(name = "reversal_reason", length = 200)
  private String reversalReason;

  @Column(name = "reversed_by", length = 50)
  private String reversedBy;

  @Enumerated(EnumType.STRING)
  @Column(name = "deposit_status", nullable = false, length = 20)
  private DepositStatus depositStatus;

  @Column(name = "deposit_slip_id")
  private Long depositSlipId;

  @Column(name = "deposited_on")
  private LocalDate depositedOn;

  @Column(name = "pdc_id")
  private Long pdcId;

  @Column(name = "application_count", nullable = false)
  private int applicationCount;

  @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private final List<ReceiptAllocation> allocations = new ArrayList<>();

  protected Receipt() {}

  /**
   * Creates a receipt pending approval.
   *
   * @param v values
   */
  public Receipt(ReceiptValues v) {
    this.companyId = v.companyId();
    this.branchId = v.branchId();
    this.receiptNo = v.receiptNo();
    this.receiptDate = v.receiptDate();
    this.payerType = v.payerType();
    this.partyId = v.partyId();
    this.partyCode = v.partyCode();
    this.payerName = v.payerName();
    this.department = v.department();
    this.mode = v.mode();
    this.instrumentNo = v.instrumentNo();
    this.instrumentDate = v.instrumentDate();
    this.draweeBank = v.draweeBank();
    this.currency = v.currency();
    this.exchangeRate = v.exchangeRate();
    this.amount = v.amount();
    this.baseAmount = v.baseAmount();
    this.bankAccountCode = v.bankAccountCode();
    this.incomeAccountCode = v.incomeAccountCode();
    this.allocationMethod = v.allocationMethod();
    this.narration = v.narration();
    this.pdcId = v.pdcId();
    this.depositStatus =
        v.mode().requiresDeposit() ? DepositStatus.UNDEPOSITED : DepositStatus.NOT_REQUIRED;
  }

  /**
   * Adds a requested (manual) allocation, matched when the receipt is approved.
   *
   * @param debitItemId debit open item
   * @param documentNo debit note number
   * @param value amount in receipt currency
   * @return the allocation
   */
  public ReceiptAllocation addAllocation(Long debitItemId, String documentNo, BigDecimal value) {
    ReceiptAllocation allocation = new ReceiptAllocation(this, debitItemId, documentNo, value);
    allocations.add(allocation);
    return allocation;
  }

  /**
   * Fails unless the receipt is in the expected status.
   *
   * @param expected expected status
   */
  public void requireStatus(ReceiptStatus expected) {
    if (status != expected) {
      throw new BusinessRuleException(
          "RECEIPT_STATUS", "Receipt " + receiptNo + " is " + status + ", expected " + expected);
    }
  }

  /**
   * Approves the receipt after its journal was posted (checker action).
   *
   * @param checker approving user (must differ from the maker)
   * @param when timestamp
   * @param batchNo journal batch that recorded the receipt
   * @param openItemId CREDIT open item of the receipt (null for OTHER receipts)
   */
  public void approve(String checker, Instant when, String batchNo, Long openItemId) {
    requireStatus(ReceiptStatus.PENDING_APPROVAL);
    requireChecker(checker);
    this.status = ReceiptStatus.APPROVED;
    this.approvedBy = checker;
    this.approvedAt = when;
    this.journalBatchNo = batchNo;
    this.creditItemId = openItemId;
  }

  /**
   * Rejects a pending receipt (checker action).
   *
   * @param checker user
   * @param date rejection date
   * @param reason reason
   */
  public void reject(String checker, LocalDate date, String reason) {
    requireStatus(ReceiptStatus.PENDING_APPROVAL);
    requireChecker(checker);
    this.status = ReceiptStatus.REJECTED;
    recordReversal(checker, date, reason);
  }

  /**
   * Marks an approved receipt as cancelled or bounced after its journal was reversed.
   *
   * @param to CANCELLED or BOUNCED
   * @param user acting user
   * @param date reversal date
   * @param reason reason
   */
  public void reverse(ReceiptStatus to, String user, LocalDate date, String reason) {
    requireStatus(ReceiptStatus.APPROVED);
    if (date.isBefore(receiptDate)) {
      throw new BusinessRuleException(
          "INVALID_REVERSAL_DATE", "Reversal date cannot precede the receipt date " + receiptDate);
    }
    if (to == ReceiptStatus.BOUNCED && !mode.isCheque()) {
      throw new BusinessRuleException("NOT_A_CHEQUE", "Only cheque receipts can bounce");
    }
    if (depositStatus == DepositStatus.IN_SLIP) {
      throw new BusinessRuleException(
          "RECEIPT_IN_SLIP", "Remove receipt " + receiptNo + " from its deposit slip first");
    }
    this.status = to;
    recordReversal(user, date, reason);
  }

  /**
   * Records money applied to debit notes.
   *
   * @param value amount applied
   */
  public void apply(BigDecimal value) {
    if (value.signum() <= 0 || value.compareTo(unapplied()) > 0) {
      throw new BusinessRuleException(
          "OVER_ALLOCATION",
          "Allocation "
              + value
              + " exceeds the unapplied amount "
              + unapplied()
              + " of "
              + receiptNo);
    }
    appliedAmount = appliedAmount.add(value);
  }

  /**
   * Allocates the next sequence number for applying unapplied money (idempotency key).
   *
   * @return sequence number starting at 1
   */
  public int nextApplicationNo() {
    applicationCount++;
    return applicationCount;
  }

  /**
   * Amount not applied to any debit note (on account).
   *
   * @return amount minus applied amount
   */
  public BigDecimal unapplied() {
    return amount.subtract(appliedAmount);
  }

  /**
   * Puts the receipt on a deposit slip.
   *
   * @param slipId slip
   */
  public void addToSlip(Long slipId) {
    requireStatus(ReceiptStatus.APPROVED);
    if (depositStatus != DepositStatus.UNDEPOSITED) {
      throw new BusinessRuleException(
          "RECEIPT_NOT_UNDEPOSITED", "Receipt " + receiptNo + " is " + depositStatus);
    }
    this.depositStatus = DepositStatus.IN_SLIP;
    this.depositSlipId = slipId;
  }

  /** Takes the receipt off a cancelled deposit slip. */
  public void removeFromSlip() {
    this.depositStatus = DepositStatus.UNDEPOSITED;
    this.depositSlipId = null;
  }

  /**
   * Records that the money reached the bank.
   *
   * @param date deposit date
   */
  public void markDeposited(LocalDate date) {
    this.depositStatus = DepositStatus.DEPOSITED;
    this.depositedOn = date;
  }

  private void requireChecker(String checker) {
    if (Objects.equals(getCreatedBy(), checker)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A receipt cannot be approved by the user who entered it");
    }
  }

  private void recordReversal(String user, LocalDate date, String reason) {
    this.reversedBy = user;
    this.reversalDate = date;
    this.reversalReason = reason;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getReceiptNo() {
    return receiptNo;
  }

  public LocalDate getReceiptDate() {
    return receiptDate;
  }

  public PayerType getPayerType() {
    return payerType;
  }

  public Long getPartyId() {
    return partyId;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public String getPayerName() {
    return payerName;
  }

  public String getDepartment() {
    return department;
  }

  public ReceiptMode getMode() {
    return mode;
  }

  public String getInstrumentNo() {
    return instrumentNo;
  }

  public LocalDate getInstrumentDate() {
    return instrumentDate;
  }

  public String getDraweeBank() {
    return draweeBank;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getExchangeRate() {
    return exchangeRate;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getBaseAmount() {
    return baseAmount;
  }

  public BigDecimal getAppliedAmount() {
    return appliedAmount;
  }

  public String getBankAccountCode() {
    return bankAccountCode;
  }

  public String getIncomeAccountCode() {
    return incomeAccountCode;
  }

  public AllocationMethod getAllocationMethod() {
    return allocationMethod;
  }

  public String getNarration() {
    return narration;
  }

  public ReceiptStatus getStatus() {
    return status;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public Long getCreditItemId() {
    return creditItemId;
  }

  public LocalDate getReversalDate() {
    return reversalDate;
  }

  public String getReversalReason() {
    return reversalReason;
  }

  public String getReversedBy() {
    return reversedBy;
  }

  public DepositStatus getDepositStatus() {
    return depositStatus;
  }

  public Long getDepositSlipId() {
    return depositSlipId;
  }

  public LocalDate getDepositedOn() {
    return depositedOn;
  }

  public Long getPdcId() {
    return pdcId;
  }

  public List<ReceiptAllocation> getAllocations() {
    return allocations;
  }
}
