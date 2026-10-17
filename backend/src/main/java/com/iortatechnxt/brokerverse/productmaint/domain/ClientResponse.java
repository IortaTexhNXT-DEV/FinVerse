package com.iortatechnxt.brokerverse.productmaint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The client's response to a proposal (BDOI FRS FRPM.010.01): Accepted (with the insurers
 * accepted), Rejected or Return for Revision, with the response date, the client remarks and who
 * recorded it when.
 */
@Entity
@Table(name = "pm_client_response")
public class ClientResponse {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "record_type", nullable = false, length = 30, updatable = false)
  private String recordType;

  @Column(name = "record_id", nullable = false, updatable = false)
  private Long recordId;

  @Column(nullable = false, length = 20, updatable = false)
  private String response;

  @Column(name = "response_date", nullable = false, updatable = false)
  private LocalDate responseDate;

  @Column(length = 1000, updatable = false)
  private String remarks;

  @Column(length = 500, updatable = false)
  private String insurers;

  @Column(name = "recorded_by", nullable = false, length = 50, updatable = false)
  private String recordedBy;

  @Column(name = "recorded_at", nullable = false, updatable = false)
  private Instant recordedAt;

  /** For JPA. */
  protected ClientResponse() {}

  /**
   * Records a response.
   *
   * @param record the record
   * @param response ACCEPTED, REJECTED or RETURNED
   * @param details response date, remarks and insurers accepted
   * @param recordedBy user
   * @param recordedAt time
   */
  public ClientResponse(
      TermsRecord record, String response, Details details, String recordedBy, Instant recordedAt) {
    this.recordType = record.type();
    this.recordId = record.id();
    this.response = response;
    this.responseDate = details.responseDate();
    this.remarks = details.remarks();
    this.insurers = details.insurers();
    this.recordedBy = recordedBy;
    this.recordedAt = recordedAt;
  }

  public Long getId() {
    return id;
  }

  public String getResponse() {
    return response;
  }

  public LocalDate getResponseDate() {
    return responseDate;
  }

  public String getRemarks() {
    return remarks;
  }

  public String getInsurers() {
    return insurers;
  }

  public String getRecordedBy() {
    return recordedBy;
  }

  public Instant getRecordedAt() {
    return recordedAt;
  }

  /**
   * What the client answered.
   *
   * @param responseDate response date
   * @param remarks client remarks
   * @param insurers insurers accepted (comma separated), may be null
   */
  public record Details(LocalDate responseDate, String remarks, String insurers) {}
}
