package com.iortatechnxt.brokerverse.screening.matching.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchAlgorithm;
import com.iortatechnxt.brokerverse.screening.config.domain.MatchField;
import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A client matched against a watchlist entry version under a configuration version (SNSRP-301,
 * FR-SS-031): score, algorithm, matched fields and status POTENTIAL, TRUE_MATCH or FALSE_POSITIVE.
 * Unique per client, entry, entry version and configuration version, so a re-run records no second
 * match (FR-SS-030 R1).
 */
@Entity
@Table(name = "scr_match")
public class ScreeningMatch extends BaseEntity {

  private static final int MAX_NAME = 300;
  private static final int MAX_REMARKS = 2000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "run_id", nullable = false, updatable = false)
  private Long runId;

  @Column(name = "client_id", nullable = false, updatable = false)
  private Long clientId;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(name = "client_name", nullable = false, length = MAX_NAME, updatable = false)
  private String clientName;

  @Column(name = "entry_id", nullable = false, updatable = false)
  private Long entryId;

  @Column(name = "entry_version", nullable = false, updatable = false)
  private int entryVersion;

  @Column(name = "entry_name", nullable = false, length = MAX_NAME, updatable = false)
  private String entryName;

  @Column(name = "source_code", nullable = false, length = 30, updatable = false)
  private String sourceCode;

  @Column(name = "list_type", nullable = false, length = 30, updatable = false)
  private String listType;

  @Enumerated(EnumType.STRING)
  @Column(name = "subject_type", nullable = false, length = 20, updatable = false)
  private SubjectType subjectType;

  @Column(name = "match_version_id", nullable = false, updatable = false)
  private Long matchVersionId;

  @Column(name = "match_rule_id", updatable = false)
  private Long matchRuleId;

  @Column(name = "score", nullable = false, precision = 5, scale = 4, updatable = false)
  private BigDecimal score;

  @Enumerated(EnumType.STRING)
  @Column(name = "algorithm", nullable = false, length = 10, updatable = false)
  private MatchAlgorithm algorithm;

  @Column(name = "matched_fields", nullable = false, length = 100, updatable = false)
  private String matchedFields;

  @Column(name = "case_threshold", nullable = false, updatable = false)
  private boolean caseThreshold;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private MatchStatus status = MatchStatus.POTENTIAL;

  @Column(name = "case_id")
  private Long caseId;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_remarks", length = MAX_REMARKS)
  private String decisionRemarks;

  /** For JPA. */
  protected ScreeningMatch() {}

  /**
   * Records a POTENTIAL match.
   *
   * @param runId the run
   * @param pair the client and the entry
   * @param result score, algorithm, fields and rule
   */
  public ScreeningMatch(Long runId, MatchPair pair, MatchScore result) {
    this.companyId = pair.companyId();
    this.runId = runId;
    this.clientId = pair.clientId();
    this.clientCode = pair.clientCode();
    this.clientName = cap(pair.clientName(), MAX_NAME);
    this.entryId = pair.entryId();
    this.entryVersion = pair.entryVersion();
    this.entryName = cap(pair.entryName(), MAX_NAME);
    this.sourceCode = pair.sourceCode();
    this.listType = pair.listType();
    this.subjectType = pair.subjectType();
    this.matchVersionId = result.matchVersionId();
    this.matchRuleId = result.matchRuleId();
    this.score = result.score();
    this.algorithm = result.algorithm();
    this.matchedFields =
        result.fields().stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    this.caseThreshold = result.reachesCase();
  }

  /**
   * Confirms the match (investigation decision, SNSRP-302 evaluation point "TRUE_MATCH").
   *
   * @param user who decided
   * @param when when
   * @param remarks remarks, may be {@code null}
   */
  public void confirm(String user, Instant when, String remarks) {
    requirePotential();
    decide(MatchStatus.TRUE_MATCH, user, when, remarks);
  }

  /**
   * Clears the match as a false positive (SNSRP-304).
   *
   * @param user who decided
   * @param when when
   * @param justification justification
   */
  public void clear(String user, Instant when, String justification) {
    if (status == MatchStatus.FALSE_POSITIVE) {
      throw new BusinessRuleException(
          "SCR_MATCH_ALREADY_DECIDED", "The match is already a false positive");
    }
    decide(MatchStatus.FALSE_POSITIVE, user, when, justification);
  }

  /**
   * Stamps the screening case the match belongs to.
   *
   * @param id the case
   */
  public void linkCase(Long id) {
    this.caseId = id;
  }

  private void requirePotential() {
    if (status != MatchStatus.POTENTIAL) {
      throw new BusinessRuleException(
          "SCR_MATCH_ALREADY_DECIDED", "The match is already " + status.name().replace('_', ' '));
    }
  }

  private void decide(MatchStatus to, String user, Instant when, String remarks) {
    this.status = to;
    this.decidedBy = user;
    this.decidedAt = when;
    this.decisionRemarks = cap(remarks, MAX_REMARKS);
  }

  private static String cap(String value, int max) {
    return value == null || value.length() <= max ? value : value.substring(0, max);
  }

  /**
   * The matched fields.
   *
   * @return fields
   */
  public Set<MatchField> fields() {
    Set<MatchField> fields = EnumSet.noneOf(MatchField.class);
    if (matchedFields != null && !matchedFields.isBlank()) {
      Arrays.stream(matchedFields.split(",")).map(MatchField::valueOf).forEach(fields::add);
    }
    return fields;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getRunId() {
    return runId;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getClientName() {
    return clientName;
  }

  public Long getEntryId() {
    return entryId;
  }

  public int getEntryVersion() {
    return entryVersion;
  }

  public String getEntryName() {
    return entryName;
  }

  public String getSourceCode() {
    return sourceCode;
  }

  public String getListType() {
    return listType;
  }

  public SubjectType getSubjectType() {
    return subjectType;
  }

  public Long getMatchVersionId() {
    return matchVersionId;
  }

  public Long getMatchRuleId() {
    return matchRuleId;
  }

  public BigDecimal getScore() {
    return score;
  }

  public MatchAlgorithm getAlgorithm() {
    return algorithm;
  }

  public String getMatchedFields() {
    return matchedFields;
  }

  public boolean isCaseThreshold() {
    return caseThreshold;
  }

  public MatchStatus getStatus() {
    return status;
  }

  public Long getCaseId() {
    return caseId;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecisionRemarks() {
    return decisionRemarks;
  }

  /**
   * The client and entry of a match.
   *
   * @param companyId company
   * @param clientId client
   * @param clientCode client code
   * @param clientName client name
   * @param entryId entry
   * @param entryVersion entry version
   * @param entryName entry primary name
   * @param sourceCode source code
   * @param listType list type
   * @param subjectType individual or entity
   */
  public record MatchPair(
      Long companyId,
      Long clientId,
      String clientCode,
      String clientName,
      Long entryId,
      int entryVersion,
      String entryName,
      String sourceCode,
      String listType,
      SubjectType subjectType) {}

  /**
   * The scoring result of a match.
   *
   * @param matchVersionId MATCH_CRITERIA version
   * @param matchRuleId matching rule
   * @param score score, 0 to 1
   * @param algorithm algorithm of the score
   * @param fields matched fields
   * @param reachesCase whether the case threshold is reached
   */
  public record MatchScore(
      Long matchVersionId,
      Long matchRuleId,
      BigDecimal score,
      MatchAlgorithm algorithm,
      Set<MatchField> fields,
      boolean reachesCase) {

    /** Defensive copy. */
    public MatchScore {
      fields = Set.copyOf(fields);
    }
  }
}
