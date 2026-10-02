package com.iortatechnxt.brokerverse.commission.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * A commission billing of direct payment accounts to one insurer (CMRID.009-012, workflow {@code
 * OPS_DP_BILLING}): handler, totals, the billing file sent with password protection, the feedback
 * due date in working days ({@code CMR_FEEDBACK_WORKING_DAYS}), the insurer's answer and the
 * commission OR. The stage mirrors the work case.
 */
@Entity
@Table(name = "cmr_billing")
public class DpBilling extends BaseEntity {

  /** Workflow of the billings. */
  public static final String WORKFLOW = "OPS_DP_BILLING";

  /** First stage. */
  public static final String FOR_BILLING = "DP_FOR_BILLING";

  /** Sent, waiting for the insurer (SLA running). */
  public static final String AWAITING = "AWAITING_INSURER";

  /** Approved (at least one account). */
  public static final String APPROVED = "APPROVED";

  /** Commission collected. */
  public static final String COLLECTED = "COLLECTED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "billing_no", nullable = false, length = 30, updatable = false)
  private String billingNo;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(length = 50)
  private String handler;

  @Column(nullable = false, length = 40)
  private String stage;

  @Column(name = "item_count", nullable = false)
  private int itemCount;

  @Column(name = "total_commission", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalCommission = BigDecimal.ZERO;

  @Column(name = "total_vat", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalVat = BigDecimal.ZERO;

  @Column(name = "total_wtax", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalWtax = BigDecimal.ZERO;

  @Column(name = "total_net", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalNet = BigDecimal.ZERO;

  @Column(name = "file_id")
  private Long fileId;

  @Column(name = "file_name", length = 255)
  private String fileName;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "sla_due")
  private LocalDate slaDue;

  @Column(name = "overdue_alerted", nullable = false)
  private boolean overdueAlerted;

  @Column(name = "responded_at")
  private Instant respondedAt;

  @Column(name = "message_id")
  private Long messageId;

  @Column(length = 500)
  private String recipients;

  @Column(name = "or_no", length = 40)
  private String orNo;

  @Column(name = "or_status", length = 20)
  private String orStatus;

  protected DpBilling() {}

  /**
   * A new billing.
   *
   * @param companyId company
   * @param billingNo billing number
   * @param insurerCode insurer
   * @param handler assigned handler
   */
  public DpBilling(Long companyId, String billingNo, String insurerCode, String handler) {
    this.companyId = companyId;
    this.billingNo = billingNo;
    this.insurerCode = insurerCode;
    this.handler = handler;
    this.stage = FOR_BILLING;
  }

  /**
   * Recomputes the totals from the billing's accounts.
   *
   * @param items accounts
   */
  public void total(List<DpItem> items) {
    this.itemCount = items.size();
    this.totalCommission = sum(items, DpItem::getCommission);
    this.totalVat = sum(items, DpItem::getCommissionVat);
    this.totalWtax = sum(items, DpItem::getWtax);
    this.totalNet = sum(items, DpItem::getNetCommission);
  }

  private static BigDecimal sum(List<DpItem> items, Function<DpItem, BigDecimal> amount) {
    return items.stream().map(amount).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Records the sending (CMRID.009/012) and the feedback due date (CMRID.011).
   *
   * @param file billing file
   * @param at time sent
   * @param due feedback due date
   * @param message outbound message and recipients
   */
  public void sent(FileRef file, Instant at, LocalDate due, Sent message) {
    this.fileId = file.id();
    this.fileName = file.name();
    this.sentAt = at;
    this.slaDue = due;
    this.messageId = message.messageId();
    this.recipients = message.recipients();
  }

  /**
   * Records that the insurer answered every account.
   *
   * @param at time
   */
  public void responded(Instant at) {
    this.respondedAt = at;
  }

  /** Records that the feedback delay was flagged (CMRID.011). */
  public void overdueAlerted() {
    this.overdueAlerted = true;
  }

  /**
   * Records the commission OR.
   *
   * @param or OR number, null when handed over
   * @param status ISSUED or DEFERRED
   */
  public void receipt(String or, String status) {
    this.orNo = or;
    this.orStatus = status;
  }

  /**
   * Mirrors the work case stage.
   *
   * @param stageCode stage
   */
  public void mirrorStage(String stageCode) {
    this.stage = stageCode;
  }

  /**
   * Whether the insurer's feedback is late on a date.
   *
   * @param date date
   * @return true when awaiting the insurer past the due date
   */
  public boolean isOverdue(LocalDate date) {
    return AWAITING.equals(stage) && slaDue != null && date.isAfter(slaDue);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBillingNo() {
    return billingNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getHandler() {
    return handler;
  }

  public String getStage() {
    return stage;
  }

  public int getItemCount() {
    return itemCount;
  }

  public BigDecimal getTotalCommission() {
    return totalCommission;
  }

  public BigDecimal getTotalVat() {
    return totalVat;
  }

  public BigDecimal getTotalWtax() {
    return totalWtax;
  }

  public BigDecimal getTotalNet() {
    return totalNet;
  }

  public Long getFileId() {
    return fileId;
  }

  public String getFileName() {
    return fileName;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public LocalDate getSlaDue() {
    return slaDue;
  }

  public boolean isOverdueAlerted() {
    return overdueAlerted;
  }

  public Instant getRespondedAt() {
    return respondedAt;
  }

  public Long getMessageId() {
    return messageId;
  }

  public String getRecipients() {
    return recipients;
  }

  public String getOrNo() {
    return orNo;
  }

  public String getOrStatus() {
    return orStatus;
  }

  /**
   * The stored billing file.
   *
   * @param id repository file
   * @param name file name
   */
  public record FileRef(Long id, String name) {}

  /**
   * The e-mail sent.
   *
   * @param messageId outbound message
   * @param recipients recipients
   */
  public record Sent(Long messageId, String recipients) {}
}
