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

/** One line of a return: a worksheet summary figure frozen when the return is filed. */
@Entity
@Table(name = "tax_return_line")
public class TaxReturnLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "return_id", nullable = false)
  private TaxReturn taxReturn;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @Column(name = "line_code", nullable = false, length = 40)
  private String lineCode;

  @Column(nullable = false, length = 200)
  private String description;

  @Column(name = "base_amount", precision = 19, scale = 2)
  private BigDecimal baseAmount;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  protected TaxReturnLine() {}

  TaxReturnLine(TaxReturn taxReturn, int lineNo, ReturnLineValues values) {
    this.taxReturn = taxReturn;
    this.lineNo = lineNo;
    this.lineCode = values.code();
    this.description = values.description();
    this.baseAmount = values.base();
    this.amount = values.amount();
  }

  public Long getId() {
    return id;
  }

  public int getLineNo() {
    return lineNo;
  }

  public String getLineCode() {
    return lineCode;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getBaseAmount() {
    return baseAmount;
  }

  public BigDecimal getAmount() {
    return amount;
  }
}
