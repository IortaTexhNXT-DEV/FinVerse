package com.iortatechnxt.brokerverse.reserves.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Reserve parameters of one line of business, effective from a date (maker-checker). A change of
 * parameters is a new record with a later effective date, so the values used by past valuation runs
 * stay traceable; a record can be edited only while it is pending authorization.
 */
@Entity
@Table(name = "rsv_parameter")
public class ReserveParameter extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "business_line", nullable = false, length = 20)
  private String businessLine;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Enumerated(EnumType.STRING)
  @Column(name = "ibnr_method", nullable = false, length = 20)
  private IbnrMethod ibnrMethod;

  @Column(name = "ibnr_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal ibnrRate;

  @Enumerated(EnumType.STRING)
  @Column(name = "triangle_basis", nullable = false, length = 20)
  private TriangleBasis triangleBasis;

  @Enumerated(EnumType.STRING)
  @Column(name = "development_period", nullable = false, length = 20)
  private DevelopmentPeriod developmentPeriod;

  @Column(name = "accident_periods", nullable = false)
  private int accidentPeriods;

  @Column(name = "mfad_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal mfadPct;

  @Column(name = "ulae_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal ulaePct;

  @Column(name = "expected_loss_ratio", nullable = false, precision = 19, scale = 8)
  private BigDecimal expectedLossRatio;

  @Column(name = "treaty_commission_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal treatyCommissionPct;

  @Column(name = "fac_commission_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal facCommissionPct;

  @Column(length = 300)
  private String remarks;

  protected ReserveParameter() {}

  /**
   * Creates a parameter set pending authorization.
   *
   * @param companyId company
   * @param businessLine line of business
   * @param effectiveFrom first valuation date the values apply to
   * @param terms values
   */
  public ReserveParameter(
      Long companyId, String businessLine, LocalDate effectiveFrom, ReserveParameterTerms terms) {
    this.companyId = companyId;
    this.businessLine = businessLine;
    this.effectiveFrom = effectiveFrom;
    apply(terms);
  }

  /**
   * Changes the values of a parameter set that is not yet authorized.
   *
   * @param terms new values
   */
  public void update(ReserveParameterTerms terms) {
    if (getRecordStatus() != RecordStatus.PENDING_AUTHORIZATION) {
      throw new BusinessRuleException(
          "RESERVE_PARAMETER_AUTHORIZED",
          "Authorized reserve parameters cannot be changed: add a new effective date instead");
    }
    apply(terms);
    markModified();
  }

  private void apply(ReserveParameterTerms t) {
    this.ibnrMethod = t.ibnrMethod();
    this.ibnrRate = t.ibnrRate();
    this.triangleBasis = t.triangleBasis();
    this.developmentPeriod = t.developmentPeriod();
    this.accidentPeriods = t.accidentPeriods();
    this.mfadPct = t.mfadPct();
    this.ulaePct = t.ulaePct();
    this.expectedLossRatio = t.expectedLossRatio();
    this.treatyCommissionPct = t.treatyCommissionPct();
    this.facCommissionPct = t.facCommissionPct();
    this.remarks = t.remarks();
  }

  /**
   * Current values.
   *
   * @return values
   */
  public ReserveParameterTerms terms() {
    return new ReserveParameterTerms(
        ibnrMethod,
        ibnrRate,
        triangleBasis,
        developmentPeriod,
        accidentPeriods,
        mfadPct,
        ulaePct,
        expectedLossRatio,
        treatyCommissionPct,
        facCommissionPct,
        remarks);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBusinessLine() {
    return businessLine;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }
}
