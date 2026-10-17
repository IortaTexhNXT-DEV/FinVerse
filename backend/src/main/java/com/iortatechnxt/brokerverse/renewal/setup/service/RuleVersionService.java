package com.iortatechnxt.brokerverse.renewal.setup.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.domain.BucketRule;
import com.iortatechnxt.brokerverse.renewal.domain.BucketRuleSet;
import com.iortatechnxt.brokerverse.renewal.domain.BucketRuleSetRepository;
import com.iortatechnxt.brokerverse.renewal.domain.DecisionMatrix;
import com.iortatechnxt.brokerverse.renewal.domain.DecisionMatrixRepository;
import com.iortatechnxt.brokerverse.renewal.domain.DecisionRule;
import com.iortatechnxt.brokerverse.renewal.domain.RuleSetStatus;
import com.iortatechnxt.brokerverse.renewal.domain.VersionedRules;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Versions of the bucket rules (BRRN.023) and of the decision matrix (BRRN.031/034): a draft is
 * edited, submitted and activated by a checker other than its maker; activation retires the
 * previous version. Each evaluation records the version it used.
 */
@Service
@Transactional
public class RuleVersionService {

  /** Audit entity of a bucket rule set. */
  public static final String ENTITY_BUCKET_RULES = "RenewalBucketRules";

  /** Audit entity of a decision matrix. */
  public static final String ENTITY_MATRIX = "RenewalDecisionMatrix";

  private static final String BUCKET_RULES = "Bucket rules";
  private static final String MATRIX = "Decision matrix";

