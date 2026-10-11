package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * A record of a programme and, for the marketing records, of one of its cycles: the company,
 * programme and cycle columns shared by the franchise requests, TOR, insurer requests, proposals,
 * revisions, comparatives, confirmations and submissions.
 */
@MappedSuperclass
public abstract class EbCycleRecord extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "programme_id", nullable = false, updatable = false)
  private Long programmeId;

  @Column(name = "cycle_id", updatable = false)
  private Long cycleId;

  protected EbCycleRecord() {}

  /**
   * A record of a cycle.
   *
   * @param cycle cycle
   */
  protected EbCycleRecord(EbCycle cycle) {
    this(cycle.getCompanyId(), cycle.getProgrammeId(), cycle.getId());
  }

  /**
   * A record of a programme, with or without its cycle.
   *
   * @param companyId company
   * @param programmeId programme
   * @param cycleId cycle, may be null
   */
  protected EbCycleRecord(Long companyId, Long programmeId, Long cycleId) {
    this.companyId = companyId;
    this.programmeId = programmeId;
    this.cycleId = cycleId;
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
}
