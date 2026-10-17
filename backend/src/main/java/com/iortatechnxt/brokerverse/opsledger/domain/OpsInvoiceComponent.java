package com.iortatechnxt.brokerverse.opsledger.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * One component of an Operations invoice with its buckets and outstanding balance (CSHID.022,
 * RMTID.038): {@code balance = booked + adjusted - applied + reversed - remitted - writtenOff}.
 */
@Entity
@Table(name = "ops_invoice_component")
public class OpsInvoiceComponent extends BaseEntity {

  private static final int SCALE = 2;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
  private OpsInvoice invoice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private LedgerComponent component;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal booked = BigDecimal.ZERO.setScale(SCALE);

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal applied = BigDecimal.ZERO.setScale(SCALE);

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal reversed = BigDecimal.ZERO.setScale(SCALE);

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal remitted = BigDecimal.ZERO.setScale(SCALE);

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal adjusted = BigDecimal.ZERO.setScale(SCALE);

  @Column(name = "written_off", nullable = false, precision = 19, scale = 2)
  private BigDecimal writtenOff = BigDecimal.ZERO.setScale(SCALE);

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balance = BigDecimal.ZERO.setScale(SCALE);

  protected OpsInvoiceComponent() {}

  OpsInvoiceComponent(OpsInvoice invoice, LedgerComponent component) {
    this.invoice = invoice;
    this.component = component;
  }

  /**
   * Adds a signed amount to a bucket and recomputes the balance.
   *
   * @param bucket bucket
   * @param amount signed amount
   */
  void move(MovementType.Bucket bucket, BigDecimal amount) {
    BigDecimal a = amount.setScale(SCALE, RoundingMode.HALF_UP);
    switch (bucket) {
      case BOOKED -> booked = booked.add(a);
      case APPLIED -> applied = applied.add(a);
      case REVERSED -> reversed = reversed.add(a);
      case REMITTED -> remitted = remitted.add(a);
      case ADJUSTED -> adjusted = adjusted.add(a);
      default -> writtenOff = writtenOff.add(a);
    }
    balance =
        booked
            .add(adjusted)
            .subtract(applied)
            .add(reversed)
            .subtract(remitted)
            .subtract(writtenOff);
  }

  /**
   * The amount due before any settlement: booked plus adjustments.
   *
   * @return booked + adjusted
   */
  public BigDecimal due() {
    return booked.add(adjusted);
  }

  /**
   * Net applied payments: applied less reversed applications.
   *
   * @return applied - reversed
   */
  public BigDecimal netApplied() {
    return applied.subtract(reversed);
  }

  public LedgerComponent getComponent() {
    return component;
  }

  public BigDecimal getBooked() {
    return booked;
  }

  public BigDecimal getApplied() {
    return applied;
  }

  public BigDecimal getReversed() {
    return reversed;
  }

  public BigDecimal getRemitted() {
    return remitted;
  }

  public BigDecimal getAdjusted() {
    return adjusted;
  }

  public BigDecimal getWrittenOff() {
    return writtenOff;
  }

  public BigDecimal getBalance() {
    return balance;
  }
}
