package com.iortatechnxt.brokerverse.payables.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link BankAccount}. */
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

  /**
   * Lists the bank accounts of a company.
   *
   * @param companyId company
   * @return accounts ordered by code
   */
  List<BankAccount> findByCompanyIdOrderByCode(Long companyId);

  /**
   * Finds a bank account by code.
   *
   * @param companyId company
   * @param code code
   * @return account
   */
  Optional<BankAccount> findByCompanyIdAndCode(Long companyId, String code);

  /**
   * Finds the bank account mapped to a GL account.
   *
   * @param companyId company
   * @param glAccountCode GL account code
   * @return account
   */
  Optional<BankAccount> findByCompanyIdAndGlAccountCode(Long companyId, String glAccountCode);

  /**
   * Checks whether a code is taken.
   *
   * @param companyId company
   * @param code code
   * @return true when used
   */
  boolean existsByCompanyIdAndCode(Long companyId, String code);
}
