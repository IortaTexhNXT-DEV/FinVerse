package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Decision matrices. */
public interface DecisionMatrixRepository extends JpaRepository<DecisionMatrix, Long> {

  /**
   * Versions of a company, newest first.
   *
   * @param companyId company
   * @return versions
   */
  List<DecisionMatrix> findByCompanyIdOrderByVersionNoDesc(Long companyId);

  /**
   * The versions in a status.
   *
   * @param companyId company
   * @param status status
   * @return versions
   */
  List<DecisionMatrix> findByCompanyIdAndStatus(Long companyId, RuleSetStatus status);

  /**
   * Versions in a status of every company.
   *
   * @param status status
   * @return versions
   */
  List<DecisionMatrix> findByStatus(RuleSetStatus status);
}
