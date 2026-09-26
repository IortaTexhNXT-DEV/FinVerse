package com.iortatechnxt.brokerverse.screening.risk.service;

import com.iortatechnxt.brokerverse.crm.service.ClientRiskProfile;
import com.iortatechnxt.brokerverse.crm.service.ClientRiskService;
import com.iortatechnxt.brokerverse.crm.service.RiskProfileChange;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.screening.config.service.ActiveConfig;
import com.iortatechnxt.brokerverse.screening.config.service.RiskRules;
import com.iortatechnxt.brokerverse.screening.matching.domain.MatchStatus;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningMatchRepository;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningSubject;
import com.iortatechnxt.brokerverse.screening.risk.domain.RiskProfileEntry;
import com.iortatechnxt.brokerverse.screening.risk.domain.RiskProfileEntryRepository;
import com.iortatechnxt.brokerverse.screening.risk.domain.RiskSource;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskRuleEvaluator.ClientFacts;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskRuleEvaluator.MatchFacts;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskRuleEvaluator.Qualification;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tags the client risk profile by rule (SNSRP-302; FR-SS-033): evaluates the risk rules of a
 * version on the client and its live matches, sets the category's rating and adds its tags through
 * {@code crm.service.ClientRiskService} (source RULE, reference = run and match), and writes the
 * history row. Nothing is written when no rule qualifies or the client already has the rating and
 * tags. A rule never lowers the rating (R2: a rating set by hand after a false positive stays until
 * a stronger rule qualifies); the order of ratings is the sort order of {@code KYC_RISK_RATING}.
 */
@Service
@Transactional
public class RiskProfiler {

  private static final Logger LOG = LoggerFactory.getLogger(RiskProfiler.class);
  private static final String RATING_LIST = "KYC_RISK_RATING";
  private static final String TAG_LIST = "CLIENT_TAG";
  private static final Set<MatchStatus> LIVE =
      Set.of(MatchStatus.POTENTIAL, MatchStatus.TRUE_MATCH);

  private final ActiveConfig config;
  private final ClientRiskService clientRisk;
  private final ScreeningMatchRepository matches;
  private final RiskProfileEntryRepository history;
  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the profiler.
   *
   * @param config active configuration
   * @param clientRisk client master risk service
   * @param matches matches
   * @param history risk-profile history
   * @param lovs lists of values
   * @param clock clock
   */
  public RiskProfiler(
      ActiveConfig config,
      ClientRiskService clientRisk,
      ScreeningMatchRepository matches,
      RiskProfileEntryRepository history,
      LovService lovs,
      Clock clock) {
    this.config = config;
    this.clientRisk = clientRisk;
    this.matches = matches;
    this.history = history;
    this.lovs = lovs;
    this.clock = clock;
  }

  /**
   * Evaluates the rules for a client and applies the qualifying category.
   *
   * @param subject the client
   * @param riskVersionId the RISK_RULES version, {@code null} when none is in force
   * @param runId the screening run, {@code null} for a match decision
   * @param reference the reference written to the client audit (run or case number)
   * @return the outcome, empty when no rule qualifies
   */
  public Optional<RiskOutcome> evaluate(
      ScreeningSubject subject, Long riskVersionId, Long runId, String reference) {
    if (riskVersionId == null || "INACTIVE".equals(subject.status())) {
      return Optional.empty();
    }
    RiskRules rules = config.riskRules(riskVersionId);
    Set<String> tags = clientRisk.activeTags(subject.clientId());
    List<MatchFacts> live =
        matches.findByClientIdAndStatusInOrderByIdAsc(subject.clientId(), LIVE).stream()
            .map(m -> new MatchFacts(m.getId(), m.getListType(), m.getStatus().name()))
            .toList();
    ClientFacts facts =
        new ClientFacts(
            subject.clientType(),
            subject.nationality(),
            subject.occupation(),
            subject.sourceOfFunds(),
            subject.marketSegment(),
            tags);
    return RiskRuleEvaluator.evaluate(rules, facts, live)
        .map(q -> apply(subject, tags, q, new Evaluation(riskVersionId, runId, reference)));
  }

  private RiskOutcome apply(
      ScreeningSubject subject, Set<String> tags, Qualification q, Evaluation at) {
    RiskRules.Category category = q.category();
    Map<String, Integer> ranks = ranks();
    String target = category.kycRiskRating();
    boolean raise =
        target != null
            && ranks.containsKey(target)
            && ranks.getOrDefault(target, 0) > ranks.getOrDefault(subject.riskRating(), -1);
    Set<String> validTags = validTags();
    Set<String> add =
        category.tags().stream()
            .filter(t -> validTags.contains(t) && !tags.contains(t))
            .collect(Collectors.toCollection(TreeSet::new));
    if (target != null && !ranks.containsKey(target)) {
      LOG.warn("Risk category {} sets an unknown rating {}", category.code(), target);
    }
    if (!raise && add.isEmpty()) {
      return outcome(subject, q, at, subject.riskRating(), Set.of(), null);
    }
    ClientRiskProfile profile =
        clientRisk.applyRiskProfile(
            subject.clientId(),
            new RiskProfileChange(
                raise ? target : null,
                add,
                Set.of(),
                ClientRiskService.SOURCE_RULE,
                "Risk category " + category.code() + " (rule " + q.rule().priority() + ")",
                reference(at, q)));
    RiskProfileEntry entry =
        history.save(
            new RiskProfileEntry(
                new RiskProfileEntry.ClientChange(
                    subject.companyId(),
                    subject.clientId(),
                    subject.code(),
                    profile.previousRating(),
                    profile.riskRating(),
                    profile.tagsAdded(),
                    profile.tagsRemoved(),
                    profile.activeTags(),
                    profile.kycReviewDue()),
                new RiskProfileEntry.Cause(
                    RiskSource.RULE,
                    category.code(),
                    at.riskVersionId(),
                    q.rule().id(),
                    q.matchId(),
                    at.runId(),
                    category.name(),
                    null,
                    null),
                clock.instant()));
    return outcome(subject, q, at, profile.riskRating(), profile.tagsAdded(), entry.getId());
  }

  private static RiskOutcome outcome(
      ScreeningSubject subject,
      Qualification q,
      Evaluation at,
      String rating,
      Set<String> added,
      Long entryId) {
    RiskRules.Category c = q.category();
    return new RiskOutcome(
        subject.clientId(),
        c.code(),
        c.name(),
        c.tier(),
        c.kycRiskRating(),
        rating,
        added,
        c.caseType(),
        c.requiresEdd(),
        at.riskVersionId(),
        q.rule().id(),
        q.matchId(),
        entryId,
        entryId != null);
  }

  private static String reference(Evaluation at, Qualification q) {
    String match = q.matchId() == null ? "" : ", match " + q.matchId();
    return Objects.requireNonNullElse(at.reference(), "screening") + match;
  }

  private Map<String, Integer> ranks() {
    return lovs.activeValues(RATING_LIST, LocalDate.now(clock)).stream()
        .collect(Collectors.toMap(LovValue::getCode, LovValue::getSortOrder, (a, b) -> a));
  }

  private Set<String> validTags() {
    return lovs.activeValues(TAG_LIST, LocalDate.now(clock)).stream()
        .map(LovValue::getCode)
        .collect(Collectors.toSet());
  }

  private record Evaluation(Long riskVersionId, Long runId, String reference) {}
}
