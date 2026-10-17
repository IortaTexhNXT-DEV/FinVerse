package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.domain.SimulatedAccount;
import com.iortatechnxt.brokerverse.identity.domain.SimulatedAccountRepository;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The directory of the Enterprise SSO simulator (SIT and UAT). */
@Service
@ConditionalOnProperty(name = "brokerverse.identity.simulator", havingValue = "true")
public class SimulatedEnterpriseDirectory implements EnterpriseDirectory {

  private final SimulatedAccountRepository accounts;

  /**
   * Creates the directory.
   *
   * @param accounts simulator accounts
   */
  public SimulatedEnterpriseDirectory(SimulatedAccountRepository accounts) {
    this.accounts = accounts;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<DirectoryAccount> find(String windowsId) {
    return windowsId == null
        ? Optional.empty()
        : accounts.findByWindowsIdIgnoreCase(windowsId.trim()).map(SimulatedAccount::toAccount);
  }

  @Override
  public String name() {
    return "Enterprise SSO simulator";
  }
}
