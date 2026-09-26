package com.iortatechnxt.brokerverse.frbs.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.LineStatus;
import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.RunStage;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLineRepository;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRun;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRunRepository;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tags of the service-fee lines (FRBS 2.10.1-2.10.2): RELEASED with the date the recipient was
 * credited (also set automatically when Disbursement reports the payout paid), LIQUIDATED with the
 * liquidation date and the unit's liquidation report. When every line is released the run moves to
 * RELEASED, when every line is liquidated to LIQUIDATED.
 */
@Service
@Transactional
public class ServiceFeeTagService {

  private static final Set<LineStatus> RELEASED =
      EnumSet.of(LineStatus.RELEASED, LineStatus.LIQUIDATED);

  private final ServiceFeeRunRepository runs;
  private final ServiceFeeLineRepository lines;
  private final AttachmentService attachments;
  private final WorkflowService workflow;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param runs runs
   * @param lines lines
   * @param attachments liquidation reports
   * @param workflow workflow engine
   * @param currentUser current user
   * @param audit audit trail
   * @param clock clock
   */
  public ServiceFeeTagService(
      ServiceFeeRunRepository runs,
      ServiceFeeLineRepository lines,
      AttachmentService attachments,
      WorkflowService workflow,
      CurrentUser currentUser,
      AuditTrailService audit,
      Clock clock) {
    this.runs = runs;
    this.lines = lines;
    this.attachments = attachments;
    this.workflow = workflow;
    this.currentUser = currentUser;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Tags a line released (FRBS 2.10.2).
   *
   * @param lineId line
   * @param on date the recipient was credited
   * @return line
   */
  public ServiceFeeLine release(Long lineId, LocalDate on) {
    ServiceFeeLine line = line(lineId);
    ServiceFeeRun run = run(line);
    requireNotFuture(on);
    line.release(on, currentUser.username());
    audit.record(
        ServiceFees.ENTITY,
        run.getRunNo(),
        AuditAction.UPDATE,
        "Line " + line.getLineNo() + " tagged released on " + on);
    progress(run);
    return line;
  }

  /**
   * Tags a line liquidated with the unit's liquidation report (FRBS 2.10.1-2.10.2).
   *
   * @param lineId line
   * @param on liquidation date
   * @param remarks remarks
   * @param fileName report file name
   * @param content report file
   * @return line
   */
  public ServiceFeeLine liquidate(
      Long lineId, LocalDate on, String remarks, String fileName, byte[] content) {
    ServiceFeeLine line = line(lineId);
    ServiceFeeRun run = run(line);
    requireNotFuture(on);
    if (content == null || content.length == 0) {
      throw new BusinessRuleException(
          "SERVICE_FEE_LIQUIDATION_REPORT", "Attach the liquidation report of the unit");
    }
    Attachment report =
        attachments.upload(
            new AttachmentTarget(ServiceFees.LINE_ENTITY, String.valueOf(lineId)),
            fileName,
            content,
            "Liquidation report " + run.getRunNo() + " line " + line.getLineNo());
    line.liquidate(on, currentUser.username(), String.valueOf(report.getId()), remarks);
    audit.record(
        ServiceFees.ENTITY,
        run.getRunNo(),
        AuditAction.UPDATE,
        "Line " + line.getLineNo() + " tagged liquidated on " + on);
    progress(run);
    return line;
  }

  /**
   * Moves the run on when its lines are all released, then all liquidated.
   *
   * @param run run
   */
  void progress(ServiceFeeRun run) {
    List<ServiceFeeLine> paid =
        lines.findByRunIdOrderByLineNoAsc(run.getId()).stream()
            .filter(l -> l.getFee().signum() > 0)
            .toList();
    TransitionNote note = TransitionNote.comment("All lines tagged");
    if (run.getStage() == RunStage.APPROVED
        && paid.stream().allMatch(l -> RELEASED.contains(l.getStatus()))) {
      workflow.systemTransition(ServiceFees.ENTITY, String.valueOf(run.getId()), "release", note);
    }
    if (run.getStage() == RunStage.RELEASED
        && paid.stream().allMatch(l -> l.getStatus() == LineStatus.LIQUIDATED)) {
      workflow.systemTransition(ServiceFees.ENTITY, String.valueOf(run.getId()), "liquidate", note);
    }
  }

  private void requireNotFuture(LocalDate on) {
    if (on == null || on.isAfter(LocalDate.now(clock.withZone(ServiceFeeBase.MANILA)))) {
      throw new BusinessRuleException("SERVICE_FEE_DATES", "Give a date that is not in the future");
    }
  }

  private ServiceFeeLine line(Long lineId) {
    return lines
        .findById(lineId)
        .orElseThrow(() -> new ResourceNotFoundException("Service-fee line", lineId));
  }

  private ServiceFeeRun run(ServiceFeeLine line) {
    return runs.findById(line.getRunId())
        .orElseThrow(() -> new ResourceNotFoundException("Service-fee run", line.getRunId()));
  }
}
