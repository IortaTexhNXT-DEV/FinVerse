package com.iortatechnxt.brokerverse.frbs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A fully paid invoice counted in a service-fee line (FRBS 2.10.0). An invoice is in one live run
 * only; cancelling or recomputing the run frees it.
 */
@Entity
@Table(name = "frbs_service_fee_item")
public class ServiceFeeItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "run_id", nullable = false, updatable = false)
  private Long runId;

  @Column(name = "line_id", nullable = false, updatable = false)
  private Long lineId;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "root_invoice_no", length = 40, updatable = false)
  private String rootInvoiceNo;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(name = "assured_name", nullable = false, length = 250, updatable = false)
  private String assuredName;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "market_segment", length = 40, updatable = false)
  private String marketSegment;

  @Column(name = "paid_on", nullable = false, updatable = false)
  private LocalDate paidOn;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal commission;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal wtax;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal base;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal fee;

  @Column(nullable = false)
  private boolean live = true;

  protected ServiceFeeItem() {}

  /**
   * An invoice of a line.
   *
   * @param runId run
   * @param lineId line
   * @param invoice paid invoice
   * @param base commission base
   * @param fee service fee
   */
  public ServiceFeeItem(
      Long runId, Long lineId, PaidInvoice invoice, BigDecimal base, BigDecimal fee) {
    this.runId = runId;
    this.lineId = lineId;
    this.invoiceNo = invoice.invoiceNo();
    this.rootInvoiceNo = invoice.rootInvoiceNo();
    this.clientCode = invoice.clientCode();
    this.assuredName = invoice.assuredName();
    this.insurerCode = invoice.insurerCode();
    this.marketSegment = invoice.marketSegment();
    this.paidOn = invoice.paidOn();
    this.commission = invoice.commission();
    this.wtax = invoice.wtax();
    this.base = base;
    this.fee = fee;
  }

  /** Frees the invoice for another run. */
  public void release() {
    live = false;
  }

  public Long getId() {
    return id;
  }

  public Long getRunId() {
    return runId;
  }

  public Long getLineId() {
    return lineId;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getRootInvoiceNo() {
    return rootInvoiceNo;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getMarketSegment() {
    return marketSegment;
  }

  public LocalDate getPaidOn() {
    return paidOn;
  }

  public BigDecimal getCommission() {
    return commission;
  }

  public BigDecimal getWtax() {
    return wtax;
  }

  public BigDecimal getBase() {
    return base;
  }

  public BigDecimal getFee() {
    return fee;
  }

  public boolean isLive() {
    return live;
  }
}
