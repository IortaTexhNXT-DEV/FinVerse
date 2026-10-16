package com.iortatechnxt.brokerverse.report.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Report subscriptions (G04). */
public interface ReportSubscriptionRepository extends JpaRepository<ReportSubscription, Long> {

  /**
   * Subscriptions of a user.
   *
   * @param username subscriber
   * @return subscriptions, newest first
   */
  List<ReportSubscription> findByUsernameOrderByIdDesc(String username);

  /**
   * Active subscriptions.
   *
   * @return subscriptions
   */
  List<ReportSubscription> findByActiveTrueOrderByIdAsc();
}
