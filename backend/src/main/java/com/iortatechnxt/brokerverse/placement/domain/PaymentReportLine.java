package com.iortatechnxt.brokerverse.placement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** One line of a payment report with its match (matched, unpaid, unmatched or ambiguous). */
@Entity
@Table(name = "plc_payment_report_line")
public class PaymentReportLine {

  private static final int MAX_TEXT = 300;
  private static final int MAX_CANDIDATES = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "report_id", nullable = false, updatable = false)
  private PaymentReport report;

  @Column(name = "row_no", nullable = false, updatable = false)
  private int rowNo;

  @Column(nullable = false, length = 80, updatable = false)
  private String reference;

  @Column(nullable = false, updatable = false)
  private boolean paid;

  @Column(precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(name = "paid_on", updatable = false)
  private LocalDate paidOn;

  @Enumerated(EnumType.STRING)
  @Column(name = "match_status", nullable = false, length = 20)
  private MatchStatus matchStatus = MatchStatus.UNMATCHED;

  @Column(name = "account_id")
  private Long accountId;

  @Column(length = 30)
  private String arn;

  @Column(length = MAX_CANDIDATES)
  private String candidates;

  @Column(length = MAX_TEXT)
  private String message;

  @Column(name = "manually_matched", nullable = false)
  private boolean manuallyMatched;

  @Column(nullable = false)
  private boolean applied;

  @Column(name = "apply_message", length = MAX_TEXT)
  private String applyMessage;

  protected PaymentReportLine() {}

  PaymentReportLine(PaymentReport report, ReportedPayment row) {
    this.report = report;
    this.rowNo = row.rowNo();
    this.reference = row.reference();
    this.paid = row.paid();
    this.amount = row.amount();
    this.paidOn = row.paidOn();
  }

  /**
   * Records the automatic match.
   *
   * @param status outcome
   * @param account matched account (MATCHED / UNPAID), may be null
   * @param candidateArns candidate ARNs (AMBIGUOUS)
   * @param note explanation shown on the review screen
   */
  public void match(
      MatchStatus status, MatchedAccount account, List<String> candidateArns, String note) {
    this.matchStatus = status;
    this.accountId = account == null ? null : account.accountId();
    this.arn = account == null ? null : account.arn();
    this.candidates =
        candidateArns.isEmpty() ? null : clip(String.join(", ", candidateArns), MAX_CANDIDATES);
    this.message = clip(note, MAX_TEXT);
  }

  /**
   * Records a match chosen by the user for an ambiguous or unmatched line.
   *
   * @param account chosen account
   * @param user user
   */
  public void matchManually(MatchedAccount account, String user) {
    this.matchStatus = paid ? MatchStatus.MATCHED : MatchStatus.UNPAID;
    this.accountId = account.accountId();
    this.arn = account.arn();
    this.manuallyMatched = true;
    this.message = "Matched by " + user;
  }

  /**
   * Records whether the payment gate opened for the line's account.
   *
   * @param done opened
   * @param note outcome or reason
   */
  public void markApplied(boolean done, String note) {
    this.applied = done;
    this.applyMessage = clip(note, MAX_TEXT);
  }

  private static String clip(String text, int max) {
    return text == null || text.length() <= max ? text : text.substring(0, max);
  }

  public Long getId() {
    return id;
  }

  public PaymentReport getReport() {
    return report;
  }

  public int getRowNo() {
    return rowNo;
  }

  public String getReference() {
    return reference;
  }

  public boolean isPaid() {
    return paid;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDate getPaidOn() {
    return paidOn;
  }

  public MatchStatus getMatchStatus() {
    return matchStatus;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getArn() {
    return arn;
  }

  public String getCandidates() {
    return candidates;
  }

  public String getMessage() {
    return message;
  }

  public boolean isManuallyMatched() {
    return manuallyMatched;
  }

  public boolean isApplied() {
    return applied;
  }

  public String getApplyMessage() {
    return applyMessage;
  }

  /**
   * The account a line is matched to.
   *
   * @param accountId account id
   * @param arn Account Reference Number
   */
  public record MatchedAccount(Long accountId, String arn) {}
}
