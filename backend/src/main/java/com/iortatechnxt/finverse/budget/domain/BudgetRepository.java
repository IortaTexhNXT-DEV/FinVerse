package com.iortatechnxt.finverse.budget.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link Budget}. */
public interface BudgetRepository extends JpaRepository<Budget, Long> {

  /**
   * Lists the versions of a company, newest year and version first.
   *
   * @param companyId company
   * @return versions
   */
  List<Budget> findByCompanyIdOrderByFiscalYearDescVersionNoDesc(Long companyId);

  /**
   * Lists the versions of a company and fiscal year, newest first.
   *
   * @param companyId company
   * @param fiscalYear year
   * @return versions
   */
  List<Budget> findByCompanyIdAndFiscalYearOrderByVersionNoDesc(Long companyId, int fiscalYear);

  /**
   * Finds the latest version in given statuses.
   *
   * @param companyId company
   * @param fiscalYear year
   * @param statuses statuses
   * @return version if any
   */
  Optional<Budget> findFirstByCompanyIdAndFiscalYearAndStatusInOrderByVersionNoDesc(
      Long companyId, int fiscalYear, Collection<BudgetStatus> statuses);

  /**
   * Highest version number used for a company and year.
   *
   * @param companyId company
   * @param fiscalYear year
   * @return max version or 0
   */
  @Query(
      "select coalesce(max(b.versionNo), 0) from Budget b"
          + " where b.companyId = :companyId and b.fiscalYear = :fiscalYear")
  int maxVersion(@Param("companyId") Long companyId, @Param("fiscalYear") int fiscalYear);

  /**
   * Counts versions of a type that are not rejected (duplicate original budget check).
   *
   * @param companyId company
   * @param fiscalYear year
   * @param type version type
   * @param excluded statuses ignored
   * @return count
   */
  long countByCompanyIdAndFiscalYearAndVersionTypeAndStatusNotIn(
      Long companyId, int fiscalYear, BudgetVersionType type, Collection<BudgetStatus> excluded);
}
