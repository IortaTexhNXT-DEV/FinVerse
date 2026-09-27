package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.csf.domain.CsfVerification.ClientRef;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The client of a CSF request and its accounts, checked against the company of the request: a
 * client of another company reads as not found. CSF roles see every segment (CQ06), so no further
 * scoping applies.
 */
@Component
@Transactional(readOnly = true)
public class CsfClients {

  private final ClientService clients;
  private final AccountQueryService accounts;

  /**
   * Creates the helper.
   *
   * @param clients client master
   * @param accounts account reads
   */
  public CsfClients(ClientService clients, AccountQueryService accounts) {
    this.clients = clients;
    this.accounts = accounts;
  }

  /**
   * A client of the company.
   *
   * @param companyId company
   * @param clientId client
   * @return client
   */
  public Client require(Long companyId, Long clientId) {
    Client client = clients.get(clientId);
    if (!Objects.equals(client.getCompanyId(), companyId)) {
      throw new ResourceNotFoundException(ClientService.ENTITY, clientId);
    }
    return client;
  }

  /**
   * The accounts of a client, newest first.
   *
   * @param client client
   * @return accounts
   */
  public List<Account> accountsOf(Client client) {
    return accounts.byClient(client.getId());
  }

  /**
   * An account of the client.
   *
   * @param client client
   * @param accountId account
   * @return account
   */
  public Account requireAccount(Client client, Long accountId) {
    return accountsOf(client).stream()
        .filter(a -> a.getId().equals(accountId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
  }

  /**
   * The reference of a client on CSF records.
   *
   * @param client client
   * @return reference
   */
  public static ClientRef refOf(Client client) {
    return new ClientRef(client.getCompanyId(), client.getId(), client.getCode());
  }
}
