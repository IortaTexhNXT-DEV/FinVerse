package com.iortatechnxt.brokerverse.payables.domain;

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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Payment voucher: pays one or more open CREDIT items (payables) of one party by cheque, bank
 * transfer or post-dated cheque.
 *
 * <p>Lifecycle DRAFT → PENDING_APPROVAL → APPROVED (posted) → VOIDED (reversal of an unpresented
 * cheque or a cancelled PDC); CANCELLED before approval.
 */
@Entity
@Table(name = "pay_voucher")
public class PaymentVoucher extends SubmittableDocument {

  private static final Set<VoucherStatus> EDITABLE = EnumSet.of(VoucherStatus.DRAFT);
  private static final Set<VoucherStatus> APPROVED = EnumSet.of(VoucherStatus.APPROVED);

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "voucher_no", nullable = false, length = 40)
  private String voucherNo;

  @Column(name = "party_id", nullable = false)
  private Long partyId;

  @Column(name = "party_code", nullable = false, length = 30)
  private String partyCode;

  @Column(name = "payee_name", nullable = false, length = 200)
  private String payeeName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PaymentCategory category;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_mode", nullable = false, length = 20)
  private PaymentMode paymentMode;

  @Column(name = "bank_account_id", nullable = false)
  private Long bankAccountId;

  @Column(name = "voucher_date", nullable = false)
  private LocalDate voucherDate;

  @Column(name = "cheque_no", length = 20)
  private String chequeNo;

  @Column(name = "cheque_date")
  private LocalDate chequeDate;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount = BigDecimal.ZERO;

  @Column(name = "base_amount", precision = 19, scale = 2)
  private BigDecimal baseAmount;

  @Column(length = 20)
  private String department;

  @Column(length = 250)
  private String narration;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private VoucherStatus status = VoucherStatus.DRAFT;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Column(name = "open_item_id")
  private Long openItemId;

  @Column(name = "presented_on")
  private LocalDate presentedOn;

  @Column(name = "voided_on")
  private LocalDate voidedOn;

  @Column(name = "void_batch_no", length = 40)
  private String voidBatchNo;

  @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("dueDate, documentNo")
  private final List<VoucherAllocation> allocations = new ArrayList<>();

  protected PaymentVoucher() {}

  /**
   * Creates a draft voucher.
   *
   * @param voucherNo voucher number
   * @param header header values
   */
  public PaymentVoucher(String voucherNo, VoucherHeader header) {
    this.voucherNo = voucherNo;
    this.companyId = header.companyId();
    this.partyId = header.partyId();
    this.partyCode = header.partyCode();
    applyHeader(header);
  }

  /**
   * Updates a draft's header (company and party are immutable).
   *
   * @param header header values
   */
  public void updateHeader(VoucherHeader header) {
    requireStatus(EDITABLE, "edit");
    applyHeader(header);
  }

  /**
   * Removes the items of a draft (flush before adding new ones: an item appears once per voucher).
   */
  public void clearAllocations() {
    requireStatus(EDITABLE, "edit");
    allocations.clear();
  }

  /**
   * Replaces the items paid; the voucher amount is their total.
   *
   * @param values allocations (at least one, distinct items, positive amounts)
   */
  public void replaceAllocations(List<AllocationValues> values) {
    requireStatus(EDITABLE, "edit");
    if (values.isEmpty()) {
      throw new BusinessRuleException("NOTHING_TO_PAY", "Select at least one open item to pay");
    }
    Set<Long> seen = new HashSet<>();
    allocations.clear();
    for (AllocationValues v : values) {
      if (!seen.add(v.openItemId()) || !Money.isPositive(v.amount())) {
        throw new BusinessRuleException(
            "INVALID_ALLOCATION", "Each item may be paid once, with a positive amount");
      }
      allocations.add(new VoucherAllocation(this, v));
    }
    amount =
        Money.round(
            allocations.stream()
                .map(VoucherAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  /**
   * Submits the voucher for approval.
   *
   * @param user maker
   * @param when timestamp
   */
  public void submit(String user, Instant when) {
    requireStatus(EDITABLE, "submit");
    if (allocations.isEmpty()) {
      throw new BusinessRuleException("NOTHING_TO_PAY", "Select at least one open item to pay");
    }
    recordSubmission(user, when);
    status = VoucherStatus.PENDING_APPROVAL;
  }

  /**
   * Approves the voucher once posted.
   *
   * @param checker approving user
   * @param when timestamp
   * @param posting posting results
   */
  public void approve(String checker, Instant when, VoucherPosting posting) {
    requireStatus(EnumSet.of(VoucherStatus.PENDING_APPROVAL), "approve");
    recordApproval(checker, when);
    this.chequeNo = posting.chequeNo();
    this.journalBatchNo = posting.journalBatchNo();
    this.openItemId = posting.openItemId();
    this.baseAmount = posting.baseAmount();
    status = VoucherStatus.APPROVED;
  }

  /**
   * Returns a submitted voucher to the maker.
   *
   * @param checker checker
   * @param reason reason
   */
  public void reject(String checker, String reason) {
    requireStatus(EnumSet.of(VoucherStatus.PENDING_APPROVAL), "reject");
    requireChecker(checker);
    clearSubmission();
    recordReason(reason);
    status = VoucherStatus.DRAFT;
  }

  /**
   * Cancels a voucher that was not approved.
   *
   * @param reason reason
   */
  public void cancel(String reason) {
    requireStatus(EnumSet.of(VoucherStatus.DRAFT, VoucherStatus.PENDING_APPROVAL), "cancel");
    recordReason(reason);
    status = VoucherStatus.CANCELLED;
  }

  /**
   * Confirms that the cheque was presented to the bank (it can no longer be voided).
   *
   * @param date presentation date
   */
  public void markPresented(LocalDate date) {
    requireStatus(APPROVED, "confirm presentation of");
    if (paymentMode != PaymentMode.CHEQUE) {
      throw new BusinessRuleException(
          "NOT_A_CHEQUE", "Only cheque payments are confirmed here; PDCs use the PDC register");
    }
    if (date.isBefore(voucherDate)) {
      throw new BusinessRuleException("INVALID_DATE", "Presentation precedes the voucher date");
    }
    this.presentedOn = date;
  }

  /**
   * Records the reversal of the payment.
   *
   * @param date void date
   * @param batchNo reversal journal
   * @param reason reason
   */
  public void markVoided(LocalDate date, String batchNo, String reason) {
    requireStatus(APPROVED, "void");
    if (presentedOn != null) {
      throw new BusinessRuleException(
          "CHEQUE_PRESENTED", "Cheque " + chequeNo + " was presented on " + presentedOn);
    }
    this.voidedOn = date;
    this.voidBatchNo = batchNo;
    recordReason(reason);
    status = VoucherStatus.VOIDED;
  }

  /**
   * Records the replacement cheque of a post-dated cheque payment.
   *
   * @param newChequeNo replacement cheque number
   * @param newChequeDate replacement cheque date
   */
  public void replaceCheque(String newChequeNo, LocalDate newChequeDate) {
    requireStatus(APPROVED, "replace the cheque of");
    this.chequeNo = newChequeNo;
    this.chequeDate = newChequeDate;
  }

  private void applyHeader(VoucherHeader h) {
    this.branchId = h.branchId();
    this.payeeName = h.payeeName();
    this.category = h.category();
    this.paymentMode = h.mode();
    this.bankAccountId = h.bankAccountId();
    this.voucherDate = h.voucherDate();
    this.chequeDate = h.mode() == PaymentMode.PDC ? h.chequeDate() : h.voucherDate();
    if (chequeDate == null || chequeDate.isBefore(voucherDate)) {
      throw new BusinessRuleException(
          "INVALID_CHEQUE_DATE", "A post-dated cheque needs a cheque date on or after the voucher");
    }
    this.currency = h.currency();
    this.department = h.department();
    this.narration = h.narration();
  }

  private void requireStatus(Set<VoucherStatus> allowed, String action) {
    if (!allowed.contains(status)) {
      throw new BusinessRuleException(
          "INVALID_VOUCHER_STATUS", "Cannot " + action + " voucher " + voucherNo + " in " + status);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getVoucherNo() {
    return voucherNo;
  }

  public Long getPartyId() {
    return partyId;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public String getPayeeName() {
    return payeeName;
  }

  public PaymentCategory getCategory() {
    return category;
  }

  public PaymentMode getPaymentMode() {
    return paymentMode;
  }

  public Long getBankAccountId() {
    return bankAccountId;
  }

  public LocalDate getVoucherDate() {
    return voucherDate;
  }

  public String getChequeNo() {
    return chequeNo;
  }

  public LocalDate getChequeDate() {
    return chequeDate;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getBaseAmount() {
    return baseAmount;
  }

  public String getDepartment() {
    return department;
  }

  public String getNarration() {
    return narration;
  }

  public VoucherStatus getStatus() {
    return status;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public Long getOpenItemId() {
    return openItemId;
  }

  public LocalDate getPresentedOn() {
    return presentedOn;
  }

  public LocalDate getVoidedOn() {
    return voidedOn;
  }

  public String getVoidBatchNo() {
    return voidBatchNo;
  }

  public List<VoucherAllocation> getAllocations() {
    return Collections.unmodifiableList(allocations);
  }
}
