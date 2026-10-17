package com.iortatechnxt.brokerverse.renewal.marketing.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.renewal.check.service.BlockingChecks;
import com.iortatechnxt.brokerverse.renewal.check.service.CheckNames;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.ClosedAs;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPath;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.renewal.service.RenewalBatch;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Team Leader review (FR-RN-050): the TL returns dispositioned renewals to the AO with a reason and
 * remarks (red Returned flag), or posts them; each posted renewal moves on by its disposition - For
 * Renewal to Processing, For Quotation and For Proposal to the New Business path, Not for Renewal
 * to the letter step, Lost Business to closure. A renewal with a failing blocking check is not
 * posted until the check is overridden.
 */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ReviewService {

  private final RenewalRecords records;
  private final BlockingChecks blocking;
  private final RemarkService remarks;
  private final RenewalFlow flow;
  private final RenewalNotices notices;
  private final RenewalBatch batch;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param blocking blocking checks
   * @param remarks remarks
   * @param flow workflow
   * @param notices notifications
   * @param batch batch runner
   * @param lovs lists of values
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ReviewService(
      RenewalRecords records,
      BlockingChecks blocking,
      RemarkService remarks,
      RenewalFlow flow,
      RenewalNotices notices,
      RenewalBatch batch,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.blocking = blocking;
    this.remarks = remarks;
    this.flow = flow;
    this.notices = notices;
    this.batch = batch;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Returns renewals to the AO.
   *
   * @param companyId company
   * @param refs renewals
   * @param reasonCode reason (list RNW_RETURN_REASON)
   * @param remarksText remarks
   * @return returned and refused renewals
   */
  public BatchOutcome returnToAo(
      Long companyId, List<String> refs, String reasonCode, String remarksText) {
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BusinessRuleException("WORKFLOW_REASON_REQUIRED", "Select a reason for 'Return'");
    }
    lovs.requireValid(RenewalCodes.LOV_RETURN_REASON, reasonCode, BusinessClock.today(clock));
    String text = RemarkService.requireText(remarksText, "Enter the remarks");
    String label = lovs.label(RenewalCodes.LOV_RETURN_REASON, reasonCode);
    return batch.run(
        refs,
        ref -> {
          RenewalCandidate c = records.get(companyId, ref);
          RenewalRecords.requireStage(c, RenewalStage.FOR_TL_REVIEW);
          flow.act(c, "return", new TransitionNote(reasonCode, text));
          c.getFlags().setReturned(true);
          flow.assign(c, c.getAssignedAo());
          remarks.add(c, "Returned (" + label + "): " + text);
          audit.record(RenewalCodes.ENTITY, ref, AuditAction.REJECT, label + ": " + text);
          notices.users(
              Collections.singletonList(c.getAssignedAo()),
              RenewalCodes.EVENT_RETURNED,
              c,
              new RenewalNotices.Text(ref + " returned to you", label + ": " + text));
        });
  }

  /**
   * Posts renewals: each moves on by its disposition.
   *
   * @param companyId company
   * @param refs renewals
   * @return posted and refused renewals
   */
  public BatchOutcome post(Long companyId, List<String> refs) {
    return batch.run(refs, ref -> postOne(records.get(companyId, ref)));
  }

  private void requireNoBlockingCheck(RenewalCandidate c) {
    List<CheckResult> failing = blocking.failing(c);
    if (!failing.isEmpty()) {
      throw new BusinessRuleException(
          "RNW_BLOCKING_CHECK",
          "Renewal "
              + c.getRenewalRef()
              + " cannot be posted: "
              + failing.stream()
                  .map(r -> CheckNames.of(r.getCheckCode()))
                  .collect(Collectors.joining(", "))
              + " failed");
    }
  }

  private void postOne(RenewalCandidate c) {
    RenewalRecords.requireStage(c, RenewalStage.FOR_TL_REVIEW);
    RenewalDisposition code = c.getDisposition().code();
    if (code == null) {
      throw new BusinessRuleException(
          "RNW_DISPOSITION_REQUIRED", "Renewal " + c.getRenewalRef() + " has no disposition");
    }
    if (code == RenewalDisposition.FOR_RENEWAL || code.isNewBusinessPath()) {
      requireNoBlockingCheck(c);
    }
    String note = "Posted: " + code.label();
    switch (code) {
      case FOR_RENEWAL -> {
        flow.act(c, "post_processing", TransitionNote.comment(note));
        notices.holders(
            Permission.RNW_PROCESS_ASSIGN,
            RenewalCodes.EVENT_POSTED,
            new RenewalNotices.Text(
                c.getRenewalRef() + " posted for processing", c.getSnapshot().clientName()),
            RenewalCodes.LINK + c.getRenewalRef());
      }
      case FOR_QUOTATION, FOR_PROPOSAL -> {
        c.takePath(RenewalPath.NB_PATH);
        flow.act(c, "post_nb_path", TransitionNote.comment(note));
      }
      case NOT_FOR_RENEWAL -> flow.act(c, "post_letter", TransitionNote.comment(note));
      default -> {
        flow.act(c, "post_close", TransitionNote.comment(note));
        c.close(ClosedAs.LOST, null, clock.instant());
      }
    }
    flow.assign(c, null);
    audit.record(RenewalCodes.ENTITY, c.getRenewalRef(), AuditAction.POST, note);
  }
}
