package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.crm.service.ClientRecordsProvider;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The accounts of a client for the client 360 view (BRNB.099). */
@Component
public class AccountClientRecords implements ClientRecordsProvider {

  private final AccountRepository accounts;

  /**
   * Creates the provider.
   *
   * @param accounts accounts
   */
  public AccountClientRecords(AccountRepository accounts) {
    this.accounts = accounts;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientRecord> recordsOf(Long clientId) {
    return accounts.findByClientIdOrderByCreatedAtDesc(clientId).stream()
        .map(AccountClientRecords::record)
        .toList();
  }

  private static ClientRecord record(Account a) {
    String insurer = a.getInsurerCode() == null ? "insurer to be selected" : a.getInsurerCode();
    LocalDate date =
        a.getPeriodFrom() != null
            ? a.getPeriodFrom()
            : LocalDate.ofInstant(a.getCreatedAt(), ZoneOffset.UTC);
    return new ClientRecord(
        "Account",
        a.getArn(),
        a.getProductCode() + " - " + insurer + " - SI " + a.getTotalSumInsured().toPlainString(),
        a.getStatus().name(),
        date,
        "/accounts/" + a.getId());
  }
}
