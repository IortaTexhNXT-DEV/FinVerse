package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An invoice looked up for remittance during the day and queued for the end-of-day extraction
 * (RMTID.005): one request per invoice and day, so a search never triggers twice; the run records
 * the tag it gave. The exact meaning of the BRD rule is parked (OQ18).
 */
@Entity
@Table(name = "rem_eod_request")
public class EodRequest extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "requested_on", nullable = false, updatable = false)
  private LocalDate requestedOn;

  @Column(name = "requested_by", nullable = false, length = 50, updatable = false)
  private String requestedBy;

  @Column(name = "run_no", length = 30)
  private String runNo;

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  private ExtractionTag tag;

  @Column(name = "processed_at")
  private Instant processedAt;

  protected EodRequest() {}

  /**
   * A request.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param requestedOn day
   * @param requestedBy user
   */
  public EodRequest(Long companyId, String invoiceNo, LocalDate requestedOn, String requestedBy) {
    this.companyId = companyId;
    this.invoiceNo = invoiceNo;
    this.requestedOn = requestedOn;
    this.requestedBy = requestedBy;
  }

  /**
   * Records the run that processed the request.
   *
   * @param run run number
   * @param outcome tag given
   * @param at time
   */
  public void processed(String run, ExtractionTag outcome, Instant at) {
    this.runNo = run;
    this.tag = outcome;
    this.processedAt = at;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public LocalDate getRequestedOn() {
    return requestedOn;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public String getRunNo() {
    return runNo;
  }

  public ExtractionTag getTag() {
    return tag;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }
}
