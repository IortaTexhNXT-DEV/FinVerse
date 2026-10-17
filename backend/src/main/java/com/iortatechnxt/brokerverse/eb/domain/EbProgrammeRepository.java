package com.iortatechnxt.brokerverse.eb.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** EB programmes. */
public interface EbProgrammeRepository
    extends JpaRepository<EbProgramme, Long>, JpaSpecificationExecutor<EbProgramme> {

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

  /**
   * A programme of a company, with its lines.
   *
   * @param id programme
   * @param companyId company
   * @return programme
   */
  @EntityGraph(attributePaths = "lines", type = EntityGraph.EntityGraphType.LOAD)
  Optional<EbProgramme> findByIdAndCompanyId(Long id, Long companyId);

  /**
   * Programmes with an active line expiring in a window (renewal advice candidates, BRID-001).
   *
   * @param from first expiry date
   * @param to last expiry date
   * @return programmes, by number
   */
  @Query(
      "select distinct p from EbProgramme p join p.lines l where l.active = true"
          + " and l.periodTo between :from and :to order by p.programmeNo")
  List<EbProgramme> findWithLinesExpiring(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