  private final BucketRuleSetRepository ruleSets;
  private final DecisionMatrixRepository matrices;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param ruleSets bucket rule sets
   * @param matrices decision matrices
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public RuleVersionService(
      BucketRuleSetRepository ruleSets,
      DecisionMatrixRepository matrices,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.ruleSets = ruleSets;
    this.matrices = matrices;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The bucket rule sets of a company, newest first.
   *
   * @param companyId company
   * @return versions
   */
  @Transactional(readOnly = true)
  public List<BucketRuleSet> bucketRuleSets(Long companyId) {
    List<BucketRuleSet> sets = ruleSets.findByCompanyIdOrderByVersionNoDesc(companyId);
    sets.forEach(s -> Hibernate.initialize(s.getRules()));
    return sets;
  }

  /**
   * Saves a draft of the bucket rules: a new version when no id is given.
   *
   * @param companyId company
   * @param id draft, null for a new version
   * @param header effective date and description
   * @param rules rules
   * @return draft
   */
  public BucketRuleSet saveBucketRules(
      Long companyId, Long id, Header header, List<BucketRule.Data> rules) {
    requireDistinct(rules.stream().map(BucketRule.Data::priority).toList());
    BucketRuleSet set;
    if (id == null) {
      int next =
          ruleSets.findByCompanyIdOrderByVersionNoDesc(companyId).stream()
                  .mapToInt(VersionedRules::getVersionNo)
                  .max()
                  .orElse(0)
              + 1;
      set = new BucketRuleSet(companyId, next, effective(header), header.description());
    } else {
      set = bucketRuleSet(companyId, id);
      set.describe(effective(header), header.description());
    }
    set.replaceRules(rules);
    BucketRuleSet saved = ruleSets.save(set);
    audit.record(
        ENTITY_BUCKET_RULES,
        saved.getId(),
        id == null ? AuditAction.CREATE : AuditAction.UPDATE,
        BUCKET_RULES + " version " + saved.getVersionNo() + ", " + rules.size() + " rule(s)");
    return saved;
  }

  /**
   * Submits, activates or rejects a bucket rule set.
   *
   * @param companyId company
   * @param id version
   * @param step step
   * @param remarks remarks (reject)
   * @return version
   */
  public BucketRuleSet decideBucketRules(Long companyId, Long id, Step step, String remarks) {
    BucketRuleSet set = bucketRuleSet(companyId, id);
    List<BucketRuleSet> active = ruleSets.findByCompanyIdAndStatus(companyId, RuleSetStatus.ACTIVE);
    retireBeforeActivation(set, step, active);
    ruleSets.flush();
    apply(set, step, remarks);
    audit.record(ENTITY_BUCKET_RULES, id, action(step), BUCKET_RULES + " " + describe(set));
    return set;
  }

  /**
   * The decision matrices of a company, newest first.
   *
   * @param companyId company
   * @return versions
   */
  @Transactional(readOnly = true)
  public List<DecisionMatrix> matrices(Long companyId) {
    List<DecisionMatrix> list = matrices.findByCompanyIdOrderByVersionNoDesc(companyId);
    list.forEach(m -> Hibernate.initialize(m.getRules()));
    return list;
  }

  /**
   * Saves a draft of the decision matrix: a new version when no id is given.
   *
   * @param companyId company
   * @param id draft, null for a new version
   * @param header effective date and description
   * @param rules rules
   * @return draft
   */
  public DecisionMatrix saveMatrix(
      Long companyId, Long id, Header header, List<DecisionRule.Data> rules) {
    requireDistinct(rules.stream().map(DecisionRule.Data::priority).toList());
    DecisionMatrix matrix;
    if (id == null) {
      int next =
          matrices.findByCompanyIdOrderByVersionNoDesc(companyId).stream()
                  .mapToInt(VersionedRules::getVersionNo)
                  .max()
                  .orElse(0)
              + 1;
      matrix = new DecisionMatrix(companyId, next, effective(header), header.description());
    } else {
      matrix = matrix(companyId, id);
      matrix.describe(effective(header), header.description());
    }
    matrix.replaceRules(rules);
    DecisionMatrix saved = matrices.save(matrix);
    audit.record(
        ENTITY_MATRIX,
        saved.getId(),
        id == null ? AuditAction.CREATE : AuditAction.UPDATE,
        MATRIX + " version " + saved.getVersionNo() + ", " + rules.size() + " rule(s)");
    return saved;
  }

  /**
   * Submits, activates or rejects a decision matrix.
   *
   * @param companyId company
   * @param id version
   * @param step step
   * @param remarks remarks (reject)
   * @return version
   */
  public DecisionMatrix decideMatrix(Long companyId, Long id, Step step, String remarks) {
    DecisionMatrix matrix = matrix(companyId, id);
    List<DecisionMatrix> active =
        matrices.findByCompanyIdAndStatus(companyId, RuleSetStatus.ACTIVE);
    retireBeforeActivation(matrix, step, active);
    matrices.flush();
    apply(matrix, step, remarks);
    audit.record(ENTITY_MATRIX, id, action(step), MATRIX + " " + describe(matrix));
    return matrix;
  }

  private static void retireBeforeActivation(
      VersionedRules version, Step step, List<? extends VersionedRules> active) {
    if (step == Step.ACTIVATE && version.getStatus() == RuleSetStatus.SUBMITTED) {
      active.stream()
          .filter(a -> !a.getId().equals(version.getId()))
          .forEach(VersionedRules::retire);
    }
  }

  private void apply(VersionedRules version, Step step, String remarks) {
    String user = currentUser.username();
    if (step == Step.SUBMIT) {
      version.submit(user, clock.instant());
    } else if (step == Step.ACTIVATE) {
      version.activate(user, clock.instant());
    } else {
      if (remarks == null || remarks.isBlank()) {
        throw new BusinessRuleException("RNW_RULES_REJECT_REMARKS", "Enter the reason");
      }
      version.reject(user, remarks.strip(), clock.instant());
    }
  }

  private static AuditAction action(Step step) {
    return switch (step) {
      case SUBMIT -> AuditAction.SUBMIT;
      case ACTIVATE -> AuditAction.AUTHORIZE;
      case REJECT -> AuditAction.REJECT;
    };
  }

  private static String describe(VersionedRules v) {
    return "version " + v.getVersionNo() + " " + v.getStatus().name().toLowerCase(Locale.ROOT);
  }

  private LocalDate effective(Header header) {
    return header.effectiveFrom() == null ? BusinessClock.today(clock) : header.effectiveFrom();
  }

  private static void requireDistinct(List<Integer> priorities) {
    Set<Integer> seen = new HashSet<>();
    for (Integer p : priorities) {
      if (!seen.add(p)) {
        throw new BusinessRuleException(
            "RNW_RULE_PRIORITY_DUPLICATE", "Two rules have the priority " + p);
      }
    }
  }

  private BucketRuleSet bucketRuleSet(Long companyId, Long id) {
    return ruleSets
        .findById(id)
        .filter(s -> s.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException(BUCKET_RULES, id));
  }

  private DecisionMatrix matrix(Long companyId, Long id) {
    return matrices
        .findById(id)
        .filter(s -> s.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException(MATRIX, id));
  }

  /**
   * Header of a version.
   *
   * @param effectiveFrom effective date, default today
   * @param description description
   */
  public record Header(LocalDate effectiveFrom, String description) {}

  /** Steps of a version. */
  public enum Step {
    /** Maker submits the draft. */
    SUBMIT,
    /** Checker activates it. */
    ACTIVATE,
    /** Checker rejects it. */
    REJECT
  }
}
