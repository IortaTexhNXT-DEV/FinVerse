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
 * A generated Consolidated Placement Update Report in the repository (BDOI FRS FRPM.007.01): the
 * company, report date and reporting period, the file name and the archived run to download, the
 * rows shown in the preview, and who generated it when (the job or a user on request).
 */
@Entity
@Table(name = "pm_placement_report")
public class PlacementReport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "report_date", nullable = false, updatable = false)
  private LocalDate reportDate;

  @Column(name = "period_from", nullable = false, updatable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false, updatable = false)
  private LocalDate periodTo;

  @Column(nullable = false, length = 30, updatable = false)
  private String scope;

  @Column(name = "trigger_type", nullable = false, length = 20, updatable = false)
  private String triggerType;

  @Column(name = "file_name", nullable = false, length = 200, updatable = false)
  private String fileName;

  @Column(name = "run_id", updatable = false)
  private Long runId;

  @Column(name = "record_count", nullable = false, updatable = false)
  private int recordCount;

  @Column(name = "rows_json", nullable = false, updatable = false)
  private String rowsJson;

  @Column(name = "generated_by", nullable = false, length = 50, updatable = false)
  private String generatedBy;

  @Column(name = "generated_at", nullable = false, updatable = false)
  private Instant generatedAt;

  /** For JPA. */
  protected PlacementReport() {}

  /**
   * Records a generated report.
   *
   * @param period company, report date, reporting period, scope and trigger
   * @param file file name and archived run
   * @param rows number of rows and the rows as JSON
   * @param generated who and when
   */
  public PlacementReport(Period period, FileRef file, Rows rows, ProposalFile.Generated generated) {
    this.companyId = period.companyId();
    this.reportDate = period.reportDate();
    this.periodFrom = period.from();
    this.periodTo = period.to();
    this.scope = period.scope();
    this.triggerType = period.trigger();
    this.fileName = file.fileName();
    this.runId = file.runId();
    this.recordCount = rows.count();
    this.rowsJson = rows.json();
    this.generatedBy = generated.by();
    this.generatedAt = generated.at();
  }

  public Long getId() {
    return id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public LocalDate getReportDate() {
    return reportDate;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public String getScope() {
    return scope;
  }

  public String getTriggerType() {
    return triggerType;
  }

  public String getFileName() {
    return fileName;
  }

  public Long getRunId() {
    return runId;
  }

  public int getRecordCount() {
    return recordCount;
  }

  public String getRowsJson() {
    return rowsJson;
  }

  public String getGeneratedBy() {
    return generatedBy;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  /**
   * The period of a report.
   *
   * @param companyId company
   * @param reportDate report date
   * @param from first day
   * @param to last day
   * @param scope QUOTATION or QUOTATION_AND_PACKAGE
   * @param trigger SCHEDULED or ON_REQUEST
   */
  public record Period(
      Long companyId,
      LocalDate reportDate,
      LocalDate from,
      LocalDate to,
      String scope,
      String trigger) {}

  /**
   * The file of a report.
   *
   * @param fileName file name
   * @param runId archived report run
   */
  public record FileRef(String fileName, Long runId) {}

  /**
   * The rows of a report.
   *
   * @param count number of rows
   * @param json rows as JSON
   */
  public record Rows(int count, String json) {}
}
