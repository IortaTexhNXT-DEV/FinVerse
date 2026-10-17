package com.iortatechnxt.brokerverse.commission.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Incentive runs. */
public interface IncentiveRunRepository extends JpaRepository<IncentiveRun, Long> {

  /**
   * Runs of a company, newest first.
   *
   * @param companyId company
   * @param schemeId scheme, null for all
   * @param pageable page
   * @return runs
   */
  @Query(
      """
      select r from IncentiveRun r where r.companyId = :companyId
        and (cast(:schemeId as Long) is null or r.schemeId = :schemeId)
      order by r.id desc
      """)
  Page<IncentiveRun> search(
      @Param("companyId") Long companyId, @Param("schemeId") Long schemeId, Pageable pageable);
}
