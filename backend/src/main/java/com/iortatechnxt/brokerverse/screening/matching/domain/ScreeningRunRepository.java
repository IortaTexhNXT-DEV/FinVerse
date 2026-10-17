package com.iortatechnxt.brokerverse.screening.matching.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Screening runs (SNSRP-602). */
public interface ScreeningRunRepository extends JpaRepository<ScreeningRun, Long> {

  /**
   * The latest successful run of a company and trigger (the delta of the batch starts from it).
   *
   * @param companyId company
   * @param trigger trigger
   * @param status status
   * @return the run
   */
  Optional<ScreeningRun> findFirstByCompanyIdAndTriggerAndStatusOrderByStartedAtDesc(
      Long companyId, ScreeningTrigger trigger, ScreeningRunStatus status);

  /**
   * The runs of a company, optionally of one trigger, newest first.
   *
   * @param companyId company
   * @param trigger trigger, {@code null} for all
   * @param pageable page
   * @return runs
   */
  @Query(
      "select r from ScreeningRun r where r.companyId = :companyId"
          + " and (:trigger is null or r.trigger = :trigger) order by r.startedAt desc, r.id desc")
  Page<ScreeningRun> search(
      @Param("companyId") Long companyId,
      @Param("trigger") ScreeningTrigger trigger,
      Pageable pageable);
}
