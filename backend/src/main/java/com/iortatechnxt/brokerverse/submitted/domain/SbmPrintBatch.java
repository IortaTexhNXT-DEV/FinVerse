package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A print batch for the mail house (BRIDSP-22; SP SQ09): the letters of one type and day merged
 * into one PDF with a control list, kept in the file store until the mail house collects them.
 */
@Entity
@Table(name = "sbm_print_batch")
public class SbmPrintBatch extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, updatable = false, length = 30)
  private String batchNo;

  @Column(nullable = false, updatable = false, length = 20)
  private String source;

  @Column(name = "letter_type", nullable = false, updatable = false, length = 30)
  private String letterType;

  @Column(name = "batch_date", nullable = false, updatable = false)
  private LocalDate batchDate;

  @Column(name = "letter_count", nullable = false)
  private int letterCount;

  @Column(name = "merged_file_id")
  private Long mergedFileId;

  @Column(name = "control_file_id")
  private Long controlFileId;

  @Column(name = "handed_to", length = 120)
  private String handedTo;

  @Column(name = "handed_at")
  private Instant handedAt;

  protected SbmPrintBatch() {}

  /**
   * A batch.
   *
   * @param companyId company
   * @param batchNo number
   * @param kind source, letter type and date
   * @param count letters
   */
  public SbmPrintBatch(Long companyId, String batchNo, Kind kind, int count) {
    this.companyId = companyId;
    this.batchNo = batchNo;
    this.source = kind.source();
    this.letterType = kind.letterType();
    this.batchDate = kind.batchDate();
    this.letterCount = count;
  }

  /**
   * The files of the batch.
   *
   * @param merged merged PDF
   * @param control control list
   */
  public void files(Long merged, Long control) {
    this.mergedFileId = merged;
    this.controlFileId = control;
  }

  /**
   * Handed over to the mail house.
   *
   * @param to who collected it
   * @param at time
   */
  public void handedOver(String to, Instant at) {
    this.handedTo = to;
    this.handedAt = at;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public String getSource() {
    return source;
  }

  public String getLetterType() {
    return letterType;
  }

  public LocalDate getBatchDate() {
    return batchDate;
  }

  public int getLetterCount() {
    return letterCount;
  }

  public Long getMergedFileId() {
    return mergedFileId;
  }

  public Long getControlFileId() {
    return controlFileId;
  }

  public String getHandedTo() {
    return handedTo;
  }

  public Instant getHandedAt() {
    return handedAt;
  }

  /**
   * What the batch holds.
   *
   * @param source SUBMITTED or RENEWAL
   * @param letterType letter type
   * @param batchDate date
   */
  public record Kind(String source, String letterType, LocalDate batchDate) {}
}
