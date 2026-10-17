package com.iortatechnxt.brokerverse.tax.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A BIR 2307 certificate BDOI received from a withholding agent (DIS 2.11.0-2.11.2, OQ41, AQ16):
 * the insurer's certificate of the tax it withheld on BDOI's commission and incentives. The one
 * register behind Disbursement's CWT tagging, the Commission certificate submission (CMRID.015) and
 * the SAWT. Recording it posts {@code TAX_CWT_CERT_RECEIVED}; a cancellation reverses it.
 */
@Entity
@Table(name = "tax_certificate_received")
public class ReceivedCertificate extends BaseEntity {

  private static final int MAX_TEXT = 250;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "certificate_no", nullable = false, length = 40, updatable = false)
  private String certificateNo;

  @Column(name = "agent_code", nullable = false, length = 30, updatable = false)
  private String agentCode;

  @Column(name = "agent_name", nullable = false, length = 200, updatable = false)
  private String agentName;

  @Column(name = "agent_tin", length = 20, updatable = false)
  private String agentTin;

  @Column(name = "period_from", nullable = false, updatable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false, updatable = false)
  private LocalDate periodTo;

  @Column(name = "received_on", nullable = false, updatable = false)
  private LocalDate receivedOn;

  @Column(name = "source_module", length = 30, updatable = false)
  private String sourceModule;

  @Column(name = "source_ref", length = 80, updatable = false)
  private String sourceRef;

  @Column(nullable = false, length = 20)
  private String status = RECORDED;

  @Column(name = "income_total", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal incomeTotal;

  @Column(name = "tax_total", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal taxTotal;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Column(name = "cancel_journal_no", length = 40)
  private String cancelJournalNo;

  @Column(name = "cancel_reason", length = MAX_TEXT)
  private String cancelReason;

  @Column(length = MAX_TEXT, updatable = false)
  private String remarks;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "tax_certificate_received_line",
      joinColumns = @JoinColumn(name = "certificate_id"))
  @OrderBy("lineNo")
  private final List<ReceivedCertificateLine> lines = new ArrayList<>();

  /** Status of a recorded certificate. */
  public static final String RECORDED = "RECORDED";

  /** Status of a cancelled certificate. */
  public static final String CANCELLED = "CANCELLED";

  protected ReceivedCertificate() {}

  /**
   * A certificate received.
   *
   * @param companyId company
   * @param facts certificate facts
   * @param lines income payments
   */
  public ReceivedCertificate(Long companyId, Facts facts, List<ReceivedCertificateLine> lines) {
    this.companyId = companyId;
    this.certificateNo = facts.certificateNo();
    this.agentCode = facts.agentCode();
    this.agentName = facts.agentName();
    this.agentTin = facts.agentTin();
    this.periodFrom = facts.periodFrom();
    this.periodTo = facts.periodTo();
    this.receivedOn = facts.receivedOn();
    this.sourceModule = facts.sourceModule();
    this.sourceRef = facts.sourceRef();
    this.remarks = facts.remarks();
    this.lines.addAll(lines);
    this.incomeTotal =
        lines.stream()
            .map(ReceivedCertificateLine::income)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    this.taxTotal =
        lines.stream().map(ReceivedCertificateLine::tax).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * The tax withheld of one kind of income.
   *
   * @param kind commission or incentive
   * @return total tax of that kind
   */
  public BigDecimal taxOf(ReceivedCertificateLine.Kind kind) {
    return lines.stream()
        .filter(l -> l.kind() == kind)
        .map(ReceivedCertificateLine::tax)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Records the journal of the receipt.
   *
   * @param batchNo journal batch
   */
  public void posted(String batchNo) {
    journalBatchNo = batchNo;
  }

  /**
   * Cancels the certificate.
   *
   * @param reason reason
   * @param batchNo reversing journal, null when nothing was posted
   */
  public void cancel(String reason, String batchNo) {
    if (CANCELLED.equals(status)) {
      throw new BusinessRuleException(
          "CERTIFICATE_CANCELLED", "Certificate " + certificateNo + " is already cancelled");
    }
    status = CANCELLED;
    cancelReason = reason.length() <= MAX_TEXT ? reason : reason.substring(0, MAX_TEXT);
    cancelJournalNo = batchNo;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getCertificateNo() {
    return certificateNo;
  }

  public String getAgentCode() {
    return agentCode;
  }

  public String getAgentName() {
    return agentName;
  }

  public String getAgentTin() {
    return agentTin;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public LocalDate getReceivedOn() {
    return receivedOn;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public String getStatus() {
    return status;
  }

  public BigDecimal getIncomeTotal() {
    return incomeTotal;
  }

  public BigDecimal getTaxTotal() {
    return taxTotal;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public String getCancelJournalNo() {
    return cancelJournalNo;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public String getRemarks() {
    return remarks;
  }

  public List<ReceivedCertificateLine> getLines() {
    return List.copyOf(lines);
  }

  /**
   * What a certificate says.
   *
   * @param certificateNo certificate number (or the agent's reference)
   * @param agentCode withholding agent (insurer party code)
   * @param agentName withholding agent name
   * @param agentTin withholding agent TIN
   * @param periodFrom first day of the period covered
   * @param periodTo last day of the period covered
   * @param receivedOn date received
   * @param sourceModule module that recorded it (DISBURSEMENT, COMMISSION, TAX)
   * @param sourceRef its reference there (DV number, submission), may be null
   * @param remarks remarks
   */
  public record Facts(
      String certificateNo,
      String agentCode,
      String agentName,
      String agentTin,
      LocalDate periodFrom,
      LocalDate periodTo,
      LocalDate receivedOn,
      String sourceModule,
      String sourceRef,
      String remarks) {}
}
