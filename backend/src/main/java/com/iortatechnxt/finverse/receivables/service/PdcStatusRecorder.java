package com.iortatechnxt.finverse.receivables.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.receivables.domain.PdcEvent;
import com.iortatechnxt.finverse.receivables.domain.PdcEventRepository;
import com.iortatechnxt.finverse.receivables.domain.PdcStatus;
import com.iortatechnxt.finverse.receivables.domain.PostDatedCheque;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Moves post-dated cheques through their life cycle and writes the status history (the PDC
 * confirmation audit trail). Shared by the PDC register and the receipt services.
 */
@Component
public class PdcStatusRecorder {

  static final String ENTITY = "PostDatedCheque";

  private final PdcEventRepository events;
  private final AuditTrailService audit;

  /**
   * Creates the recorder.
   *
   * @param events event repository
   * @param audit audit trail
   */
  public PdcStatusRecorder(PdcEventRepository events, AuditTrailService audit) {
    this.events = events;
    this.audit = audit;
  }

  /**
   * Records the registration of a cheque.
   *
   * @param pdc cheque (saved)
   */
  public void registered(PostDatedCheque pdc) {
    events.save(
        new PdcEvent(pdc.getId(), null, pdc.getStatus(), pdc.getReceivedDate(), "Received", null));
    audit.record(
        ENTITY,
        pdc.getPdcNo(),
        AuditAction.CREATE,
        "PDC "
            + pdc.getChequeNo()
            + " of "
            + pdc.getAmount()
            + " received from "
            + pdc.getPartyCode());
  }

  /**
   * Moves a cheque to a new status and records the event.
   *
   * @param pdc cheque
   * @param to new status
   * @param date status date
   * @param remarks remarks
   * @param receiptNo related receipt number
   */
  public void transition(
      PostDatedCheque pdc, PdcStatus to, LocalDate date, String remarks, String receiptNo) {
    PdcStatus from = pdc.transition(to, date);
    events.save(new PdcEvent(pdc.getId(), from, to, date, remarks, receiptNo));
    audit.record(
        ENTITY,
        pdc.getPdcNo(),
        AuditAction.UPDATE,
        from + " -> " + to + " on " + date + note(remarks));
  }

  private static String note(String remarks) {
    return remarks == null || remarks.isBlank() ? "" : ": " + remarks;
  }
}
