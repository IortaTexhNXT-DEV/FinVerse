package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.service.ClientSearch;
import com.iortatechnxt.brokerverse.crm.service.ClientSearchService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * The clients in scope of list-change and batch screening (SNSRP-602 AC2; FR-SS-030 R2): the
 * statuses of parameter {@code SCR_SCREENING_SCOPE} (default prospects and confirmed clients,
 * SQ11). Inactive clients are never in scope.
 */
@Component
public class ScreeningScope {

  /** Parameter holding the client statuses in scope. */
  public static final String PARAMETER = "SCR_SCREENING_SCOPE";

  /** Clients read per page by the batch. */
  static final int PAGE_SIZE = 200;

  private final SystemParameterService parameters;
  private final ClientSearchService search;

  /**
   * Creates the scope.
   *
   * @param parameters system parameters
   * @param search client search
   */
  public ScreeningScope(SystemParameterService parameters, ClientSearchService search) {
    this.parameters = parameters;
    this.search = search;
  }

  /**
   * The statuses in scope.
   *
   * @return statuses, never INACTIVE
   */
  public List<ClientStatus> statuses() {
    List<ClientStatus> statuses = new ArrayList<>();
    for (String item : parameters.items(PARAMETER)) {
      for (ClientStatus s : ClientStatus.values()) {
        if (s.name().equals(item.trim()) && s != ClientStatus.INACTIVE) {
          statuses.add(s);
        }
      }
    }
    return statuses.isEmpty() ? List.of(ClientStatus.PROSPECT, ClientStatus.CONFIRMED) : statuses;
  }

  /**
   * The scope as written on the run log.
   *
   * @return e.g. "PROSPECT,CONFIRMED"
   */
  public String label() {
    return statuses().stream().map(Enum::name).collect(Collectors.joining(","));
  }

  /**
   * Whether a client is in scope.
   *
   * @param client the client
   * @return true when its status is in scope
   */
  public boolean includes(Client client) {
    return statuses().contains(client.getStatus());
  }

  /**
   * Whether a company has clients in scope.
   *
   * @param companyId company
   * @return true when at least one client is in scope
   */
  public boolean hasClients(Long companyId) {
    return statuses().stream()
        .anyMatch(s -> search.search(criteria(companyId, s), PageRequest.of(0, 1)).hasContent());
  }

  /**
   * Hands the in-scope clients of a company to a consumer, one page at a time (by id).
   *
   * @param companyId company
   * @param pageConsumer receives each page of clients
   */
  public void forEachPage(Long companyId, Consumer<List<Client>> pageConsumer) {
    for (ClientStatus status : statuses()) {
      int page = 0;
      Page<Client> clients;
      do {
        clients =
            search.search(
                criteria(companyId, status), PageRequest.of(page++, PAGE_SIZE, Sort.by("id")));
        if (clients.hasContent()) {
          pageConsumer.accept(clients.getContent());
        }
      } while (clients.hasNext());
    }
  }

  private static ClientSearch criteria(Long companyId, ClientStatus status) {
    return new ClientSearch(
        companyId, null, null, null, null, null, null, status, null, null, null, null, false);
  }
}
