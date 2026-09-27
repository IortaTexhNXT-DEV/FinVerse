package com.iortatechnxt.brokerverse.migration.trueup.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** FY2027 true-ups. */
public interface MigTrueupRepository extends JpaRepository<MigTrueup, Long> {

  /**
   * True-ups of a company in order.
   *
   * @param companyId company
   * @return true-ups
   */
  List<MigTrueup> findByCompanyIdOrderByIdAsc(Long companyId);

  /**
   * A true-up of a company.
   *
   * @param companyId company
   * @param trueupNo number
   * @return true-up
   */
  Optional<MigTrueup> findByCompanyIdAndTrueupNo(Long companyId, String trueupNo);

  /**
   * True-ups in a status.
   *
   * @param status status
   * @return true-ups
   */
  List<MigTrueup> findByStatusOrderByIdAsc(MigTrueup.Status status);
}
