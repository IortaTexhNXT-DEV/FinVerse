package com.iortatechnxt.brokerverse.renewal.marketing.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalTransfer;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalTransferRepository;
import com.iortatechnxt.brokerverse.renewal.domain.TransferStatus;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.renewal.service.RenewalScope;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transfers of renewals between Marketing units (FR-RN-031, 032): the TL or the AO requests the
 * transfer with remarks; the account leaves the sender's lists; a TL of the receiving unit accepts
 * it into the unit's Unassigned Disposition tab, flagged Transferred, or declines it with remarks,
 * which returns it to the sender at its previous stage. The requester may cancel an open request.
 */
@Service
@Transactional
public class TransferService {

  private final RenewalRecords records;
  private final RenewalTransferRepository transfers;
  private final RemarkService remarks;
  private final RenewalFlow flow;
  private final RenewalScope scope;
  private final RenewalNotices notices;
  private final SalesOrganisationService sales;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param transfers transfer requests
   * @param remarks remarks
   * @param flow workflow
   * @param scope data scope
   * @param notices notifications
   * @param sales sales units
   * @param lovs lists of values
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public TransferService(
      RenewalRecords records,
      RenewalTransferRepository transfers,
      RemarkService remarks,
      RenewalFlow flow,
      RenewalScope scope,
      RenewalNotices notices,
      SalesOrganisationService sales,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.records = records;
    this.transfers = transfers;
    this.remarks = remarks;
    this.flow = flow;
    this.scope = scope;
    this.notices = notices;
    this.sales = sales;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Requests the transfer of a renewal to another Marketing unit.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param request receiving unit, reason and remarks
   * @return the request
   */
  public RenewalTransfer request(Long companyId, String renewalRef, Request request) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    return request(c, request);
  }

