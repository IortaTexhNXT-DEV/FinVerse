package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidate;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidateProvider;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionQueries;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retention candidates of record type {@value #RECORD_TYPE} (BRNB.106): clients in the rule's
 * statuses (INACTIVE, PROSPECT...) whose last change is on or before the cutoff. Read only.
 */
@Component
@Transactional(readOnly = true)
public class ClientRetentionProvider implements RetentionCandidateProvider {

  /** Record type in the retention rules. */
  public static final String RECORD_TYPE = "CLIENT";

  private final ClientRepository clients;

  /**
   * Creates the provider.
   *
   * @param clients clients
   */
  public ClientRetentionProvider(ClientRepository clients) {
    this.clients = clients;
  }

  @Override
  public String recordType() {
    return RECORD_TYPE;
  }

  @Override
  public long countEligible(RetentionCriteria criteria) {
    return RetentionQueries.count(clients, criteria, ClientStatus.class);
  }

  @Override
  public List<RetentionCandidate> eligible(RetentionCriteria criteria, int limit) {
    return RetentionQueries.oldestFirst(clients, criteria, ClientStatus.class, limit).stream()
        .map(ClientRetentionProvider::candidate)
        .toList();
  }

  private static RetentionCandidate candidate(Client c) {
    return new RetentionCandidate(
        c.getCode(),
        c.getDisplayName(),
        c.getStatus().name(),
        RetentionQueries.lastActivity(c),
        "/crm/clients/" + c.getId());
  }
}
