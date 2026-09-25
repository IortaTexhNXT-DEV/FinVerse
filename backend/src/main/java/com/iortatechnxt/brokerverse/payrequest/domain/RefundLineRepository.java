package com.iortatechnxt.brokerverse.payrequest.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Refund lines (MKT 2.23.0 duplicate check). */
public interface RefundLineRepository extends JpaRepository<RefundLine, Long> {

  /**
   * Request numbers of the live refunds of AR numbers, excluding one request.
   *
   * @param arNos AR numbers
   * @param requestId request to exclude (the one being changed), or -1
   * @return AR number and request number
   */
  @Query(
      """
      select l.arNo, r.requestNo from PaymentRequest r join r.lines l
      where l.live = true and l.arNo in :arNos and r.id <> :requestId
      """)
  List<Object[]> liveRefunds(
      @Param("arNos") Collection<String> arNos, @Param("requestId") Long requestId);
}
