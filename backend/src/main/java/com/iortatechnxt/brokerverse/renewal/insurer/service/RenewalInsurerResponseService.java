package com.iortatechnxt.brokerverse.renewal.insurer.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.CurrentDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.DispositionSource;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatch;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatchLine;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatchLineRepository;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponseCode;
import com.iortatechnxt.brokerverse.renewal.domain.MatchOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalInsurerResponseRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.rules.service.ReevaluationService;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalDispositions;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insurer responses (FR-RN-071; BRRN.035): append-only; the latest valid response drives the
 * renewal. A matched Renew As Is moves it to RA Ready without re-keying; Revise returns it to
 * processing and the financial-impact check runs; Reject tags it Not for Renewal (insurer declined)
 * or returns it to Marketing to re-market. A mismatched or late response is kept but never
 * progresses the renewal: the checks put it in the Exception bucket until an override.
 */
@Service
@Transactional
public class RenewalInsurerResponseService {

  private final RenewalRecords records;
  private final RenewalInsurerResponseRepository responses;
  private final InsurerBatchLineRepository lines;
  private final InsurerBatchService batches;
  private final RenewalDispositions dispositions;
  private final ReevaluationService reevaluation;
  private final RenewalFlow flow;
  private final RenewalNotices notices;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param responses responses
   * @param lines batch lines
   * @param batches batches
   * @param dispositions disposition history
   * @param reevaluation checks
   * @param flow workflow
   * @param notices notifications
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public RenewalInsurerResponseService(
      RenewalRecords records,
      RenewalInsurerResponseRepository responses,
      InsurerBatchLineRepository lines,
      InsurerBatchService batches,
      RenewalDispositions dispositions,
      ReevaluationService reevaluation,
      RenewalFlow flow,
      RenewalNotices notices,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.responses = responses;
    this.lines = lines;
    this.batches = batches;
    this.dispositions = dispositions;
    this.reevaluation = reevaluation;
    this.flow = flow;
    this.notices = notices;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records a response keyed on the Insurer tab.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param content response
   * @param remarket for a Reject, return to Marketing instead of Not for Renewal
   * @return the response
   */
  public InsurerResponse manual(
      Long companyId, String renewalRef, InsurerResponse.Content content, boolean remarket) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    return record(
        c, content, new Source("MANUAL", null, null, c.getSnapshot().policyNo()), remarket);
  }

  /**
   * Problems of a response before it is recorded (form and upload validation).
   *
   * @param c renewal
   * @param content response
   * @return messages, empty when it can be recorded
   */
  @Transactional(readOnly = true)
  public List<String> problems(RenewalCandidate c, InsurerResponse.Content content) {
    if (c.getStage() != RenewalStage.WITH_INSURER) {
      return List.of(
          "Renewal "
              + c.getRenewalRef()
              + " is not with the insurer ("
              + c.getStage().label()
              + ")");
    }
    if (content.response() == null) {
      return List.of("Select the response");
    }
    boolean revised =
        content.revisedPremium() != null
            || content.revisedSumInsured() != null
            || content.revisedRate() != null;
    if (content.response() == InsurerResponseCode.REVISE && !revised) {
      return List.of("Enter the revised premium, sum insured or rate");
    }
    if (negative(content.revisedPremium()) || negative(content.revisedSumInsured())) {
      return List.of("Revised amounts cannot be negative");
    }
    if (content.receivedOn() != null && content.receivedOn().isAfter(BusinessClock.today(clock))) {
      return List.of("The received date cannot be in the future");
    }
    return List.of();
  }

