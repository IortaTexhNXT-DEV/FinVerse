package com.iortatechnxt.brokerverse.screening.cases.api.dto;

import com.iortatechnxt.brokerverse.screening.cases.domain.CommitteeVote;
import java.time.Instant;

/**
 * An AML Committee vote (FR-SS-064 Decisions tab).
 *
 * @param id vote id
 * @param roundNo committee round
 * @param member member
 * @param decision decision
 * @param remarks remarks
 * @param votedAt time
 */
public record VoteDto(
    Long id, int roundNo, String member, String decision, String remarks, Instant votedAt) {

  /**
   * Maps a vote.
   *
   * @param v the vote
   * @return the DTO
   */
  public static VoteDto from(CommitteeVote v) {
    return new VoteDto(
        v.getId(), v.getRoundNo(), v.getMember(), v.getDecision(), v.getRemarks(), v.getVotedAt());
  }
}
