package com.iortatechnxt.brokerverse.acsl.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Correction entries (ACSL 2.7-2.15). */
public interface CorrectionRepository extends JpaRepository<Correction, Long> {

  /**
   * A correction with its lines.
   *
   * @param id id
   * @return correction
   */
  @EntityGraph(attributePaths = "lines")
  @Query("select c from Correction c where c.id = :id")
  Optional<Correction> findLoaded(@Param("id") Long id);

  /**
   * Corrections by stage and text (correction, invoice or description).
   *
   * @param companyId company
   * @param stage stage or null
   * @param q lower-case like pattern ({@code %} for all)
   * @param pageable page
   * @return corrections
   */
  @Query(
      """
      select c from Correction c
      where c.companyId = :companyId
        and (:stage is null or c.stage = :stage)
        and (lower(c.correctionNo) like :q
             or lower(c.description) like :q
             or lower(coalesce(c.invoiceNo, '')) like :q
             or lower(coalesce(c.rootInvoiceNo, '')) like :q)
      """)
  Page<Correction> search(
      @Param("companyId") Long companyId,
      @Param("stage") CorrectionStage stage,
      @Param("q") String q,
      Pageable pageable);

  /**
   * Corrections per stage.
   *
   * @param companyId company
   * @return stage and count
   */
  @Query(
      "select c.stage, count(c) from Correction c where c.companyId = :companyId group by c.stage")
  List<Object[]> countByStage(@Param("companyId") Long companyId);

  /**
   * Corrections waiting in some stages (approval inbox).
   *
   * @param stages stages
   * @return corrections, oldest first
   */
  List<Correction> findByStageInOrderByIdAsc(Collection<CorrectionStage> stages);
}
