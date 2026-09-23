package com.iortatechnxt.finverse.underwriting.domain;

import com.iortatechnxt.finverse.common.util.Money;
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
import java.time.Instant;

/** One negotiation round of a quotation (immutable once created). Owned by {@link Quotation}. */
@Entity
@Table(name = "uw_quotation_iteration")
public class QuotationIteration {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "quotation_id")
  private Quotation quotation;

  @Column(name = "iteration_no", nullable = false)
  private int iterationNo;

  @Column(name = "sum_insured", nullable = false, precision = 19, scale = 2)
  private BigDecimal sumInsured;

  @Column(name = "gross_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal grossPremium;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal discount;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal loading;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal charges;

  @Column(length = 300)
  private String remarks;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false, length = 50)
  private String createdBy;

  protected QuotationIteration() {}

  QuotationIteration(
      Quotation quotation, int iterationNo, IterationValues v, String user, Instant when) {
    this.quotation = quotation;
    this.iterationNo = iterationNo;
    this.sumInsured = Money.round(v.sumInsured());
    this.grossPremium = Money.round(v.grossPremium());
    this.discount = Money.round(v.discount());
    this.loading = Money.round(v.loading());
    this.charges = Money.round(v.charges());
    this.remarks = v.remarks();
    this.createdBy = user;
    this.createdAt = when;
  }

  /**
   * Net premium at 100 %.
   *
   * @return gross + loading − discount
   */
  public BigDecimal netPremium() {
    return grossPremium.add(loading).subtract(discount);
  }

  public Long getId() {
    return id;
  }

  public int getIterationNo() {
    return iterationNo;
  }

  public BigDecimal getSumInsured() {
    return sumInsured;
  }

  public BigDecimal getGrossPremium() {
    return grossPremium;
  }

  public BigDecimal getDiscount() {
    return discount;
  }

  public BigDecimal getLoading() {
    return loading;
  }

  public BigDecimal getCharges() {
    return charges;
  }

  public String getRemarks() {
    return remarks;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }
}
