package com.iortatechnxt.brokerverse.alert.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import java.time.LocalDate;
import java.util.List;

/**
 * Port for scheduled exception checks. Every bean implementing it is evaluated by the daily alert
 * job ({@link AlertDailyJob}); each returned signal is raised through {@link AlertService#raise}
 * (deduplicated). Modules add checks for their own conditions by implementing this interface in
 * their {@code service} package; posting-time or event-time conditions call {@link
 * AlertService#raise} directly instead.
 */
public interface AlertCheck {

  /**
   * Evaluates the condition(s).
   *
   * @param asOf business date
   * @return conditions found
   */
  List<AlertSignal> evaluate(LocalDate asOf);

  /**
   * A condition found by a check.
   *
   * @param code exception code
   * @param facts what and where
   */
  record AlertSignal(String code, AlertFacts facts) {}
}
