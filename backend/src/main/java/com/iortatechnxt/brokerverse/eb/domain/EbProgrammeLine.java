package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A benefit line of a programme (design 4.2): benefit line (list EB_BENEFIT_LINE), product, the
 * incumbent insurer, the current policy and ARN (the ARN a renewal renews, BT0 {@code
 * renewal_of_ref}), the period and the headcount.
 */
@Entity
@Table(name = "eb_programme_line")
public class EbProgrammeLine extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "programme_id", nullable = false, updatable = false)
  private EbProgramme programme;

  @Column(name = "line_no", nullable = false, updatable = false)
  private int lineNo;

  @Column(name = "benefit_line", nullable = false, length = 20)
  private String benefitLine;

  @Column(name = "product_code", length = 20)
  private String productCode;

  @Column(name = "incumbent_insurer", length = 30)
  private String incumbentInsurer;

  @Column(name = "current_policy_no", length = 60)
  private String currentPolicyNo;

  @Column(name = "current_arn", length = 30)
  private String currentArn;

  @Column(name = "period_from")
  private LocalDate periodFrom;

  @Column(name = "period_to")
  private LocalDate periodTo;

  @Column private Integer headcount;

  @Column(nullable = false)
  private boolean active = true;

  protected EbProgrammeLine() {}

  EbProgrammeLine(EbProgramme programme, int lineNo, Data data) {
    this.programme = programme;
    this.lineNo = lineNo;
    apply(data);
  }

  /**
   * Replaces the data of the line.
   *
   * @param data line data
   */
  public void update(Data data) {
    apply(data);
  }

  private void apply(Data data) {
    this.benefitLine = data.benefitLine();
    this.productCode = data.productCode();
    this.incumbentInsurer = data.incumbentInsurer();
    this.currentPolicyNo = data.currentPolicyNo();
    this.currentArn = data.currentArn();
    this.periodFrom = data.periodFrom();
    this.periodTo = data.periodTo();
    this.headcount = data.headcount();
  }

  /** The line is no longer part of the programme (kept for history). */
  public void deactivate() {
    this.active = false;
  }

  public EbProgramme getProgramme() {
    return programme;
  }

  public int getLineNo() {
    return lineNo;
  }

  public String getBenefitLine() {
    return benefitLine;
  }

  public String getProductCode() {
    return productCode;
  }

  public String getIncumbentInsurer() {
    return incumbentInsurer;
  }

  public String getCurrentPolicyNo() {
    return currentPolicyNo;
  }

  public String getCurrentArn() {
    return currentArn;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public Integer getHeadcount() {
    return headcount;
  }

  public boolean isActive() {
    return active;
  }

  /**
   * Data of a benefit line.
   *
   * @param benefitLine benefit line (list EB_BENEFIT_LINE)
   * @param productCode catalog product, null until chosen
   * @param incumbentInsurer insurer party code of the current policy
   * @param currentPolicyNo current policy number
   * @param currentArn ARN of the current account, null when not placed through BIBS
   * @param periodFrom current period start
   * @param periodTo current period end (drives the RA lead time)
   * @param headcount members covered
   */
  public record Data(
      String benefitLine,
      String productCode,
      String incumbentInsurer,
      String currentPolicyNo,
      String currentArn,
      LocalDate periodFrom,
      LocalDate periodTo,
      Integer headcount) {}
}
