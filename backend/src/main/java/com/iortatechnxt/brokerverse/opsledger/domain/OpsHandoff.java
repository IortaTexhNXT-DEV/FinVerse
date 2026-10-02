package com.iortatechnxt.brokerverse.opsledger.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Work handed over through a port whose owning module is not active (the default adapters of {@code
 * ReceiptIssuer} and {@code UnappliedSink}): an OR to issue or an unapplied item to set up by hand.
 * Listed on the Interfaces screen until someone closes it; never a simulated result.
 */
@Entity
@Table(name = "ops_handoff")
public class OpsHandoff extends BaseEntity {

  private static final int MAX_NOTE = 250;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 40, updatable = false)
  private String port;

  @Column(name = "source_module", nullable = false, length = 30, updatable = false)
  private String sourceModule;

  @Column(name = "source_ref", nullable = false, length = 80, updatable = false)
  private String sourceRef;

  @Column(length = 80, updatable = false)
  private String reference;

  @Column(precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(length = 3, updatable = false)
  private String currency;

  @Column(nullable = false, length = 500, updatable = false)
  private String summary;

  @Column(columnDefinition = "text", updatable = false)
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.OPEN;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Column(name = "closed_by", length = 50)
  private String closedBy;

  @Column(name = "closing_note", length = MAX_NOTE)
  private String closingNote;

  protected OpsHandoff() {}

  /**
   * A hand-off.
   *
   * @param companyId company
   * @param port port name (e.g. ReceiptIssuer)
   * @param spec source, reference, amount, summary and payload
   */
  public OpsHandoff(Long companyId, String port, Spec spec) {
    this.companyId = companyId;
    this.port = port;
    this.sourceModule = spec.sourceModule();
    this.sourceRef = spec.sourceRef();
    this.reference = spec.reference();
    this.amount = spec.amount();
    this.currency = spec.currency();
    this.summary = spec.summary();
    this.payload = spec.payload();
  }

  /**
   * Closes the hand-off once the work was done by hand.
   *
   * @param note what was done
   * @param by user
   * @param at time
   */
  public void close(String note, String by, Instant at) {
    if (status == Status.CLOSED) {
      throw new BusinessRuleException("HANDOFF_CLOSED", "The hand-off is already closed");
    }
    status = Status.CLOSED;
    closingNote = note.length() <= MAX_NOTE ? note : note.substring(0, MAX_NOTE);
    closedBy = by;
    closedAt = at;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getPort() {
    return port;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public String getReference() {
    return reference;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public String getSummary() {
    return summary;
  }

  public String getPayload() {
    return payload;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public String getClosedBy() {
    return closedBy;
  }

  public String getClosingNote() {
    return closingNote;
  }

  /** Status of a hand-off. */
  public enum Status {
    /** Waiting to be done by hand. */
    OPEN,
    /** Done. */
    CLOSED
  }

  /**
   * A hand-off to record.
   *
   * @param sourceModule module asking
   * @param sourceRef its reference (unique per port and module)
   * @param reference business reference (invoice, batch)
   * @param amount amount, may be null
   * @param currency currency, may be null
   * @param summary what has to be done
   * @param payload request details (JSON)
   */
  public record Spec(
      String sourceModule,
      String sourceRef,
      String reference,
      BigDecimal amount,
      String currency,
      String summary,
      String payload) {}
}
