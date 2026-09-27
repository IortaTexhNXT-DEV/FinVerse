package com.iortatechnxt.brokerverse.migration.load.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Resubmissions of corrected rows. */
public interface ResubmissionRepository extends JpaRepository<Resubmission, Long> {

  /**
   * A resubmission by number.
   *
   * @param no number
   * @return resubmission
   */
  Optional<Resubmission> findByResubmissionNo(String no);

  /**
   * Resubmissions in a status.
   *
   * @param status status
   * @return resubmissions
   */
  List<Resubmission> findByStatusOrderByPreparedAtAsc(Resubmission.Status status);

  /**
   * Resubmissions of a company, newest first.
   *
   * @param companyId company
   * @return resubmissions
   */
  List<Resubmission> findByCompanyIdOrderByIdDesc(Long companyId);
}
