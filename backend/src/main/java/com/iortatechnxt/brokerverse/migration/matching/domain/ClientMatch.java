package com.iortatechnxt.brokerverse.migration.matching.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A matched pair of legacy clients (DATA_MIGRATION_DESIGN section 9; FR-DM-031): the two staged
 * rows (or a staged row and a client already in BIBS), the score, the keys that matched, the
 * decision (merged automatically, waiting for review, merged or kept separate by the Data Steward)
 * and the values that lost to the survivor.
 */
@Entity
@Table(name = "mig_client_match")
public class ClientMatch extends BaseEntity {

  /** Decision on a pair. */
  public enum Decision {
    /** Score at or above the auto-merge score. */
    AUTO_MERGE,
    /** Waiting for the Data Steward. */
    REVIEW,
    /** Merged by the Data Steward. */
    MERGE,
    /** Kept as two clients by the Data Steward. */
    KEEP_SEPARATE
  }

  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long batchId;

  @Column(name = "cluster_no", nullable = false)
  private int clusterNo;

  @Column(name = "left_row_id", nullable = false, updatable = false)
  private Long leftRowId;

  @Column(name = "left_key", nullable = false, length = 200, updatable = false)
  private String leftKey;

  @Column(name = "right_row_id", updatable = false)
  private Long rightRowId;

  @Column(name = "right_key", length = 200, updatable = false)
  private String rightKey;

  @Column(name = "right_client_code", length = 30, updatable = false)
  private String rightClientCode;

  @Column(nullable = false)
  private int score;

  @Column(name = "matched_keys", nullable = false, length = 200)
  private String matchedKeys;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Decision decision;

  @Column(name = "survivor_key", length = 200)
  private String survivorKey;

  @Column(name = "lost_values", columnDefinition = "text")
  private String lostValues;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  protected ClientMatch() {}

  /**
   * A new pair.
   *
   * @param batchId batch
   * @param left left staged row and key
   * @param right right staged row and key, or the BIBS client code
   * @param score score
   * @param matchedKeys keys that matched
   * @param decision first decision
   */
  public ClientMatch(
      Long batchId, Side left, Side right, int score, String matchedKeys, Decision decision) {
    this.batchId = batchId;
    this.leftRowId = left.rowId();
    this.leftKey = left.key();
    this.rightRowId = right.rowId();
    this.rightKey = right.key();
    this.rightClientCode = right.clientCode();
    this.score = score;
    this.matchedKeys = matchedKeys;
    this.decision = decision;
  }

  /**
   * The Data Steward decided the pair.
   *
   * @param merge merge the two
   * @param user Data Steward
   * @param when time
   */
  public void decide(boolean merge, String user, Instant when) {
    this.decision = merge ? Decision.MERGE : Decision.KEEP_SEPARATE;
    this.decidedBy = user;
    this.decidedAt = when;
  }

  /**
   * The cluster and survivor of the pair.
   *
   * @param cluster cluster number
   * @param survivor legacy key of the surviving record
   * @param lost values that lost to the survivor
   */
  public void survivor(int cluster, String survivor, String lost) {
    this.clusterNo = cluster;
    this.survivorKey = survivor;
    this.lostValues = lost;
  }

  public boolean merges() {
    return decision == Decision.AUTO_MERGE || decision == Decision.MERGE;
  }

  public Long getBatchId() {
    return batchId;
  }

  public int getClusterNo() {
    return clusterNo;
  }

  public Long getLeftRowId() {
    return leftRowId;
  }

  public String getLeftKey() {
    return leftKey;
  }

  public Long getRightRowId() {
    return rightRowId;
  }

  public String getRightKey() {
    return rightKey;
  }

  public String getRightClientCode() {
    return rightClientCode;
  }

  public int getScore() {
    return score;
  }

  public String getMatchedKeys() {
    return matchedKeys;
  }

  public Decision getDecision() {
    return decision;
  }

  public String getSurvivorKey() {
    return survivorKey;
  }

  public String getLostValues() {
    return lostValues;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  /**
   * One side of a pair.
   *
   * @param rowId staged row, null for a BIBS client
   * @param key legacy key (source:number) or the BIBS client code
   * @param clientCode BIBS client code, null for a staged row
   */
  public record Side(Long rowId, String key, String clientCode) {}
}
