package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A received / released stamp of an activity of the TAT annex (BRID-022, 024; design 4.2): written
 * by the EB services at each step, read by the {@code EB-TAT} report. The actor is a BIBS user or,
 * for work the insurer or client does, the party recorded by the EB user.
 */
@Entity
@Table(name = "eb_activity_log")
public class EbActivity extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "programme_id", nullable = false, updatable = false)
  private Long programmeId;

  @Column(name = "cycle_id", updatable = false)
  private Long cycleId;

  @Enumerated(EnumType.STRING)
  @Column(name = "activity_code", nullable = false, length = 40, updatable = false)
  private TatActivity activity;

  @Column(length = 40, updatable = false)
  private String reference;

  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;

  @Column(name = "released_at")
  private Instant releasedAt;

  @Column(nullable = false, length = 60)
  private String actor;

  @Column(length = 500)
  private String remarks;

  protected EbActivity() {}

  /**
   * Stamps the start (received) of an activity.
   *
   * @param companyId company
   * @param place programme and cycle
   * @param activity TAT activity
   * @param reference business reference (request, franchise, SOA number), may be null
   * @param receivedAt received time
   * @param actor who performs it
   */
  public EbActivity(
      Long companyId,
      EbDocument.Place place,
      TatActivity activity,
      String reference,
      Instant receivedAt,
      String actor) {
    this.companyId = companyId;
    this.programmeId = place.programmeId();
    this.cycleId = place.cycleId();
    this.activity = activity;
    this.reference = reference;
    this.receivedAt = receivedAt;
    this.actor = actor;
  }

  /**
   * Stamps the end (released) of the activity.
   *
   * @param when released time, not before the received time
   * @param note remarks, may be null
   */
  public void release(Instant when, String note) {
    if (when.isBefore(receivedAt)) {
      throw new BusinessRuleException(
          "EB_ACTIVITY_RELEASE_BEFORE_RECEIPT",
          "An activity cannot be released before it was received");
    }
    this.releasedAt = when;
    this.remarks = note;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getProgrammeId() {
    return programmeId;
  }

  public Long getCycleId() {
    return cycleId;
  }

  public TatActivity getActivity() {
    return activity;
  }

  public String getReference() {
    return reference;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public Instant getReleasedAt() {
    return releasedAt;
  }

  public String getActor() {
    return actor;
  }

  public String getRemarks() {
    return remarks;
  }
}
