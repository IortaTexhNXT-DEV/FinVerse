package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidate;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidateProvider;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionQueries;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequestRepository;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalStatus;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retention candidates of record type {@value #RECORD_TYPE} (BRNB.106): PRFs not proceeded or
 * voided whose last change is on or before the cutoff. Read only.
 */
@Component
@Transactional(readOnly = true)
public class ProposalRetentionProvider implements RetentionCandidateProvider {

  /** Record type in the retention rules. */
  public static final String RECORD_TYPE = "PROPOSAL";

  private final ProposalRequestRepository proposals;

  /**
   * Creates the provider.
   *
   * @param proposals PRFs
   */
  public ProposalRetentionProvider(ProposalRequestRepository proposals) {
    this.proposals = proposals;
  }

  @Override
  public String recordType() {
    return RECORD_TYPE;
  }

  @Override
  public long countEligible(RetentionCriteria criteria) {
    return RetentionQueries.count(proposals, criteria, ProposalStatus.class);
  }

  @Override
  public List<RetentionCandidate> eligible(RetentionCriteria criteria, int limit) {
    return RetentionQueries.oldestFirst(proposals, criteria, ProposalStatus.class, limit).stream()
        .map(
            p ->
                new RetentionCandidate(
                    p.getPrfNo(),
                    describe(p),
                    p.getStatus().name(),
                    RetentionQueries.lastActivity(p),
                    "/proposals/" + p.getId()))
        .toList();
  }

  private static String describe(ProposalRequest p) {
    return p.getArn() + " - " + p.getClientName() + " - " + p.getProductCode();
  }
}
