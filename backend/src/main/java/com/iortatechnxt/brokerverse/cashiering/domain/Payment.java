package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.MatchCategory;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentChannel;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A payment received through a channel (file row, over the counter, matured PDC, check pick-up),
 * with the raw references it carried and the matching result (CSHID.008/020, BRQID.006).
 */
@Entity
@Table(name = "csh_payment")
public class Payment extends BaseEntity {

  private static final int MESSAGE_MAX = 250;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(name = "payment_no", nullable = false, length = 30, updatable = false)
  private String paymentNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private PaymentChannel channel;

  @Column(name = "batch_ref", length = 40, updatable = false)
  private String batchRef;

  @Column(name = "source_key", nullable = false, length = 120, updatable = false)
  private String sourceKey;

  @Column(name = "row_no", updatable = false)
  private Integer rowNo;

  @Column(length = 80, updatable = false)
  private String reference;

  @Column(name = "other_refs", length = 300, updatable = false)
  private String otherRefs;

  @Column(name = "payor_name", nullable = false, length = 250, updatable = false)
  private String payorName;

  @Column(name = "assured_name", length = 250, updatable = false)
  private String assuredName;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(name = "value_date", nullable = false, updatable = false)
  private LocalDate valueDate;

  @Column(name = "paid_time", length = 10, updatable = false)
  private String paidTime;

  @Column(name = "late_deposit", nullable = false, updatable = false)
  private boolean lateDeposit;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_mode", nullable = false, length = 20, updatable = false)
  private PaymentMode mode;

  @Column(name = "check_no", length = 40, updatable = false)
  private String checkNo;

  @Column(name = "check_bank", length = 60, updatable = false)
  private String checkBank;

  @Enumerated(EnumType.STRING)
  @Column(name = "match_category", nullable = false, length = 30)
  private MatchCategory matchCategory = MatchCategory.UNAPPLIED_NO_MATCH;

  @Column(name = "matched_ref", length = 40)
  private String matchedRef;

  @Column(name = "receipt_id")
  private Long receiptId;

  @Column(name = "applied_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal appliedAmount = BigDecimal.ZERO.setScale(2);

  @Column(name = "unapplied_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal unappliedAmount = BigDecimal.ZERO.setScale(2);

  @Column(length = 250)
  private String message;

  protected Payment() {}

  /**
   * Creates a received payment.
   *
   * @param companyId company
   * @param branchId receiving branch
   * @param paymentNo payment number
   * @param intake channel, references, payor, amount and tender
   */
  public Payment(Long companyId, Long branchId, String paymentNo, PaymentIntake intake) {
    this.companyId = companyId;
    this.branchId = branchId;
    this.paymentNo = paymentNo;
    this.channel = intake.channel();
    this.batchRef = intake.batchRef();
    this.sourceKey = intake.sourceKey();
    this.rowNo = intake.rowNo();
    this.reference = intake.reference();
    this.otherRefs = intake.otherRefs().isEmpty() ? null : String.join(",", intake.otherRefs());
    this.payorName = intake.payorName();
    this.assuredName = intake.assuredName();
    this.amount = intake.amount();
    this.currency = intake.currency();
    this.valueDate = intake.valueDate();
    this.paidTime = intake.paidTime();
    this.lateDeposit = intake.lateDeposit();
    this.mode = intake.mode();
    this.checkNo = intake.checkNo();
    this.checkBank = intake.checkBank();
  }

  /**
   * Records the receipt issued for the payment.
   *
   * @param id receipt id
   */
  public void receipted(Long id) {
    this.receiptId = id;
  }

  /**
   * Records the matching result.
   *
   * @param category category
   * @param ref invoice or ARN matched, may be null
   * @param applied amount applied
   * @param text message
   */
  public void matched(MatchCategory category, String ref, BigDecimal applied, String text) {
    this.matchCategory = category;
    this.matchedRef = ref;
    this.appliedAmount = applied;
    this.unappliedAmount = amount.subtract(applied);
    this.message =
        text == null || text.length() <= MESSAGE_MAX ? text : text.substring(0, MESSAGE_MAX);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getPaymentNo() {
    return paymentNo;
  }

  public PaymentChannel getChannel() {
    return channel;
  }

  public String getBatchRef() {
    return batchRef;
  }

  public String getSourceKey() {
    return sourceKey;
  }

  public Integer getRowNo() {
    return rowNo;
  }

  public String getReference() {
    return reference;
  }

  public String getOtherRefs() {
    return otherRefs;
  }

  public String getPayorName() {
    return payorName;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public LocalDate getValueDate() {
    return valueDate;
  }

  public String getPaidTime() {
    return paidTime;
  }

  public boolean isLateDeposit() {
    return lateDeposit;
  }

  public PaymentMode getMode() {
    return mode;
  }

  public String getCheckNo() {
    return checkNo;
  }

  public String getCheckBank() {
    return checkBank;
  }

  public MatchCategory getMatchCategory() {
    return matchCategory;
  }

  public String getMatchedRef() {
    return matchedRef;
  }

  public Long getReceiptId() {
    return receiptId;
  }

  public BigDecimal getAppliedAmount() {
    return appliedAmount;
  }

  public BigDecimal getUnappliedAmount() {
    return unappliedAmount;
  }

  public String getMessage() {
    return message;
  }
}
