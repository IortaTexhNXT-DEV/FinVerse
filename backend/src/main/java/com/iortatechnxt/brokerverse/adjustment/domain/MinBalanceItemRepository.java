package com.iortatechnxt.brokerverse.adjustment.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Minimal balance write-offs and credits (ADJID.026). */
public interface MinBalanceItemRepository extends JpaRepository<MinBalanceItem, Long> {

  /**
   * The write-off of an invoice.
   *
   * @param invoiceNo invoice
   * @return item
   */
  Optional<MinBalanceItem> findByInvoiceNo(String invoiceNo);

  /**
   * Items of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return items
   */
  Page<MinBalanceItem> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);

  /**
   * Items of a period (report).
   *
   * @param companyId company
   * @param from from (inclusive)
   * @param to to (exclusive)
   * @return items, oldest first
   */
  @Query(
      """
      select i from MinBalanceItem i
      where i.companyId = :companyId and i.createdAt >= :from and i.createdAt < :to
      order by i.id
      """)
  List<MinBalanceItem> createdBetween(
      @Param("companyId") Long companyId, @Param("from") Instant from, @Param("to") Instant to);
}
