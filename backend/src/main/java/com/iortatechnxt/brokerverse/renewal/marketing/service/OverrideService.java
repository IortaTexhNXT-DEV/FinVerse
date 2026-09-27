package com.iortatechnxt.brokerverse.renewal.marketing.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.renewal.check.service.CheckNames;
import com.iortatechnxt.brokerverse.renewal.check.service.OutstandingPremiumCheck;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResultRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.CurrentDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.DispositionSource;
import com.iortatechnxt.brokerverse.renewal.domain.OverrideKind;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalOverride;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalOverrideRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.rules.service.ReevaluationService;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.renewal.service.RenewalBatch;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalDispositions;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Controlled overrides (FR-RN-051): outstanding balance, a failing check, the bucket, the
 * disposition, an insurer-response mismatch and the Renewal Advice unlock. Every override needs a
 * reason and remarks, is logged, and runs the checks again; an override never turns a failed check
 * into the Clean bucket.
 */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class OverrideService {

  private final RenewalRecords records;
  private final RenewalOverrideRepository overrides;
  private final CheckResultRepository results;
  private final RenewalDispositions dispositions;
  private final ReevaluationService reevaluation;
  private final RemarkService remarks;
  private final RenewalFlow flow;
  private final RenewalBatch batch;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param overrides overrides
   * @param results check results
   * @param dispositions disposition history
   * @param reevaluation checks
   * @param remarks remarks
   * @param flow workflow
   * @param batch batch runner
   * @param lovs lists of values
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public OverrideService(
      RenewalRecords records,
      RenewalOverrideRepository overrides,
      CheckResultRepository results,
      RenewalDispositions dispositions,
      ReevaluationService reevaluation,
      RemarkService remarks,
      RenewalFlow flow,
      RenewalBatch batch,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.overrides = overrides;
    this.results = results;
    this.dispositions = dispositions;
    this.reevaluation = reevaluation;
    this.remarks = remarks;
    this.flow = flow;
    this.batch = batch;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Overrides renewals.
   *
   * @param companyId company
   * @param refs renewals
   * @param request kind, target, reason and remarks
   * @return overridden and refused renewals
   */
  public BatchOutcome override(Long companyId, List<String> refs, Request request) {
    if (request.kind() == null) {
      throw new BusinessRuleException("RNW_OVERRIDE_KIND", "Select the kind of override");
    }
    if (request.reasonCode() == null || request.reasonCode().isBlank()) {
      throw new BusinessRuleException("RNW_OVERRIDE_REASON", "Select the reason of the override");
    }
    lovs.requireValid(
        RenewalCodes.LOV_OVERRIDE_REASON, request.reasonCode(), BusinessClock.today(clock));
    String text = RemarkService.requireText(request.remarks(), "Enter the remarks of the override");
    RenewalOverride.Reason reason = new RenewalOverride.Reason(request.reasonCode(), text);
    return batch.run(refs, ref -> apply(records.get(companyId, ref), request, reason));
  }

  private void apply(RenewalCandidate c, Request request, RenewalOverride.Reason reason) {
    RenewalRecords.requireStage(c, openStages());
    switch (request.kind()) {
      case OUTSTANDING_BALANCE ->
          checkOverride(c, OverrideKind.OUTSTANDING_BALANCE, OutstandingPremiumCheck.CODE, reason);
      case CHECK -> checkOverride(c, OverrideKind.CHECK, request.target(), reason);
      case BUCKET -> bucket(c, request.target(), reason);
      case DISPOSITION -> disposition(c, request.target(), reason);
      case INSURER_MISMATCH -> {
        RenewalRecords.requireStage(c, RenewalStage.WITH_INSURER);
        record(
            c,
            OverrideKind.INSURER_MISMATCH,
            null,
            new RenewalOverride.Change("MISMATCH", "ACCEPTED"),
            reason);
        flow.act(c, "override_insurer", TransitionNote.comment(reason.remarks()));
      }
      default -> unlock(c, reason);
    }
    remarks.add(
        c,
        "Override "
            + request.kind().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ')
            + ": "
            + reason.remarks());
  }

  private void checkOverride(
      RenewalCandidate c, OverrideKind kind, String checkCode, RenewalOverride.Reason reason) {
    if (checkCode == null || !failing(c, checkCode)) {
      throw new BusinessRuleException(
          "RNW_OVERRIDE_NOT_FAILING",
          "Renewal " + c.getRenewalRef() + ": " + CheckNames.of(checkCode) + " is not failing");
    }
    record(c, kind, checkCode, new RenewalOverride.Change("FAIL", "OVERRIDDEN"), reason);
    reevaluation.reevaluate(c, CheckTrigger.OVERRIDE);
  }

  private void bucket(RenewalCandidate c, String target, RenewalOverride.Reason reason) {
    Bucket to = parse(Bucket.class, target, "Select the bucket");
    if (to == c.getBucket()) {
      throw new BusinessRuleException(
          "RNW_OVERRIDE_SAME", "Renewal " + c.getRenewalRef() + " is already " + to.label());
    }
    boolean anyFail =
        c.getLastCheckRunId() != null
            && results.findByRunIdOrderByIdAsc(c.getLastCheckRunId()).stream()
                .anyMatch(r -> r.getOutcome() == CheckOutcome.FAIL);
    if (to == Bucket.CLEAN && anyFail) {
      throw new BusinessRuleException(
          "RNW_OVERRIDE_CLEAN", "An override never turns a failed check into Clean");
    }
    overrides.findByCandidateIdAndActiveTrue(c.getId()).stream()
        .filter(o -> o.getKind() == OverrideKind.BUCKET)
        .forEach(RenewalOverride::end);
    String from = c.getBucket() == null ? null : c.getBucket().name();
    record(c, OverrideKind.BUCKET, null, new RenewalOverride.Change(from, to.name()), reason);
    reevaluation.reevaluate(c, CheckTrigger.OVERRIDE);
  }

  private void disposition(RenewalCandidate c, String target, RenewalOverride.Reason reason) {
    RenewalRecords.requireUnlocked(c);
    RenewalDisposition to = parse(RenewalDisposition.class, target, "Select the disposition");
    if (to == RenewalDisposition.NOT_FOR_RENEWAL) {
      throw new BusinessRuleException(
          "RNW_OVERRIDE_NFR", "Give Not for Renewal through the disposition with its reason");
    }
    RenewalDisposition from = c.getDisposition().code();
    record(
        c,
        OverrideKind.DISPOSITION,
        null,
        new RenewalOverride.Change(from == null ? null : from.name(), to.name()),
        reason);
    dispositions.record(
        c,
        new CurrentDisposition(to, null, DispositionSource.USER, reason.remarks(), null),
        null,
        null);
    if (c.getStage() != RenewalStage.FOR_TL_REVIEW) {
      flow.act(c, "override_disposition", TransitionNote.comment(reason.remarks()));
    }
  }

  private void unlock(RenewalCandidate c, RenewalOverride.Reason reason) {
    RenewalRecords.requireStage(c, RenewalStage.RA_GENERATED, RenewalStage.RA_SENT);
    record(
        c, OverrideKind.RA_UNLOCK, null, new RenewalOverride.Change("LOCKED", "UNLOCKED"), reason);
    flow.act(c, "cancel_ra", TransitionNote.comment(reason.remarks()));
    c.lockMarketing(null);
  }

  private void record(
      RenewalCandidate c,
      OverrideKind kind,
      String checkCode,
      RenewalOverride.Change change,
      RenewalOverride.Reason reason) {
    overrides.save(new RenewalOverride(c.getId(), kind, checkCode, change, reason));
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.UPDATE,
        "Override "
            + kind.name()
            + (checkCode == null ? "" : " " + checkCode)
            + " from "
            + change.from()
            + " to "
            + change.to()
            + ": "
            + reason.remarks());
  }

  private boolean failing(RenewalCandidate c, String checkCode) {
    return c.getLastCheckRunId() != null
        && results.findByRunIdOrderByIdAsc(c.getLastCheckRunId()).stream()
            .anyMatch(
                r -> r.getCheckCode().equals(checkCode) && r.getOutcome() == CheckOutcome.FAIL);
  }

  private static RenewalStage[] openStages() {
    return java.util.Arrays.stream(RenewalStage.values())
        .filter(RenewalStage::isOpen)
        .toArray(RenewalStage[]::new);
  }

  private static <E extends Enum<E>> E parse(Class<E> type, String value, String missing) {
    if (value == null || value.isBlank()) {
      throw new BusinessRuleException("RNW_OVERRIDE_TARGET", missing);
    }
    try {
      return Enum.valueOf(type, value.strip());
    } catch (IllegalArgumentException e) {
      throw new BusinessRuleException("RNW_OVERRIDE_TARGET", missing, e);
    }
  }

  /**
   * An override.
   *
   * @param kind kind
   * @param target check code (CHECK), bucket (BUCKET) or disposition (DISPOSITION)
   * @param reasonCode reason (list RNW_OVERRIDE_REASON)
   * @param remarks remarks
   */
  public record Request(OverrideKind kind, String target, String reasonCode, String remarks) {}
}
