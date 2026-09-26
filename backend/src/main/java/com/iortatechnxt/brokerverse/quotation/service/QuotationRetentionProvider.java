package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidate;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCandidateProvider;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionQueries;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationRepository;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationStatus;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retention candidates of record type {@value #RECORD_TYPE} (BRNB.106): quotations not proceeded or
 * voided whose last change is on or before the cutoff. Read only.
 */
@Component
@Transactional(readOnly = true)
public class QuotationRetentionProvider implements RetentionCandidateProvider {

  /** Record type in the retention rules. */
  public static final String RECORD_TYPE = "QUOTATION";

  private final QuotationRepository quotations;

  /**
   * Creates the provider.
   *
   * @param quotations quotations
   */
  public QuotationRetentionProvider(QuotationRepository quotations) {
    this.quotations = quotations;
  }

  @Override
  public String recordType() {
    return RECORD_TYPE;
  }

  @Override
  public long countEligible(RetentionCriteria criteria) {
    return RetentionQueries.count(quotations, criteria, QuotationStatus.class);
  }

  @Override
  public List<RetentionCandidate> eligible(RetentionCriteria criteria, int limit) {
    return RetentionQueries.oldestFirst(quotations, criteria, QuotationStatus.class, limit).stream()
        .map(QuotationRetentionProvider::candidate)
        .toList();
  }

  private static RetentionCandidate candidate(Quotation q) {
    return new RetentionCandidate(
        q.getQuotationNo(),
        q.getArn() + " - " + q.getClientName() + " - " + q.getProductCode(),
        q.getStatus().name(),
        RetentionQueries.lastActivity(q),
        "/quotations/" + q.getId());
  }
}
