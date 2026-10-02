package com.iortatechnxt.brokerverse.brokerclaims.claim.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.BrokerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.catalog.service.CatalogNames;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.crm.service.ClientRecordsProvider;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.crm.service.RecordDescriptions;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The claims of a client for the client 360 view (BRCLM.040; CLAIMS_BROKING_DESIGN 3.1): claim
 * number, cover, loss date, status or phase, linked to the claim record.
 */
@Component
public class BrokerClaimClientRecords implements ClientRecordsProvider {

  private final BrokerClaimRepository claims;
  private final ClientService clients;
  private final CatalogNames names;

  /**
   * Creates the provider.
   *
   * @param claims claims
   * @param clients client codes
   * @param names product names
   */
  public BrokerClaimClientRecords(
      BrokerClaimRepository claims, ClientService clients, CatalogNames names) {
    this.names = names;
    this.claims = claims;
    this.clients = clients;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientRecord> recordsOf(Long clientId) {
    Client client = clients.get(clientId);
    return claims
        .findByCompanyIdAndCoverClientCodeOrderByIdDesc(
            client.getCompanyId(), client.getClientCode())
        .stream()
        .map(this::record)
        .toList();
  }

  private ClientRecord record(Claim c) {
    String status =
        c.getProgress().getStatusCode() == null
            ? c.getProgress().getPhase().name()
            : c.getProgress().getStatusCode();
    return new ClientRecord(
        "Claim",
        c.getClaimNo(),
        RecordDescriptions.claim(
            c.getCover().getArn(),
            names.productName(c.getCover().getProductCode()),
            c.getLoss().getLossDate()),
        status,
        c.getLoss().getReportedDate(),
        "/claims-handling/" + c.getId());
  }
}
