package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.catalog.service.CatalogNames;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.crm.service.ClientRecordsProvider;
import com.iortatechnxt.brokerverse.crm.service.RecordDescriptions;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequestRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The proposal requests (PRF) of a client for the client 360 view (BRNB.099). */
@Component
public class ProposalClientRecords implements ClientRecordsProvider {

  private final ProposalRequestRepository proposals;
  private final CatalogNames names;

  /**
   * Creates the provider.
   *
   * @param proposals PRFs
   * @param names product and insurer names
   */
  public ProposalClientRecords(ProposalRequestRepository proposals, CatalogNames names) {
    this.proposals = proposals;
    this.names = names;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientRecord> recordsOf(Long clientId) {
    return proposals.findByClientIdOrderByCreatedAtDesc(clientId).stream()
        .map(this::toRecord)
        .toList();
  }

  private ClientRecord toRecord(ProposalRequest p) {
    return new ClientRecord(
        "Proposal",
        p.getPrfNo(),
        RecordDescriptions.proposal(
            p.getArn(), names.productName(p.getProductCode()), names.insurer(p.getChosenInsurer())),
        p.getStatus().name(),
        BusinessClock.dateOf(p.getCreatedAt()),
        "/proposals/" + p.getId());
  }
}
