package com.iortatechnxt.finverse.receivables.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Post-dated cheque received (PDC register).
 *
 * <p>Accounting treatment: memorandum only while the cheque is held (ON_HAND / DUE); nothing is
 * posted to the GL. When the cheque is banked it is converted into an official receipt (mode PDC)
 * that is approved and accounted for like any other receipt; a bounce reverses that receipt.
 */
@Entity
@Table(name = "rcv_pdc")
public class PostDatedCheque extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "pdc_no", nullable = false, length = 40)
  private String pdcNo;

  @Column(name = "received_date", nullable = false)
  private LocalDate receivedDate;

  @Column(name = "party_id", nullable = false)
  private Long partyId;

  @Column(name = "party_code", nullable = false, length = 30)
  private String partyCode;

  @Column(name = "payer_name", nullable = false, length = 200)
  private String payerName;

  @Column(length = 20)
  private String department;

  @Column(name = "cheque_no", nullable = false, length = 40)
  private String chequeNo;

  @Column(name = "cheque_date", nullable = false)
  private LocalDate chequeDate;

  @Column(name = "drawee_bank", nullable = false, length = 120)
  private String draweeBank;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal exchangeRate;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "base_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal baseAmount;

  @Column(name = "bank_account_code", nullable = false, length = 30)
  private String bankAccountCode;

  @Column(name = "debit_item_id")
  private Long debitItemId;

  @Column(length = 250)
  private String narration;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PdcStatus status = PdcStatus.ON_HAND;

  @Column(name = "status_date", nullable = false)
  private LocalDate statusDate;

  @Column(name = "receipt_id")
  private Long receiptId;

  @Column(name = "replaced_by_id")
  private Long replacedById;

  protected PostDatedCheque() {}

  /**
   * Registers a cheque on hand.
   *
   * @param v values
   */
  public PostDatedCheque(PdcValues v) {
    this.companyId = v.companyId();
    this.branchId = v.branchId();
    this.pdcNo = v.pdcNo();
    this.receivedDate = v.receivedDate();
    this.partyId = v.partyId();
    this.partyCode = v.partyCode();
    this.payerName = v.payerName();
    this.department = v.department();
    this.chequeNo = v.chequeNo();
    this.chequeDate = v.chequeDate();
    this.draweeBank = v.draweeBank();
    this.currency = v.currency();
    this.exchangeRate = v.exchangeRate();
    this.amount = v.amount();
    this.baseAmount = v.baseAmount();
    this.bankAccountCode = v.bankAccountCode();
    this.debitItemId = v.debitItemId();
    this.narration = v.narration();
    this.statusDate = v.receivedDate();
  }

  /**
   * Moves the cheque to a new status.
   *
   * @param to new status
   * @param date status date
   * @return previous status
   */
  public PdcStatus transition(PdcStatus to, LocalDate date) {
    if (!status.next().contains(to)) {
      throw new BusinessRuleException(
          "PDC_STATUS", "PDC " + pdcNo + " cannot move from " + status + " to " + to);
    }
    if (date.isBefore(receivedDate)) {
      throw new BusinessRuleException(
          "INVALID_PDC_DATE", "Date cannot precede the received date " + receivedDate);
    }
    PdcStatus previous = status;
    this.status = to;
    this.statusDate = date;
    return previous;
  }

  /**
   * Links the receipt raised when the cheque was banked.
   *
   * @param id receipt id (null when the receipt was rejected)
   */
  public void linkReceipt(Long id) {
    this.receiptId = id;
  }

  /**
   * Links the cheque that replaced this one.
   *
   * @param id replacing PDC
   */
  public void replacedBy(Long id) {
    this.replacedById = id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getPdcNo() {
    return pdcNo;
  }

  public LocalDate getReceivedDate() {
    return receivedDate;
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

  public String getChequeNo() {
    return chequeNo;
  }

  public LocalDate getChequeDate() {
    return chequeDate;
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

  public String getBankAccountCode() {
    return bankAccountCode;
  }

  public Long getDebitItemId() {
    return debitItemId;
  }

  public String getNarration() {
    return narration;
  }

  public PdcStatus getStatus() {
    return status;
  }

  public LocalDate getStatusDate() {
    return statusDate;
  }

  public Long getReceiptId() {
    return receiptId;
  }

  public Long getReplacedById() {
    return replacedById;
  }
}
