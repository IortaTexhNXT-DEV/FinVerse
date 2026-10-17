package com.iortatechnxt.brokerverse.screening.matching.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** False-positive suppressions (SNSRP-304, SQ12). */
public interface MatchSuppressionRepository extends JpaRepository<MatchSuppression, Long> {

  /**
   * Whether a client is suppressed for an entry version.
   *
   * @param clientId client
   * @param entryId entry
   * @param entryVersion entry version
   * @return true when suppressed
   */
  boolean existsByClientIdAndEntryIdAndEntryVersion(Long clientId, Long entryId, int entryVersion);
}
