package com.iortatechnxt.brokerverse.placement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An uploaded payment report and its match (BRNB.067/068): a CLPC report matched by PN or loan
 * application number against a billing batch, or a report of the other segments matched by ARN.
 * Confirming it opens the payment gate of the matched, paid accounts.
 */
@Entity
@Table(name = "plc_payment_report")
public class PaymentReport extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "report_no", nullable = false, length = 30, updatable = false)
  private String reportNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private PaymentReportKind kind;

  @Column(name = "batch_id", updatable = false)
  private Long batchId;

  @Column(name = "file_name", nullable = false, length = 255, updatable = false)
  private String fileName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PaymentReportStatus status = PaymentReportStatus.REVIEW;

  @Column(name = "confirmed_by", length = 50)
  private String confirmedBy;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("rowNo")
  private final List<PaymentReportLine> lines = new ArrayList<>();

  protected PaymentReport() {}

  /**
   * Creates a report under review.
   *
   * @param companyId company
   * @param reportNo report number
   * @param kind CLPC or reference (ARN) report
   * @param batchId billing batch answered (CLPC)
   * @param fileName uploaded file name
   */
  public PaymentReport(
      Long companyId, String reportNo, PaymentReportKind kind, Long batchId, String fileName) {
    this.companyId = companyId;
    this.reportNo = reportNo;
    this.kind = kind;
    this.batchId = batchId;
    this.fileName = fileName;
  }

  /**
   * Adds a line.
   *
   * @param row the reported payment
   * @return the line (not yet matched)
   */
  public PaymentReportLine add(ReportedPayment row) {
    PaymentReportLine line = new PaymentReportLine(this, row);
    lines.add(line);
    return line;
  }

  /**
   * Confirms the report.
   *
   * @param user user
   * @param when time
   */
  public void confirm(String user, Instant when) {
    requireReview();
    this.status = PaymentReportStatus.CONFIRMED;
    this.confirmedBy = user;
    this.confirmedAt = when;
  }

  /** Discards the report without effect. */
  public void discard() {
    requireReview();
    this.status = PaymentReportStatus.DISCARDED;
  }

  /** Refuses changes once the report left review. */
  public void requireReview() {
    if (status != PaymentReportStatus.REVIEW) {
      throw new BusinessRuleException(
          "PAYMENT_REPORT_CLOSED", "Payment report " + reportNo + " is " + status);
    }
  }

  /**
   * Number of lines with a match status.
   *
   * @param matchStatus status
   * @return count
   */
  public long count(MatchStatus matchStatus) {
    return lines.stream().filter(l -> l.getMatchStatus() == matchStatus).count();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getReportNo() {
    return reportNo;
  }

  public PaymentReportKind getKind() {
    return kind;
  }

  public Long getBatchId() {
    return batchId;
  }

  public String getFileName() {
    return fileName;
  }

  public PaymentReportStatus getStatus() {
    return status;
  }

  public String getConfirmedBy() {
    return confirmedBy;
  }

  public Instant getConfirmedAt() {
    return confirmedAt;
  }

  public List<PaymentReportLine> getLines() {
    return List.copyOf(lines);
  }
}
