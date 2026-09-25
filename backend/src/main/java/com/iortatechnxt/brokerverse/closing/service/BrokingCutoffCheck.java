package com.iortatechnxt.brokerverse.closing.service;

import java.time.LocalDate;
import java.util.List;

/**
 * Port through which a broking module lists what is still pending when its books are cut off at
 * month end (FRBS 3.4.1: unposted DVs, unapplied batches...). Implement it in the module's {@code
 * service} package; the items are recorded on the cut-off and shown on the Planning &amp; Closing
 * screen. The cut-off never waits for them.
 */
public interface BrokingCutoffCheck {

  /**
   * Items still pending in a period.
   *
   * @param companyId company
   * @param from first day of the period
   * @param to last day of the period
   * @return one readable line per kind of pending item, empty when nothing is pending
   */
  List<String> pendingItems(Long companyId, LocalDate from, LocalDate to);
}
