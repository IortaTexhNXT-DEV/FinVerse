package com.iortatechnxt.brokerverse.submitted.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The Insurance Adequacy Assessment Form of a policy (BRIDSP-05-07): one per policy, generated once
 * the last review is adequate, approved by the IAAF matrix and sent to the bank counterpart.
 */
@Entity
@Table(name = "sbm_iaaf")
public class SbmIaaf extends SbmApprovable {

  @Column(name = "iaaf_no", nullable = false, updatable = false, length = 30)
  private String iaafNo;

  @Column(name = "policy_id", nullable = false, updatable = false)
  private Long policyId;

  @Column(name = "template_version")
  private Integer templateVersion;

  @Column(name = "sent_to", length = 300)
  private String sentTo;

  @Column(name = "sent_at")
  private Instant sentAt;

  protected SbmIaaf() {}

  /**
   * A new IAAF.
   *
   * @param companyId company
   * @param iaafNo number
   * @param policyId policy
   */
  public SbmIaaf(Long companyId, String iaafNo, Long policyId) {
    super(companyId);
    this.iaafNo = iaafNo;
    this.policyId = policyId;
  }

  @Override
  public String number() {
    return iaafNo;
  }

  /**
   * Records the template of the PDF.
   *
   * @param version template version
   */
  public void rendered(int version) {
    this.templateVersion = version;
  }

  /**
   * Sent to the bank counterpart (ISSUED).
   *
   * @param to recipients
   * @param at time
   */
  public void issued(String to, Instant at) {
    end(SbmDocStatus.ISSUED);
    this.sentTo = to;
    this.sentAt = at;
  }

  public String getIaafNo() {
    return iaafNo;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public Integer getTemplateVersion() {
    return templateVersion;
  }

  public String getSentTo() {
    return sentTo;
  }

  public Instant getSentAt() {
    return sentAt;
  }
}
