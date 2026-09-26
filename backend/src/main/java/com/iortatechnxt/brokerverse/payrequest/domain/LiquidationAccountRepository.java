package com.iortatechnxt.brokerverse.payrequest.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Accounts of the liquidation event roles. */
public interface LiquidationAccountRepository extends JpaRepository<LiquidationAccount, Long> {

  /**
   * The accounts of a company.
   *
   * @param companyId company
   * @return accounts by role
   */
  List<LiquidationAccount> findByCompanyIdOrderByAccountRole(Long companyId);

  /**
   * The account of one role.
   *
   * @param companyId company
   * @param accountRole role
   * @return account
   */
  Optional<LiquidationAccount> findByCompanyIdAndAccountRole(Long companyId, String accountRole);
}
