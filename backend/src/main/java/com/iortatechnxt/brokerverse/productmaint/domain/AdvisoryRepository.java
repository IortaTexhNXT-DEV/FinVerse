package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Package advisories. */
public interface AdvisoryRepository extends JpaRepository<Advisory, Long> {

  /**
   * Advisories of a request, newest first.
   *
   * @param requestId request
   * @return advisories
   */
  List<Advisory> findByRequestIdOrderByIdDesc(Long requestId);

  /**
   * Advisories of a company in a status, newest first.
   *
   * @param companyId company
   * @param status status
   * @return advisories
   */
  List<Advisory> findByCompanyIdAndStatusOrderByIdDesc(Long companyId, Advisory.Status status);

  /**
   * Number of advisories of a company in a status (home tile).
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, Advisory.Status status);
}
