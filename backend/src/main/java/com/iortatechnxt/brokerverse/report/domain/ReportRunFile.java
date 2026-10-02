package com.iortatechnxt.brokerverse.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The file of an archived report export (CSHID.018), kept apart from the run list. */
@Entity
@Table(name = "report_run_file")
public class ReportRunFile {

  @Id
  @Column(name = "run_id")
  private Long runId;

  @Column(nullable = false, updatable = false)
  private byte[] content;

  protected ReportRunFile() {}

  /**
   * Creates the file of a run.
   *
   * @param runId archived export
   * @param content bytes
   */
  public ReportRunFile(Long runId, byte[] content) {
    this.runId = runId;
    this.content = content.clone();
  }

  public Long getRunId() {
    return runId;
  }

  /**
   * The file.
   *
   * @return bytes
   */
  public byte[] getContent() {
    return content.clone();
  }
}
