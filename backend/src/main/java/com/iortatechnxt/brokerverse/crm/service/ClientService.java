package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientContactChanged;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientProfile;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.domain.ContactChange;
import com.iortatechnxt.brokerverse.crm.domain.ContactChange.FieldChange;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Client master (core): prospect creation, lookup and update. Other broking modules use {@link
 * #get}, {@link #requireByCode} and {@link #requireUsable}; onboarding (KYC verification and
 * confirmation), duplicate checks, tags and instructions extend this service in the crm module.
 *
 * <p>Events (SANCTION_SCREENING_DESIGN section 9): {@link ClientRegistered} when a client is
 * created and {@link ClientIdentityChanged} when an update changes the name, birth date,
 * nationality, TIN or ID document; both are published inside the transaction. The contact-only
 * contract {@link #updateContact} (CUSTOMER_SERVICING_DESIGN section 11) publishes {@link
 * ClientContactChanged}.
 */
@Service
@Transactional
public class ClientService {

  /** Audit / attachment entity type. */
  public static final String ENTITY = "Client";

  private static final int LOOKUP_SIZE = 20;
  private static final int AUDIT_MAX = 500;

  private final ClientRepository clients;
  private final DocumentNumberService numbers;
  private final ClientValidator validator;
  private final DuplicateCheckService duplicates;
  private final ClientWorkflow workflow;
  private final AuditTrailService audit;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param clients clients
   * @param numbers document numbers
   * @param validator client data validation
   * @param duplicates duplicate detection
   * @param workflow onboarding workflow
   * @param audit audit trail
   * @param events event publisher
   * @param clock clock
   */
  public ClientService(
      ClientRepository clients,
      DocumentNumberService numbers,
      ClientValidator validator,
      DuplicateCheckService duplicates,
      ClientWorkflow workflow,
      AuditTrailService audit,
      ApplicationEventPublisher events,
      Clock clock) {
    this.clients = clients;
    this.numbers = numbers;
    this.validator = validator;
    this.duplicates = duplicates;
    this.workflow = workflow;
    this.audit = audit;
    this.events = events;
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
    return create(companyId, details, ClientProfile.EMPTY);
  }

  /**
   * Creates a prospect with its KYC profile (BRNB.030/048): validated, checked for duplicates
   * (blocked on a hard match, BRNB.032) and opened in the onboarding workflow (BRNB.090).
   *
   * @param companyId company
   * @param details client data
   * @param profile KYC profile
   * @return the prospect
   */
  public Client create(Long companyId, ClientDetails details, ClientProfile profile) {
    validator.validate(details, profile);
    duplicates.requireNoHardMatch(
        companyId, DuplicateProbe.of(details), null, "creation of " + nameOf(details));
    String code = numbers.next("PR-" + BusinessClock.today(clock).getYear());
    Client client = new Client(companyId, code, details);
    client.applyProfile(profile);
    Client saved = clients.save(client);
    workflow.start(saved);
    audit.record(ENTITY, code, AuditAction.CREATE, "Prospect " + saved.getDisplayName());
    events.publishEvent(
        new ClientRegistered(companyId, saved.getId(), code, saved.getClientType()));
    return saved;
  }

  /**
   * Updates client data; the KYC profile is kept.
   *
   * @param id client
   * @param details new data
   * @return the client
   */
  public Client update(Long id, ClientDetails details) {
    return update(id, details, get(id).profile());
  }

  /**
   * Updates client data and KYC profile (BRNB.049).
   *
   * @param id client
   * @param details new data
   * @param profile new KYC profile
   * @return the client
   */
  public Client update(Long id, ClientDetails details, ClientProfile profile) {
    Client client = get(id);
    validator.validate(details, profile);
    duplicates.requireNoHardMatch(
        client.getCompanyId(),
        DuplicateProbe.of(details),
        id,
        "update of " + client.getCode() + " " + nameOf(details));
    List<Object> identityBefore = identityOf(client);
    client.update(details);
    client.applyProfile(profile);
    workflow.describe(client);
    audit.record(
        ENTITY,
        client.getProspectCode(),
        AuditAction.UPDATE,
        "Updated " + client.getCode() + " " + client.getDisplayName());
    if (!identityBefore.equals(identityOf(client))) {
      events.publishEvent(new ClientIdentityChanged(client.getCompanyId(), client.getId()));
    }
    return client;
  }

