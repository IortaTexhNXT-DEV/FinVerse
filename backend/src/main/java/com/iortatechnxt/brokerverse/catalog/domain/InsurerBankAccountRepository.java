package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Bank accounts of the insurers. */
public interface InsurerBankAccountRepository extends JpaRepository<InsurerBankAccount, Long> {

  /**
   * Accounts of an insurer.
   *
   * @param insurerId insurer profile
   * @return accounts by purpose and number
   */
  List<InsurerBankAccount> findByInsurerIdOrderByPurposeAscAccountNoAsc(Long insurerId);

  /**
   * An account of an insurer by number.
   *
   * @param insurerId insurer profile
   * @param accountNo account number
   * @return account
   */
  Optional<InsurerBankAccount> findByInsurerIdAndAccountNo(Long insurerId, String accountNo);
}
