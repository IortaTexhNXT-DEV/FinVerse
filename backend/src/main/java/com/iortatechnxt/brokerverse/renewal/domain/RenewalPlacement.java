package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The placement of a renewal account with one insurer (FRRN.029, FRRN.030): its placement slip, the
 * consolidated placement file it is in, how it was sent, the turnaround time with the With Issue
 * tag and the insurer's response.
 */
@Entity
@Table(name = "rnw_placement")
public class RenewalPlacement extends BaseEntity {

  /** Status: slip generated, not yet sent. */
  public static final String GENERATED = "GENERATED";

  /** Status: sent to the insurer, waiting for its response. */
  public static final String SENT = "SENT";

  /** Status: approved by the insurer. */
  public static final String APPROVED = "APPROVED";

  /** Status: rejected by the insurer. */
  public static final String REJECTED = "REJECTED";

  /** Status: cancelled. */
  public static final String CANCELLED = "CANCELLED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "share_percent", nullable = false, precision = 9, scale = 4, updatable = false)
  private BigDecimal sharePercent;

  @Column(precision = 19, scale = 2, updatable = false)
  private BigDecimal premium;

  @Column(name = "sum_insured", precision = 19, scale = 2, updatable = false)
  private BigDecimal sumInsured;

  @Column(name = "slip_file_name", nullable = false, length = 200, updatable = false)
  private String slipFileName;

  @Column(name = "slip_attachment_id")
  private Long slipAttachmentId;

  @Column(name = "placement_file_id")
  private Long placementFileId;

  @Column(nullable = false, length = 20)
  private String status = GENERATED;

  @Column(length = 10)
  private String channel;

  @Column(name = "message_no", length = 30)
  private String messageNo;

  @Column(name = "transmission_status", length = 30)
  private String transmissionStatus;

  @Column(length = 1000)
  private String recipients;

  @Column(length = 1000)
  private String cc;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "tat_start")
  private LocalDate tatStart;

  @Column(name = "sla_days")
  private Integer slaDays;

  @Column(name = "with_issue", nullable = false)
  private boolean withIssue;

  @Column(name = "resolution_date")
  private LocalDate resolutionDate;

  @Column(length = 20)
  private String response;

  @Column(name = "response_date")
  private LocalDate responseDate;

  @Column(name = "response_reason", length = 60)
  private String responseReason;

  @Column(name = "response_remarks", length = 1000)
  private String responseRemarks;

  /** For JPA. */
  protected RenewalPlacement() {}

  /**
   * A generated placement slip.
   *
   * @param c renewal
   * @param share insurer and its share
   * @param slipFileName file name of the slip
   */
  public RenewalPlacement(RenewalCandidate c, Share share, String slipFileName) {
    this.companyId = c.getCompanyId();
    this.candidateId = c.getId();
    this.insurerCode = share.insurerCode();
    this.sharePercent = share.percent();
    this.premium = share.premium();
    this.sumInsured = share.sumInsured();
    this.slipFileName = slipFileName;
  }

  /**
   * Records the stored slip and the placement file it is in.
   *
   * @param attachmentId stored slip
   * @param fileId placement file, may be null
   */
  public void stored(Long attachmentId, Long fileId) {
    this.slipAttachmentId = attachmentId;
    this.placementFileId = fileId;
  }

  /**
   * Records the sending to the insurer.
   *
   * @param sending channel, message, status and recipients
   * @param at time of sending
   * @param tat first day of the turnaround time and target in business days
   */
  public void sent(Sending sending, Instant at, Tat tat) {
    this.channel = sending.channel();
    this.messageNo = sending.messageNo();
    this.transmissionStatus = sending.status();
    this.recipients = sending.to();
    this.cc = sending.cc();
    if (sending.accepted()) {
      this.status = SENT;
      this.submittedAt = at;
      this.tatStart = tat.start();
      this.slaDays = tat.slaDays();
    }
  }

  /**
   * Records a delivery status reported by the channel.
   *
   * @param newStatus status
   */
  public void transmission(String newStatus) {
    this.transmissionStatus = newStatus;
  }

  /**
   * Tags or clears the With Issue tag.
   *
   * @param issue whether the placement has an issue
   * @param resolution resolution date, may be null
   */
  public void issue(boolean issue, LocalDate resolution) {
    this.withIssue = issue;
    this.resolutionDate = issue ? resolution : null;
  }

  /**
   * Records the insurer's response.
   *
   * @param approved approved or rejected
   * @param date response date
   * @param reason rejection reason code, may be null
   * @param remarks insurer remarks, may be null
   */
  public void respond(boolean approved, LocalDate date, String reason, String remarks) {
    this.response = approved ? APPROVED : REJECTED;
    this.status = this.response;
    this.responseDate = date;
    this.responseReason = reason;
    this.responseRemarks = remarks;
  }

  /** Cancels the placement. */
  public void cancel() {
    this.status = CANCELLED;
  }

  /**
   * Whether the placement is still current (not cancelled).
   *
   * @return true unless cancelled
   */
  public boolean isCurrent() {
    return !CANCELLED.equals(status);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public BigDecimal getSharePercent() {
    return sharePercent;
  }

  public BigDecimal getPremium() {
    return premium;
  }

  public BigDecimal getSumInsured() {
    return sumInsured;
  }

  public String getSlipFileName() {
    return slipFileName;
  }

  public Long getSlipAttachmentId() {
    return slipAttachmentId;
  }

  public Long getPlacementFileId() {
    return placementFileId;
  }

  public String getStatus() {
    return status;
  }

  public String getChannel() {
    return channel;
  }

  public String getMessageNo() {
    return messageNo;
  }

  public String getTransmissionStatus() {
    return transmissionStatus;
  }

  public String getRecipients() {
    return recipients;
  }

  public String getCc() {
    return cc;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public LocalDate getTatStart() {
    return tatStart;
  }

  public Integer getSlaDays() {
    return slaDays;
  }

  public boolean isWithIssue() {
    return withIssue;
  }

  public LocalDate getResolutionDate() {
    return resolutionDate;
  }

  public String getResponse() {
    return response;
  }

  public LocalDate getResponseDate() {
    return responseDate;
  }

  public String getResponseReason() {
    return responseReason;
  }

  public String getResponseRemarks() {
    return responseRemarks;
  }

  /**
   * An insurer's share of the renewal.
   *
   * @param insurerCode insurer
   * @param percent share in percent
   * @param premium premium of the share
   * @param sumInsured sum insured of the share
   */
  public record Share(
      String insurerCode, BigDecimal percent, BigDecimal premium, BigDecimal sumInsured) {}

  /**
   * How a placement was sent.
   *
   * @param channel MFT or CCM (or EMAIL)
   * @param messageNo channel message
   * @param status transmission status
   * @param to recipients or MFT location
   * @param cc copy recipients
   * @param accepted whether the channel accepted it
   */
  public record Sending(
      String channel, String messageNo, String status, String to, String cc, boolean accepted) {}

  /**
   * The turnaround time of a placement.
   *
   * @param start first business day counted
   * @param slaDays target in business days
   */
  public record Tat(LocalDate start, int slaDays) {}
}
