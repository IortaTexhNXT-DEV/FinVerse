package com.iortatechnxt.brokerverse.submitted.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Terms of Reference of an account above the insurer limits (BRIDSP-17-19): the breached limits
 * and the proposed terms, approved by the TSU matrix and released when the Account Officer opens or
 * downloads it.
 */
@Entity
@Table(name = "sbm_tor")
public class SbmTor extends SbmApprovable {

  @Column(name = "tor_no", nullable = false, updatable = false, length = 30)
  private String torNo;

  @Column(name = "policy_id", updatable = false)
  private Long policyId;

  @Column(length = 30)
  private String arn;

  @Column(nullable = false, length = 2000)
  private String breaches;

  @Column(name = "proposed_terms", nullable = false, length = 4000)
  private String proposedTerms;

  @Column(name = "ao_username", nullable = false, length = 50)
  private String aoUsername;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "released_at")
  private Instant releasedAt;

  protected SbmTor() {}

  /**
   * A new TOR.
   *
   * @param companyId company
   * @param torNo number
   * @param account masterlist record and ARN
   * @param content breaches, proposed terms and Account Officer
   */
  public SbmTor(Long companyId, String torNo, Account account, Content content) {
    super(companyId);
    this.torNo = torNo;
    this.policyId = account.policyId();
    this.arn = account.arn();
    change(content);
  }

  @Override
  public String number() {
    return torNo;
  }

  /**
   * Changes a draft or returned TOR.
   *
   * @param content breaches, proposed terms and Account Officer
   */
  public void change(Content content) {
    this.breaches = content.breaches();
    this.proposedTerms = content.proposedTerms();
    this.aoUsername = content.aoUsername();
  }

  /**
   * Time of the last approval.
   *
   * @param at time
   */
  public void approvedAt(Instant at) {
    this.approvedAt = at;
  }

  /**
   * Released to the Account Officer.
   *
   * @param at time
   */
  public void released(Instant at) {
    end(SbmDocStatus.RELEASED);
    this.releasedAt = at;
  }

  public String getTorNo() {
    return torNo;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public String getArn() {
    return arn;
  }

  public String getBreaches() {
    return breaches;
  }

  public String getProposedTerms() {
    return proposedTerms;
  }

  public String getAoUsername() {
    return aoUsername;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public Instant getReleasedAt() {
    return releasedAt;
  }

  /**
   * The account of a TOR.
   *
   * @param policyId masterlist record, may be null
   * @param arn renewal account, may be null
   */
  public record Account(Long policyId, String arn) {}

  /**
   * The content of a TOR.
   *
   * @param breaches breached limits (one per line)
   * @param proposedTerms proposed terms
   * @param aoUsername Account Officer
   */
  public record Content(String breaches, String proposedTerms, String aoUsername) {}
}
