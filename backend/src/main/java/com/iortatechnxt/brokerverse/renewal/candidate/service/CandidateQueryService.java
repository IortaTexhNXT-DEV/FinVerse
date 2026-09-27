package com.iortatechnxt.brokerverse.renewal.candidate.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.domain.BucketHistory;
import com.iortatechnxt.brokerverse.renewal.domain.BucketHistoryRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateEndorsement;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateEndorsementRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResultRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CheckRun;
import com.iortatechnxt.brokerverse.renewal.domain.CheckRunRepository;
import com.iortatechnxt.brokerverse.renewal.domain.Disposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAssignment;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAssignmentRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDispositionRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalOverride;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalOverrideRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.renewal.service.RenewalScope;
import com.iortatechnxt.brokerverse.renewal.service.RenewalScope.Scope;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads of the renewal lists and record page (FR-RN-011-013, 040, 041, 045, 062): the lists in one
 * scrollable view served in chunks, earliest expiry first with the urgent renewals of the go-live
 * window on top, always within the user's data scope (FR-RN-002); the check runs, bucket history,
 * dispositions, assignments, overrides and linked endorsements of a renewal.
 */
@Service
@Transactional(readOnly = true)
public class CandidateQueryService {

  /** Largest chunk of a list. */
  public static final int MAX_CHUNK = 1000;

  private static final Sort ORDER =
      Sort.by(
          Sort.Order.desc("flags.urgent"),
          Sort.Order.asc("snapshot.expiryDate"),
          Sort.Order.asc("id"));

  private final RenewalCandidateRepository candidates;
  private final RenewalRecords records;
  private final RenewalScope scope;
  private final CheckRunRepository runs;
  private final CheckResultRepository results;
  private final BucketHistoryRepository buckets;
  private final RenewalDispositionRepository dispositions;
  private final RenewalAssignmentRepository assignments;
  private final RenewalOverrideRepository overrides;
  private final CandidateEndorsementRepository endorsements;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param candidates candidates
   * @param records look-up in scope
   * @param scope data scope
   * @param runs check runs
   * @param results check results
   * @param buckets bucket history
   * @param dispositions disposition history
   * @param assignments assignments
   * @param overrides overrides
   * @param endorsements endorsements linked
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public CandidateQueryService(
      RenewalCandidateRepository candidates,
      RenewalRecords records,
      RenewalScope scope,
      CheckRunRepository runs,
      CheckResultRepository results,
      BucketHistoryRepository buckets,
      RenewalDispositionRepository dispositions,
      RenewalAssignmentRepository assignments,
      RenewalOverrideRepository overrides,
      CandidateEndorsementRepository endorsements,
      CurrentUser currentUser,
      Clock clock) {
    this.candidates = candidates;
    this.records = records;
    this.scope = scope;
    this.runs = runs;
    this.results = results;
    this.buckets = buckets;
    this.dispositions = dispositions;
    this.assignments = assignments;
    this.overrides = overrides;
    this.endorsements = endorsements;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * A chunk of a renewal list in the user's scope.
   *
   * @param filter criteria
   * @param page chunk number
   * @param size chunk size (at most {@value #MAX_CHUNK})
   * @return chunk with the total row count
   */
  public Page<RenewalCandidate> list(CandidateFilter filter, int page, int size) {
    Scope userScope = scope.current(filter.companyId());
    Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_CHUNK), ORDER);
    return candidates.findAll(
        userScope
            .specification()
            .and(
                CandidateSpecifications.of(
                    filter, currentUser.username(), BusinessClock.today(clock))),
        pageable);
  }

  /**
   * The number of renewals of a list in the user's scope.
   *
   * @param filter criteria
   * @return count
   */
  public long count(CandidateFilter filter) {
    Scope userScope = scope.current(filter.companyId());
    return candidates.count(
        userScope
            .specification()
            .and(
                CandidateSpecifications.of(
                    filter, currentUser.username(), BusinessClock.today(clock))));
  }

  /**
   * The data scope of the user (premium columns hidden for LAMD and Contact Center).
   *
   * @param companyId company
   * @return scope
   */
  public Scope scope(Long companyId) {
    return scope.current(companyId);
  }

  /**
   * A renewal in the user's scope.
   *
   * @param companyId company
   * @param renewalRef renewal reference
   * @return candidate
   */
  public RenewalCandidate get(Long companyId, String renewalRef) {
    return records.get(companyId, renewalRef);
  }

  /**
   * The check runs of a renewal, newest first.
   *
   * @param candidate candidate
   * @return runs
   */
  public List<CheckRun> runs(RenewalCandidate candidate) {
    return runs.findByCandidateIdOrderByRunAtDescIdDesc(candidate.getId());
  }

  /**
   * The results of the latest check run.
   *
   * @param candidate candidate
   * @return results
   */
  public List<CheckResult> latestResults(RenewalCandidate candidate) {
    return candidate.getLastCheckRunId() == null
        ? List.of()
        : results.findByRunIdOrderByIdAsc(candidate.getLastCheckRunId());
  }

  /**
   * Bucket changes, newest first.
   *
   * @param candidate candidate
   * @return changes
   */
  public List<BucketHistory> buckets(RenewalCandidate candidate) {
    return buckets.findByCandidateIdOrderByIdDesc(candidate.getId());
  }

  /**
   * Dispositions, newest first.
   *
   * @param candidate candidate
   * @return rows
   */
  public List<Disposition> dispositions(RenewalCandidate candidate) {
    return dispositions.findByCandidateIdOrderByIdDesc(candidate.getId());
  }

  /**
   * Assignments, newest first.
   *
   * @param candidate candidate
   * @return assignments
   */
  public List<RenewalAssignment> assignments(RenewalCandidate candidate) {
    return assignments.findByCandidateIdOrderByIdDesc(candidate.getId());
  }

  /**
   * Overrides, newest first.
   *
   * @param candidate candidate
   * @return overrides
   */
  public List<RenewalOverride> overrides(RenewalCandidate candidate) {
    return overrides.findByCandidateIdOrderByIdDesc(candidate.getId());
  }

  /**
   * Endorsements linked to the renewal.
   *
   * @param candidate candidate
   * @return endorsements
   */
  public List<CandidateEndorsement> endorsements(RenewalCandidate candidate) {
    return endorsements.findByCandidateIdOrderByIdAsc(candidate.getId());
  }
}
