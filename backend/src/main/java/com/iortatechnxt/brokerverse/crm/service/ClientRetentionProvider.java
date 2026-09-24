package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidate;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidateProvider;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

  private static final String UPDATED_AT = "updatedAt";
  private static final String CREATED_AT = "createdAt";

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
    Set<ClientStatus> statuses = statuses(criteria);
    return statuses.isEmpty()
        ? 0
        : clients.count(eligible(statuses, criteria.lastActivityOnOrBefore()));
  }

  @Override
  public List<RetentionCandidate> eligible(RetentionCriteria criteria, int limit) {
    Set<ClientStatus> statuses = statuses(criteria);
    if (statuses.isEmpty()) {
      return List.of();
    }
    return clients
        .findAll(
            eligible(statuses, criteria.lastActivityOnOrBefore()),
            PageRequest.of(0, limit, Sort.by(UPDATED_AT, CREATED_AT)))
        .getContent()
        .stream()
        .map(ClientRetentionProvider::candidate)
        .toList();
  }

  private static Specification<Client> eligible(Set<ClientStatus> statuses, LocalDate cutoff) {
    Instant limit = cutoff.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    return (root, query, cb) ->
        cb.and(
            root.get("status").in(statuses),
            cb.lessThan(
                cb.coalesce(root.<Instant>get(UPDATED_AT), root.<Instant>get(CREATED_AT)), limit));
  }

  private static Set<ClientStatus> statuses(RetentionCriteria criteria) {
    Set<String> known =
        Arrays.stream(ClientStatus.values()).map(Enum::name).collect(Collectors.toSet());
    return criteria.statuses().stream()
        .filter(known::contains)
        .map(ClientStatus::valueOf)
        .collect(Collectors.toSet());
  }

  private static RetentionCandidate candidate(Client c) {
    Instant last = c.getUpdatedAt() != null ? c.getUpdatedAt() : c.getCreatedAt();
    return new RetentionCandidate(
        c.getCode(),
        c.getDisplayName(),
        c.getStatus().name(),
        last.atZone(ZoneOffset.UTC).toLocalDate(),
        "/crm/clients/" + c.getId());
  }
}
