package com.iortatechnxt.brokerverse.submitted.masterlist.service;

import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyHistory;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyHistoryRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistFilter.Tab;
import com.iortatechnxt.brokerverse.submitted.service.SbmScopeService;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads of the masterlist (BRIDSP-04, 28, 29; FRS FR-SP-010-012): the work list with its tabs and
 * filters, the tab counters and the field history, always within the user's data scope.
 */
@Service
@Transactional(readOnly = true)
public class MasterlistQueryService {

  private final SbmPolicyRepository policies;
  private final SbmPolicyHistoryRepository history;
  private final SbmScopeService scope;

  /**
   * Creates the service.
   *
   * @param policies masterlist
   * @param history field history
   * @param scope data scope
   */
  public MasterlistQueryService(
      SbmPolicyRepository policies, SbmPolicyHistoryRepository history, SbmScopeService scope) {
    this.policies = policies;
    this.history = history;
    this.scope = scope;
  }

  /**
   * A page of the masterlist.
   *
   * @param filter criteria
   * @param pageable page and sort
   * @return records
   */
  public Page<SbmPolicy> list(MasterlistFilter filter, Pageable pageable) {
    return policies.findAll(specification(filter), pageable);
  }

  /**
   * Every record of a filter (bulk actions, exports), within the scope.
   *
   * @param filter criteria
   * @return records
   */
  public List<SbmPolicy> all(MasterlistFilter filter) {
    return policies.findAll(specification(filter));
  }

  /**
   * The number of records of each tab for the other criteria of a filter.
   *
   * @param filter criteria
   * @return count per tab
   */
  public Map<Tab, Long> counts(MasterlistFilter filter) {
    Map<Tab, Long> counts = new EnumMap<>(Tab.class);
    for (Tab tab : Tab.values()) {
      MasterlistFilter f =
          new MasterlistFilter(
              filter.companyId(),
              tab,
              filter.text(),
              filter.segment(),
              filter.businessType(),
              filter.bucket(),
              filter.expiryMonth(),
              filter.insurerCode(),
              filter.handler(),
              filter.conversionStatus(),
              filter.migrated(),
              filter.changedSince());
      counts.put(tab, policies.count(specification(f)));
    }
    return counts;
  }

  /**
   * The field history of a record, newest first.
   *
   * @param policyId record
   * @return changes
   */
  public List<SbmPolicyHistory> history(Long policyId) {
    return history.findByPolicyIdOrderByIdDesc(policyId);
  }

  private Specification<SbmPolicy> specification(MasterlistFilter filter) {
    return MasterlistSpecifications.of(filter)
        .and(scope.current(filter.companyId()).specification());
  }
}
