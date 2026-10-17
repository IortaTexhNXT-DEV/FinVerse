package com.iortatechnxt.brokerverse.migration.matching.api.dto;

import com.iortatechnxt.brokerverse.migration.matching.domain.ClientMatch;
import java.time.Instant;

/**
 * A pair of client records found by the matching (FR-DM-031).
 *
 * @param id pair
 * @param batchNo batch
 * @param clusterNo cluster
 * @param leftKey legacy key of the first record
 * @param rightKey legacy key of the second record, if a legacy record
 * @param rightClientCode BIBS client code, if a BIBS client
 * @param score score 0 to 100
 * @param matchedKeys keys that matched
 * @param decision AUTO_MERGE, REVIEW, MERGE or SEPARATE
 * @param survivorKey surviving record
 * @param lostValues values not kept by the survivorship rules
 * @param decidedBy reviewer
 * @param decidedAt time
 */
public record PairResponse(
    Long id,
    String batchNo,
    int clusterNo,
    String leftKey,
    String rightKey,
    String rightClientCode,
    int score,
    String matchedKeys,
    String decision,
    String survivorKey,
    String lostValues,
    String decidedBy,
    Instant decidedAt) {

  /**
   * Maps a pair.
   *
   * @param m pair
   * @param batchNo its batch
   * @return response
   */
  public static PairResponse from(ClientMatch m, String batchNo) {
    return new PairResponse(
        m.getId(),
        batchNo,
        m.getClusterNo(),
        m.getLeftKey(),
        m.getRightKey(),
        m.getRightClientCode(),
        m.getScore(),
        m.getMatchedKeys(),
        m.getDecision().name(),
        m.getSurvivorKey(),
        m.getLostValues(),
        m.getDecidedBy(),
        m.getDecidedAt());
  }
}
