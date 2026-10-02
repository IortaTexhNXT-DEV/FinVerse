package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditLog;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import com.iortatechnxt.brokerverse.crm.service.Client360.Warning;
import com.iortatechnxt.brokerverse.party.domain.Party;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Client 360 view (BRNB.099): aggregates the records every module holds for the client through the
 * port {@link ClientRecordsProvider} (accounts, quotations, proposals...) and flags missing
 * linkages, e.g. a confirmed client without its sub-ledger party.
 */
@Service
@Transactional(readOnly = true)
public class Client360Service {

  private final ClientService clients;
  private final List<ClientRecordsProvider> providers;
  private final ClientPartyLink parties;
  private final ClientCompleteness completeness;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param clients clients
   * @param providers record providers of the other modules
   * @param parties party link
   * @param completeness completeness rules
   * @param audit audit trail
   */
  public Client360Service(
      ClientService clients,
      List<ClientRecordsProvider> providers,
      ClientPartyLink parties,
      ClientCompleteness completeness,
      AuditTrailService audit) {
    this.clients = clients;
    this.providers = List.copyOf(providers);
    this.parties = parties;
    this.completeness = completeness;
    this.audit = audit;
  }

  /**
   * Linked records and warnings of a client.
   *
   * @param clientId client
   * @return 360 view
   */
  public Client360 view(Long clientId) {
    Client client = clients.get(clientId);
    List<ClientRecord> records =
        providers.stream()
            .flatMap(p -> p.recordsOf(clientId).stream())
            .sorted(
                Comparator.comparing(
                    ClientRecord::date, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    return new Client360(records, warnings(client));
  }

  /**
   * Linkage and data warnings of a client.
   *
   * @param client client
   * @return warnings
   */
  public List<Warning> warnings(Client client) {
    List<Warning> warnings = new ArrayList<>();
    if (client.getStatus() == ClientStatus.CONFIRMED) {
      partyWarning(client).ifPresent(warnings::add);
    }
    List<String> missing = completeness.missing(client);
    if (!missing.isEmpty()) {
      warnings.add(
          new Warning(
              "INFO_INCOMPLETE", "Client information incomplete: " + String.join(", ", missing)));
    }
    if (client.getKycStatus() == KycStatus.EXPIRED) {
      warnings.add(
          new Warning(
              "KYC_EXPIRED", "The periodic KYC review was due on " + client.getKycReviewDue()));
    }
    return warnings;
  }

  private Optional<Warning> partyWarning(Client client) {
    if (client.getPartyCode() == null) {
      return Optional.of(
          new Warning("MISSING_PARTY", "Confirmed client without a sub-ledger party code"));
    }
    Optional<Party> party = parties.partyOf(client);
    if (party.isEmpty()) {
      return Optional.of(
          new Warning("PARTY_NOT_FOUND", "Party " + client.getPartyCode() + " does not exist"));
    }
    if (!party.get().isActive()) {
      return Optional.of(
          new Warning("PARTY_INACTIVE", "Party " + client.getPartyCode() + " is not active"));
    }
    return Optional.empty();
  }

  /**
   * Audit history of a client (under its prospect and client codes), newest first.
   *
   * @param clientId client
   * @return entries
   */
  public List<AuditLog> history(Long clientId) {
    Client client = clients.get(clientId);
    List<String> keys =
        Stream.of(client.getProspectCode(), client.getClientCode()).filter(k -> k != null).toList();
    return audit.history(ClientService.ENTITY, keys);
  }

  /**
   * Whether the client's KYC review falls due by a date (for flags on lists).
   *
   * @param client client
   * @param horizon last date of the window
   * @return true when expired or due within the window
   */
  public static boolean kycDue(Client client, LocalDate horizon) {
    return client.getKycStatus() == KycStatus.EXPIRED
        || client.getKycStatus() == KycStatus.VERIFIED
            && client.getKycReviewDue() != null
            && !client.getKycReviewDue().isAfter(horizon);
  }
}
