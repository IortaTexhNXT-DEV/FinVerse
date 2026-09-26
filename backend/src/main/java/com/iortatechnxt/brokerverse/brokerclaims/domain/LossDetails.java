package com.iortatechnxt.brokerverse.brokerclaims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The loss of a claim and its claimant (BRCLM.004/006/036; loss notice content p.24-25;
 * CLAIMS_BROKING_DESIGN 5.1): loss and reported dates, nature and type of loss, description and
 * place, catastrophe code and event, claim amount, deductible, initial reserve and the claimant
 * with its override.
 *
 * <p>Owned by wave CL1-A, which adds the factory and the invariants (loss date within the cover
 * period as a warning, CLQ02; reported date between the loss date and today). Mapped by CL0 to the
 * V1021 columns of {@code bcl_claim}.
 */
@Embeddable
public class LossDetails {

  @Column(name = "loss_date", nullable = false)
  private LocalDate lossDate;

  @Column(name = "reported_date", nullable = false)
  private LocalDate reportedDate;

  @Column(name = "loss_nature", length = 40)
  private String lossNature;

  @Column(name = "claim_type", length = 40)
  private String claimType;

  @Column(name = "loss_description", length = 2000)
  private String lossDescription;

  @Column(name = "loss_place", length = 500)
  private String lossPlace;

  @Column(name = "catastrophe_code", length = 40)
  private String catastropheCode;

  @Column(name = "catastrophe_event", length = 200)
  private String catastropheEvent;

  @Column(name = "claim_amount", precision = 19, scale = 2)
  private BigDecimal claimAmount;

  @Column(precision = 19, scale = 2)
  private BigDecimal deductible;

  @Column(name = "initial_reserve", precision = 19, scale = 2)
  private BigDecimal initialReserve;

  @Column(name = "claimant_name", length = 250)
  private String claimantName;

  @Column(name = "claimant_overridden", nullable = false)
  private boolean claimantOverridden;

  @Column(name = "claimant_reason", length = 500)
  private String claimantReason;

  protected LossDetails() {}

  public LocalDate getLossDate() {
    return lossDate;
  }

  public LocalDate getReportedDate() {
    return reportedDate;
  }

  public String getLossNature() {
    return lossNature;
  }

  public String getClaimType() {
    return claimType;
  }

  public String getLossDescription() {
    return lossDescription;
  }

  public String getLossPlace() {
    return lossPlace;
  }

  public String getCatastropheCode() {
    return catastropheCode;
  }

  public String getCatastropheEvent() {
    return catastropheEvent;
  }

  public BigDecimal getClaimAmount() {
    return claimAmount;
  }

  public BigDecimal getDeductible() {
    return deductible;
  }

  public BigDecimal getInitialReserve() {
    return initialReserve;
  }

  public String getClaimantName() {
    return claimantName;
  }

  public boolean isClaimantOverridden() {
    return claimantOverridden;
  }

  public String getClaimantReason() {
    return claimantReason;
  }
}
