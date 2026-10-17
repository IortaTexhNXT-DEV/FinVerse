package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration of legacy clients by the data migration (DATA_MIGRATION_DESIGN sections 9 and 10): a
 * migrated client is created as a confirmed client with its client code and sub-ledger party, keeps
 * the KYC status and review date of legacy, skips the onboarding workflow and publishes {@link
 * ClientRegistered} with {@code migrated = true}, which sanction screening ignores (the cut-over
 * plan runs one full screening after the load). The client carries its origin: source system,
 * legacy client code and migration batch. A rolled-back batch deactivates its clients.
 */
@Service
@Transactional
public class MigratedClientRegistration {

  /** Deactivation reason of a client whose migration batch is rolled back. */
  public static final String ROLLBACK_REASON = "MIGRATION_ROLLBACK";

  private final ClientRepository clients;
  private final ClientValidator validator;
  private final ClientPartyLink parties;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final ApplicationEventPublisher events;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param clients clients
   * @param validator client data validation
   * @param parties sub-ledger party link
   * @param numbers document numbers
   * @param audit audit trail
   * @param events event publisher
   * @param currentUser current user (the migration loader)
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public MigratedClientRegistration(
      ClientRepository clients,
      ClientValidator validator,
      ClientPartyLink parties,
      DocumentNumberService numbers,
      AuditTrailService audit,
      ApplicationEventPublisher events,
      CurrentUser currentUser,
      Clock clock) {
    this.clients = clients;
    this.validator = validator;
    this.parties = parties;
    this.numbers = numbers;
    this.audit = audit;
    this.events = events;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Registers a legacy client as a confirmed client.
   *
   * @param request client, KYC and origin
   * @return the client
   */
  public Client register(MigratedClient request) {
    if (request.origin() == null || !request.origin().isMigrated()) {
      throw new BusinessRuleException(
          "CLIENT_ORIGIN_REQUIRED", "A migrated client needs its source system and legacy code");
    }
    validator.validate(request.details(), request.profile());
    String code = numbers.next("CL-" + BusinessClock.today(clock).getYear());
    Client client = new Client(request.companyId(), code, request.details());
    client.applyProfile(request.profile());
    client.migratedFrom(request.origin());
    String user = currentUser.username();
    client.confirm(code, parties.openParty(client, code), user, clock.instant());
    applyKyc(client, request.kyc(), user);
    Client saved = clients.save(client);
    audit.record(
        ClientService.ENTITY,
        code,
        AuditAction.CREATE,
        "Migrated client "
            + saved.getDisplayName()
            + " from "
            + request.origin().sourceSystem()
            + " "
            + request.origin().legacyRef()
            + " (batch "
            + request.origin().migrationBatch()
            + ")");
    events.publishEvent(
        new ClientRegistered(
            saved.getCompanyId(), saved.getId(), code, saved.getClientType(), true));
    return saved;
  }

  /**
   * Applies a legacy delta to a migrated client before the freeze; no screening event.
   *
   * @param clientId client
   * @param request new data
   * @return the client
   */
  public Client update(Long clientId, MigratedClient request) {
    Client client = migrated(clientId);
    validator.validate(request.details(), request.profile());
    client.update(request.details());
    client.applyProfile(request.profile());
    applyKyc(client, request.kyc(), currentUser.username());
    audit.record(
        ClientService.ENTITY,
        client.getCode(),
        AuditAction.UPDATE,
        "Legacy changes of " + client.getRecordOrigin().legacyRef() + " applied");
    return client;
  }

  /**
   * Undoes the registration of a migrated client of a rolled-back batch: the client is deactivated
   * (it stays for the audit trail and is no longer matched by later loads).
   *
   * @param clientId client
   * @param batchNo rolled-back batch
   */
  public void rollback(Long clientId, String batchNo) {
    Client client = migrated(clientId);
    if (client.getStatus() != ClientStatus.INACTIVE) {
      client.deactivate(
          ROLLBACK_REASON,
          "Migration batch " + batchNo + " rolled back",
          currentUser.username(),
          clock.instant());
      audit.record(
          ClientService.ENTITY,
          client.getCode(),
          AuditAction.DEACTIVATE,
          "Migration batch " + batchNo + " rolled back");
    }
  }

  private Client migrated(Long clientId) {
    Client client =
        clients
            .findById(clientId)
            .orElseThrow(() -> new ResourceNotFoundException(ClientService.ENTITY, clientId));
    if (!client.getRecordOrigin().isMigrated()) {
      throw new BusinessRuleException(
          "CLIENT_NOT_MIGRATED", "Client " + client.getCode() + " was not migrated");
    }
    return client;
  }

  private void applyKyc(Client client, MigratedClient.Kyc kyc, String user) {
    MigratedClient.Kyc k = kyc == null ? MigratedClient.Kyc.NONE : kyc;
    if (k.status() == KycStatus.VERIFIED) {
      client.verifyKyc(
          user, k.verifiedAt() == null ? clock.instant() : k.verifiedAt(), k.reviewDue());
    } else {
      client.setKycStatus(k.status());
    }
  }
}
