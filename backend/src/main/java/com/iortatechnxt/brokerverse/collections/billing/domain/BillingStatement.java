package com.iortatechnxt.brokerverse.collections.billing.domain;

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
import org.hibernate.Hibernate;

/**
 * A statement of account (SOA) for one billing cycle of an installment plan (BRCLXN.058/060, CQ18):
 * the client and account, the cycle's coverage period and due date, the installment of the cycle
 * and any earlier one still unpaid, the totals, the document rendered from the template {@code
 * CLX_SOA} and when it was sent. Billing is monitoring only: an SOA creates no receivable and no
 * commission billing.
 */
@Entity
@Table(name = "clx_billing_statement")
public class BillingStatement extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "soa_no", nullable = false, length = 40, updatable = false)
  private String soaNo;

  @Column(name = "plan_id", nullable = false, updatable = false)
  private Long planId;

  @Column(name = "cycle_seq", nullable = false, updatable = false)
  private int cycleSeq;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(name = "assured_name", nullable = false, length = 250, updatable = false)
  private String assuredName;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(nullable = false, length = 40, updatable = false)
  private String frequency;

  @Column(name = "cycle_from", nullable = false, updatable = false)
  private LocalDate cycleFrom;

  @Column(name = "cycle_to", nullable = false, updatable = false)
  private LocalDate cycleTo;

  @Column(name = "due_date", nullable = false, updatable = false)
  private LocalDate dueDate;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal total;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal paid;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal balance;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatementStatus status = StatementStatus.GENERATED;

  @Column(name = "template_code", length = 40)
  private String templateCode;

  @Column(name = "template_version")
  private Integer templateVersion;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "sent_to", length = 500)
  private String sentTo;

  @Column(name = "message_id")
  private Long messageId;

  @Column(name = "cancel_reason", length = 500)
  private String cancelReason;

  @OneToMany(mappedBy = "statement", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("lineNo")
  private final List<BillingStatementLine> lines = new ArrayList<>();

  protected BillingStatement() {}

  /**
   * A new statement.
   *
   * @param header plan, cycle and account
   * @param facts lines (at least one)
   * @return the statement
   */
  public static BillingStatement create(Header header, List<BillingStatementLine.Facts> facts) {
    BillingStatement s = new BillingStatement();
    s.companyId = header.companyId();
    s.soaNo = header.soaNo();
    s.planId = header.planId();
    s.cycleSeq = header.cycleSeq();
    s.arn = header.arn();
    s.clientCode = header.clientCode();
    s.assuredName = header.assuredName();
    s.currency = header.currency();
    s.frequency = header.frequency();
    s.cycleFrom = header.cycleFrom();
    s.cycleTo = header.cycleTo();
    s.dueDate = header.dueDate();
    BigDecimal amount = BigDecimal.ZERO;
    BigDecimal settled = BigDecimal.ZERO;
    int n = 1;
    for (BillingStatementLine.Facts f : facts) {
      s.lines.add(new BillingStatementLine(s, n++, f));
      amount = amount.add(f.amount());
      settled = settled.add(f.paid());
    }
    s.total = amount;
    s.paid = settled;
    s.balance = amount.subtract(settled);
    return s;
  }

  /**
   * Records the template version the document was rendered from.
   *
   * @param code template code
   * @param version template version
   */
  public void renderedWith(String code, int version) {
    this.templateCode = code;
    this.templateVersion = version;
  }

  /**
   * Records that the statement was e-mailed.
   *
   * @param to recipients
   * @param message outbound message
   * @param at time
   */
  public void sent(String to, Long message, Instant at) {
    requireLive();
    this.status = StatementStatus.SENT;
    this.sentTo = to;
    this.messageId = message;
    this.sentAt = at;
  }

  /**
   * Cancels the statement so the cycle can be billed again.
   *
   * @param reason why
   */
  public void cancel(String reason) {
    requireLive();
    this.status = StatementStatus.CANCELLED;
    this.cancelReason = reason;
  }

  private void requireLive() {
    if (status == StatementStatus.CANCELLED) {
      throw new BusinessRuleException("CLX_SOA_CANCELLED", "Statement " + soaNo + " is cancelled");
    }
  }

  /** Loads the lines (reads outside the persistence context). */
  public void loadLines() {
    Hibernate.initialize(lines);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getSoaNo() {
    return soaNo;
  }

  public Long getPlanId() {
    return planId;
  }

  public int getCycleSeq() {
    return cycleSeq;
  }

  public String getArn() {
    return arn;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public String getCurrency() {
    return currency;
  }

  public String getFrequency() {
    return frequency;
  }

  public LocalDate getCycleFrom() {
    return cycleFrom;
  }

  public LocalDate getCycleTo() {
    return cycleTo;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public BigDecimal getPaid() {
    return paid;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public StatementStatus getStatus() {
    return status;
  }

  public String getTemplateCode() {
    return templateCode;
  }

  public Integer getTemplateVersion() {
    return templateVersion;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public String getSentTo() {
    return sentTo;
  }

  public Long getMessageId() {
    return messageId;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public List<BillingStatementLine> getLines() {
    return lines;
  }

  /** Status of a statement. */
  public enum StatementStatus {
    /** Generated, not sent. */
    GENERATED,
    /** E-mailed to the client. */
    SENT,
    /** Cancelled; the cycle may be billed again. */
    CANCELLED
  }

  /**
   * The facts of a statement.
   *
   * @param companyId company
   * @param soaNo number
   * @param planId installment plan
   * @param cycleSeq billing cycle (installment sequence)
   * @param arn account
   * @param clientCode client
   * @param assuredName assured
   * @param currency currency
   * @param frequency billing frequency
   * @param cycleFrom first day of the cycle
   * @param cycleTo last day of the cycle
   * @param dueDate due date
   */
  public record Header(
      Long companyId,
      String soaNo,
      Long planId,
      int cycleSeq,
      String arn,
      String clientCode,
      String assuredName,
      String currency,
      String frequency,
      LocalDate cycleFrom,
      LocalDate cycleTo,
      LocalDate dueDate) {}
}
