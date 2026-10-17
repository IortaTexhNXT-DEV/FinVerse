package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Sign-offs of the comparatives. */
public interface EbComparativeSignoffRepository extends JpaRepository<EbComparativeSignoff, Long> {

  /**
   * Decisions on a comparative.
   *
   * @param comparativeId comparative
   * @return decisions, oldest first
   */
  List<EbComparativeSignoff> findByComparativeIdOrderByIdAsc(Long comparativeId);
}
