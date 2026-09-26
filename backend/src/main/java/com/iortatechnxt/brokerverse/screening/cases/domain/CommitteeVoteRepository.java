package com.iortatechnxt.brokerverse.screening.cases.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** AML Committee votes (SNSRP-704). */
public interface CommitteeVoteRepository extends JpaRepository<CommitteeVote, Long> {

  /**
   * The votes of a case round, in order.
   *
   * @param caseId case
   * @param roundNo committee round
   * @return votes
   */
  List<CommitteeVote> findByCaseIdAndRoundNoOrderByIdAsc(Long caseId, int roundNo);

  /**
   * Every vote of a case, in order (Decisions tab).
   *
   * @param caseId case
   * @return votes
   */
  List<CommitteeVote> findByCaseIdOrderByIdAsc(Long caseId);

  /**
   * Whether a member voted on a round (FR-SS-064 R1).
   *
   * @param caseId case
   * @param roundNo round
   * @param member member (as stored)
   * @return true when voted
   */
  boolean existsByCaseIdAndRoundNoAndMemberIgnoreCase(Long caseId, int roundNo, String member);
}
