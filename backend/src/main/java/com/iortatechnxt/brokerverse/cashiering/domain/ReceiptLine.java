package com.iortatechnxt.brokerverse.cashiering.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * One line of an official receipt: an invoice or commission item with its gross, VAT, withholding
 * tax and net (CSHID.002/007).
 */
@Embeddable
public class ReceiptLine {

  @Column(name = "invoice_no", length = 40)
  private String invoiceNo;

  @Column(name = "insurer_code", length = 30)
  private String insurerCode;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal gross;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal vat;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal wtax;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal net;

  @Column(length = 250)
  private String description;

  protected ReceiptLine() {}

  /**
   * Creates a line; the net is gross + VAT - withholding tax.
   *
   * @param invoiceNo invoice, may be null
   * @param insurerCode insurer, may be null
   * @param amounts gross, VAT and withholding tax
   * @param description description
   */
  public ReceiptLine(String invoiceNo, String insurerCode, OrAmounts amounts, String description) {
    this.invoiceNo = invoiceNo;
    this.insurerCode = insurerCode;
    this.gross = amounts.gross();
    this.vat = amounts.vat();
    this.wtax = amounts.wtax();
    this.net = gross.add(vat).subtract(wtax);
    this.description = description;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public BigDecimal getGross() {
    return gross;
  }

  public BigDecimal getVat() {
    return vat;
  }

  public BigDecimal getWtax() {
    return wtax;
  }

  public BigDecimal getNet() {
    return net;
  }

  public String getDescription() {
    return description;
  }
}
