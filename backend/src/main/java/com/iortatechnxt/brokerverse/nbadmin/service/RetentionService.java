package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbadmin.domain.RetentionRule;
import com.iortatechnxt.brokerverse.nbadmin.domain.RetentionRuleRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.RetentionRun;
import com.iortatechnxt.brokerverse.nbadmin.domain.RetentionRunRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.RetentionTerms;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data retention (BRNB.106, NFR 5 years online / 15 years archive): maintains the retention rules
 * and counts, per rule, the records that have become eligible, through the modules' {@link
 * RetentionCandidateProvider}s. It never deletes or moves data: archiving and purging are parked
 * until the DBA decides the archive storage and backup (Q39).
 */
@Service
@Transactional
public class RetentionService {

  private static final String ENTITY = "RetentionRule";

  private final RetentionRuleRepository rules;
  private final RetentionRunRepository runs;
  private final Map<String, RetentionCandidateProvider> providers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param rules retention rules
   * @param runs review results
   * @param providers record type providers of the business modules
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public RetentionService(
      RetentionRuleRepository rules,
      RetentionRunRepository runs,
      List<RetentionCandidateProvider> providers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.rules = rules;
    this.runs = runs;
    this.providers =
        providers.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    RetentionCandidateProvider::recordType, Function.identity()));
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Every rule with its latest review result.
   *
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<RuleStatus> rules() {
    return rules.findAllByOrderByRecordTypeAscIdAsc().stream()
        .map(
            r ->
                new RuleStatus(
                    r,
                    providers.containsKey(r.getRecordType()),
                    runs.findTopByRuleIdOrderByIdDesc(r.getId()).orElse(null)))
        .toList();
  }

  /**
   * Changes a rule (years, statuses, action, active).
   *
   * @param id rule
   * @param terms new terms
   * @return the rule
   */
  public RetentionRule update(Long id, RetentionTerms terms) {
    RetentionRule rule = get(id);
    rule.change(terms);
    audit.record(
        ENTITY,
        id,
        AuditAction.UPDATE,
        rule.getRecordType()
            + " ["
            + rule.getStatuses()
            + "]: "
            + rule.getYearsOnline()
            + " years online, "
            + rule.getYearsArchive()
            + " years archive, "
            + rule.getAction()
            + (rule.isActive() ? "" : " (inactive)"));
    return rule;
  }

  /**
   * Records currently eligible under a rule (drill-down).
   *
   * @param id rule
   * @param limit maximum number of records
   * @return rule, whether a provider exists, cutoff and records
   */
  @Transactional(readOnly = true)
  public DrillDown eligible(Long id, int limit) {
    RetentionRule rule = get(id);
    LocalDate cutoff = rule.cutoff(LocalDate.now(clock));
    Optional<RetentionCandidateProvider> provider =
        Optional.ofNullable(providers.get(rule.getRecordType()));
    List<RetentionCandidate> records =
        provider
            .map(p -> p.eligible(new RetentionCriteria(rule.statusSet(), cutoff), limit))
            .orElse(List.of());
    return new DrillDown(rule, provider.isPresent(), cutoff, records);
  }

  /**
   * Monthly review: counts the eligible records of every active rule and records the result.
   *
   * @param businessDate business date
   * @return number of rules evaluated and total eligible records
   */
  public ReviewResult review(LocalDate businessDate) {
    int evaluated = 0;
    long eligible = 0;
    for (RetentionRule rule : rules.findAllByOrderByRecordTypeAscIdAsc()) {
      if (!rule.isActive()) {
        continue;
      }
      LocalDate cutoff = rule.cutoff(businessDate);
      RetentionCandidateProvider provider = providers.get(rule.getRecordType());
      Long count =
          provider == null
              ? null
              : provider.countEligible(new RetentionCriteria(rule.statusSet(), cutoff));
      runs.save(
          new RetentionRun(
              rule.getId(), businessDate, cutoff, count, currentUser.username(), clock.instant()));
      evaluated++;
      eligible += count == null ? 0 : count;
    }
    audit.record(
        ENTITY,
        businessDate,
        AuditAction.RUN,
        "Retention review: " + evaluated + " rule(s), " + eligible + " eligible record(s)");
    return new ReviewResult(evaluated, eligible);
  }

  private RetentionRule get(Long id) {
    return rules.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * A rule with its latest result.
   *
   * @param rule rule
   * @param providerAvailable whether a module provides the record type
   * @param latestRun latest review, null before the first run
   */
  public record RuleStatus(RetentionRule rule, boolean providerAvailable, RetentionRun latestRun) {}

  /**
   * Eligible records of a rule.
   *
   * @param rule rule
   * @param providerAvailable whether a module provides the record type
   * @param cutoff last activity date counted
   * @param records records
   */
  public record DrillDown(
      RetentionRule rule,
      boolean providerAvailable,
      LocalDate cutoff,
      List<RetentionCandidate> records) {

    /** Defensive copy. */
    public DrillDown {
      records = List.copyOf(records);
    }
  }

  /**
   * Outcome of a review.
   *
   * @param rulesEvaluated active rules evaluated
   * @param eligibleRecords total eligible records
   */
  public record ReviewResult(int rulesEvaluated, long eligibleRecords) {}
}
