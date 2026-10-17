package com.iortatechnxt.brokerverse.opsledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One change of an invoice's statuses, flags or lock, with who, when and why (RMTID.032 hold and
 * remittance status history, RMTID.040 lock log).
 */
@Entity
@Table(name = "ops_invoice_status_change")
public class OpsInvoiceStatusChange {

  private static final int MAX_REASON = 250;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "invoice_id", nullable = false, updatable = false)
  private Long invoiceId;

  @Column(nullable = false, length = 30, updatable = false)
  private String field;

  @Column(name = "from_value", length = 40, updatable = false)
  private String fromValue;

  @Column(name = "to_value", length = 40, updatable = false)
  private String toValue;

  @Column(nullable = false, length = 30, updatable = false)
  private String module;

  @Column(length = MAX_REASON, updatable = false)
  private String reason;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private Instant changedAt;

  @Column(name = "changed_by", nullable = false, length = 50, updatable = false)
  private String changedBy;

  protected OpsInvoiceStatusChange() {}

  /**
   * Records a change.
   *
   * @param invoiceId invoice
   * @param change field, old and new value
   * @param module module that changed it
   * @param reason reason, may be null
   * @param by user
   * @param at time
   */
  public OpsInvoiceStatusChange(
      Long invoiceId, Change change, String module, String reason, String by, Instant at) {
    this.invoiceId = invoiceId;
    this.field = change.field();
    this.fromValue = change.from();
    this.toValue = change.to();
    this.module = module;
    this.reason =
        reason == null || reason.length() <= MAX_REASON ? reason : reason.substring(0, MAX_REASON);
    this.changedBy = by;
    this.changedAt = at;
  }

  public Long getId() {
    return id;
  }

  public Long getInvoiceId() {
    return invoiceId;
  }

  public String getField() {
    return field;
  }

  public String getFromValue() {
    return fromValue;
  }

  public String getToValue() {
    return toValue;
  }

  public String getModule() {
    return module;
  }

  public String getReason() {
    return reason;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  public String getChangedBy() {
    return changedBy;
  }

  /**
   * A field change.
   *
   * @param field field (e.g. REMITTANCE_STATUS, HOLD, LOCK)
   * @param from old value
   * @param to new value
   */
  public record Change(String field, String from, String to) {}
}
