package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A LAMD report uploaded by the LAMD user (BRRN.029): paid-off or RMU loans of a month, with the
 * upload job and the number of lines matched to renewals by PN.
 */
@Entity
@Table(name = "rnw_lamd_report")
public class LamdReport extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "report_no", nullable = false, length = 30, updatable = false)
  private String reportNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "report_type", nullable = false, length = 20, updatable = false)
  private LamdStatus reportType;

  @Column(nullable = false, length = 7, updatable = false)
  private String period;

  @Column(name = "job_no", nullable = false, length = 40, updatable = false)
  private String jobNo;

  @Column(name = "line_count", nullable = false)
  private int lineCount;

  @Column(name = "matched_count", nullable = false)
  private int matchedCount;

  protected LamdReport() {}

  /**
   * Records a report.
   *
   * @param companyId company
   * @param reportNo report number
   * @param reportType paid-off or RMU
   * @param period month (yyyy-MM)
   * @param jobNo upload job
   */
  public LamdReport(
      Long companyId, String reportNo, LamdStatus reportType, String period, String jobNo) {
    this.companyId = companyId;
    this.reportNo = reportNo;
    this.reportType = reportType;
    this.period = period;
    this.jobNo = jobNo;
  }

  /**
   * Counts a line.
   *
   * @param matched whether it matched a renewal
   */
  public void count(boolean matched) {
    lineCount++;
    if (matched) {
      matchedCount++;
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getReportNo() {
    return reportNo;
  }

  public LamdStatus getReportType() {
    return reportType;
  }

  public String getPeriod() {
    return period;
  }

  public String getJobNo() {
    return jobNo;
  }

  public int getLineCount() {
    return lineCount;
  }

  public int getMatchedCount() {
    return matchedCount;
  }
}
