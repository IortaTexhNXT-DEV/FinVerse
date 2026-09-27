package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Submissions of documents to insurers. */
public interface EbSubmissionRepository extends JpaRepository<EbSubmission, Long> {

  /**
   * Submissions of a programme.
   *
   * @param programmeId programme
   * @return submissions, latest first
   */
  List<EbSubmission> findByProgrammeIdOrderBySentAtDescIdDesc(Long programmeId);

  /**
   * Submissions of a member change.
   *
   * @param memberChangeId member change
   * @return submissions, latest first
   */
  List<EbSubmission> findByMemberChangeIdOrderBySentAtDescIdDesc(Long memberChangeId);
}
