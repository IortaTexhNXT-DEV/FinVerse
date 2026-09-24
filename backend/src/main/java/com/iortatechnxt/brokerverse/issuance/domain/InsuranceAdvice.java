package com.iortatechnxt.brokerverse.issuance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An Insurance Advice IA-yyyy-nnnnnn (BRNB.070): the notice to the mortgagee bank that a mortgaged
 * account is insured, generated from the INSURANCE_ADVICE template. Kept with its PDF for the
 * register (BRNB.060) and sent password protected (BRNB.035).
 */
@Entity
@Table(name = "iss_insurance_advice")
public class InsuranceAdvice extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "ia_no", nullable = false, length = 30, updatable = false)
  private String iaNo;

  @Column(name = "account_id", nullable = false, updatable = false)
  private Long accountId;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "client_id", nullable = false, updatable = false)
  private Long clientId;

  @Column(name = "client_name", nullable = false, length = 250, updatable = false)
  private String clientName;

  @Column(name = "mortgagee_bank", nullable = false, length = 40, updatable = false)
  private String mortgageeBank;

  @Column(name = "insurer_code", length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "policy_numbers", length = 500, updatable = false)
  private String policyNumbers;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_event", nullable = false, length = 20, updatable = false)
  private AdviceTrigger triggerEvent;

  @Column(name = "template_version", nullable = false, length = 60, updatable = false)
  private String templateVersion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AdviceStatus status = AdviceStatus.GENERATED;

  @Column(name = "file_name", nullable = false, length = 255, updatable = false)
  private String fileName;

  @Column(nullable = false, length = 64, updatable = false)
  private String sha256;

  @Column(nullable = false, updatable = false)
  private byte[] content;

  @Column(name = "send_count", nullable = false)
  private int sendCount;

  @Column(name = "last_sent_at")
  private Instant lastSentAt;

  @Column(name = "last_sent_to", length = 500)
  private String lastSentTo;

  protected InsuranceAdvice() {}

  /**
   * Creates a generated advice.
   *
   * @param companyId company
   * @param iaNo IA number
   * @param subject account the advice is about
   * @param document template version, trigger and rendered PDF
   */
  public InsuranceAdvice(
      Long companyId, String iaNo, AdviceSubject subject, AdviceDocument document) {
    this.companyId = companyId;
    this.iaNo = iaNo;
    this.accountId = subject.accountId();
    this.arn = subject.arn();
    this.clientId = subject.clientId();
    this.clientName = subject.clientName();
    this.mortgageeBank = subject.mortgageeBank();
    this.insurerCode = subject.insurerCode();
    this.policyNumbers = subject.policyNumbers();
    this.triggerEvent = document.trigger();
    this.templateVersion = document.templateVersion();
    this.fileName = document.fileName();
    this.sha256 = document.sha256();
    this.content = document.content();
  }

  /**
   * Records a send.
   *
   * @param to recipients
   * @param when time
   */
  public void sent(String to, Instant when) {
    this.status = AdviceStatus.SENT;
    this.sendCount++;
    this.lastSentAt = when;
    this.lastSentTo = to;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getIaNo() {
    return iaNo;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getArn() {
    return arn;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getClientName() {
    return clientName;
  }

  public String getMortgageeBank() {
    return mortgageeBank;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getPolicyNumbers() {
    return policyNumbers;
  }

  public AdviceTrigger getTriggerEvent() {
    return triggerEvent;
  }

  public String getTemplateVersion() {
    return templateVersion;
  }

  public AdviceStatus getStatus() {
    return status;
  }

  public String getFileName() {
    return fileName;
  }

  public String getSha256() {
    return sha256;
  }

  public byte[] getContent() {
    return content.clone();
  }

  public int getSendCount() {
    return sendCount;
  }

  public Instant getLastSentAt() {
    return lastSentAt;
  }

  public String getLastSentTo() {
    return lastSentTo;
  }
}
