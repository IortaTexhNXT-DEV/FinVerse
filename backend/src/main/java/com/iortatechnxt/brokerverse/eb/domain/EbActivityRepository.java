package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Activity stamps of the TAT annex. */
public interface EbActivityRepository extends JpaRepository<EbActivity, Long> {

  /**
   * The open (not released) stamp of an activity and reference.
   *
   * @param activity activity
   * @param reference business reference
   * @return stamp
   */
  Optional<EbActivity> findFirstByActivityAndReferenceAndReleasedAtIsNullOrderByIdDesc(
      TatActivity activity, String reference);

  /**
   * The stamps of a programme, oldest first (History tab).
   *
   * @param programmeId programme
   * @return stamps
   */
  List<EbActivity> findByProgrammeIdOrderByReceivedAtAscIdAsc(Long programmeId);
}
