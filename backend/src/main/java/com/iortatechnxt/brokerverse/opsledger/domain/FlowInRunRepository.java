package com.iortatechnxt.brokerverse.opsledger.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Flow-in runs (BRQID.005 fetch log). */
public interface FlowInRunRepository extends JpaRepository<FlowInRun, Long> {

  /**
   * Runs of a feed, newest first.
   *
   * @param feedCode feed
   * @param pageable page
   * @return runs
   */
  Page<FlowInRun> findByFeedCodeOrderByIdDesc(String feedCode, Pageable pageable);

  /**
   * All runs, newest first.
   *
   * @param pageable page
   * @return runs
   */
  Page<FlowInRun> findAllByOrderByIdDesc(Pageable pageable);

  /**
   * Runs with one of some statuses started since a time (Operations home).
   *
   * @param statuses statuses
   * @param since start
   * @return count
   */
  long countByStatusInAndStartedAtGreaterThanEqual(
      List<FlowInEnums.RunStatus> statuses, Instant since);
}
