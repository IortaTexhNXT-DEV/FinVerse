package com.iortatechnxt.brokerverse.submitted.processing.service;

import com.iortatechnxt.brokerverse.submitted.domain.SbmClassification;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoan;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoanRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitCheck;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleOutcome;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRunResult;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRunResult.Decision;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRunResult.Outcome;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRunResultRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmStep;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmFacts.Duplicates;
import com.iortatechnxt.brokerverse.submitted.service.SbmHistoryService;
import com.iortatechnxt.brokerverse.submitted.service.SbmPolicyFlow;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the steps of a processing run for one record (BRIDSP-08-16; design section 3.2): SANITATION,
 * MATCHING (the latest LAMD snapshot row of the PN), CLASSIFICATION, DISPOSITION and LIMITS. Each
 * step writes one result row with the rule and rule-set version; a fallout ends the run of the
 * record. The final bucket moves the record: RENEW to For Renewal, MANUAL to For Manual
 * Disposition, EXCLUDE to Excluded, a REVIEW flag to In Review, no bucket to Classified. A manual
 * renewal tag overrides the rule's (outcome OVERRIDDEN).
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class PolicyProcessor {

  private static final Set<SbmPolicyStatus> OPEN =
      EnumSet.complementOf(
          EnumSet.of(SbmPolicyStatus.CLOSED, SbmPolicyStatus.BOOKED, SbmPolicyStatus.NOT_RENEWED));

  private final SbmPolicyRepository policies;
  private final SbmLamdLoanRepository lamd;
  private final SbmRunResultRepository results;
  private final LimitsStep limits;
  private final SbmHistoryService history;
  private final SbmPolicyFlow flow;

  /**
   * Creates the processor.
   *
   * @param policies masterlist
   * @param lamd LAMD loans
   * @param results run results
   * @param limits limits step
   * @param history field history
   * @param flow work case
   */
  public PolicyProcessor(
      SbmPolicyRepository policies,
      SbmLamdLoanRepository lamd,
      SbmRunResultRepository results,
      LimitsStep limits,
      SbmHistoryService history,
      SbmPolicyFlow flow) {
    this.policies = policies;
    this.lamd = lamd;
    this.results = results;
    this.limits = limits;
    this.history = history;
    this.flow = flow;
  }

  /**
   * Processes a record.
   *
   * @param ctx run, rules, date and bucket actions
   * @param p record (processable)
   * @return what happened
   */
  public Processed process(RunContext ctx, SbmPolicy p) {
    Map<String, String> before = history.snapshot(p);
    String oldBucket = p.getBucket();
    Map<String, Object> facts = SbmFacts.of(p, ctx.today(), duplicates(p));
    Carry carry = new Carry();
    for (SbmStep step : List.of(SbmStep.SANITATION, SbmStep.MATCHING, SbmStep.CLASSIFICATION)) {
      if (step == SbmStep.MATCHING) {
        match(p, facts);
      }
      StepDecision d = StepDecision.evaluate(ctx.rules().of(step, p), facts);
      if (d.fallout()) {
        return fallout(ctx, p, step, d, before);
      }
      save(ctx, p, step, d, outcomeOf(d, false));
      carry.add(d.outcome(), d.reasonCode());
      if (step == SbmStep.CLASSIFICATION && d.outcome().classification() != null) {
        p.classify(SbmClassification.valueOf(d.outcome().classification()));
        facts.put("classification", d.outcome().classification());
      }
    }
    StepDecision disposition = StepDecision.evaluate(ctx.rules().of(SbmStep.DISPOSITION, p), facts);
    if (disposition.fallout()) {
      return fallout(ctx, p, SbmStep.DISPOSITION, disposition, before);
    }
    carry.add(disposition.outcome(), disposition.reasonCode());
    boolean overridden = applyTag(p, ctx, carry);
    save(ctx, p, SbmStep.DISPOSITION, disposition, outcomeOf(disposition, overridden));
    List<SbmLimitCheck> breaches = limits.check(ctx.run().getId(), p, facts);
    p.insurerApproval(!breaches.isEmpty());
    saveLimits(ctx, p, breaches);
    p.bucket(carry.bucket, carry.reason, carry.template);
    p.fallout(null, ctx.run().getRunNo());
    route(ctx, p, carry);
    history.record(p, before, SbmHistorySource.RUN, ctx.run().getRunNo());
    Outcome kind = overridden ? Outcome.OVERRIDDEN : bucketed(carry);
    return new Processed(
        p,
        kind,
        !Objects.equals(oldBucket, p.getBucket()) && p.getBucket() != null,
        !breaches.isEmpty());
  }

  private Duplicates duplicates(SbmPolicy p) {
    String pn = p.getLoan().pnNo();
    boolean samePn = pn != null && policies.countSamePn(p.getCompanyId(), pn, p.getId(), OPEN) > 0;
    String serial = p.getRisk().serialNo();
    String motor = p.getRisk().motorNo();
    boolean sameUnit =
        serial != null && policies.countSameSerial(p.getCompanyId(), serial, p.getId(), OPEN) > 0
            || motor != null
                && policies.countSameMotor(p.getCompanyId(), motor, p.getId(), OPEN) > 0;
    return new Duplicates(samePn, sameUnit);
  }

  private void match(SbmPolicy p, Map<String, Object> facts) {
    String pn = p.getLoan().pnNo();
    SbmLamdLoan loan =
        pn == null
            ? null
            : lamd.findFirstByCompanyIdAndPnNoOrderBySnapshotDateDescIdDesc(p.getCompanyId(), pn)
                .orElse(null);
    p.matched(loan == null ? null : loan.getLoanStatus(), loan != null && loan.isAmortised());
    SbmFacts.matched(facts, p, loan);
  }

  private boolean applyTag(SbmPolicy p, RunContext ctx, Carry carry) {
    String ruleTag = carry.tag;
    if (ruleTag == null && carry.bucket != null) {
      String action = ctx.actionOf(carry.bucket);
      if (RunContext.RENEW.equals(action)) {
        ruleTag = SubmittedCodes.RENEWABLE;
      } else if (RunContext.EXCLUDE.equals(action)) {
        ruleTag = SubmittedCodes.NON_RENEWABLE;
      }
    }
    if (ruleTag == null) {
      return false;
    }
    boolean applied = p.tagByRule(ruleTag, carry.reason);
    return !applied && !ruleTag.equals(p.getRenewalTag());
  }

  private void route(RunContext ctx, SbmPolicy p, Carry carry) {
    SbmPolicyStatus target;
    if (StepDecision.REVIEW.equals(carry.flag)) {
      target = SbmPolicyStatus.IN_REVIEW;
    } else if (SubmittedCodes.NON_RENEWABLE.equals(manualTag(p))) {
      target = SbmPolicyStatus.EXCLUDED;
    } else if (SubmittedCodes.RENEWABLE.equals(manualTag(p))) {
      target = SbmPolicyStatus.FOR_RENEWAL;
    } else if (carry.bucket == null) {
      target = SbmPolicyStatus.CLASSIFIED;
    } else {
      target =
          switch (ctx.actionOf(carry.bucket)) {
            case RunContext.RENEW -> SbmPolicyStatus.FOR_RENEWAL;
            case RunContext.EXCLUDE -> SbmPolicyStatus.EXCLUDED;
            default -> SbmPolicyStatus.FOR_MANUAL_DISPOSITION;
          };
    }
    flow.route(p, target, "Run " + ctx.run().getRunNo());
  }

  private static String manualTag(SbmPolicy p) {
    return SbmPolicy.TAG_MANUAL.equals(p.getRenewalTagSource()) ? p.getRenewalTag() : null;
  }

  private Processed fallout(
      RunContext ctx, SbmPolicy p, SbmStep step, StepDecision d, Map<String, String> before) {
    save(ctx, p, step, d, Outcome.FALLOUT);
    p.fallout(d.falloutReason(), ctx.run().getRunNo());
    history.record(p, before, SbmHistorySource.RUN, ctx.run().getRunNo());
    return new Processed(p, Outcome.FALLOUT, false, false);
  }

  private void save(RunContext ctx, SbmPolicy p, SbmStep step, StepDecision d, Outcome outcome) {
    var set = d.rule() == null ? null : ctx.rules().setOf(d.rule());
    String reason = outcome == Outcome.FALLOUT ? d.falloutReason() : d.reasonCode();
    results.save(
        new SbmRunResult(
            ctx.run().getId(),
            p.getId(),
            step,
            new Decision(
                outcome,
                d.outcome().bucket(),
                reason,
                d.rule() == null ? null : d.rule().getId(),
                d.rule() == null ? null : d.rule().getName(),
                set == null ? null : set.getCode(),
                set == null ? null : set.getVersionNo(),
                d.rule() == null ? "No rule applies to the policy" : null)));
  }

  private void saveLimits(RunContext ctx, SbmPolicy p, List<SbmLimitCheck> breaches) {
    String message =
        breaches.isEmpty()
            ? null
            : breaches.size() + " limit(s) exceeded: insurer approval required";
    results.save(
        new SbmRunResult(
            ctx.run().getId(),
            p.getId(),
            SbmStep.LIMITS,
            new Decision(
                Outcome.PASSED,
                null,
                breaches.isEmpty() ? null : "LIMIT_BREACH",
                null,
                null,
                null,
                null,
                message)));
  }

  private static Outcome outcomeOf(StepDecision d, boolean overridden) {
    if (overridden) {
      return Outcome.OVERRIDDEN;
    }
    return d.outcome().bucket() == null ? Outcome.PASSED : Outcome.BUCKETED;
  }

  private static Outcome bucketed(Carry carry) {
    return carry.bucket == null ? Outcome.PASSED : Outcome.BUCKETED;
  }

  /** The parts decided so far; the earliest step that decides a part wins. */
  private static final class Carry {
    private String bucket;
    private String tag;
    private String template;
    private String flag;
    private String reason;

    void add(SbmRuleOutcome o, String reasonCode) {
      if (bucket == null && o.bucket() != null) {
        bucket = o.bucket();
        reason = reasonCode;
      }
      tag = tag == null ? o.tag() : tag;
      template = template == null ? o.raTemplate() : template;
      flag = flag == null ? o.flag() : flag;
    }
  }

  /**
   * What happened to a record.
   *
   * @param policy record
   * @param outcome outcome of the run
   * @param bucketChanged whether its bucket changed
   * @param breached whether it is above an insurer limit
   */
  public record Processed(
      SbmPolicy policy, SbmRunResult.Outcome outcome, boolean bucketChanged, boolean breached) {}
}
