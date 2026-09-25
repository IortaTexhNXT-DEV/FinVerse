package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.MatchCategory;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentChannel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Payments received (CSHID.008/020). */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  /**
   * The payment of a source key (idempotency of uploads and feeds).
   *
   * @param companyId company
   * @param channel channel
   * @param sourceKey key
   * @return payment
   */
  Optional<Payment> findByCompanyIdAndChannelAndSourceKey(
      Long companyId, PaymentChannel channel, String sourceKey);

  /**
   * Payments of an upload or batch, in row order.
   *
   * @param batchRef batch
   * @return payments
   */
  List<Payment> findByBatchRefOrderByIdAsc(String batchRef);

  /**
   * Payments of a company, filtered, newest first.
   *
   * @param companyId company
   * @param channel channel, null for all
   * @param category category, null for all
   * @param batchRef batch, null for all
   * @param pageable page
   * @return payments
   */
  @Query(
      "select p from Payment p where p.companyId = :companyId"
          + " and (:channel is null or p.channel = :channel)"
          + " and (:category is null or p.matchCategory = :category)"
          + " and (:batchRef is null or p.batchRef = :batchRef) order by p.id desc")
  Page<Payment> search(
      @Param("companyId") Long companyId,
      @Param("channel") PaymentChannel channel,
      @Param("category") MatchCategory category,
      @Param("batchRef") String batchRef,
      Pageable pageable);
}
