package com.iortatechnxt.brokerverse.opsledger.service.port;

import java.time.LocalDate;
import java.util.List;

/**
 * Port to the Claims system (BRQID.004, MKTID.009 claims special remittance, OQ46). Parked: no
 * adapter is built until the Claims BRD; callers obtain it with {@code ObjectProvider<ClaimsFeed>}
 * and treat a missing bean as "not connected".
 */
public interface ClaimsFeed {

  /**
   * Items of a Claims feed changed since a date.
   *
   * @param companyId company
   * @param feedCode feed
   * @param since first date
   * @return items
   */
  List<FeedItem> fetch(Long companyId, String feedCode, LocalDate since);
}
