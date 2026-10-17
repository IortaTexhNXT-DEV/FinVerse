package com.iortatechnxt.brokerverse.report.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Runs of the report subscriptions (G04). */
public interface ReportSubscriptionRunRepository
    extends JpaRepository<ReportSubscriptionRun, Long> {

  /**
   * Whether a subscription has run on a date.
   *
   * @param subscriptionId subscription
   * @param runDate date
   * @return true when a run is recorded
   */
  boolean existsBySubscriptionIdAndRunDate(Long subscriptionId, LocalDate runDate);

  /**
   * Runs of a subscription.
   *
   * @param subscriptionId subscription
   * @return runs, newest first
   */
  List<ReportSubscriptionRun> findBySubscriptionIdOrderByRunDateDesc(Long subscriptionId);
}
