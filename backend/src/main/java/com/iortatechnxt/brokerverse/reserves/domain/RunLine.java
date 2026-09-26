package com.iortatechnxt.brokerverse.reserves.domain;

import com.iortatechnxt.brokerverse.common.util.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Closing balance of one reserve type for one reporting unit (branch, line of business, product,
 * channel) of a valuation run, gross and reinsurers' share.
 */
@Entity
@Table(name = "rsv_run_line")
public class RunLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "run_id")
  private ValuationRun run;

  @Enumerated(EnumType.STRING)
  @Column(name = "reserve_type", nullable = false, length = 20)
  private ReserveType reserveType;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "business_line", nullable = false, length = 20)
  private String businessLine;

  @Column(name = "product_code", nullable = false, length = 20)
  private String productCode;

  @Column(name = "source_type", nullable = false, length = 10)
  private String sourceType;

  @Column(name = "gross_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal grossAmount;

  @Column(name = "ri_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal riAmount;

  @Column(name = "base_amount", precision = 19, scale = 2)
  private BigDecimal baseAmount;

  @Column(precision = 19, scale = 8)
  private BigDecimal rate;

  @Column(length = 20)
  private String method;

  protected RunLine() {}

  /**
   * Creates a line.
   *
   * @param run owning run
   * @param values values
   */
  RunLine(ValuationRun run, ReserveLineValues values) {
    this.run = run;
    this.reserveType = values.type();
    this.branchId = values.key().branchId();
    this.businessLine = values.key().businessLine();
    this.productCode = values.key().productCode();
    this.sourceType = values.key().sourceType();
    this.grossAmount = Money.round(values.gross());
    this.riAmount = Money.round(values.ri());
    this.baseAmount = values.base() == null ? null : Money.round(values.base());
    this.rate = values.rate();
    this.method = values.method();
  }

  /**
   * Values of this line.
   *
   * @return values
   */
  public ReserveLineValues values() {
    return new ReserveLineValues(
        reserveType, key(), grossAmount, riAmount, baseAmount, rate, method);
  }

  /**
   * Reporting unit of this line.
   *
   * @return key
   */
  public ReserveKey key() {
    return new ReserveKey(branchId, businessLine, productCode, sourceType);
  }

  public Long getId() {
    return id;
  }

  public ReserveType getReserveType() {
    return reserveType;
  }

  public BigDecimal getGrossAmount() {
    return grossAmount;
  }

  public BigDecimal getRiAmount() {
    return riAmount;
  }
}
