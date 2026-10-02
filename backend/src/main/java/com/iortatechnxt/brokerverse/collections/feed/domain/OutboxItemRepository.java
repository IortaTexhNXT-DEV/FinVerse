package com.iortatechnxt.brokerverse.collections.feed.domain;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.OutboxStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** The outbox of the in-app Collection feeds. */
public interface OutboxItemRepository extends JpaRepository<OutboxItem, Long> {

  /**
   * Items of a feed in a status, oldest first.
   *
   * @param companyId company
   * @param feedCode feed
   * @param status status
   * @return items
   */
  List<OutboxItem> findByCompanyIdAndFeedCodeAndStatusOrderByIdAsc(
      Long companyId, String feedCode, OutboxStatus status);

  /**
   * Items of a feed by key.
   *
   * @param companyId company
   * @param feedCode feed
   * @param keys idempotency keys
   * @return items
   */
  List<OutboxItem> findByCompanyIdAndFeedCodeAndIdempotencyKeyIn(
      Long companyId, String feedCode, Collection<String> keys);

  /**
   * Items of an invoice, newest first.
   *
   * @param invoiceNo invoice
   * @return items
   */
  List<OutboxItem> findByInvoiceNoOrderByIdDesc(String invoiceNo);

  /**
   * Pending items created before a time (stale check).
   *
   * @param status PENDING
   * @param before created before
   * @return items
   */
  List<OutboxItem> findByStatusAndCreatedAtBeforeOrderByIdAsc(OutboxStatus status, Instant before);

  /**
   * Counts the items of a company in a status.
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, OutboxStatus status);
}
