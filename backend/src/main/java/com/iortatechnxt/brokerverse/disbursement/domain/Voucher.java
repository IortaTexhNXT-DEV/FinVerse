package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PostingStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.VoucherStage;
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

/**
 * A disbursement voucher (DV, DIS 2.7.0-2.7.12, 2.13-2.21): the payment of one request to one payee
 * with its mode, paying bank account, gross / EWT / net, purpose, root invoice (DIS 3.27.2) and the
 * editable proforma entry (DIS 2.7.6), moved through workflow {@code DISB_VOUCHER}. Only an
 * unposted voucher can be changed (DIS 3.27.0 addendum); approval posts it, cancellation of an
 * approved voucher reverses it.
 */
@Entity
@Table(name = "dsb_voucher")
public class Voucher extends BaseEntity {

  private static final int MAX_ERROR = 1000;
  private static final int MAX_REASON = 250;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id")
  private Long branchId;

  @Column(name = "dv_no", nullable = false, length = 30, updatable = false)
  private String dvNo;

  @Column(name = "request_id", nullable = false, updatable = false)
  private Long requestId;

  @Column(name = "payee_id", nullable = false, updatable = false)
  private Long payeeId;

  @Column(name = "payee_code", nullable = false, length = 30, updatable = false)
  private String payeeCode;

  @Column(name = "payee_name", nullable = false, length = 250)
  private String payeeName;

  @Column(name = "payee_class", nullable = false, length = 20)
  private String payeeClass;

  @Column(name = "disbursement_type", nullable = false, length = 30, updatable = false)
  private String disbursementType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DisbursementMode mode;

  @Column(name = "bank_account_id")
  private Long bankAccountId;

  @Column(name = "payee_account_id")
  private Long payeeAccountId;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(name = "exchange_rate", precision = 19, scale = 8)
  private BigDecimal exchangeRate;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal gross;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal ewt = BigDecimal.ZERO;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal net;

  @Column(length = 500)
  private String purpose;

  @Column(name = "root_invoice_no", length = 40, updatable = false)
  private String rootInvoiceNo;

  @Column(name = "value_date")
  private LocalDate valueDate;

  @Column(name = "cost_center", length = 20)
  private String costCenter;

  @Column(name = "expense_account", length = 30)
  private String expenseAccount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private VoucherStage stage = VoucherStage.IN_PROCESS;

  @Column(name = "auto_created", nullable = false, updatable = false)
  private boolean autoCreated;

  @Column(name = "proforma_edited", nullable = false)
  private boolean proformaEdited;

  @Enumerated(EnumType.STRING)
  @Column(name = "posting_status", nullable = false, length = 20)
  private PostingStatus postingStatus = PostingStatus.NOT_POSTED;

  @Column(name = "posting_error", length = MAX_ERROR)
  private String postingError;

  @Column(name = "journal_no", length = 30)
  private String journalNo;

