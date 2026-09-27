package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** A policy of a No Touch billing batch with the values returned by the insurer (RL #164). */
@Entity
@Table(name = "sbm_no_touch_line")
public class SbmNoTouchLine extends BaseEntity {

  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long batchId;

  @Column(name = "policy_id", nullable = false, updatable = false)
  private Long policyId;

  @Column(name = "sbm_no", nullable = false, updatable = false, length = 30)
  private String sbmNo;

  @Column(name = "pn_no", length = 40)
  private String pnNo;

  @Column(name = "assured_name", nullable = false, length = 250)
  private String assuredName;

  @Column(name = "policy_no", length = 60)
  private String policyNo;

  @Column(name = "plate_no", length = 20)
  private String plateNo;

  @Column(name = "sum_insured", precision = 19, scale = 2)
  private BigDecimal sumInsured;

  @Column(name = "basic_premium", precision = 19, scale = 2)
  private BigDecimal basicPremium;

  @Column(name = "gross_fee", precision = 19, scale = 2)
  private BigDecimal grossFee;

  @Column(precision = 19, scale = 2)
  private BigDecimal vat;

  @Column(precision = 19, scale = 2)
  private BigDecimal wtax;

  protected SbmNoTouchLine() {}

  /**
   * A line from a masterlist record.
   *
   * @param batchId batch
   * @param p record
   */
  public SbmNoTouchLine(Long batchId, SbmPolicy p) {
    this.batchId = batchId;
    this.policyId = p.getId();
    this.sbmNo = p.getSbmNo();
    this.pnNo = p.getLoan().pnNo();
    this.assuredName = p.getAssured().assuredName();
    this.policyNo = p.getTerms().policyNo();
    this.plateNo = p.getRisk().plateNo();
    this.sumInsured = p.getTerms().sumInsured();
  }

  /**
   * The values returned by the insurer.
   *
   * @param values basic premium, gross fee, VAT and withholding tax
   */
  public void returned(Values values) {
    this.basicPremium = values.basicPremium();
    this.grossFee = values.grossFee();
    this.vat = values.vat();
    this.wtax = values.wtax();
  }

  public Long getBatchId() {
    return batchId;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public String getSbmNo() {
    return sbmNo;
  }

  public String getPnNo() {
    return pnNo;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public String getPlateNo() {
    return plateNo;
  }

  public BigDecimal getSumInsured() {
    return sumInsured;
  }

  public BigDecimal getBasicPremium() {
    return basicPremium;
  }

  public BigDecimal getGrossFee() {
    return grossFee;
  }

  public BigDecimal getVat() {
    return vat;
  }

  public BigDecimal getWtax() {
    return wtax;
  }

  /**
   * The values of a line.
   *
   * @param basicPremium basic premium
   * @param grossFee gross service fee
   * @param vat VAT
   * @param wtax withholding tax
   */
  public record Values(BigDecimal basicPremium, BigDecimal grossFee, BigDecimal vat, BigDecimal wtax) {}
}
