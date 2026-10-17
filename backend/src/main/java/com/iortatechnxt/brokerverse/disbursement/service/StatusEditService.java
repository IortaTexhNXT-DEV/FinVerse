package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.EventSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.StatusEditStage;
import com.iortatechnxt.brokerverse.disbursement.domain.Instrument;
import com.iortatechnxt.brokerverse.disbursement.domain.InstrumentLifecycle;
import com.iortatechnxt.brokerverse.disbursement.domain.StatusEdit;
import com.iortatechnxt.brokerverse.disbursement.domain.StatusEditRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentService.Change;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corrections of an instrument status tag (DIS 2.8.5, AQ15; workflow {@code DISB_STATUS_EDIT}): a
 * processor asks for another status of the mode with a reason; a team leader other than the
 * requestor approves it (the status is set and recorded in the history) or rejects it from the
 * workflow panel. Only one edit per instrument waits at a time.
 */
@Service
@Transactional
public class StatusEditService {

  private final StatusEditRepository edits;
  private final InstrumentService instruments;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param edits status edits
   * @param instruments instruments
   * @param workflow status edit workflow
   * @param audit audit trail
   * @param currentUser current user
   */
  public StatusEditService(
      StatusEditRepository edits,
      InstrumentService instruments,
      WorkflowService workflow,
      AuditTrailService audit,
      CurrentUser currentUser) {
    this.edits = edits;
    this.instruments = instruments;
    this.workflow = workflow;
    this.audit = audit;
    this.currentUser = currentUser;
  }

  /**
   * Requests a status correction (DIS 2.8.5).
   *
   * @param voucherId voucher of the instrument
   * @param to requested status (a status of the mode)
   * @param reason reason
   * @return the edit
   */
  public StatusEdit request(Long voucherId, InstrumentStatus to, String reason) {
    Instrument i = instruments.forVoucher(voucherId);
    if (to == i.getStatus() || !InstrumentLifecycle.statusesOf(i.getMode()).contains(to)) {
      throw new BusinessRuleException(
          "STATUS_EDIT_INVALID", to + " is not another status of a " + i.getMode());
    }
    if (edits.existsByInstrumentIdAndStage(i.getId(), StatusEditStage.REQUESTED)) {
      throw new BusinessRuleException(
          "STATUS_EDIT_PENDING", "A status edit of this instrument already waits for approval");
    }
    Voucher v = instruments.voucherOf(i);
    StatusEdit edit =
        edits.save(new StatusEdit(v.getCompanyId(), i.getId(), i.getStatus(), to, reason.strip()));
    workflow.start(
        new StartCase(
            v.getCompanyId(),
            DisbursementSettings.WF_STATUS_EDIT,
            new CaseRecord(
                DisbursementSettings.STATUS_EDIT,
                edit.getId().toString(),
                v.getDvNo(),
                i.getMode() + " " + i.label() + ": " + i.getStatus() + " -> " + to,
                DisbursementSettings.voucherLink(v.getId()),
                null),
            null));
    audit.record(
        DisbursementSettings.INSTRUMENT,
        i.getId(),
        AuditAction.SUBMIT,
        "Status edit " + i.getStatus() + " -> " + to + ": " + reason);
    return edit;
  }

  /**
   * Approves a status edit: the status is applied (DIS 2.8.5).
   *
   * @param id edit
   * @return the edit
   */
  public StatusEdit approve(Long id) {
    StatusEdit edit = get(id);
    if (CurrentUser.sameUser(edit.getCreatedBy(), currentUser.username())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A status edit cannot be approved by its requestor");
    }
    workflow.transition(
        DisbursementSettings.STATUS_EDIT, id.toString(), "approve", TransitionNote.NONE);
    Instrument i = instruments.get(edit.getInstrumentId());
    if (i.getStatus() != edit.getFromStatus()) {
      throw new BusinessRuleException(
          "STATUS_EDIT_STALE",
          "The instrument is now " + i.getStatus() + "; request the edit again");
    }
    instruments.correct(
        i,
        edit.getToStatus(),
        new Change(EventSource.USER, "Status edit approved: " + edit.getReason(), null));
    return edit;
  }

  /**
   * An edit.
   *
   * @param id id
   * @return edit
   */
  @Transactional(readOnly = true)
  public StatusEdit get(Long id) {
    return edits
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(DisbursementSettings.STATUS_EDIT, id));
  }

  /**
   * Edits waiting for approval, oldest first.
   *
   * @return edits
   */
  @Transactional(readOnly = true)
  public List<StatusEdit> pending() {
    return edits.findByStageOrderByIdAsc(StatusEditStage.REQUESTED);
  }

  /**
   * The edits of an instrument, newest first.
   *
   * @param instrumentId instrument
   * @return edits
   */
  @Transactional(readOnly = true)
  public List<StatusEdit> of(Long instrumentId) {
    return edits.findByInstrumentIdOrderByIdDesc(instrumentId);
  }
}
