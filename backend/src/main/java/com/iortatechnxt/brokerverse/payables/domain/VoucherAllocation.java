package com.iortatechnxt.brokerverse.payables.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Payable open item settled by a payment voucher, with the amount paid against it. */
@Entity
@Table(name = "pay_voucher_allocation")
public class VoucherAllocation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "voucher_id", nullable = false)
  private PaymentVoucher voucher;

  @Column(name = "open_item_id", nullable = false)
  private Long openItemId;

  @Column(name = "document_type", nullable = false, length = 30)
  private String documentType;

  @Column(name = "document_no", nullable = false, length = 40)
  private String documentNo;

  @Column(name = "document_date", nullable = false)
  private LocalDate documentDate;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  protected VoucherAllocation() {}

  VoucherAllocation(PaymentVoucher voucher, AllocationValues v) {
    this.voucher = voucher;
    this.openItemId = v.openItemId();
    this.documentType = v.documentType();
    this.documentNo = v.documentNo();
    this.documentDate = v.documentDate();
    this.dueDate = v.dueDate();
    this.amount = v.amount();
  }

  public Long getId() {
    return id;
  }

  public Long getOpenItemId() {
    return openItemId;
  }

  public String getDocumentType() {
    return documentType;
  }

  public String getDocumentNo() {
    return documentNo;
  }

  public LocalDate getDocumentDate() {
    return documentDate;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public BigDecimal getAmount() {
    return amount;
  }
}
