package com.iortatechnxt.brokerverse.migration.cutover.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Cutover plans. */
public interface CutoverPlanRepository extends JpaRepository<CutoverPlan, Long> {

  /**
   * Plans of a company by go-live date.
   *
   * @param companyId company
   * @return plans
   */
  List<CutoverPlan> findByCompanyIdOrderByGoLiveDateAscIdAsc(Long companyId);

  /**
   * A plan by number.
   *
   * @param planNo number
   * @return plan
   */
  Optional<CutoverPlan> findByPlanNo(String planNo);
}
