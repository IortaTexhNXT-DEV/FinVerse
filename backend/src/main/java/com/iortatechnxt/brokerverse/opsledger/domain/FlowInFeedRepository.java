package com.iortatechnxt.brokerverse.opsledger.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Flow-in feeds (BRQID.004). */
public interface FlowInFeedRepository extends JpaRepository<FlowInFeed, Long> {

  /**
   * A feed by code.
   *
   * @param code code
   * @return feed
   */
  Optional<FlowInFeed> findByCode(String code);

  /**
   * Every feed by partner and code.
   *
   * @return feeds
   */
  List<FlowInFeed> findAllByOrderByPartnerSystemAscCodeAsc();
}
