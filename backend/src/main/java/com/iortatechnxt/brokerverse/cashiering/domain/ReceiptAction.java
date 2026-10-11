package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptActionType;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A cancellation or reinstatement of a receipt (CSHID.001-005) with its own transaction number,
 * reason and, for a reinstatement, the encoded fields of CSHID.005. It follows the workflow {@code
 * OPS_RECEIPT_ACTION} (maker-checker assumed, OQ06); the stage is mirrored here.
 */
@Entity
@Table(name = "csh_receipt_action")
public class ReceiptAction extends BaseEntity {

  /** Stage of a posted action. */
  public static final String POSTED = "POSTED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "receipt_id", nullable = false, updatable = false)
  private Long receiptId;

  @Column(name = "transaction_no", nullable = false, length = 30, updatable = false)
  private String transactionNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private ReceiptActionType action;

  @Column(name = "reason_code", nullable = false, length = 40)
  private String reasonCode;

  @Column(name = "reason_text", length = 250)
  private String reasonText;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "invoice_no", length = 40)
  private String invoiceNo;

  @Column(name = "document_no", length = 40)
  private String documentNo;

  @Column(name = "payor_name", length = 250)
  private String payorName;

  @Column(name = "account_officer", length = 50)
  private String accountOfficer;

  @Column(name = "unit_head", length = 100)
  private String unitHead;

  @Column(name = "team_leader", length = 100)
  private String teamLeader;

  @Column(nullable = false, length = 20)
  private String stage;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  protected ReceiptAction() {}

  /**
   * Creates a requested action.
   *
   * @param receipt receipt
   * @param transactionNo CAN- or RIN- number
   * @param action cancellation or reinstatement
   * @param reason reason code and text
   * @param amount amount cancelled or reinstated
   */
  public ReceiptAction(
      Receipt receipt,
      String transactionNo,
      ReceiptActionType action,
      Reason reason,
      BigDecimal amount) {
    this.companyId = receipt.getCompanyId();
    this.receiptId = receipt.getId();
    this.transactionNo = transactionNo;
    this.action = action;
    this.reasonCode = reason.code();
    this.reasonText = reason.text();
    this.amount = amount;
    this.stage = "REQUESTED";
  }

  /**
   * Records the encoded reinstatement fields (CSHID.005).
   *
   * @param fields invoice, AR / OR number, payor and sales people
   */
  public void encode(ReinstatementFields fields) {
    this.invoiceNo = fields.invoiceNo();
    this.documentNo = fields.documentNo();
    this.payorName = fields.payorName();
    this.accountOfficer = fields.accountOfficer();
    this.unitHead = fields.unitHead();
    this.teamLeader = fields.teamLeader();
  }

  /**
   * Mirrors the workflow stage.
   *
   * @param stageCode stage
   */
  public void markStage(String stageCode) {
    this.stage = stageCode;
  }

  /**
   * Records the approval and the journal.
   *
   * @param by approver
   * @param at time
   * @param batchNo journal batch, may be null
   */
  public void approved(String by, Instant at, String batchNo) {
    this.approvedBy = by;
    this.approvedAt = at;
    this.journalBatchNo = batchNo;
  }

  /**
   * Whether the action is a reinstatement.
   *
   * @return true for full or partial reinstatement
   */
  public boolean isReinstatement() {
    return action != ReceiptActionType.CANCEL;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getReceiptId() {
    return receiptId;
  }

  public String getTransactionNo() {
    return transactionNo;
  }

  public ReceiptActionType getAction() {
    return action;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public String getReasonText() {
    return reasonText;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getDocumentNo() {
    return documentNo;
  }

  public String getPayorName() {
    return payorName;
  }

  public String getAccountOfficer() {
    return accountOfficer;
  }

  public String getUnitHead() {
    return unitHead;
  }

  public String getTeamLeader() {
    return teamLeader;
  }

  public String getStage() {
    return stage;
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

  /**
   * Reason of an action.
   *
   * @param code LOV code (RECEIPT_CANCEL_REASON or REINSTATEMENT_REASON)
   * @param text free text ("Others (specify)")
   */
  public record Reason(String code, String text) {}

  /**
   * Encoded reinstatement fields (CSHID.005).
   *
   * @param invoiceNo invoice number
   * @param documentNo AR number (premium) or OR number (direct payment)
   * @param payorName assured / payor
   * @param accountOfficer account officer (premium)
   * @param unitHead unit head (premium)
   * @param teamLeader team leader (premium)
   */
  public record ReinstatementFields(
      String invoiceNo,
      String documentNo,
      String payorName,
      String accountOfficer,
      String unitHead,
      String teamLeader) {}
}
