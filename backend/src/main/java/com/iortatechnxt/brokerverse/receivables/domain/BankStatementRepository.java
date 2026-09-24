package com.iortatechnxt.brokerverse.receivables.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link BankStatement}. */
public interface BankStatementRepository extends JpaRepository<BankStatement, Long> {

  /**
   * Lists the statements of a company, newest first.
   *
   * @param companyId company
   * @return statements
   */
  List<BankStatement> findByCompanyIdOrderByPeriodToDescIdDesc(Long companyId);

  /**
   * Checks whether a statement reference was already imported for a bank account.
   *
   * @param companyId company
   * @param bankAccountCode bank GL account
   * @param statementRef reference
   * @return true when present
   */
  boolean existsByCompanyIdAndBankAccountCodeAndStatementRef(
      Long companyId, String bankAccountCode, String statementRef);
}
