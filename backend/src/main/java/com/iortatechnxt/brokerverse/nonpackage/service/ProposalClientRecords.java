package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.crm.service.ClientRecordsProvider;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequestRepository;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The proposal requests (PRF) of a client for the client 360 view (BRNB.099). */
@Component
public class ProposalClientRecords implements ClientRecordsProvider {

  private final ProposalRequestRepository proposals;

  /**
   * Creates the provider.
   *
   * @param proposals PRFs
   */
  public ProposalClientRecords(ProposalRequestRepository proposals) {
    this.proposals = proposals;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientRecord> recordsOf(Long clientId) {
    return proposals.findByClientIdOrderByCreatedAtDesc(clientId).stream()
        .map(ProposalClientRecords::toRecord)
        .toList();
  }

  private static ClientRecord toRecord(ProposalRequest p) {
    String insurer = p.getChosenInsurer() == null ? "insurer to be chosen" : p.getChosenInsurer();
    return new ClientRecord(
        "Proposal",
        p.getPrfNo(),
        p.getArn() + " - " + p.getProductCode() + " - " + insurer,
        p.getStatus().name(),
        p.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
        "/proposals/" + p.getId());
  }
}
