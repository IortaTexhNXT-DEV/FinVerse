package com.iortatechnxt.finverse.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/** What happened: dates, nature, cause and place of the loss (first notification of loss). */
@Embeddable
public class LossDetails {

  @Column(name = "loss_date", nullable = false)
  private LocalDate lossDate;

  @Column(name = "reported_date", nullable = false)
  private LocalDate reportedDate;

  @Column(name = "nature_of_loss", nullable = false, length = 60)
  private String natureOfLoss;

  @Column(name = "cause_of_loss", nullable = false, length = 120)
  private String causeOfLoss;

  @Column(name = "loss_location", nullable = false, length = 200)
  private String lossLocation;

  @Column(name = "description", nullable = false, length = 1000)
  private String description;

  protected LossDetails() {}

  /**
   * Creates loss details.
   *
   * @param lossDate date of loss
   * @param reportedDate date the loss was notified
   * @param natureOfLoss nature of loss (e.g. Fire, Collision, Theft)
   * @param causeOfLoss cause of loss
   * @param lossLocation place of loss
   * @param description narrative
   */
  public LossDetails(
      LocalDate lossDate,
      LocalDate reportedDate,
      String natureOfLoss,
      String causeOfLoss,
      String lossLocation,
      String description) {
    this.lossDate = lossDate;
    this.reportedDate = reportedDate;
    this.natureOfLoss = natureOfLoss;
    this.causeOfLoss = causeOfLoss;
    this.lossLocation = lossLocation;
    this.description = description;
  }

  public LocalDate getLossDate() {
    return lossDate;
  }

  public LocalDate getReportedDate() {
    return reportedDate;
  }

  public String getNatureOfLoss() {
    return natureOfLoss;
  }

  public String getCauseOfLoss() {
    return causeOfLoss;
  }

  public String getLossLocation() {
    return lossLocation;
  }

  public String getDescription() {
    return description;
  }
}
