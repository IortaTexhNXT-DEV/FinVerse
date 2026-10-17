package com.iortatechnxt.brokerverse.receivables.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link StatementLayout}. */
public interface StatementLayoutRepository extends JpaRepository<StatementLayout, Long> {

  /**
   * The layout of a bank account.
   *
   * @param companyId company
   * @param bankAccountCode GL bank account
   * @return layout if any
   */
  Optional<StatementLayout> findByCompanyIdAndBankAccountCode(
      Long companyId, String bankAccountCode);

  /**
   * Layouts of a company.
   *
   * @param companyId company
   * @return layouts by bank account
   */
  List<StatementLayout> findByCompanyIdOrderByBankAccountCode(Long companyId);
}
