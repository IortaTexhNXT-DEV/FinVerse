package com.iortatechnxt.brokerverse.reserves.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Persistence for {@link TakafulLine}. */
public interface TakafulLineRepository extends JpaRepository<TakafulLine, Long> {

  /**
   * Takaful surplus lines of a run.
   *
   * @param runId run
   * @return lines
   */
  List<TakafulLine> findByRunIdOrderByPolicyNo(Long runId);

  /**
   * Takaful surplus lines of several runs.
   *
   * @param runIds runs
   * @return lines
   */
  List<TakafulLine> findByRunIdIn(Collection<Long> runIds);

  /**
   * Removes the lines of a run (recalculation).
   *
   * @param runId run
   */
  @Modifying
  @Query("delete from TakafulLine t where t.runId = :runId")
  void deleteByRunId(Long runId);
}
