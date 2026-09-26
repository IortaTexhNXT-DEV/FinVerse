package com.iortatechnxt.brokerverse.brokerclaims.claim.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.BrokerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.crm.service.ClientRecordsProvider;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
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

  /**
   * Creates the provider.
   *
   * @param claims claims
   * @param clients client codes
   */
  public BrokerClaimClientRecords(BrokerClaimRepository claims, ClientService clients) {
    this.claims = claims;
    this.clients = clients;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientRecord> recordsOf(Long clientId) {
    Client client = clients.get(clientId);
    return claims
        .findByCompanyIdAndCoverClientCodeOrderByIdDesc(client.getCompanyId(), client.getClientCode())
        .stream()
        .map(BrokerClaimClientRecords::record)
        .toList();
  }

  private static ClientRecord record(Claim c) {
    String status =
        c.getProgress().getStatusCode() == null
            ? c.getProgress().getPhase().name()
            : c.getProgress().getStatusCode();
    return new ClientRecord(
        "Claim",
        c.getClaimNo(),
        c.getCover().getArn() + " - " + c.getCover().getProductCode() + " - loss " + c.getLoss().getLossDate(),
        status,
        c.getLoss().getReportedDate(),
        "/claims-handling/" + c.getId());
  }
}
