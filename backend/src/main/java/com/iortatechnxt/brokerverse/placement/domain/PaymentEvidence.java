package com.iortatechnxt.brokerverse.placement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The evidence behind a payment gate decision (BRD 2.3.1, BRNB.067/068/114): a matched payment with
 * its source and reference, or a client confirmation with the user and date (created by / at).
 * Placement keeps only the evidence, never receipts or cash entries.
 */
@Entity
@Table(name = "plc_payment_evidence")
public class PaymentEvidence extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "account_id", nullable = false, updatable = false)
  private Long accountId;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30, updatable = false)
  private EvidenceKind kind;

  @Column(nullable = false, length = 30, updatable = false)
  private String source;

  @Column(nullable = false, length = 80, updatable = false)
  private String reference;

  @Column(precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "paid_on")
  private LocalDate paidOn;

  @Column(length = 40)
  private String channel;

  @Column(length = 500)
  private String remarks;

  @Column(name = "attachment_id")
  private Long attachmentId;

  @Column(name = "gate_opened", nullable = false)
  private boolean gateOpened;

  protected PaymentEvidence() {}

  /**
   * Records evidence.
   *
   * @param account company, account id and ARN
   * @param kind kind
   * @param origin source and reference
   * @param detail amount, date, channel, remarks and document
   */
  public PaymentEvidence(
      EvidenceAccount account, EvidenceKind kind, EvidenceOrigin origin, EvidenceDetail detail) {
    this.companyId = account.companyId();
    this.accountId = account.accountId();
    this.arn = account.arn();
    this.kind = kind;
    this.source = origin.source();
    this.reference = origin.reference();
    this.amount = detail.amount();
    this.paidOn = detail.paidOn();
    this.channel = detail.channel();
    this.remarks = detail.remarks();
    this.attachmentId = detail.attachmentId();
  }

  /** Marks this evidence as the one that opened the gate. */
  public void markGateOpened() {
    this.gateOpened = true;
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

  public EvidenceKind getKind() {
    return kind;
  }

  public String getSource() {
    return source;
  }

  public String getReference() {
    return reference;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDate getPaidOn() {
    return paidOn;
  }

  public String getChannel() {
    return channel;
  }

  public String getRemarks() {
    return remarks;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public boolean isGateOpened() {
    return gateOpened;
  }

  /**
   * The account the evidence belongs to.
   *
   * @param companyId company
   * @param accountId account id
   * @param arn Account Reference Number
   */
  public record EvidenceAccount(Long companyId, Long accountId, String arn) {}

  /**
   * Where the evidence comes from.
   *
   * @param source source code (CLPC_REPORT, PAYMENT_REPORT, MANUAL or a confirmation source)
   * @param reference source reference (report line, receipt application, confirmation)
   */
  public record EvidenceOrigin(String source, String reference) {}

  /**
   * Evidence details, all optional.
   *
   * @param amount amount paid
   * @param paidOn payment date
   * @param channel client confirmation channel (list CLIENT_CONFIRMATION_CHANNEL)
   * @param remarks remarks
   * @param attachmentId supporting document on the account
   */
  public record EvidenceDetail(
      BigDecimal amount, LocalDate paidOn, String channel, String remarks, Long attachmentId) {

    /** No details. */
    public static final EvidenceDetail NONE = new EvidenceDetail(null, null, null, null, null);
  }
}
