package com.iortatechnxt.brokerverse.cashiering.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Commission payment details (CSHID.007). */
public interface CommissionLineRepository extends JpaRepository<CommissionLine, Long> {

  /**
   * Lines of a company in a status, by job and row.
   *
   * @param companyId company
   * @param status status
   * @return lines
   */
  List<CommissionLine> findByCompanyIdAndStatusOrderByJobNoAscRowNoAsc(
      Long companyId, String status);

  /**
   * Lines of an upload job in a status.
   *
   * @param jobNo job
   * @param status status
   * @return lines
   */
  List<CommissionLine> findByJobNoAndStatusOrderByRowNoAsc(String jobNo, String status);

  /**
   * Whether a row was already staged (reprocessing).
   *
   * @param jobNo job
   * @param rowNo row
   * @return true when staged
   */
  boolean existsByJobNoAndRowNo(String jobNo, int rowNo);
}
