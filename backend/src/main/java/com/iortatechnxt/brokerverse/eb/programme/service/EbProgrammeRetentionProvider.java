package com.iortatechnxt.brokerverse.eb.programme.service;

import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeStatus;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidate;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidateProvider;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionQueries;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retention candidates of record type {@code EB_PROGRAMME} (NFR p.37; rule seeded by V1036: lost or
 * inactive programmes, 5 years online, 15 in total): programmes in the rule's statuses whose last
 * change is on or before the cutoff. Read only; archive and purge stay parked.
 */
@Component
@Transactional(readOnly = true)
public class EbProgrammeRetentionProvider implements RetentionCandidateProvider {

  private final EbProgrammeRepository programmes;

  /**
   * Creates the provider.
   *
   * @param programmes programmes
   */
  public EbProgrammeRetentionProvider(EbProgrammeRepository programmes) {
    this.programmes = programmes;
  }

  @Override
  public String recordType() {
    return EbCodes.RETENTION_PROGRAMME;
  }

  @Override
  public long countEligible(RetentionCriteria criteria) {
    return RetentionQueries.count(programmes, criteria, EbProgrammeStatus.class);
  }

  @Override
  public List<RetentionCandidate> eligible(RetentionCriteria criteria, int limit) {
    return RetentionQueries.oldestFirst(programmes, criteria, EbProgrammeStatus.class, limit)
        .stream()
        .map(EbProgrammeRetentionProvider::candidate)
        .toList();
  }

  private static RetentionCandidate candidate(EbProgramme p) {
    return new RetentionCandidate(
        p.getProgrammeNo(),
        p.getClientName() + " - " + p.getName(),
        p.getStatus().name(),
        RetentionQueries.lastActivity(p),
        EbCodes.PROGRAMME_LINK + p.getId());
  }
}
