package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.catalog.service.CatalogNames;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.crm.service.ClientRecordsProvider;
import com.iortatechnxt.brokerverse.crm.service.RecordDescriptions;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The accounts of a client for the client 360 view (BRNB.099). */
@Component
public class AccountClientRecords implements ClientRecordsProvider {

  private final AccountRepository accounts;
  private final CatalogNames names;

  /**
   * Creates the provider.
   *
   * @param accounts accounts
   * @param names product and insurer names
   */
  public AccountClientRecords(AccountRepository accounts, CatalogNames names) {
    this.accounts = accounts;
    this.names = names;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientRecord> recordsOf(Long clientId) {
    return accounts.findByClientIdOrderByCreatedAtDesc(clientId).stream()
        .map(this::record)
        .toList();
  }

  private ClientRecord record(Account a) {
    LocalDate date =
        a.getPeriodFrom() != null ? a.getPeriodFrom() : BusinessClock.dateOf(a.getCreatedAt());
    return new ClientRecord(
        "Account",
        a.getArn(),
        RecordDescriptions.account(
            names.productName(a.getProductCode()),
            names.insurer(a.getInsurerCode()),
            a.getCurrency(),
            a.getTotalSumInsured()),
        a.getStatus().name(),
        date,
        "/accounts/" + a.getId());
  }
}
