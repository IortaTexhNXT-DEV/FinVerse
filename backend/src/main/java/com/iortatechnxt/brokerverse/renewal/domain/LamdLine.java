package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A line of a {@link LamdReport} (BRRN.029): the PN, the loan status and date, the renewal it
 * matched and the routing applied. Unmatched lines are kept, never dropped.
 */
@Entity
@Table(name = "rnw_lamd_line")
public class LamdLine extends BaseEntity {

  /** Matched one renewal. */
  public static final String MATCHED = "MATCHED";

  /** No renewal has the PN. */
  public static final String UNMATCHED = "UNMATCHED";

  /** Several renewals have the PN. */
  public static final String AMBIGUOUS = "AMBIGUOUS";

  @Column(name = "report_id", nullable = false, updatable = false)
  private Long reportId;

  @Column(name = "row_no", nullable = false, updatable = false)
  private int rowNo;

  @Column(name = "pn_no", nullable = false, length = 40, updatable = false)
  private String pnNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "loan_status", nullable = false, length = 20, updatable = false)
  private LamdStatus loanStatus;

  @Column(name = "status_date", updatable = false)
  private LocalDate statusDate;

  @Column(length = 250, updatable = false)
  private String borrower;

  @Column(name = "candidate_id", updatable = false)
  private Long candidateId;

  @Column(name = "match_outcome", nullable = false, length = 20, updatable = false)
  private String matchOutcome;

  @Column(length = 40)
  private String routing;

  @Column(length = 300)
  private String message;

  protected LamdLine() {}

  /**
   * Records a line.
   *
   * @param reportId report
   * @param rowNo row of the file
   * @param loan PN, status, date and borrower
   * @param candidateId matched renewal, null when not matched
   * @param matchOutcome MATCHED, UNMATCHED or AMBIGUOUS
   */
  public LamdLine(Long reportId, int rowNo, Loan loan, Long candidateId, String matchOutcome) {
    this.reportId = reportId;
    this.rowNo = rowNo;
    this.pnNo = loan.pnNo();
    this.loanStatus = loan.status();
    this.statusDate = loan.statusDate();
    this.borrower = loan.borrower();
    this.candidateId = candidateId;
    this.matchOutcome = matchOutcome;
  }

  /**
   * Records the routing applied.
   *
   * @param newRouting routing code
   * @param text message
   */
  public void routed(String newRouting, String text) {
    this.routing = newRouting;
    this.message = text;
  }

  public Long getReportId() {
    return reportId;
  }

  public int getRowNo() {
    return rowNo;
  }

  public String getPnNo() {
    return pnNo;
  }

  public LamdStatus getLoanStatus() {
    return loanStatus;
  }

  public LocalDate getStatusDate() {
    return statusDate;
  }

  public String getBorrower() {
    return borrower;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getMatchOutcome() {
    return matchOutcome;
  }

  public String getRouting() {
    return routing;
  }

  public String getMessage() {
    return message;
  }

  /**
   * A loan reported by LAMD.
   *
   * @param pnNo PN number
   * @param status loan status
   * @param statusDate date of the status
   * @param borrower borrower name
   */
  public record Loan(String pnNo, LamdStatus status, LocalDate statusDate, String borrower) {}
}
