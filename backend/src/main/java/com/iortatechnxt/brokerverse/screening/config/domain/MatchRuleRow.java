package com.iortatechnxt.brokerverse.screening.config.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A matching rule of a MATCH_CRITERIA version (SNSRP-101). Rows are replaced as a whole while the
 * version is a draft.
 */
@Entity
@Table(name = "scr_match_rule")
public class MatchRuleRow extends BaseEntity {

  @Column(name = "version_id", nullable = false, updatable = false)
  private Long versionId;

  @Column(name = "sort_order", nullable = false, updatable = false)
  private int sortOrder;

  @Column(name = "list_type", nullable = false, length = 30, updatable = false)
  private String listType;

  @Enumerated(EnumType.STRING)
  @Column(name = "subject_type", nullable = false, length = 20, updatable = false)
  private SubjectType subjectType;

  @Enumerated(EnumType.STRING)
  @Column(name = "algorithm", nullable = false, length = 20, updatable = false)
  private MatchAlgorithm algorithm;

  @Column(name = "threshold", nullable = false, precision = 5, scale = 4, updatable = false)
  private BigDecimal threshold;

  @Column(name = "match_fields", nullable = false, length = 200, updatable = false)
  private String matchFields;

  @Column(
      name = "min_score_for_case",
      nullable = false,
      precision = 5,
      scale = 4,
      updatable = false)
  private BigDecimal minScoreForCase;

  /** For JPA. */
  protected MatchRuleRow() {}

  /**
   * Creates a row.
   *
   * @param versionId the owning version id
   * @param sortOrder display order
   * @param listType list type code (SCR_LIST_TYPE)
   * @param subjectType individual or entity
   * @param algorithm algorithm
   * @param threshold potential-match threshold
   * @param matchFields fields compared, comma separated
   * @param minScoreForCase case threshold
   */
  @SuppressWarnings("java:S107")
  public MatchRuleRow(
      Long versionId,
      int sortOrder,
      String listType,
      SubjectType subjectType,
      MatchAlgorithm algorithm,
      BigDecimal threshold,
      String matchFields,
      BigDecimal minScoreForCase) {
    this.versionId = versionId;
    this.sortOrder = sortOrder;
    this.listType = listType;
    this.subjectType = subjectType;
    this.algorithm = algorithm;
    this.threshold = threshold;
    this.matchFields = matchFields;
    this.minScoreForCase = minScoreForCase;
  }

  public Long getVersionId() {
    return versionId;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public String getListType() {
    return listType;
  }

  public SubjectType getSubjectType() {
    return subjectType;
  }

  public MatchAlgorithm getAlgorithm() {
    return algorithm;
  }

  public BigDecimal getThreshold() {
    return threshold;
  }

  public String getMatchFields() {
    return matchFields;
  }

  public BigDecimal getMinScoreForCase() {
    return minScoreForCase;
  }
}
