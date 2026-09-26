package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** EB programmes. */
public interface EbProgrammeRepository extends JpaRepository<EbProgramme, Long> {

  /**
   * A programme by number, with its lines.
   *
   * @param programmeNo programme number
   * @return programme
   */
  @EntityGraph(attributePaths = "lines", type = EntityGraph.EntityGraphType.LOAD)
  Optional<EbProgramme> findByProgrammeNo(String programmeNo);

  /**
   * The programmes of a client.
   *
   * @param clientId client
   * @return programmes, newest first
   */
  List<EbProgramme> findByClientIdOrderByIdDesc(Long clientId);
}
