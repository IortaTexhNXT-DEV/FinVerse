package com.iortatechnxt.brokerverse.prodrecon.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Production register extracts. */
public interface ReconExtractRepository extends JpaRepository<ReconExtract, Long> {

  /**
   * Extracts of a cycle, newest first.
   *
   * @param cycleId cycle
   * @return extracts
   */
  List<ReconExtract> findByCycleIdOrderByIdDesc(Long cycleId);

  /**
   * Extract register of a company (PRCID.005/034), newest first.
   *
   * @param companyId company
   * @param insurer insurer, null for all
   * @param pageable page
   * @return extracts
   */
  @Query(
      """
      select e from ReconExtract e, ReconCycle c where c.id = e.cycleId and c.companyId = :companyId
        and (:insurer is null or c.insurerCode = :insurer)
      order by e.id desc
      """)
  Page<ReconExtract> search(
      @Param("companyId") Long companyId, @Param("insurer") String insurer, Pageable pageable);
}
