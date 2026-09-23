package com.iortatechnxt.finverse.reserves.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Persistence for {@link UprDetail}. */
public interface UprDetailRepository extends JpaRepository<UprDetail, Long> {

  /**
   * Policy-level UPR of a run.
   *
   * @param runId run
   * @param pageable page
   * @return page ordered as requested
   */
  Page<UprDetail> findByRunId(Long runId, Pageable pageable);

  /**
   * Policy-level UPR of a run for one line of business.
   *
   * @param runId run
   * @param businessLine line of business
   * @param pageable page
   * @return page
   */
  Page<UprDetail> findByRunIdAndBusinessLine(Long runId, String businessLine, Pageable pageable);

  /**
   * Every policy-level UPR row of a run.
   *
   * @param runId run
   * @return rows
   */
  List<UprDetail> findByRunId(Long runId);

  /**
   * Removes the detail of a run (recalculation).
   *
   * @param runId run
   */
  @Modifying
  @Query("delete from UprDetail d where d.runId = :runId")
  void deleteByRunId(Long runId);
}
