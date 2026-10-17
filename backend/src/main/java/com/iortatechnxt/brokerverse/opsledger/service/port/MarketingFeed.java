package com.iortatechnxt.brokerverse.opsledger.service.port;

import java.time.LocalDate;
import java.util.List;

/**
 * Port to the Marketing system (BRQID.004, MKTID, OQ46). Parked: no adapter is built until BDOI
 * describes the interface; Marketing data reaches Operations through the Marketing Collection
 * screens. Callers obtain it with {@code ObjectProvider<MarketingFeed>} and treat a missing bean as
 * "not connected".
 */
public interface MarketingFeed {

  /**
   * Items of a Marketing feed changed since a date.
   *
   * @param companyId company
   * @param feedCode feed
   * @param since first date
   * @return items
   */
  List<FeedItem> fetch(Long companyId, String feedCode, LocalDate since);
}
