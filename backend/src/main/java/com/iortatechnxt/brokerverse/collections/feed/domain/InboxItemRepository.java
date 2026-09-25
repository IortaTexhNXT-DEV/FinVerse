package com.iortatechnxt.brokerverse.collections.feed.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** The inbox of the in-app Collection feeds. */
public interface InboxItemRepository extends JpaRepository<InboxItem, Long> {

  /**
   * Items of a feed, newest first.
   *
   * @param companyId company
   * @param feedCode feed
   * @return items
   */
  List<InboxItem> findByCompanyIdAndFeedCodeOrderByIdDesc(Long companyId, String feedCode);

  /**
   * Items of an invoice, newest first.
   *
   * @param invoiceNo invoice
   * @return items
   */
  List<InboxItem> findByInvoiceNoOrderByIdDesc(String invoiceNo);
}
