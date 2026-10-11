package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** History of the budget amounts (FRRN.042.06). */
public interface RenewalBudgetHistoryRepository extends JpaRepository<RenewalBudgetHistory, Long> {

  /**
   * The changes of a budget record, latest first.
   *
   * @param budgetId budget record
   * @return changes
   */
  List<RenewalBudgetHistory> findByBudgetIdOrderByIdDesc(Long budgetId);
}
