package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeRequestSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A request to maintain a payee (DIS 2.2.1): from a Marketing refund request form (RRF, MKT
 * 2.25.0), from a Disbursement user, or raised automatically when a payment request names a payee
 * that is not in the master (DIS 3.25.2). It is closed when the payee is maintained.
 */
@Entity
@Table(name = "dsb_payee_request")
public class PayeeRequest extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private PayeeRequestSource source;

  @Column(name = "payee_code", length = 30, updatable = false)
  private String payeeCode;

  @Column(name = "payee_name", nullable = false, length = 250, updatable = false)
  private String payeeName;

  @Column(length = 1000, updatable = false)
  private String details;

  @Column(name = "source_ref", length = 80, updatable = false)
  private String sourceRef;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PayeeRequestStatus status = PayeeRequestStatus.OPEN;

  @Column(name = "payee_id")
  private Long payeeId;

  @Column(name = "closed_by", length = 50)
  private String closedBy;

  @Column(name = "closed_at")
  private Instant closedAt;

  protected PayeeRequest() {}

  /**
   * An open request.
   *
   * @param companyId company
   * @param source source
   * @param payeeCode party code, may be null
   * @param payeeName payee name
   * @param details details (bank, account, purpose)
   * @param sourceRef source reference (request number)
   */
  public PayeeRequest(
      Long companyId,
      PayeeRequestSource source,
      String payeeCode,
      String payeeName,
      String details,
      String sourceRef) {
    this.companyId = companyId;
    this.source = source;
    this.payeeCode = payeeCode;
    this.payeeName = payeeName;
    this.details = details;
    this.sourceRef = sourceRef;
  }

  /**
   * Closes the request.
   *
   * @param outcome DONE or CANCELLED
   * @param payee payee maintained, may be null
   * @param user user
   * @param at time
   */
  public void close(PayeeRequestStatus outcome, Long payee, String user, Instant at) {
    if (status != PayeeRequestStatus.OPEN) {
      throw new BusinessRuleException("PAYEE_REQUEST_CLOSED", "The payee request is " + status);
    }
    status = outcome;
    payeeId = payee;
    closedBy = user;
    closedAt = at;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public PayeeRequestSource getSource() {
    return source;
  }

  public String getPayeeCode() {
    return payeeCode;
  }

  public String getPayeeName() {
    return payeeName;
  }

  public String getDetails() {
    return details;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public PayeeRequestStatus getStatus() {
    return status;
  }

  public Long getPayeeId() {
    return payeeId;
  }

  public String getClosedBy() {
    return closedBy;
  }

  public Instant getClosedAt() {
    return closedAt;
  }
}
