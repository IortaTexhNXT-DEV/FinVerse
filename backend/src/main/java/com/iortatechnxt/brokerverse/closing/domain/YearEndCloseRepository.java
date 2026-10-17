package com.iortatechnxt.brokerverse.closing.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link YearEndClose}. */
public interface YearEndCloseRepository extends JpaRepository<YearEndClose, Long> {

  /**
   * Finds the close record of a fiscal year.
   *
   * @param fiscalYearId year
   * @return record if closed
   */
  Optional<YearEndClose> findByFiscalYearId(Long fiscalYearId);

  /**
   * Lists the closes of a company, newest first.
   *
   * @param companyId company
   * @return records
   */
  List<YearEndClose> findByCompanyIdOrderByYearCodeDesc(Long companyId);
}
