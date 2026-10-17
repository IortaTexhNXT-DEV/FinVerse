package com.iortatechnxt.brokerverse.csf.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Verification of a caller before a contact change (FR-CSF-020; e-mail topic 5): the checks asked,
 * whether each answer matched, the result and how long a passed verification stays valid. Immutable
 * once recorded.
 */
@Entity
@Table(name = "csf_verification")
public class CsfVerification extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "client_id", nullable = false, updatable = false)
  private Long clientId;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(nullable = false, length = 20, updatable = false)
  private String channel;

  @Column(nullable = false, updatable = false)
  private int matches;

  @Column(nullable = false, updatable = false)
  private int required;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private VerificationResult result;

  @Column(name = "verified_at", nullable = false, updatable = false)
  private Instant verifiedAt;

  @Column(name = "valid_until", nullable = false, updatable = false)
  private Instant validUntil;

  @Column(nullable = false, length = 50, updatable = false)
  private String agent;

  @Column(length = 500, updatable = false)
  private String remarks;

  @ElementCollection
  @CollectionTable(
      name = "csf_verification_check",
      joinColumns = @JoinColumn(name = "verification_id"))
  @OrderColumn(name = "check_index")
  private final List<CheckResult> checks = new ArrayList<>();

  protected CsfVerification() {}

  /**
   * Records a verification.
   *
   * @param subject client verified
   * @param checks checks with their outcome
   * @param outcome result, required matches and validity
   * @param agent agent
   * @param remarks remarks, may be null
   */
  public CsfVerification(
      ClientRef subject, List<CheckResult> checks, Outcome outcome, String agent, String remarks) {
    this.companyId = subject.companyId();
    this.clientId = subject.clientId();
    this.clientCode = subject.clientCode();
    this.channel = outcome.channel();
    this.checks.addAll(checks);
    this.matches = (int) checks.stream().filter(CheckResult::isMatched).count();
    this.required = outcome.required();
    this.result = matches >= required ? VerificationResult.PASSED : VerificationResult.FAILED;
    this.verifiedAt = outcome.verifiedAt();
    this.validUntil = outcome.validUntil();
    this.agent = agent;
    this.remarks = remarks;
  }

  /**
   * Whether the verification allows a change at a time.
   *
   * @param now time
   * @return true when passed and still valid
   */
  public boolean allowsChangeAt(Instant now) {
    return result == VerificationResult.PASSED && now.isBefore(validUntil);
  }

  /**
   * The client a record belongs to.
   *
   * @param companyId company
   * @param clientId client id
   * @param clientCode client or prospect code
   */
  public record ClientRef(Long companyId, Long clientId, String clientCode) {}

  /**
   * How a verification is judged.
   *
   * @param channel channel of the contact (list CSF_CHANNEL)
   * @param required checks that must match
   * @param verifiedAt time of the verification
   * @param validUntil end of the validity of a pass
   */
  public record Outcome(String channel, int required, Instant verifiedAt, Instant validUntil) {}

  public Long getCompanyId() {
    return companyId;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getChannel() {
    return channel;
  }

  public int getMatches() {
    return matches;
  }

  public int getRequired() {
    return required;
  }

  public VerificationResult getResult() {
    return result;
  }

  public Instant getVerifiedAt() {
    return verifiedAt;
  }

  public Instant getValidUntil() {
    return validUntil;
  }

  public String getAgent() {
    return agent;
  }

  public String getRemarks() {
    return remarks;
  }

  public List<CheckResult> getChecks() {
    return List.copyOf(checks);
  }
}
