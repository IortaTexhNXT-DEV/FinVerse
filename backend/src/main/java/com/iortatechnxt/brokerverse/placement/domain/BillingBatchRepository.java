package com.iortatechnxt.brokerverse.placement.domain;

import java.util.Collection;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** CLPC billing batches. */
public interface BillingBatchRepository extends JpaRepository<BillingBatch, Long> {

  /**
   * Batches of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return batches
   */
  Page<BillingBatch> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);

  /**
   * Accounts already billed and not reported unpaid (they are not billed again).
   *
   * @param accountIds candidate accounts
   * @param unpaid the unpaid status
   * @return the ids of those already billed
   */
  @Query(
      "select i.accountId from BillingItem i where i.accountId in :accountIds"
          + " and i.paymentStatus <> :unpaid")
  Set<Long> alreadyBilled(
      @Param("accountIds") Collection<Long> accountIds, @Param("unpaid") BillingItemStatus unpaid);
}
