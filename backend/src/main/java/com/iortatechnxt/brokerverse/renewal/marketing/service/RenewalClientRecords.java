package com.iortatechnxt.brokerverse.renewal.marketing.service;

import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.crm.service.ClientRecordsProvider;
import com.iortatechnxt.brokerverse.crm.service.RecordDescriptions;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The renewals of a client for the client 360 view: renewal reference, expiring policy and expiry,
 * status, linked to the renewal record page.
 */
@Component
public class RenewalClientRecords implements ClientRecordsProvider {

  private final RenewalCandidateRepository candidates;

  /**
   * Creates the provider.
   *
   * @param candidates renewals
   */
  public RenewalClientRecords(RenewalCandidateRepository candidates) {
    this.candidates = candidates;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientRecord> recordsOf(Long clientId) {
    return candidates.findByClient(clientId).stream().map(RenewalClientRecords::record).toList();
  }

  private static ClientRecord record(RenewalCandidate c) {
    String policy = c.getSnapshot().policyNo();
    return new ClientRecord(
        "Renewal",
        c.getRenewalRef(),
        RecordDescriptions.renewal(policy == null ? c.getExpiringArn() : policy, c.getExpiryDate()),
        c.getStage().name(),
        c.getExpiryDate(),
        RenewalCodes.LINK + c.getRenewalRef());
  }
}
