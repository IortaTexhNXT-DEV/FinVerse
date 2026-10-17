package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountSearch;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientSearch;
import com.iortatechnxt.brokerverse.crm.service.ClientSearchService;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.csf.domain.ActivityAction;
import com.iortatechnxt.brokerverse.csf.domain.CsfActivity;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ClientHitView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.LegacyAccountView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.SearchView;
import com.iortatechnxt.brokerverse.csf.service.port.LegacyAccountLookup;
import com.iortatechnxt.brokerverse.csf.service.port.LegacyAccountLookup.LegacyAccount;
import com.iortatechnxt.brokerverse.placement.service.PlacementQueryService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer Search (FR-CSF-010; BRCSF-003 / 3.001): one search box by key type over the client
 * master, the accounts, the CLPC billing items and, for accounts not in BIBS, the legacy lookup.
 * Results are grouped by client with the matching accounts; each search is logged with its
 * criteria.
 */
@Service
@Transactional(readOnly = true)
public class CustomerSearchService {

  private static final String DISPLAY_NAME = "displayName";

  private final ClientSearchService clientSearch;
  private final ClientService clients;
  private final AccountQueryService accounts;
  private final PlacementQueryService placement;
  private final LegacyAccountLookup legacy;
  private final AccountLines lines;
  private final CsfParameters parameters;
  private final ActivityLog activity;

  /**
   * Creates the service.
   *
   * @param clientSearch client master search
   * @param clients client master
   * @param accounts account reads
   * @param placement billing items by loan application
   * @param legacy legacy-only accounts
   * @param lines account lines
   * @param parameters CSF parameters
   * @param activity activity log
   */
  public CustomerSearchService(
      ClientSearchService clientSearch,
      ClientService clients,
      AccountQueryService accounts,
      PlacementQueryService placement,
      LegacyAccountLookup legacy,
      AccountLines lines,
      CsfParameters parameters,
      ActivityLog activity) {
    this.clientSearch = clientSearch;
    this.clients = clients;
    this.accounts = accounts;
    this.placement = placement;
    this.legacy = legacy;
    this.lines = lines;
    this.parameters = parameters;
    this.activity = activity;
  }

  /**
   * Searches clients by one key.
   *
   * @param companyId company
   * @param key key type
   * @param value value
   * @return clients with their matching accounts, and legacy accounts when nothing is in BIBS
   */
  public SearchView search(Long companyId, SearchKey key, String value) {
    String text = value == null ? "" : value.strip();
    requireValid(key, text);
    int max = parameters.searchMaxResults();
    Map<Long, List<Account>> hits =
        switch (key) {
          case NAME -> byName(companyId, text, max);
          case CLIENT_ID -> byClientId(companyId, text, max);
          case ACCOUNT_NO -> group(accounts.byAccountNumber(companyId, text));
          case PN_NO -> group(byPn(companyId, text, max));
          case APPLICATION_NO -> group(byApplication(companyId, text));
        };
    List<LegacyAccount> legacyHits = legacyOf(companyId, key, text, hits.isEmpty());
    boolean truncated = hits.size() > max;
    List<ClientHitView> found =
        hits.entrySet().stream().limit(max).map(e -> hit(e.getKey(), e.getValue())).toList();
    log(companyId, key, text, found, truncated);
    return new SearchView(
        key.name(),
        text,
        truncated,
        max,
        found,
        legacyHits.stream()
            .map(
                l ->
                    new LegacyAccountView(
                        l.source(), l.reference(), l.clientName(), l.description()))
            .toList());
  }

  private List<LegacyAccount> legacyOf(Long companyId, SearchKey key, String text, boolean none) {
    boolean accountKey = key != SearchKey.NAME && key != SearchKey.CLIENT_ID;
    return none && accountKey ? legacy.find(companyId, key.name(), text) : List.of();
  }

  private void log(
      Long companyId, SearchKey key, String text, List<ClientHitView> found, boolean truncated) {
    ClientHitView single = found.size() == 1 ? found.get(0) : null;
    activity.record(
        companyId,
        ActivityAction.SEARCH,
        new CsfActivity.Subject(
            single == null ? null : single.id(),
            single == null ? null : single.code(),
            null,
            key.label() + ": " + text + " (" + found.size() + (truncated ? "+" : "") + " found)"));
  }

  private ClientHitView hit(Long clientId, List<Account> matched) {
    Client c = clients.get(clientId);
    return new ClientHitView(
        c.getId(),
        c.getCode(),
        c.getDisplayName(),
        c.getClientType().name(),
        c.getStatus().name(),
        c.getEmail(),
        c.getMobile(),
        c.getCity(),
        lines.of(matched));
  }

  private void requireValid(SearchKey key, String text) {
    if (text.isEmpty()) {
      throw new BusinessRuleException("CSF_SEARCH_VALUE_REQUIRED", "Enter the value to search");
    }
    int min = parameters.searchMinChars();
    if (key == SearchKey.NAME && text.length() < min) {
      throw new BusinessRuleException(
          "CSF_SEARCH_TOO_SHORT", "Enter at least " + min + " characters");
    }
  }

  private Map<Long, List<Account>> byName(Long companyId, String text, int max) {
    Map<Long, List<Account>> found = new LinkedHashMap<>();
    clientSearch
        .search(
            new ClientSearch(
                companyId, null, text, null, null, null, null, null, null, null, null, null, false),
            PageRequest.of(0, max + 1, Sort.by(DISPLAY_NAME)))
        .forEach(c -> found.put(c.getId(), List.of()));
    return found;
  }

  private Map<Long, List<Account>> byClientId(Long companyId, String text, int max) {
    Map<Long, List<Account>> found = new LinkedHashMap<>();
    ClientSearch byCode =
        new ClientSearch(
            companyId, text, null, null, null, null, null, null, null, null, null, null, false);
    ClientSearch byId =
        new ClientSearch(
            companyId, null, null, null, text, null, null, null, null, null, null, null, false);
    for (ClientSearch criteria : List.of(byCode, byId)) {
      clientSearch
          .search(criteria, PageRequest.of(0, max + 1, Sort.by(DISPLAY_NAME)))
          .forEach(c -> found.putIfAbsent(c.getId(), List.of()));
    }
    return found;
  }

  private List<Account> byPn(Long companyId, String text, int max) {
    AccountSearch criteria =
        new AccountSearch(
            companyId, null, text, null, null, null, null, null, List.of(), null, null, null, null,
            null, true);
    return accounts.search(criteria, PageRequest.of(0, max * 2, Sort.by("arn"))).getContent();
  }

  private List<Account> byApplication(Long companyId, String text) {
    Map<Long, Account> found = new LinkedHashMap<>();
    accounts.byLoanApplication(companyId, text).forEach(a -> found.put(a.getId(), a));
    for (String arn : placement.arnsByLoanApplication(companyId, text)) {
      Account a = accounts.requireByArn(arn);
      if (Objects.equals(a.getCompanyId(), companyId)) {
        found.putIfAbsent(a.getId(), a);
      }
    }
    return new ArrayList<>(found.values());
  }

  private static Map<Long, List<Account>> group(List<Account> matched) {
    Map<Long, List<Account>> found = new LinkedHashMap<>();
    for (Account a : matched) {
      found.computeIfAbsent(a.getClientId(), k -> new ArrayList<>()).add(a);
    }
    return found;
  }
}
