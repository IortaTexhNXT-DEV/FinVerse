package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A hold cover request of a renewal account to one insurer (FRRN.036): the initial request, an
 * extension, the request of a Clean CBG account in the file of its insurer, or the monthly CBG Home
 * extension; with how it was sent and the insurer's response (FRRN.037).
 */
@Entity
@Table(name = "rnw_hold_cover_request")
public class HoldCoverAsk extends BaseEntity {

  /** Kind: a request of the account. */
  public static final String REQUEST = "REQUEST";

  /** Kind: an extension of a confirmed hold cover. */
  public static final String EXTENSION = "EXTENSION";

  /** Kind: the request of a Clean CBG account in the file of its insurer. */
  public static final String CBG_BATCH = "CBG_BATCH";

  /** Kind: the monthly extension of an expired and unbooked CBG Home account. */
  public static final String MONTHLY_EXTENSION = "MONTHLY_EXTENSION";

  /** Status: sent, waiting for the insurer. */
  public static final String REQUESTED = "REQUESTED";

  /** Status: approved by the insurer. */
  public static final String APPROVED = "APPROVED";

  /** Status: rejected by the insurer. */
  public static final String REJECTED = "REJECTED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(nullable = false, length = 20, updatable = false)
  private String kind;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "share_percent", precision = 9, scale = 4, updatable = false)
  private BigDecimal sharePercent;

  @Column(nullable = false, updatable = false)
  private int days;

  @Column(name = "start_date", nullable = false, updatable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(nullable = false, length = 20)
  private String status = REQUESTED;

  @Column(name = "batch_no", length = 40)
  private String batchNo;

  @Column(length = 10)
  private String channel;

  @Column(name = "channel_message", length = 30)
  private String channelMessage;

  @Column(name = "insurer_ref", length = 60)
  private String insurerRef;

  @Column(name = "response_remarks", length = 1000)
  private String responseRemarks;

  @Column(name = "responded_at")
  private Instant respondedAt;

  protected HoldCoverAsk() {}

  /**
   * A new request.
   *
   * @param c renewal account
   * @param requestNo reference number
   * @param kind kind
   * @param insurer insurer and share
   * @param period start and days
   */
  public HoldCoverAsk(
      RenewalCandidate c, String requestNo, String kind, Insurer insurer, Period period) {
    this.companyId = c.getCompanyId();
    this.candidateId = c.getId();
    this.requestNo = requestNo;
    this.kind = kind;
    this.insurerCode = insurer.code();
    this.sharePercent = insurer.share();
    this.days = period.days();
    this.startDate = period.start();
    this.endDate = period.start().plusDays(period.days());
  }

  /**
   * Records how the request was sent.
   *
   * @param via MFT or EMAIL
   * @param message message number
   * @param batch file of the insurer, may be null
   */
  public void sent(String via, String message, String batch) {
    this.channel = via;
    this.channelMessage = message;
    this.batchNo = batch;
  }

  /**
   * Records the insurer's response.
   *
   * @param approved approved or rejected
   * @param end end of the hold cover confirmed, null to keep the requested one
   * @param reference insurer reference
   * @param remarks insurer remarks
   * @param at time
   */
  public void respond(
      boolean approved, LocalDate end, String reference, String remarks, Instant at) {
    if (!REQUESTED.equals(status)) {
      throw new BusinessRuleException(
          "RNW_HOLD_COVER_RESPONDED", "Hold cover request " + requestNo + " is " + status);
    }
    this.status = approved ? APPROVED : REJECTED;
    if (approved && end != null) {
      this.endDate = end;
    }
    this.insurerRef = reference;
    this.responseRemarks = remarks;
    this.respondedAt = at;
  }

  /** Cancels a request not answered. */
  public void cancel() {
    if (REQUESTED.equals(status)) {
      this.status = "CANCELLED";
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getKind() {
    return kind;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public BigDecimal getSharePercent() {
    return sharePercent;
  }

  public int getDays() {
    return days;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public String getStatus() {
    return status;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public String getChannel() {
    return channel;
  }

  public String getChannelMessage() {
    return channelMessage;
  }

  public String getInsurerRef() {
    return insurerRef;
  }

  public String getResponseRemarks() {
    return responseRemarks;
  }

  public Instant getRespondedAt() {
    return respondedAt;
  }

  /**
   * The insurer of a request.
   *
   * @param code insurer
   * @param share share of a co-insured account, may be null
   */
  public record Insurer(String code, BigDecimal share) {}

  /**
   * The period requested.
   *
   * @param start first day
   * @param days duration
   */
  public record Period(LocalDate start, int days) {}
}