  /**
   * Records a response and applies it when it matches.
   *
   * @param c renewal
   * @param content response
   * @param source source, upload job and row, and the policy number given by the insurer
   * @param remarket for a Reject, return to Marketing instead of Not for Renewal
   * @return the response
   */
  public InsurerResponse record(
      RenewalCandidate c, InsurerResponse.Content content, Source source, boolean remarket) {
    List<String> problems = problems(c, content);
    if (!problems.isEmpty()) {
      throw new BusinessRuleException("RNW_RESPONSE_INVALID", problems.get(0));
    }
    InsurerResponse.Content dated =
        content.receivedOn() != null
            ? content
            : new InsurerResponse.Content(
                content.response(),
                content.insurerRef(),
                content.revisedPremium(),
                content.revisedSumInsured(),
                content.revisedRate(),
                content.terms(),
                BusinessClock.today(clock),
                content.remarks());
    Optional<InsurerBatch> batch = batches.openBatchOf(c);
    MatchOutcome match = match(c, source.policyNo());
    boolean late =
        batch
            .map(InsurerBatch::getReplyDue)
            .map(due -> dated.receivedOn().isAfter(due))
            .orElse(false);
    InsurerResponse saved =
        responses.save(
            new InsurerResponse(
                c.getId(),
                batch.map(InsurerBatch::getId).orElse(null),
                dated,
                new InsurerResponse.Origin(
                    source.kind(), match, late, source.jobNo(), source.rowNo())));
    boolean valid = match == MatchOutcome.MATCHED && !late;
    if (valid) {
      responses.findByCandidateIdOrderByIdDesc(c.getId()).forEach(r -> r.markLatestValid(false));
      saved.markLatestValid(true);
      batch.ifPresent(b -> answer(b, c));
      apply(c, saved, remarket);
    }
    reevaluation.reevaluate(c, CheckTrigger.UPLOAD);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.UPDATE,
        "Insurer response "
            + label(saved.getResponse())
            + " ("
            + source.kind().toLowerCase(Locale.ROOT)
            + ", "
            + match.name().toLowerCase(Locale.ROOT).replace('_', ' ')
            + (late ? ", late" : "")
            + ")");
    notices.users(
        Collections.singletonList(c.getAssignedPo()),
        RenewalCodes.EVENT_INSURER_RESPONDED,
        c,
        new RenewalNotices.Text(
            c.getRenewalRef() + ": insurer response " + label(saved.getResponse()),
            valid ? "Applied to the renewal" : "Not applied: check the Insurer tab"));
    return saved;
  }

  /**
   * The responses of a renewal, newest first.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @return responses
   */
  @Transactional(readOnly = true)
  public List<InsurerResponse> of(Long companyId, String renewalRef) {
    return responses.findByCandidateIdOrderByIdDesc(records.get(companyId, renewalRef).getId());
  }

  private void apply(RenewalCandidate c, InsurerResponse r, boolean remarket) {
    String note = "Insurer response " + label(r.getResponse());
    switch (r.getResponse()) {
      case RENEW_AS_IS -> flow.system(c, "insurer_renew", note);
      case REVISE -> flow.system(c, "insurer_revise", note);
      default -> {
        if (remarket) {
          flow.system(c, "insurer_reject_remarket", note);
        } else {
          dispositions.record(
              c,
              new CurrentDisposition(
                  RenewalDisposition.NOT_FOR_RENEWAL,
                  RenewalCodes.REASON_INSURER_DECLINED,
                  DispositionSource.INSURER,
                  r.getRemarks(),
                  null),
              null,
              null);
          flow.system(c, "insurer_reject_close", note);
        }
      }
    }
  }

  private void answer(InsurerBatch batch, RenewalCandidate c) {
    List<InsurerBatchLine> all = lines.findByBatchIdOrderByIdAsc(batch.getId());
    all.stream()
        .filter(l -> l.getCandidateId().equals(c.getId()))
        .forEach(InsurerBatchLine::respond);
    batch.responded(all.stream().allMatch(InsurerBatchLine::isResponded));
  }

  private static MatchOutcome match(RenewalCandidate c, String policyNo) {
    String expected = c.getSnapshot().policyNo();
    if (policyNo == null || expected == null) {
      return MatchOutcome.MATCHED;
    }
    return Objects.equals(policyNo.strip(), expected.strip())
        ? MatchOutcome.MATCHED
        : MatchOutcome.POLICY_MISMATCH;
  }

  private static boolean negative(BigDecimal value) {
    return value != null && value.signum() < 0;
  }

  private static String label(InsurerResponseCode code) {
    return code.name().replace('_', ' ').toLowerCase(Locale.ROOT);
  }

  /**
   * Where a response comes from.
   *
   * @param kind UPLOAD or MANUAL
   * @param jobNo upload job, null for a manual response
   * @param rowNo row of the file
   * @param policyNo policy number given by the insurer, null when not given
   */
  public record Source(String kind, String jobNo, Integer rowNo, String policyNo) {}
}
