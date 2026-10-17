package com.iortatechnxt.brokerverse.renewal.marketing.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.domain.ReferralStatus;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalReferral;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalReferralRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.renewal.service.RenewalScope;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transfer requests for a New Business opportunity (FRRN.011): a user refers a renewal account to
 * another Marketing unit with a justification; the request gets a Transfer Request Number and the
 * status Pending Acceptance; a Team Leader of the receiving unit accepts, rejects or returns it for
 * clarification; on acceptance the receiving unit creates a New Business account from the copied
 * data, which it may change first ({@link ReferralNewBusiness}). The renewal account keeps its unit
 * and Account Officer. Available when {@value #MODE} is REFERRAL or BOTH.
 */
@Service
@Transactional
public class ReferralService {

  private static final Set<ReferralStatus> OPEN =
      EnumSet.of(ReferralStatus.DRAFT, ReferralStatus.PENDING_ACCEPTANCE, ReferralStatus.RETURNED);

  private final RenewalRecords records;
  private final RenewalReferralRepository referrals;
  private final DocumentNumberService numbers;
  private final RenewalScope scope;
  private final RenewalNotices notices;
  private final SalesOrganisationService sales;
  private final TransferModes modes;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param referrals requests
   * @param numbers Transfer Request Numbers
   * @param scope data scope
   * @param notices notifications
   * @param sales Marketing units
   * @param modes referral and transfer in use
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ReferralService(
      RenewalRecords records,
      RenewalReferralRepository referrals,
      DocumentNumberService numbers,
      RenewalScope scope,
      RenewalNotices notices,
      SalesOrganisationService sales,
      TransferModes modes,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.records = records;
    this.referrals = referrals;
    this.numbers = numbers;
    this.scope = scope;
    this.notices = notices;
    this.sales = sales;
    this.modes = modes;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Creates a request from a renewal account.
   *
   * @param companyId company
   * @param renewalRef renewal account
   * @param request receiving unit, justification and whether to submit
   * @return the request
   */
  public RenewalReferral request(Long companyId, String renewalRef, Request request) {
    if (!modes.referral()) {
      throw new BusinessRuleException(
          "RNW_REFERRAL_OFF", "Transfer requests for New Business are not in use");
    }
    RenewalCandidate c = records.get(companyId, renewalRef);
    if (!c.getStage().isOpen()) {
      throw new BusinessRuleException(
          "RNW_REFERRAL_CLOSED", "Renewal account " + renewalRef + " is closed");
    }
    String toUnit = requireUnit(c, request.toUnit());
    String justification = requireText(request.justification(), "Enter the justification");
    if (referrals.findFirstByCandidateIdAndToUnitAndStatusIn(c.getId(), toUnit, OPEN).isPresent()) {
      throw new BusinessRuleException(
          "RNW_REFERRAL_OPEN",
          "Renewal account " + renewalRef + " already has an open transfer request to " + toUnit);
    }
    String no = numbers.next("TRQ-" + BusinessClock.today(clock).getYear());
    RenewalReferral r =
        referrals.save(new RenewalReferral(c, no, toUnit, justification, request.submit()));
    audit.record(
        RenewalCodes.ENTITY,
        renewalRef,
        AuditAction.CREATE,
        "Transfer request " + no + " to " + toUnit + " (" + r.getStatus().label() + ")");
    if (request.submit()) {
      notifyReceiver(c, r);
    }
    return r;
  }

  /**
   * Submits a draft, or a request returned for clarification with the clarification.
   *
   * @param companyId company
   * @param id request
   * @param justification justification, possibly clarified
   * @return the request
   */
  public RenewalReferral submit(Long companyId, Long id, String justification) {
    RenewalReferral r = get(companyId, id);
    requireRequester(r);
    r.submit(requireText(justification, "Enter the justification"));
    RenewalCandidate c = records.byId(r.getCandidateId());
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.SUBMIT,
        "Transfer request " + r.getReferralNo() + " submitted");
    notifyReceiver(c, r);
    return r;
  }

  /**
   * The decision of the receiving unit: accept, reject or return for clarification.
   *
   * @param companyId company
   * @param id request
   * @param decision outcome and remarks
   * @return the request
   */
  public RenewalReferral decide(Long companyId, Long id, Decision decision) {
    RenewalReferral r = get(companyId, id);
    requireReceiver(r);
    ReferralStatus outcome = decision.outcome();
    if (!EnumSet.of(ReferralStatus.ACCEPTED, ReferralStatus.REJECTED, ReferralStatus.RETURNED)
        .contains(outcome)) {
      throw new BusinessRuleException("RNW_REFERRAL_OUTCOME", "Accept, reject or return");
    }
    String remarks =
        outcome == ReferralStatus.ACCEPTED
            ? blankToNull(decision.remarks())
            : requireText(decision.remarks(), "Enter the remarks");
    r.decide(outcome, currentUser.username(), remarks, clock.instant());
    RenewalCandidate c = records.byId(r.getCandidateId());
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        outcome == ReferralStatus.ACCEPTED ? AuditAction.AUTHORIZE : AuditAction.REJECT,
        "Transfer request " + r.getReferralNo() + " " + outcome.label());
    notices.users(
        List.of(r.getCreatedBy()),
        RenewalCodes.EVENT_TRANSFER_DECIDED,
        c,
        new RenewalNotices.Text(
            r.getReferralNo() + ": " + outcome.label(),
            "Transfer request of "
                + c.getRenewalRef()
                + " to "
                + r.getToUnit()
                + ": "
                + outcome.label()
                + (remarks == null ? "" : " - " + remarks)));
    return r;
  }

  /**
   * Withdraws a request (its requester).
   *
   * @param companyId company
   * @param id request
   * @return the request
   */
  public RenewalReferral cancel(Long companyId, Long id) {
    RenewalReferral r = get(companyId, id);
    requireRequester(r);
    r.cancel();
    return r;
  }

  /**
   * Transfer Request Monitoring: the requests from or to the user's units (every request for a user
   * who sees every unit), newest first.
   *
   * @param companyId company
   * @return requests
   */
  @Transactional(readOnly = true)
  public List<RenewalReferral> list(Long companyId) {
    RenewalScope.Scope s = scope.current(companyId);
    List<RenewalReferral> all = referrals.findByCompanyIdOrderByIdDesc(companyId);
    if (s.kind() == RenewalScope.Kind.ALL) {
      return all;
    }
    Set<String> units = scope.unitsOf(companyId, currentUser.username());
    String user = currentUser.username();
    return all.stream()
        .filter(
            r ->
                units.contains(r.getToUnit())
                    || units.contains(r.getFromUnit())
                    || CurrentUser.sameUser(r.getCreatedBy(), user))
        .toList();
  }

  /**
   * The requests of a renewal account.
   *
   * @param companyId company
   * @param renewalRef renewal account
   * @return requests, newest first
   */
  @Transactional(readOnly = true)
  public List<RenewalReferral> ofRenewal(Long companyId, String renewalRef) {
    return referrals.findByCandidateIdOrderByIdDesc(records.get(companyId, renewalRef).getId());
  }

  private void notifyReceiver(RenewalCandidate c, RenewalReferral r) {
    notices.teamLeaders(
        c.getCompanyId(),
        r.getToUnit(),
        RenewalCodes.EVENT_TRANSFER_REQUESTED,
        c,
        new RenewalNotices.Text(
            r.getReferralNo() + ": transfer request for a New Business opportunity",
            "Review the transfer request of "
                + c.getSnapshot().clientName()
                + " from "
                + r.getFromUnit()
                + ": "
                + r.getJustification()));
  }

  RenewalReferral get(Long companyId, Long id) {
    return referrals
        .findById(id)
        .filter(r -> Objects.equals(r.getCompanyId(), companyId))
        .orElseThrow(() -> new ResourceNotFoundException("Transfer request", id));
  }

  private String requireUnit(RenewalCandidate c, String unit) {
    String toUnit = unit == null ? "" : unit.strip();
    if (toUnit.isEmpty()) {
      throw new BusinessRuleException("RNW_REFERRAL_UNIT", "Select the receiving Marketing unit");
    }
    if (toUnit.equals(c.getOwnerUnit())) {
      throw new BusinessRuleException(
          "RNW_REFERRAL_SAME_UNIT",
          "The receiving Marketing unit must be other than the originating unit");
    }
    boolean active =
        sales.units(c.getCompanyId()).stream()
            .filter(SalesUnit::isActive)
            .anyMatch(u -> toUnit.equals(u.getCode()));
    if (!active) {
      throw new BusinessRuleException("RNW_REFERRAL_UNIT", toUnit + " is not an active unit");
    }
    return toUnit;
  }

  private void requireRequester(RenewalReferral r) {
    if (!CurrentUser.sameUser(r.getCreatedBy(), currentUser.username())) {
      throw new BusinessRuleException(
          "RNW_REFERRAL_NOT_REQUESTER", "Only the requester can change the transfer request");
    }
  }

  void requireReceiver(RenewalReferral r) {
    boolean all = scope.current(r.getCompanyId()).kind() == RenewalScope.Kind.ALL;
    if (!all && !scope.unitsOf(r.getCompanyId(), currentUser.username()).contains(r.getToUnit())) {
      throw new BusinessRuleException(
          "RNW_REFERRAL_NOT_RECEIVER", "Only the receiving unit " + r.getToUnit() + " decides it");
    }
  }

  private static String requireText(String text, String message) {
    if (text == null || text.isBlank()) {
      throw new BusinessRuleException("RNW_REFERRAL_TEXT", message);
    }
    return text.strip();
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }

  /**
   * A new request.
   *
   * @param toUnit receiving Marketing unit
   * @param justification business justification
   * @param submit true to submit, false to save as Draft
   */
  public record Request(String toUnit, String justification, boolean submit) {}

  /**
   * A decision of the receiving unit.
   *
   * @param outcome ACCEPTED, REJECTED or RETURNED
   * @param remarks remarks (required to reject or return)
   */
  public record Decision(ReferralStatus outcome, String remarks) {}
}
