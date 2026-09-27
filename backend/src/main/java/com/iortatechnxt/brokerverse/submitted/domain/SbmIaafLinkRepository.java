package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Links of the IAAFs. */
public interface SbmIaafLinkRepository extends JpaRepository<SbmIaafLink, Long> {

  /**
   * Links of an IAAF.
   *
   * @param iaafId IAAF
   * @return links
   */
  List<SbmIaafLink> findByIaafIdOrderByIdAsc(Long iaafId);
}
