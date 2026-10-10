package com.iortatechnxt.brokerverse.identity.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Accounts of the Enterprise SSO simulator. */
public interface SimulatedAccountRepository extends JpaRepository<SimulatedAccount, Long> {

  /**
   * The account of a Windows ID.
   *
   * @param windowsId Windows ID
   * @return account
   */
  Optional<SimulatedAccount> findByWindowsIdIgnoreCase(String windowsId);

  /**
   * Every account, by Windows ID.
   *
   * @return accounts
   */
  List<SimulatedAccount> findAllByOrderByWindowsIdAsc();
}
