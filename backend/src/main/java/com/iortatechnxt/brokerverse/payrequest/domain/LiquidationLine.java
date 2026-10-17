package com.iortatechnxt.brokerverse.payrequest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One fieldwork day of a Cash Advance Liquidation Form (Appendix D): date, particulars and the
 * expenses per category.
 */
@Entity
@Table(name = "prq_liquidation_line")
public class LiquidationLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @Column(name = "fieldwork_date", nullable = false)
  private LocalDate fieldworkDate;

  @Column(nullable = false, length = 250)
  private String particulars;

  @Column(name = "per_diem", nullable = false, precision = 19, scale = 2)
  private BigDecimal perDiem;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal representation;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal transport;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal lodging;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal others;

  protected LiquidationLine() {}

  /**
   * Creates a line.
   *
   * @param lineNo line number
   * @param values the form values
   */
  public LiquidationLine(int lineNo, ExpenseValues values) {
    this.lineNo = lineNo;
    this.fieldworkDate = values.fieldworkDate();
    this.particulars = values.particulars();
    this.perDiem = values.perDiem();
    this.representation = values.representation();
    this.transport = values.transport();
    this.lodging = values.lodging();
    this.others = values.others();
  }

  /**
   * Total expenses of the day.
   *
   * @return sum of the categories
   */
  public BigDecimal total() {
    return perDiem.add(representation).add(transport).add(lodging).add(others);
  }

  public Long getId() {
    return id;
  }

  public int getLineNo() {
    return lineNo;
  }

  public LocalDate getFieldworkDate() {
    return fieldworkDate;
  }

  public String getParticulars() {
    return particulars;
  }

  public BigDecimal getPerDiem() {
    return perDiem;
  }

  public BigDecimal getRepresentation() {
    return representation;
  }

  public BigDecimal getTransport() {
    return transport;
  }

  public BigDecimal getLodging() {
    return lodging;
  }

  public BigDecimal getOthers() {
    return others;
  }
}
