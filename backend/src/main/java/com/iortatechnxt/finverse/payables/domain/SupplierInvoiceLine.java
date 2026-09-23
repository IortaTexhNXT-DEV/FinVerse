package com.iortatechnxt.finverse.payables.domain;

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

/** Expense line of a supplier invoice with its computed input VAT and withholding tax. */
@Entity
@Table(name = "pay_supplier_invoice_line")
public class SupplierInvoiceLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invoice_id", nullable = false)
  private SupplierInvoice invoice;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @Column(name = "expense_account_code", nullable = false, length = 30)
  private String expenseAccountCode;

  @Column(name = "cost_center", length = 20)
  private String costCenter;

  @Column(nullable = false, length = 200)
  private String description;

  @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal netAmount;

  @Column(name = "vat_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal vatAmount;

  @Column(name = "wht_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal whtAmount;

  protected SupplierInvoiceLine() {}

  SupplierInvoiceLine(
      SupplierInvoice invoice,
      int lineNo,
      InvoiceLineValues values,
      BigDecimal vatAmount,
      BigDecimal whtAmount) {
    this.invoice = invoice;
    this.lineNo = lineNo;
    this.expenseAccountCode = values.expenseAccountCode();
    this.costCenter = values.costCenter();
    this.description = values.description();
    this.netAmount = values.netAmount();
    this.vatAmount = vatAmount;
    this.whtAmount = whtAmount;
  }

  /**
   * Payable portion of the line.
   *
   * @return net + VAT - withholding
   */
  public BigDecimal payableAmount() {
    return InvoiceCalculator.payable(netAmount, vatAmount, whtAmount);
  }

  public Long getId() {
    return id;
  }

  public int getLineNo() {
    return lineNo;
  }

  public String getExpenseAccountCode() {
    return expenseAccountCode;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getNetAmount() {
    return netAmount;
  }

  public BigDecimal getVatAmount() {
    return vatAmount;
  }

  public BigDecimal getWhtAmount() {
    return whtAmount;
  }
}
