package com.iortatechnxt.brokerverse.adjustment.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.Hibernate;

/**
 * A posting batch (validation batch {@code VB-<yyyy>}, ADJID.006/017): the requests posted together
 * with the outcome of each; a request that fails stays for posting and does not stop the others.
 */
@Entity
@Table(name = "adj_posting_batch")
public class PostingBatch extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, length = 40, updatable = false)
  private String batchNo;

  @Column(name = "posted_count", nullable = false)
  private int postedCount;

  @Column(name = "pending_count", nullable = false)
  private int pendingCount;

  @Column(name = "failed_count", nullable = false)
  private int failedCount;

  @Column(length = 500)
  private String remarks;

  @ElementCollection
  @CollectionTable(name = "adj_posting_batch_line", joinColumns = @JoinColumn(name = "batch_id"))
  @OrderColumn(name = "line_index")
  private final List<Line> lines = new ArrayList<>();

  protected PostingBatch() {}

  /**
   * A new batch.
   *
   * @param companyId company
   * @param batchNo batch number
   * @param remarks remarks, may be null
   */
  public PostingBatch(Long companyId, String batchNo, String remarks) {
    this.companyId = companyId;
    this.batchNo = batchNo;
    this.remarks = remarks;
  }

  /**
   * Adds the outcome of one request.
   *
   * @param line request and outcome
   */
  public void add(Line line) {
    lines.add(line);
    switch (line.outcome()) {
      case POSTED -> postedCount++;
      case AWAITING_REAPPLICATION -> pendingCount++;
      default -> failedCount++;
    }
  }

  /** Loads the lines (reads outside the persistence context). */
  public void loadLines() {
    Hibernate.initialize(lines);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public int getPostedCount() {
    return postedCount;
  }

  public int getPendingCount() {
    return pendingCount;
  }

  public int getFailedCount() {
    return failedCount;
  }

  public String getRemarks() {
    return remarks;
  }

  public List<Line> getLines() {
    return Collections.unmodifiableList(lines);
  }

  /**
   * Outcome of one request of the batch.
   *
   * @param requestId request
   * @param requestNo request number
   * @param invoiceNo invoice
   * @param outcome posted, awaiting re-application or failed
   * @param message what happened
   */
  @Embeddable
  public record Line(
      @Column(name = "request_id", nullable = false) Long requestId,
      @Column(name = "request_no", nullable = false, length = 40) String requestNo,
      @Column(name = "invoice_no", nullable = false, length = 40) String invoiceNo,
      @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) BatchOutcome outcome,
      @Column(length = 1000) String message) {}
}
