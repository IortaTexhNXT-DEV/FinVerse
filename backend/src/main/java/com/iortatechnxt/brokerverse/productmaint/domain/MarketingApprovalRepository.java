package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Marketing approvals of package requests. */
public interface MarketingApprovalRepository extends JpaRepository<MarketingApproval, Long> {

  /**
   * The approvals of a request in order.
   *
   * @param requestId request
   * @return approvals
   */
  List<MarketingApproval> findByRequestIdOrderByIdAsc(Long requestId);
}
