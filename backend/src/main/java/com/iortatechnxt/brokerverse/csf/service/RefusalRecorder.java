package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.csf.domain.ActivityAction;
import com.iortatechnxt.brokerverse.csf.domain.ChangeStatus;
import com.iortatechnxt.brokerverse.csf.domain.ChangedField;
import com.iortatechnxt.brokerverse.csf.domain.CsfActivity;
import com.iortatechnxt.brokerverse.csf.domain.CsfContactChange;
import com.iortatechnxt.brokerverse.csf.domain.CsfContactChangeRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records a refused contact change (FR-CSF-021: a field the contact centre cannot change) in its
 * own transaction, so the refusal stays on record while the request itself is refused.
 */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class RefusalRecorder {

  private final CsfContactChangeRepository changes;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final ActivityLog activity;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the recorder.
   *
   * @param changes contact changes
   * @param numbers document numbers
   * @param audit audit trail
   * @param activity activity log
   * @param currentUser current user
   * @param clock clock
   */
  public RefusalRecorder(
      CsfContactChangeRepository changes,
      DocumentNumberService numbers,
      AuditTrailService audit,
      ActivityLog activity,
      CurrentUser currentUser,
      Clock clock) {
    this.changes = changes;
    this.numbers = numbers;
    this.audit = audit;
    this.activity = activity;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records the refusal.
   *
   * @param client client
   * @param request verification, channel, reason and remarks
   * @param fields fields asked for that cannot be changed here, with the values asked
   * @return the refused change
   */
  public CsfContactChange record(
      Client client, CsfContactChange.Request request, List<ChangedField> fields) {
    String no = numbers.next(CsfCodes.NUMBER_PREFIX + BusinessClock.currentYear(clock).getValue());
    CsfContactChange saved =
        changes.save(
            new CsfContactChange(
                new CsfContactChange.Header(
                    no,
                    CsfClients.refOf(client),
                    client.getDisplayName(),
                    ChangeStatus.REFUSED,
                    clock.instant(),
                    currentUser.username()),
                request,
                fields));
    audit.record(
        CsfCodes.ENTITY_CHANGE,
        no,
        AuditAction.REJECT,
        "Change of "
            + String.join(", ", fields.stream().map(ChangedField::getField).toList())
            + " of "
            + client.getCode()
            + " refused: not a contact detail");
    activity.record(
        client.getCompanyId(),
        ActivityAction.CONTACT_CHANGE,
        new CsfActivity.Subject(client.getId(), client.getCode(), no, "Refused"));
    return saved;
  }
}