  /**
   * Requests the transfer of a renewal already loaded (also from the disposition reason "Transfer
   * to Another Marketing Unit").
   *
   * @param c renewal
   * @param request receiving unit, reason and remarks
   * @return the request
   */
  public RenewalTransfer request(RenewalCandidate c, Request request) {
    RenewalRecords.requireStage(c, RenewalStage.UNASSIGNED, RenewalStage.FOR_DISPOSITION);
    RenewalRecords.requireUnlocked(c);
    String toUnit = request.toUnit() == null ? "" : request.toUnit().strip();
    if (toUnit.isEmpty()) {
      throw new BusinessRuleException("RNW_TRANSFER_UNIT", "Select the receiving unit");
    }
    if (toUnit.equals(c.getOwnerUnit())) {
      throw new BusinessRuleException(
          "RNW_TRANSFER_SAME_UNIT", "The receiving unit must be another unit");
    }
    requireUnit(c.getCompanyId(), toUnit);
    String text = RemarkService.requireText(request.remarks(), "Enter the remarks");
    if (request.reasonCode() != null && !request.reasonCode().isBlank()) {
      lovs.requireValid(
          RenewalCodes.LOV_TRANSFER_REASON, request.reasonCode(), BusinessClock.today(clock));
    }
    if (transfers
        .findFirstByCandidateIdAndStatus(c.getId(), TransferStatus.REQUESTED)
        .isPresent()) {
      throw new BusinessRuleException(
          "RNW_TRANSFER_OPEN", "Renewal " + c.getRenewalRef() + " already has a transfer request");
    }
    RenewalTransfer transfer =
        transfers.save(new RenewalTransfer(c, toUnit, blankToNull(request.reasonCode()), text));
    flow.act(c, "transfer_request", TransitionNote.comment("Transfer to " + toUnit + ": " + text));
    remarks.add(c, "Transfer to " + toUnit + " requested: " + text);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.SUBMIT,
        "Transfer from " + c.getOwnerUnit() + " to " + toUnit + " requested");
    notices.teamLeaders(
        c.getCompanyId(),
        toUnit,
        RenewalCodes.EVENT_TRANSFER_REQUESTED,
        c,
        new RenewalNotices.Text(
            c.getRenewalRef() + " transferred to your unit",
            "Accept or decline the renewal of " + c.getSnapshot().clientName() + ": " + text));
    return transfer;
  }

  /**
   * Decides a request (a TL of the receiving unit).
   *
   * @param id request
   * @param accept accept or decline
   * @param decisionRemarks remarks (required to decline)
   * @return the request
   */
  public RenewalTransfer decide(Long id, boolean accept, String decisionRemarks) {
    RenewalTransfer t = open(id);
    RenewalCandidate c = records.byId(t.getCandidateId());
    requireReceivingLeader(c.getCompanyId(), t.getToUnit());
    if (accept) {
      t.decide(
          TransferStatus.ACCEPTED, currentUser.username(), blankToNull(decisionRemarks), now());
      c.moveToUnit(t.getToUnit());
      c.assignAo(null);
      c.getFlags().setTransferred(true);
      flow.act(c, "transfer_accept", TransitionNote.comment("Accepted by " + t.getToUnit()));
      flow.assign(c, null);
    } else {
      String text = RemarkService.requireText(decisionRemarks, "Enter the remarks of the decline");
      t.decide(TransferStatus.DECLINED, currentUser.username(), text, now());
      sendBack(c, t, "Declined by " + t.getToUnit() + ": " + text);
    }
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        accept ? AuditAction.AUTHORIZE : AuditAction.REJECT,
        "Transfer to " + t.getToUnit() + (accept ? " accepted" : " declined"));
    notices.users(
        List.of(t.getCreatedBy()),
        RenewalCodes.EVENT_TRANSFER_DECIDED,
        c,
        new RenewalNotices.Text(
            c.getRenewalRef() + ": transfer " + (accept ? "accepted" : "declined"),
            "The transfer to "
                + t.getToUnit()
                + (accept ? " was accepted" : " was declined: " + decisionRemarks)));
    return t;
  }

  /**
   * Cancels an open request (its requester).
   *
   * @param id request
   * @return the request
   */
  public RenewalTransfer cancel(Long id) {
    RenewalTransfer t = open(id);
    if (!CurrentUser.sameUser(t.getCreatedBy(), currentUser.username())) {
      throw new BusinessRuleException(
          "RNW_TRANSFER_NOT_REQUESTER", "Only the requester can cancel the transfer");
    }
    RenewalCandidate c = records.byId(t.getCandidateId());
    t.decide(TransferStatus.CANCELLED, currentUser.username(), null, now());
    sendBack(c, t, "Transfer to " + t.getToUnit() + " cancelled");
    return t;
  }

  /**
   * Requests received by the user's units, newest first.
   *
   * @param companyId company
   * @return requests
   */
  @Transactional(readOnly = true)
  public List<RenewalTransfer> incoming(Long companyId) {
    return transfers.findByToUnitInOrderByIdDesc(units(companyId));
  }

  /**
   * Requests sent from the user's units or by the user, newest first.
   *
   * @param companyId company
   * @return requests
   */
  @Transactional(readOnly = true)
  public List<RenewalTransfer> outgoing(Long companyId) {
    Set<String> units = units(companyId);
    List<RenewalTransfer> mine = transfers.findByCreatedByOrderByIdDesc(currentUser.username());
    if (units.isEmpty()) {
      return mine;
    }
    List<RenewalTransfer> fromUnits = transfers.findByFromUnitInOrderByIdDesc(units);
    return java.util.stream.Stream.concat(fromUnits.stream(), mine.stream())
        .distinct()
        .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
        .toList();
  }

  private Set<String> units(Long companyId) {
    RenewalScope.Scope s = scope.current(companyId);
    if (s.kind() == RenewalScope.Kind.ALL) {
      return Set.copyOf(sales.units(companyId).stream().map(SalesUnit::getCode).toList());
    }
    return scope.unitsOf(companyId, currentUser.username());
  }

  private void sendBack(RenewalCandidate c, RenewalTransfer t, String note) {
    String action =
        t.getFromStage() == RenewalStage.FOR_DISPOSITION
            ? "transfer_back_disposition"
            : "transfer_back_unassigned";
    flow.act(c, action, TransitionNote.comment(note));
    remarks.add(c, note);
  }

  private RenewalTransfer open(Long id) {
    RenewalTransfer t =
        transfers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Transfer", id));
    if (t.getStatus() != TransferStatus.REQUESTED) {
      throw new BusinessRuleException("RNW_TRANSFER_DECIDED", "The transfer is already decided");
    }
    return t;
  }

  private void requireReceivingLeader(Long companyId, String toUnit) {
    boolean all = scope.current(companyId).kind() == RenewalScope.Kind.ALL;
    if (!all && !scope.unitsOf(companyId, currentUser.username()).contains(toUnit)) {
      throw new BusinessRuleException(
          "RNW_TRANSFER_NOT_RECEIVER", "Only a Team Leader of unit " + toUnit + " decides it");
    }
  }

  private void requireUnit(Long companyId, String unit) {
    boolean exists =
        sales.units(companyId).stream()
            .anyMatch(u -> Objects.equals(u.getCode(), unit) && u.isActive());
    if (!exists) {
      throw new BusinessRuleException("RNW_TRANSFER_UNIT", unit + " is not an active unit");
    }
  }

  private java.time.Instant now() {
    return clock.instant();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * A transfer request.
   *
   * @param toUnit receiving unit
   * @param reasonCode reason (list RNW_TRANSFER_REASON), may be null
   * @param remarks remarks
   */
  public record Request(String toUnit, String reasonCode, String remarks) {}
}
