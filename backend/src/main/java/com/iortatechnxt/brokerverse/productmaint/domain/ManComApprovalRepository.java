package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** ManCom approval tasks. */
public interface ManComApprovalRepository extends JpaRepository<ManComApproval, Long> {

  /**
   * The tasks of a request, newest round first.
   *
   * @param requestId request
   * @return tasks
   */
  List<ManComApproval> findByRequestIdOrderByRoundNoDescIdAsc(Long requestId);
}
