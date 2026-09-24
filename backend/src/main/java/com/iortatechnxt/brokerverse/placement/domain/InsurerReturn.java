package com.iortatechnxt.brokerverse.placement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A placement returned by the insurer with its reason and remarks (BRNB.034/059), and how the
 * return was resolved: resubmitted, returned to Marketing or cancelled (return queue).
 */
@Entity
@Table(name = "plc_insurer_return")
public class InsurerReturn extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "account_id", nullable = false, updatable = false)
  private Long accountId;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "insurer_code", length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "slip_no", length = 30, updatable = false)
  private String slipNo;

  @Column(name = "reason_code", nullable = false, length = 40, updatable = false)
  private String reasonCode;

  @Column(length = 1000, updatable = false)
  private String remarks;

  @Column(length = 30)
  private String resolution;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  protected InsurerReturn() {}

  /**
   * Records a return.
   *
   * @param companyId company
   * @param account account id and ARN
   * @param insurerCode insurer that returned the placement
   * @param slipNo slip the return concerns, may be null
   * @param reasonCode reason (list RETURN_REASON)
   * @param remarks insurer remarks
   */
  public InsurerReturn(
      Long companyId,
      SlipAccount account,
      String insurerCode,
      String slipNo,
      String reasonCode,
      String remarks) {
    this.companyId = companyId;
    this.accountId = account.accountId();
    this.arn = account.arn();
    this.insurerCode = insurerCode;
    this.slipNo = slipNo;
    this.reasonCode = reasonCode;
    this.remarks = remarks;
  }

  /**
   * Records how the return was resolved.
   *
   * @param how the workflow action that left the return (resubmit, return, cancel_placement)
   * @param when time
   */
  public void resolve(String how, Instant when) {
    this.resolution = how;
    this.resolvedAt = when;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getArn() {
    return arn;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getSlipNo() {
    return slipNo;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public String getRemarks() {
    return remarks;
  }

  public String getResolution() {
    return resolution;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }
}
