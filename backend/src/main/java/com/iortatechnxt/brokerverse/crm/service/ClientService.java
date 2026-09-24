package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Client master (core): prospect creation, lookup and update. Other broking modules use {@link
 * #get}, {@link #requireByCode} and {@link #requireUsable}; onboarding (KYC verification and
 * confirmation), duplicate checks, tags and instructions extend this service in the crm module.
 */
@Service
@Transactional
public class ClientService {

  /** Audit / attachment entity type. */
  public static final String ENTITY = "Client";

  private static final int LOOKUP_SIZE = 20;

  private final ClientRepository clients;
  private final DocumentNumberService numbers;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param clients clients
   * @param numbers document numbers
   * @param lovs lists of values
   * @param audit audit trail
   * @param clock clock
   */
  public ClientService(
      ClientRepository clients,
      DocumentNumberService numbers,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.clients = clients;
    this.numbers = numbers;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Creates a prospect with its prospect code (BRNB.029: minimum data is enough to quote).
   *
   * @param companyId company
   * @param details client data
   * @return the prospect
   */
  public Client createProspect(Long companyId, ClientDetails details) {
    validateCodes(details);
    String code = numbers.next("PR-" + LocalDate.now(clock).getYear());
    Client saved = clients.save(new Client(companyId, code, details));
    audit.record(ENTITY, code, AuditAction.CREATE, "Prospect " + saved.getDisplayName());
    return saved;
  }

  /**
   * Updates client data.
   *
   * @param id client
   * @param details new data
   * @return the client
   */
  public Client update(Long id, ClientDetails details) {
    Client client = get(id);
    validateCodes(details);
    client.update(details);
    audit.record(
        ENTITY, client.getCode(), AuditAction.UPDATE, "Updated " + client.getDisplayName());
    return client;
  }

  private void validateCodes(ClientDetails details) {
    LocalDate today = LocalDate.now(clock);
    lovs.validateOptional("MARKET_SEGMENT", details.marketSegment(), today);
    if (details.identity() != null) {
      lovs.validateOptional("ID_TYPE", details.identity().idType(), today);
    }
  }

  /**
   * Usable clients (prospects and confirmed) whose code or name contains a term, for pickers.
   *
   * @param companyId company
   * @param term code or name fragment
   * @return up to 20 clients by name
   */
  @Transactional(readOnly = true)
  public List<Client> lookup(Long companyId, String term) {
    String like = "%" + (term == null ? "" : term.trim().toLowerCase(Locale.ROOT)) + "%";
    Specification<Client> spec =
        (root, query, cb) ->
            cb.and(
                cb.equal(root.get("companyId"), companyId),
                cb.notEqual(root.get("status"), ClientStatus.INACTIVE),
                cb.or(
                    cb.like(cb.lower(root.get("displayName")), like),
                    cb.like(cb.lower(root.get("prospectCode")), like),
                    cb.like(cb.lower(root.get("clientCode")), like)));
    return clients
        .findAll(spec, PageRequest.of(0, LOOKUP_SIZE, Sort.by("displayName")))
        .getContent();
  }

  /**
   * One client.
   *
   * @param id id
   * @return client
   */
  @Transactional(readOnly = true)
  public Client get(Long id) {
    return clients.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * A client by client or prospect code.
   *
   * @param companyId company
   * @param code code
   * @return client
   */
  @Transactional(readOnly = true)
  public Client requireByCode(Long companyId, String code) {
    return clients
        .findByCode(companyId, code)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, code));
  }

  /**
   * A client that may be used for new business (prospect or confirmed, not inactive).
   *
   * @param id client
   * @return client
   */
  @Transactional(readOnly = true)
  public Client requireUsable(Long id) {
    Client client = get(id);
    if (client.getStatus() == ClientStatus.INACTIVE) {
      throw new BusinessRuleException(
          "CLIENT_INACTIVE", "Client " + client.getCode() + " is inactive");
    }
    return client;
  }

  /**
   * A confirmed client (placement, issuance and booking need an onboarded client, BRNB.029).
   *
   * @param id client
   * @return client
   */
  @Transactional(readOnly = true)
  public Client requireConfirmed(Long id) {
    Client client = get(id);
    if (client.getStatus() != ClientStatus.CONFIRMED) {
      throw new BusinessRuleException(
          "CLIENT_NOT_CONFIRMED",
          "Client " + client.getCode() + " is not yet confirmed (onboarding and KYC incomplete)");
    }
    return client;
  }
}