  @Column(name = "cancel_journal_no", length = 30)
  private String cancelJournalNo;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "reviewed_by", length = 50)
  private String reviewedBy;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "cancel_reason", length = MAX_REASON)
  private String cancelReason;

  @Column(name = "cancelled_by", length = 50)
  private String cancelledBy;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "eod_run_id")
  private Long eodRunId;

  @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("lineNo")
  private final List<VoucherLine> lines = new ArrayList<>();

  protected Voucher() {}

  /**
   * A new voucher IN_PROCESS for a request.
   *
   * @param dvNo voucher number ({@code DV-<yyyy>-nnnnnn}, DIS 2.7.5)
   * @param facts request, payee and amounts
   */
  public Voucher(String dvNo, VoucherFacts facts) {
    this.dvNo = dvNo;
    this.companyId = facts.companyId();
    this.branchId = facts.branchId();
    this.requestId = facts.requestId();
    this.payeeId = facts.payeeId();
    this.payeeCode = facts.payeeCode();
    this.payeeName = facts.payeeName();
    this.payeeClass = facts.payeeClass();
    this.disbursementType = facts.disbursementType();
    this.currency = facts.currency();
    this.gross = Money.round(facts.gross());
    this.net = this.gross;
    this.rootInvoiceNo = facts.rootInvoiceNo();
    this.autoCreated = facts.autoCreated();
  }

  /**
   * Sets the processing terms (DIS 2.7.1-2.7.4); the EWT cannot exceed the gross.
   *
   * @param terms terms
   */
  public void setTerms(VoucherTerms terms) {
    requireEditable();
    BigDecimal withheld = Money.round(Money.nz(terms.ewt()));
    if (withheld.signum() < 0 || withheld.compareTo(gross) >= 0) {
      throw new BusinessRuleException(
          "DV_EWT", "The withholding tax must be at least zero and below the gross amount");
    }
    mode = terms.mode();
    bankAccountId = terms.bankAccountId();
    payeeAccountId = terms.payeeAccountId();
    ewt = withheld;
    net = gross.subtract(withheld);
    purpose = terms.purpose();
    valueDate = terms.valueDate();
    costCenter = terms.costCenter();
    expenseAccount = terms.expenseAccount();
  }

  /**
   * Replaces the proforma lines (DIS 2.7.6).
   *
   * @param newLines lines in order
   * @param edited whether a user changed the rule output
   */
  public void replaceLines(List<VoucherLine.LineValues> newLines, boolean edited) {
    requireEditable();
    lines.clear();
    int no = 1;
    for (VoucherLine.LineValues v : newLines) {
      lines.add(new VoucherLine(this, no++, v));
    }
    proformaEdited = edited;
  }

  /**
   * Fields still missing before the voucher can move on (DIS 2.7.4).
   *
   * @return labels of the missing or wrong fields, empty when complete
   */
  public List<String> missing() {
    List<String> out = new ArrayList<>();
    if (mode == null) {
      out.add("mode of payment");
    }
    if (bankAccountId == null) {
      out.add("paying bank account");
    }
    if (valueDate == null) {
      out.add("value date");
    }
    if (purpose == null || purpose.isBlank()) {
      out.add("purpose");
    }
    if (lines.isEmpty()) {
      out.add("proforma entry");
    } else if (debits().compareTo(credits()) != 0) {
      out.add("balanced proforma entry");
    }
    return out;
  }

  /**
   * Total debits of the proforma.
   *
   * @return amount
   */
  public BigDecimal debits() {
    return total(BalanceSide.DEBIT);
  }

  /**
   * Total credits of the proforma.
   *
   * @return amount
   */
  public BigDecimal credits() {
    return total(BalanceSide.CREDIT);
  }

  private BigDecimal total(BalanceSide side) {
    return lines.stream()
        .filter(l -> l.getSide() == side)
        .map(VoucherLine::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void requireEditable() {
    if (!stage.editable()) {
      throw new BusinessRuleException(
          "DV_NOT_EDITABLE", "DV " + dvNo + " is " + stage + " and can no longer be changed");
    }
  }

  /**
   * Mirrors the workflow stage.
   *
   * @param next stage
   */
  public void markStage(VoucherStage next) {
    stage = next;
  }

  /**
   * Records who submitted the voucher for review or approval.
   *
   * @param user processor
   */
  public void submittedBy(String user) {
    submittedBy = user;
  }

  /**
   * Records the checker.
   *
   * @param user team leader
   */
  public void reviewedBy(String user) {
    reviewedBy = user;
  }

  /**
   * The approval was posted (DIS 2.19.0).
   *
   * @param batchNo journal batch
   * @param rate exchange rate used
   * @param user approver
   * @param at time
   */
  public void posted(String batchNo, BigDecimal rate, String user, Instant at) {
    journalNo = batchNo;
    exchangeRate = rate;
    approvedBy = user;
    approvedAt = at;
    postingStatus = PostingStatus.POSTED;
    postingError = null;
  }

  /**
   * The approval could not be posted: the voucher keeps its stage and is listed as unregularised
   * (DIS 3.27.0) until it is approved again.
   *
   * @param error error
   */
  public void postingFailed(String error) {
    postingStatus = PostingStatus.FAILED;
    postingError = cut(error, MAX_ERROR);
  }

  /**
   * Cancellation of the voucher (DIS 2.9.0, 2.18.0, 2.20.0).
   *
   * @param reason reason
   * @param user user
   * @param at time
   */
  public void cancelled(String reason, String user, Instant at) {
    cancelReason = cut(reason, MAX_REASON);
    cancelledBy = user;
    cancelledAt = at;
  }

  /**
   * The approval entry was reversed (DIS 2.20.0).
   *
   * @param batchNo reversal journal
   */
  public void reversed(String batchNo) {
    cancelJournalNo = batchNo;
    postingStatus = PostingStatus.REVERSED;
    postingError = null;
  }

  /**
   * The reversal failed: listed as unregularised.
   *
   * @param error error
   */
  public void reversalFailed(String error) {
    postingStatus = PostingStatus.REVERSAL_FAILED;
    postingError = cut(error, MAX_ERROR);
  }

  /**
   * Frozen in an end-of-day run (DIS 2.16.0).
   *
   * @param runId run
   */
  public void inEod(Long runId) {
    eodRunId = runId;
  }

  private static String cut(String text, int max) {
    return text == null || text.length() <= max ? text : text.substring(0, max);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getDvNo() {
    return dvNo;
  }

  public Long getRequestId() {
    return requestId;
  }

  public Long getPayeeId() {
    return payeeId;
  }

  public String getPayeeCode() {
    return payeeCode;
  }

  public String getPayeeName() {
    return payeeName;
  }

  public String getPayeeClass() {
    return payeeClass;
  }

  public String getDisbursementType() {
    return disbursementType;
  }

  public DisbursementMode getMode() {
    return mode;
  }

  public Long getBankAccountId() {
    return bankAccountId;
  }

  public Long getPayeeAccountId() {
    return payeeAccountId;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getExchangeRate() {
    return exchangeRate;
  }

  public BigDecimal getGross() {
    return gross;
  }

  public BigDecimal getEwt() {
    return ewt;
  }

  public BigDecimal getNet() {
    return net;
  }

  public String getPurpose() {
    return purpose;
  }

  public String getRootInvoiceNo() {
    return rootInvoiceNo;
  }

  public LocalDate getValueDate() {
    return valueDate;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public String getExpenseAccount() {
    return expenseAccount;
  }

  public VoucherStage getStage() {
    return stage;
  }

  public boolean isAutoCreated() {
    return autoCreated;
  }

  public boolean isProformaEdited() {
    return proformaEdited;
  }

  public PostingStatus getPostingStatus() {
    return postingStatus;
  }

  public String getPostingError() {
    return postingError;
  }

  public String getJournalNo() {
    return journalNo;
  }

  public String getCancelJournalNo() {
    return cancelJournalNo;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public String getReviewedBy() {
    return reviewedBy;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public String getCancelledBy() {
    return cancelledBy;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public Long getEodRunId() {
    return eodRunId;
  }

  public List<VoucherLine> getLines() {
    return List.copyOf(lines);
  }

  /**
   * What a voucher is created from.
   *
   * @param companyId company
   * @param branchId branch of the posting
   * @param requestId payment request
   * @param payeeId payee
   * @param payeeCode payee party code
   * @param payeeName payee name
   * @param payeeClass payee class
   * @param disbursementType disbursement type (the component of the {@code DISB_VOUCHER} event)
   * @param currency currency
   * @param gross gross amount
   * @param rootInvoiceNo root invoice
   * @param autoCreated built automatically from a gateway request (DIS 3.25.0)
   */
  public record VoucherFacts(
      Long companyId,
      Long branchId,
      Long requestId,
      Long payeeId,
      String payeeCode,
      String payeeName,
      String payeeClass,
      String disbursementType,
      String currency,
      BigDecimal gross,
      String rootInvoiceNo,
      boolean autoCreated) {}

  /**
   * Processing terms of a voucher (DIS 2.7.1-2.7.4, 3.30.0).
   *
   * @param mode mode of payment
   * @param bankAccountId paying bank account
   * @param payeeAccountId payee bank account (credit modes), may be null
   * @param ewt expanded withholding tax withheld at payment (AQ13)
   * @param purpose purpose
   * @param valueDate value date
   * @param costCenter cost centre of expense lines
   * @param expenseAccount expense account of an OTHER payment
   */
  public record VoucherTerms(
      DisbursementMode mode,
      Long bankAccountId,
      Long payeeAccountId,
      BigDecimal ewt,
      String purpose,
      LocalDate valueDate,
      String costCenter,
      String expenseAccount) {}
}
