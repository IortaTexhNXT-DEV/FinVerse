package com.iortatechnxt.brokerverse.consolidation.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link ConsolidationRun}. */
public interface ConsolidationRunRepository extends JpaRepository<ConsolidationRun, Long> {

  /**
   * Lists the runs of a group, newest first.
   *
   * @param groupId group
   * @return runs
   */
  List<ConsolidationRun> findByGroupIdOrderByAsOfDateDescIdDesc(Long groupId);

  /**
   * Lists runs of a group and date in given statuses.
   *
   * @param groupId group
   * @param asOfDate date
   * @param statuses statuses
   * @return runs
   */
  List<ConsolidationRun> findByGroupIdAndAsOfDateAndStatusIn(
      Long groupId, LocalDate asOfDate, Collection<ConsolidationRunStatus> statuses);

  /**
   * Latest run of a group on or before a date in given statuses.
   *
   * @param groupId group
   * @param asOfDate latest date
   * @param statuses statuses
   * @return run
   */
  Optional<ConsolidationRun>
      findFirstByGroupIdAndAsOfDateLessThanEqualAndStatusInOrderByAsOfDateDescIdDesc(
          Long groupId, LocalDate asOfDate, Collection<ConsolidationRunStatus> statuses);

  /**
   * Latest run of a group in given statuses.
   *
   * @param groupId group
   * @param statuses statuses
   * @return run
   */
  Optional<ConsolidationRun> findFirstByGroupIdAndStatusInOrderByAsOfDateDescIdDesc(
      Long groupId, Collection<ConsolidationRunStatus> statuses);
}
