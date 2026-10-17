package com.iortatechnxt.brokerverse.tax.domain;

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

/** Part II line of BIR Form 2307: income by month of the quarter and tax withheld for one ATC. */
@Entity
@Table(name = "tax_2307_line")
public class Certificate2307Line {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "certificate_id", nullable = false)
  private Certificate2307 certificate;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @Column(nullable = false, length = 10)
  private String atc;

  @Column(name = "income_nature", nullable = false, length = 200)
  private String incomeNature;

  @Column(name = "month1_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal month1Amount;

  @Column(name = "month2_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal month2Amount;

  @Column(name = "month3_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal month3Amount;

  @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "tax_withheld", nullable = false, precision = 19, scale = 2)
  private BigDecimal taxWithheld;

  protected Certificate2307Line() {}

  Certificate2307Line(Certificate2307 certificate, int lineNo, AtcQuarterAmounts amounts) {
    this.certificate = certificate;
    this.lineNo = lineNo;
    this.atc = amounts.atc();
    this.incomeNature = amounts.incomeNature();
    this.month1Amount = amounts.month1();
    this.month2Amount = amounts.month2();
    this.month3Amount = amounts.month3();
    this.totalAmount = amounts.total();
    this.taxWithheld = amounts.tax();
  }

  public Long getId() {
    return id;
  }

  public int getLineNo() {
    return lineNo;
  }

  public String getAtc() {
    return atc;
  }

  public String getIncomeNature() {
    return incomeNature;
  }

  public BigDecimal getMonth1Amount() {
    return month1Amount;
  }

  public BigDecimal getMonth2Amount() {
    return month2Amount;
  }

  public BigDecimal getMonth3Amount() {
    return month3Amount;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public BigDecimal getTaxWithheld() {
    return taxWithheld;
  }
}
