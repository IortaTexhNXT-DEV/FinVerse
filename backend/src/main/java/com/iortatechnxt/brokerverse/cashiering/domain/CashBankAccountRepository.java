package com.iortatechnxt.brokerverse.cashiering.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** The list "Post to Bank Account". */
public interface CashBankAccountRepository extends JpaRepository<CashBankAccount, Long> {

  /**
   * The accounts of a company.
   *
   * @param companyId company
   * @return accounts by currency and name
   */
  List<CashBankAccount> findByCompanyIdOrderByCurrencyAscNameAsc(Long companyId);

  /**
   * One account.
   *
   * @param companyId company
   * @param code code
   * @return account
   */
  Optional<CashBankAccount> findByCompanyIdAndCode(Long companyId, String code);
}
