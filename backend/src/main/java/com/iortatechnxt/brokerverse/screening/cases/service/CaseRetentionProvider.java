package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidate;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidateProvider;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionQueries;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retention candidates of record type {@value #RECORD_TYPE} (BRD-10 p.25, 5 years online and 5 in
 * archive; design section 9): closed screening cases, with their reviews, documents and STRs, whose
 * last change is on or before the cutoff of rule SCREENING_CASE. Read only; archive and purge stay
 * parked as for BRD-1.
 */
@Component
@Transactional(readOnly = true)
public class CaseRetentionProvider implements RetentionCandidateProvider {

  /** Record type in the retention rules (V1050). */
  public static final String RECORD_TYPE = "SCREENING_CASE";

  private final ScreeningCaseRepository cases;

  /**
   * Creates the provider.
   *
   * @param cases cases
   */
  public CaseRetentionProvider(ScreeningCaseRepository cases) {
    this.cases = cases;
  }

  @Override
  public String recordType() {
    return RECORD_TYPE;
  }

  @Override
  public long countEligible(RetentionCriteria criteria) {
    return RetentionQueries.count(cases, criteria, CaseStatus.class);
  }

  @Override
  public List<RetentionCandidate> eligible(RetentionCriteria criteria, int limit) {
    return RetentionQueries.oldestFirst(cases, criteria, CaseStatus.class, limit).stream()
        .map(CaseRetentionProvider::candidate)
        .toList();
  }

  private static RetentionCandidate candidate(ScreeningCase c) {
    return new RetentionCandidate(
        c.getCaseNo(),
        c.getCaseType() + " case of " + c.getClientName() + " (" + c.getClientCode() + ")",
        c.getStatus().name(),
        RetentionQueries.lastActivity(c),
        CaseCodes.link(c.getId()));
  }
}