  /**
   * Changes the contact details only (BRCSF-004; the contract of the Customer Servicing Facility):
   * e-mail, mobile, phone and address lines, validated with the client master rules; the duplicate
   * keys are recomputed and the change is audited with the values before and after, its source,
   * reason and reference. Other client data cannot be changed through it.
   *
   * @param id client
   * @param change new contact values (null keeps a value, blank clears it) with source and reason
   * @return the fields that changed, empty when nothing changed
   */
  public List<FieldChange> updateContact(Long id, ContactChange change) {
    Client client = get(id);
    List<FieldChange> changed = new ArrayList<>();
    for (String field : ContactChange.FIELDS) {
      String before = currentContact(client, field);
      String asked = change.valueOf(field);
      String after = asked == null ? before : blankToNull(asked.strip());
      if (!Objects.equals(before, after)) {
        changed.add(new FieldChange(field, before, after));
      }
    }
    if (changed.isEmpty()) {
      return List.of();
    }
    ClientDetails.Contact contact = newContact(client, changed);
    List<ClientRules.Violation> found = ClientRules.contactViolations(contact);
    if (!found.isEmpty()) {
      throw new BusinessRuleException(found.get(0).code(), found.get(0).message());
    }
    client.changeContact(contact);
    audit.record(
        ENTITY,
        client.getProspectCode(),
        AuditAction.UPDATE,
        cut(
            "Contact of "
                + client.getCode()
                + " changed by "
                + change.source()
                + " "
                + nz(change.reference())
                + " ("
                + nz(change.reason())
                + "): "
                + describe(changed)));
    events.publishEvent(
        new ClientContactChanged(
            client.getCompanyId(),
            client.getId(),
            client.getCode(),
            changed.stream().map(FieldChange::field).toList(),
            change.source()));
    return List.copyOf(changed);
  }

  /**
   * The current value of a contact field.
   *
   * @param client client
   * @param field one of {@link ContactChange#FIELDS}
   * @return value, may be null
   */
  public static String currentContact(Client client, String field) {
    return switch (field) {
      case "EMAIL" -> client.getEmail();
      case "MOBILE" -> client.getMobile();
      case "PHONE" -> client.getPhone();
      case "ADDRESS_LINE" -> client.getAddressLine();
      case "CITY" -> client.getCity();
      case "PROVINCE" -> client.getProvince();
      case "POSTAL_CODE" -> client.getPostalCode();
      default -> throw new IllegalArgumentException("Not a contact field: " + field);
    };
  }

  private static ClientDetails.Contact newContact(Client client, List<FieldChange> changed) {
    Map<String, String> values = new HashMap<>();
    ContactChange.FIELDS.forEach(f -> values.put(f, currentContact(client, f)));
    changed.forEach(c -> values.put(c.field(), c.newValue()));
    return new ClientDetails.Contact(
        values.get("EMAIL"),
        values.get("MOBILE"),
        values.get("PHONE"),
        values.get("ADDRESS_LINE"),
        values.get("CITY"),
        values.get("PROVINCE"),
        values.get("POSTAL_CODE"));
  }

  private static String describe(List<FieldChange> changed) {
    return String.join(
        "; ",
        changed.stream()
            .map(
                c ->
                    c.field().toLowerCase(Locale.ROOT)
                        + " "
                        + nz(c.oldValue())
                        + " -> "
                        + nz(c.newValue()))
            .toList());
  }

  private static String blankToNull(String value) {
    return value.isEmpty() ? null : value;
  }

  private static String nz(String value) {
    return value == null ? "-" : value;
  }

  private static String cut(String summary) {
    return summary.length() <= AUDIT_MAX ? summary : summary.substring(0, AUDIT_MAX);
  }

  /** What identifies a client for screening: names, birth date, nationality, TIN and ID. */
  private static List<Object> identityOf(Client c) {
    return Arrays.asList(
        c.getLastName(),
        c.getFirstName(),
        c.getMiddleName(),
        c.getCorporateName(),
        c.getBirthDate(),
        c.profile().nationality(),
        c.getTin(),
        c.getIdType(),
        c.getIdNumber());
  }

  private static String nameOf(ClientDetails details) {
    return details.name() == null ? "a client" : describe(details);
  }

  private static String describe(ClientDetails details) {
    ClientDetails.PersonName n = details.name();
    return details.clientType() == ClientType.CORPORATE
        ? String.valueOf(n.corporateName())
        : n.lastName() + ", " + n.firstName();
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
