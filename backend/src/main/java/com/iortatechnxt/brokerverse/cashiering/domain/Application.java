package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ApplicationSource;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Money of a receipt (or of an unapplied item) applied to one booked invoice, split by premium
 * component in the hierarchy (CSHID.020/022). Posted as {@code OPS_PAYMENT_APPLY} with source
 * reference {@code APP:<id>} and recorded on the invoice ledger; a reversal re-posts it negative.
 */
@Entity
@Table(name = "csh_application")
public class Application extends BaseEntity {

  /** Active status. */
  public static final String ACTIVE = "ACTIVE";

  /** Reversed status. */
  public static final String REVERSED = "REVERSED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(name = "receipt_id", updatable = false)
  private Long receiptId;

  @Column(name = "unapplied_id", updatable = false)
  private Long unappliedId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private ApplicationSource source;

  @Column(name = "source_ref", length = 80, updatable = false)
  private String sourceRef;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(name = "realized_commission", nullable = false, precision = 19, scale = 2)
  private BigDecimal realizedCommission;

  @Column(name = "realized_vat", nullable = false, precision = 19, scale = 2)
  private BigDecimal realizedVat;

  @Column(name = "value_date", nullable = false, updatable = false)
  private LocalDate valueDate;

  @Column(nullable = false, length = 10)
  private String status = ACTIVE;

  @Column(name = "reversed_at")
  private Instant reversedAt;

  @Column(name = "reversal_ref", length = 80)
  private String reversalRef;

  @Column(name = "reversal_reason", length = 250)
  private String reversalReason;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Column(name = "reversal_journal_no", length = 40)
  private String reversalJournalNo;

  @ElementCollection
  @CollectionTable(
      name = "csh_application_line",
      joinColumns = @JoinColumn(name = "application_id"))
  @OrderColumn(name = "seq")
  private final List<ApplicationLine> lines = new ArrayList<>();

  protected Application() {}

  /**
   * Creates an active application.
   *
   * @param target invoice, ARN and client
   * @param origin receipt or unapplied item, source and reference
   * @param allocation amount per component in hierarchy order
   * @param valueDate value date
   */
  public Application(
      Target target,
      Origin origin,
      Map<LedgerComponent, BigDecimal> allocation,
      LocalDate valueDate) {
    this.companyId = target.companyId();
    this.invoiceNo = target.invoiceNo();
    this.arn = target.arn();
    this.clientCode = target.clientCode();
    this.receiptId = origin.receiptId();
    this.unappliedId = origin.unappliedId();
    this.source = origin.source();
    this.sourceRef = origin.sourceRef();
    this.valueDate = valueDate;
    allocation.forEach((component, value) -> lines.add(new ApplicationLine(component, value)));
    this.amount = allocation.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    this.realizedCommission = BigDecimal.ZERO.setScale(2);
    this.realizedVat = BigDecimal.ZERO.setScale(2);
  }

  /**
   * Records the commission realized with the application and its journal.
   *
   * @param commission realized commission
   * @param vat realized VAT on commission
   * @param batchNo journal batch
   */
  public void posted(BigDecimal commission, BigDecimal vat, String batchNo) {
    this.realizedCommission = commission;
    this.realizedVat = vat;
    this.journalBatchNo = batchNo;
  }

  /**
   * Reverses the application (receipt cancelled, re-application, disposition reversal).
   *
   * @param ref reversal source reference
   * @param reason reason
   * @param at time
   * @param batchNo reversal journal
   */
  public void reverse(String ref, String reason, Instant at, String batchNo) {
    if (!isActive()) {
      throw new BusinessRuleException(
          "APPLICATION_ALREADY_REVERSED", "Application " + getId() + " is already reversed");
    }
    this.status = REVERSED;
    this.reversalRef = ref;
    this.reversalReason = reason;
    this.reversedAt = at;
    this.reversalJournalNo = batchNo;
  }

  /**
   * Whether the application is in force.
   *
   * @return true when active
   */
  public boolean isActive() {
    return ACTIVE.equals(status);
  }

  /**
   * Amount per component.
   *
   * @return allocation in the order it was applied (hierarchy)
   */
  public Map<LedgerComponent, BigDecimal> allocation() {
    Map<LedgerComponent, BigDecimal> map = new LinkedHashMap<>();
    lines.forEach(l -> map.merge(l.getComponent(), l.getAmount(), BigDecimal::add));
    return map;
  }

  /**
   * The ledger and journal source reference of the application.
   *
   * @return {@code APP:<id>}
   */
  public String reference() {
    return "APP:" + getId();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getArn() {
    return arn;
  }

  public String getClientCode() {
    return clientCode;
  }

  public Long getReceiptId() {
    return receiptId;
  }

  public Long getUnappliedId() {
    return unappliedId;
  }

  public ApplicationSource getSource() {
    return source;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getRealizedCommission() {
    return realizedCommission;
  }

  public BigDecimal getRealizedVat() {
    return realizedVat;
  }

  public LocalDate getValueDate() {
    return valueDate;
  }

  public String getStatus() {
    return status;
  }

  public Instant getReversedAt() {
    return reversedAt;
  }

  public String getReversalRef() {
    return reversalRef;
  }

  public String getReversalReason() {
    return reversalReason;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public String getReversalJournalNo() {
    return reversalJournalNo;
  }

  public List<ApplicationLine> getLines() {
    return lines;
  }

  /**
   * The invoice an application goes to.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param arn account
   * @param clientCode client
   */
  public record Target(Long companyId, String invoiceNo, String arn, String clientCode) {}

  /**
   * Where the applied money comes from.
   *
   * @param receiptId receipt, may be null
   * @param unappliedId unapplied item, may be null
   * @param source source
   * @param sourceRef source reference (payment, disposition, adjustment request)
   */
  public record Origin(
      Long receiptId, Long unappliedId, ApplicationSource source, String sourceRef) {}
}
