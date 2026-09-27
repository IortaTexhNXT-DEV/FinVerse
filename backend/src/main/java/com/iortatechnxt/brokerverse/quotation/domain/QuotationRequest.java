package com.iortatechnxt.brokerverse.quotation.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A quotation request waiting to be quoted (BRNB.041, staging of BRNB.023): captured from an e-mail
 * (attached as REQUEST_EMAIL), uploaded in bulk or delivered by a source system. It names an
 * existing client or only the prospect's details.
 */
@Entity
@Table(name = "quo_request")
public class QuotationRequest extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Column(nullable = false, length = 40, updatable = false)
  private String channel;

  @Column(name = "external_ref", length = 60, updatable = false)
  private String externalRef;

  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;

  @Column(name = "client_id")
  private Long clientId;

  @Column(name = "prospect_name", length = 250)
  private String prospectName;

  @Column(name = "prospect_email", length = 120)
  private String prospectEmail;

  @Column(name = "prospect_mobile", length = 30)
  private String prospectMobile;

  @Column(name = "product_code", length = 20)
  private String productCode;

  @Column(name = "market_segment", length = 40)
  private String marketSegment;

  @Column(name = "requested_cover", length = 2000)
  private String requestedCover;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RequestStatus status = RequestStatus.NEW;

  @Column(name = "quotation_id")
  private Long quotationId;

  @Column(name = "close_reason", length = 300)
  private String closeReason;

  protected QuotationRequest() {}

  /**
   * Records a received request.
   *
   * @param companyId company
   * @param requestNo request number
   * @param facts what was requested
   */
  public QuotationRequest(Long companyId, String requestNo, RequestFacts facts) {
    this.companyId = companyId;
    this.requestNo = requestNo;
    this.channel = facts.channel();
    this.externalRef = facts.externalRef();
    this.receivedAt = facts.receivedAt();
    this.clientId = facts.clientId();
    this.prospectName = facts.prospectName();
    this.prospectEmail = facts.prospectEmail();
    this.prospectMobile = facts.prospectMobile();
    this.productCode = facts.productCode();
    this.marketSegment = facts.marketSegment();
    this.requestedCover = facts.requestedCover();
  }

  /**
   * Links the client (existing or the prospect created for the request).
   *
   * @param id client id
   */
  public void linkClient(Long id) {
    requireNew();
    this.clientId = id;
  }

  /**
   * Marks the request quoted.
   *
   * @param quotation quotation id
   */
  public void markQuoted(Long quotation) {
    requireNew();
    this.status = RequestStatus.QUOTED;
    this.quotationId = quotation;
  }

  /**
   * Closes the request without a quotation.
   *
   * @param reason reason
   */
  public void close(String reason) {
    requireNew();
    this.status = RequestStatus.CLOSED;
    this.closeReason = reason;
  }

  private void requireNew() {
    if (status != RequestStatus.NEW) {
      throw new BusinessRuleException(
          "QUOTATION_REQUEST_CLOSED", "Request " + requestNo + " is already " + status);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public String getChannel() {
    return channel;
  }

  public String getExternalRef() {
    return externalRef;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getProspectName() {
    return prospectName;
  }

  public String getProspectEmail() {
    return prospectEmail;
  }

  public String getProspectMobile() {
    return prospectMobile;
  }

  public String getProductCode() {
    return productCode;
  }

  public String getMarketSegment() {
    return marketSegment;
  }

  public String getRequestedCover() {
    return requestedCover;
  }

  public RequestStatus getStatus() {
    return status;
  }

  public Long getQuotationId() {
    return quotationId;
  }

  public String getCloseReason() {
    return closeReason;
  }

  /**
   * What a request asks for.
   *
   * @param channel source channel (list SOURCE_CHANNEL)
   * @param externalRef reference in the source system, may be null
   * @param receivedAt time received
   * @param clientId existing client, may be null
   * @param prospectName prospect name when no client
   * @param prospectEmail prospect e-mail
   * @param prospectMobile prospect mobile
   * @param productCode product requested, may be null
   * @param marketSegment market segment
   * @param requestedCover requested cover as described by the requester
   */
  public record RequestFacts(
      String channel,
      String externalRef,
      Instant receivedAt,
      Long clientId,
      String prospectName,
      String prospectEmail,
      String prospectMobile,
      String productCode,
      String marketSegment,
      String requestedCover) {}
}
