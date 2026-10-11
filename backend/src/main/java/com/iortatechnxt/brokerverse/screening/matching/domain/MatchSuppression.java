package com.iortatechnxt.brokerverse.screening.matching.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A client cleared as a false positive for one watchlist entry version (SNSRP-304; FR-SS-035 R2;
 * SQ12): the pair is not matched again until the entry changes (a new entry version).
 */
@Entity
@Table(name = "scr_match_suppression")
public class MatchSuppression extends BaseEntity {

  private static final int MAX_REASON = 2000;
  private static final int MAX_EVIDENCE = 500;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "client_id", nullable = false, updatable = false)
  private Long clientId;

  @Column(name = "entry_id", nullable = false, updatable = false)
  private Long entryId;

  @Column(name = "entry_version", nullable = false, updatable = false)
  private int entryVersion;

  @Column(name = "match_id", nullable = false, updatable = false)
  private Long matchId;

  @Column(name = "reason", nullable = false, length = MAX_REASON, updatable = false)
  private String reason;

  @Column(name = "evidence", nullable = false, length = MAX_EVIDENCE, updatable = false)
  private String evidence;

  @Column(name = "evidence_case_id", updatable = false)
  private Long evidenceCaseId;

  /** For JPA. */
  protected MatchSuppression() {}

  /**
   * Suppresses the pair of a match.
   *
   * @param match the match cleared
   * @param reason the justification
   * @param evidence the evidence attachment ids, comma separated
   * @param evidenceCaseId the case holding the evidence, may be {@code null}
   */
  public MatchSuppression(
      ScreeningMatch match, String reason, String evidence, Long evidenceCaseId) {
    this.companyId = match.getCompanyId();
    this.clientId = match.getClientId();
    this.entryId = match.getEntryId();
    this.entryVersion = match.getEntryVersion();
    this.matchId = match.getId();
    this.reason = reason.length() <= MAX_REASON ? reason : reason.substring(0, MAX_REASON);
    this.evidence =
        evidence.length() <= MAX_EVIDENCE ? evidence : evidence.substring(0, MAX_EVIDENCE);
    this.evidenceCaseId = evidenceCaseId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getClientId() {
    return clientId;
  }

  public Long getEntryId() {
    return entryId;
  }

  public int getEntryVersion() {
    return entryVersion;
  }

  public Long getMatchId() {
    return matchId;
  }

  public String getReason() {
    return reason;
  }

  public String getEvidence() {
    return evidence;
  }

  public Long getEvidenceCaseId() {
    return evidenceCaseId;
  }
}
