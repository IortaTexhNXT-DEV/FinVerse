package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** ManCom decisions. */
public interface SignoffRepository extends JpaRepository<Signoff, Long> {

  /**
   * Decisions on a request, newest first.
   *
   * @param requestId request
   * @return decisions
   */
  List<Signoff> findByRequestIdOrderByIdDesc(Long requestId);
}
