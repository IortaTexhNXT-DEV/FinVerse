package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountSearch;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Account look-ups shared by the placement services, through the account module's public query
 * service only.
 */
@Component
public class PlacementAccounts {

  /** Largest number of accounts processed in one list (billing batch, sweep). */
  public static final int MAX_ACCOUNTS = 2000;

  private static final int PAGE = 500;

  private final AccountQueryService queries;

  /**
   * Creates the helper.
   *
   * @param queries account reads
   */
  public PlacementAccounts(AccountQueryService queries) {
    this.queries = queries;
  }

  /**
   * An account by ARN with items and numbers loaded.
   *
   * @param arn Account Reference Number
   * @return account
   */
  public Account require(String arn) {
    return queries.requireByArn(arn == null ? "" : arn.strip());
  }

  /**
   * An account by ARN that must belong to the company.
   *
   * @param companyId company
   * @param arn Account Reference Number
   * @return account
   */
  public Account require(Long companyId, String arn) {
    Account account = require(arn);
    if (!account.getCompanyId().equals(companyId)) {
      throw new BusinessRuleException(
          "ACCOUNT_OTHER_COMPANY", "Account " + arn + " belongs to another company");
    }
    return account;
  }

  /**
   * One page of the accounts of a company in the given statuses, newest first.
   *
   * @param companyId company
   * @param statuses statuses
   * @param text ARN, client code or name fragment, may be null
   * @param pageable page
   * @return accounts
   */
  public Page<Account> page(
      Long companyId, List<AccountStatus> statuses, String text, Pageable pageable) {
    return queries.search(search(companyId, statuses, text, null), pageable);
  }

  /**
   * Number of accounts of a company in the given statuses.
   *
   * @param companyId company
   * @param statuses statuses
   * @return count
   */
  public long count(Long companyId, List<AccountStatus> statuses) {
    return queries
        .search(search(companyId, statuses, null, null), PageRequest.of(0, 1))
        .getTotalElements();
  }

  /**
   * Every account of a company in a status, optionally of one product line (at most {@link
   * #MAX_ACCOUNTS}).
   *
   * @param companyId company
   * @param status status
   * @param lineCode product line, null for all
   * @return accounts, oldest first
   */
  public List<Account> all(Long companyId, AccountStatus status, String lineCode) {
    AccountSearch search = search(companyId, List.of(status), null, lineCode);
    List<Account> result = new ArrayList<>();
    int page = 0;
    Page<Account> slice;
    do {
      slice = queries.search(search, PageRequest.of(page++, PAGE, Sort.by("id")));
      result.addAll(slice.getContent());
    } while (slice.hasNext() && result.size() < MAX_ACCOUNTS);
    return result;
  }

  private static AccountSearch search(
      Long companyId, List<AccountStatus> statuses, String text, String lineCode) {
    return new AccountSearch(
        companyId,
        text == null || text.isBlank() ? null : text.strip(),
        null,
        null,
        null,
        null,
        lineCode,
        null,
        statuses,
        null,
        null,
        null,
        null,
        null,
        false);
  }
}
