package com.iortatechnxt.brokerverse.submitted.renewal.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountOrigin;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.HoldCoverStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.placement.domain.HoldCover;
import com.iortatechnxt.brokerverse.placement.service.HoldCoverService;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRenewal;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRenewalRepository;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import com.iortatechnxt.brokerverse.submitted.service.SbmHistoryService;
import com.iortatechnxt.brokerverse.submitted.service.SbmParameters;
import com.iortatechnxt.brokerverse.submitted.service.SbmPolicyFlow;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff.HandOffStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What Submitted Policies follows of a renewal after the hand-off (BRIDSP-24, 26, 27, 32; FRS
 * FR-SP-063-065): the renewal account (its ARN names the masterlist number), its status (Placed,
 * Booked with the invoice), the status the Renewal module answers for a closed renewal (Not Renewed
 * with the reason), the hold cover watch (insurer not accepted within the acceptance days, hold
 * cover of an unbooked account ending soon) and the insurer re-assignment while the hold cover
 * request is open.
 */
@Service
@Transactional
public class RenewalFollowService {

  private static final String CONVERSION_PLACED = "PLACED";
  private static final String CONVERSION_RENEWED = "RENEWED";

  private final SbmRenewalRepository renewals;
  private final SbmPolicyRepository policies;
  private final MasterlistService masterlist;
  private final RenewalHandOff port;
  private final HoldCoverService holdCovers;
  private final AccountQueryService accounts;
  private final SbmPolicyFlow flow;
  private final SbmHistoryService history;
  private final SbmParameters parameters;
  private final LovService lovs;
  private final NotificationService notifications;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param renewals hand-offs
   * @param policies masterlist
   * @param masterlist masterlist (scope)
   * @param port Renewal module
   * @param holdCovers hold covers (placement)
   * @param accounts accounts
   * @param flow work case
   * @param history record history
   * @param parameters parameters
   * @param lovs lists of values
   * @param notifications notifications
   * @param alerts alerts
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the follow-up
  public RenewalFollowService(
      SbmRenewalRepository renewals,
      SbmPolicyRepository policies,
      MasterlistService masterlist,
      RenewalHandOff port,
      HoldCoverService holdCovers,
      AccountQueryService accounts,
      SbmPolicyFlow flow,
      SbmHistoryService history,
      SbmParameters parameters,
      LovService lovs,
      NotificationService notifications,
      AlertService alerts,
      AuditTrailService audit,
      Clock clock) {
    this.renewals = renewals;
    this.policies = policies;
    this.masterlist = masterlist;
    this.port = port;
    this.holdCovers = holdCovers;
    this.accounts = accounts;
    this.flow = flow;
    this.history = history;
    this.parameters = parameters;
    this.lovs = lovs;
    this.notifications = notifications;
    this.alerts = alerts;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * A renewal account changed status: its masterlist record follows (Process Placement, Placed).
   *
   * @param arn account
   * @param to new status
   */
  public void accountChanged(String arn, AccountStatus to) {
    recordOf(arn)
        .ifPresent(
            p -> {
              SbmRenewal r = renewals.findByPolicyId(p.getId()).orElse(null);
              if (r != null && r.getArn() == null) {
                r.account(arn);
              }
              p.renewal(p.getRenewalRef(), arn);
              if (to == AccountStatus.READY_FOR_PLACEMENT) {
                notifyHandler(
                    p,
                    "Renewal of " + p.getSbmNo() + " ready for placement",
                    "SBM_PLACEMENT_READY");
              } else if (to == AccountStatus.PLACED
                  && p.getStatus() == SbmPolicyStatus.RENEWAL_IN_PROGRESS) {
                flow.system(p, "placed", "Placement slip sent (" + arn + ")");
                p.convert(CONVERSION_PLACED);
                history.note(
                    p, "Renewal", "Placed with the insurer", SbmHistorySource.RENEWAL, arn);
                notifyHandler(
                    p, "Renewal of " + p.getSbmNo() + " sent to the insurer", "SBM_PLACEMENT_SENT");
              }
            });
  }

  /**
   * A renewal account was booked: the record shows BOOKED with the invoice (FR-SP-065).
   *
   * @param arn account
   * @param invoiceNo invoice
   * @param bookingDate booking date
   */
  public void booked(String arn, String invoiceNo, LocalDate bookingDate) {
    recordOf(arn)
        .filter(
            p ->
                p.getStatus() == SbmPolicyStatus.RENEWAL_IN_PROGRESS
                    || p.getStatus() == SbmPolicyStatus.PLACED)
        .ifPresent(
            p -> {
              p.renewal(p.getRenewalRef(), arn);
              p.booked(invoiceNo, bookingDate);
              p.convert(CONVERSION_RENEWED);
              flow.system(p, "booked", "Invoice " + invoiceNo);
              renewals.findByPolicyId(p.getId()).ifPresent(r -> r.close(CONVERSION_RENEWED, null));
              history.note(
                  p,
                  "Renewal",
                  "Booked, invoice " + invoiceNo,
                  SbmHistorySource.BOOKING,
                  invoiceNo);
              audit.record(
                  SubmittedCodes.ENTITY,
                  p.getSbmNo(),
                  AuditAction.UPDATE,
                  "Renewal booked " + invoiceNo);
            });
  }

  private Optional<SbmPolicy> recordOf(String arn) {
    Account account;
    try {
      account = accounts.requireByArn(arn);
    } catch (RuntimeException e) {
      return Optional.empty();
    }
    var c = account.getClassification();
    if (c.origin() != AccountOrigin.SUBMITTED_POLICY || c.renewalOfRef() == null) {
      return Optional.empty();
    }
    return policies.findBySbmNo(c.renewalOfRef());
  }

  /**
   * The daily watch of a company (job {@code SBM_HOLD_COVER_WATCH}).
   *
   * @param companyId company
   * @param today business date
   * @return what the watch did
   */
  public Watch watch(Long companyId, LocalDate today) {
    int alerted = 0;
    int closed = 0;
    for (SbmRenewal r :
        renewals.findByCompanyIdAndHandoffStatusInAndOutcome(
            companyId, List.of(SbmRenewal.HANDED_OFF), SbmRenewal.IN_PROGRESS)) {
      SbmPolicy p = policies.findById(r.getPolicyId()).orElse(null);
      if (p == null) {
        continue;
      }
      if (sync(p, r)) {
        closed++;
        continue;
      }
      if (r.getArn() != null && holdCover(p, r, today)) {
        alerted++;
      }
    }
    return new Watch(alerted, closed);
  }

  private boolean sync(SbmPolicy p, SbmRenewal r) {
    Optional<HandOffStatus> status = port.status(p.getCompanyId(), p.getSbmNo());
    if (status.isEmpty()) {
      return false;
    }
    HandOffStatus s = status.get();
    if (s.arn() != null && r.getArn() == null) {
      r.account(s.arn());
      p.renewal(s.renewalRef(), s.arn());
    }
    if (s.state() == RenewalHandOff.State.OPEN || s.state() == RenewalHandOff.State.RENEWED) {
      return false;
    }
    String outcome =
        switch (s.state()) {
          case LOST -> "LOST";
          case EXPIRED_UNRENEWED -> "EXPIRED";
          default -> "DECLINED";
        };
    r.close(outcome, s.reason());
    if (p.getStatus() == SbmPolicyStatus.RENEWAL_IN_PROGRESS
        || p.getStatus() == SbmPolicyStatus.PLACED) {
      flow.system(p, "not_renewed", s.reason() == null ? "Not renewed" : s.reason());
      p.convert("UNRENEWED");
      history.note(
          p,
          "Renewal",
          "Not renewed: " + (s.reason() == null ? "" : s.reason()),
          SbmHistorySource.RENEWAL,
          s.renewalRef());
    }
    return true;
  }

  private boolean holdCover(SbmPolicy p, SbmRenewal r, LocalDate today) {
    Optional<HoldCover> cover = holdCovers.current(r.getArn());
    if (cover.isEmpty()) {
      return false;
    }
    HoldCover c = cover.get();
    r.holdCover(c.getStartDate(), c.getConfirmedOn());
    if (today.equals(r.getAlertedOn())) {
      return false;
    }
    boolean notAccepted =
        c.getStatus() == HoldCoverStatus.REQUESTED
            && !c.getStartDate().plusDays(parameters.acceptDays()).isAfter(today);
    boolean unbooked =
        c.getStatus() == HoldCoverStatus.CONFIRMED
            && p.getBookedInvoiceNo() == null
            && !c.getExpiryDate().minusDays(parameters.unbookedAlertDays()).isAfter(today);
    if (notAccepted) {
      raise(
          p,
          r,
          "SBM_INSURER_NOT_ACCEPTED",
          "The insurer "
              + c.getInsurerCode()
              + " has not accepted the hold cover of "
              + r.getArn());
    } else if (unbooked) {
      raise(
          p,
          r,
          "SBM_HOLD_COVER_UNBOOKED",
          "The hold cover of "
              + r.getArn()
              + " ends on "
              + c.getExpiryDate()
              + " and the account is not booked");
    } else {
      return false;
    }
    r.alerted(today);
    return true;
  }

  private void raise(SbmPolicy p, SbmRenewal r, String code, String message) {
    alerts.raise(
        code,
        new AlertFacts(
            p.getCompanyId(),
            null,
            SubmittedCodes.ENTITY,
            p.getId().toString(),
            message,
            BigDecimal.ZERO,
            code + ":" + r.getArn() + ":" + r.getReassignCount()));
    notifyHandler(p, message, "SBM_HOLD_COVER_UNBOOKED".equals(code) ? code : null);
  }

  /**
   * Re-assigns the insurer while the hold cover request is open (FR-SP-063).
   *
   * @param policyId record
   * @param insurerCode new insurer
   * @param reasonCode reason (LOV SBM_DECLINE_REASON)
   * @return the hand-off
   */
  public SbmRenewal reassign(Long policyId, String insurerCode, String reasonCode) {
    SbmPolicy p = masterlist.get(policyId);
    SbmRenewal r =
        renewals
            .findByPolicyId(policyId)
            .filter(x -> x.getArn() != null)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "SBM_NO_RENEWAL_ACCOUNT",
                        "Policy "
                            + p.getSbmNo()
                            + " has no renewal account with a hold cover yet"));
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BusinessRuleException("SBM_REASON_REQUIRED", "Select the reason");
    }
    lovs.requireValid(SubmittedCodes.LOV_DECLINE, reasonCode, BusinessClock.today(clock));
    if (insurerCode == null || insurerCode.equals(r.getInsurerAssigned())) {
      throw new BusinessRuleException(
          "SBM_SAME_INSURER", "Select an insurer other than the current one");
    }
    holdCovers.reassign(r.getArn(), insurerCode, reasonCode);
    r.reassigned(insurerCode);
    history.note(
        p, "Renewal", "Insurer re-assigned to " + insurerCode, SbmHistorySource.MANUAL, r.getArn());
    audit.record(
        SubmittedCodes.ENTITY,
        p.getSbmNo(),
        AuditAction.UPDATE,
        "Insurer re-assigned to " + insurerCode);
    return r;
  }

  private void notifyHandler(SbmPolicy p, String title, String event) {
    if (p.getHandlerUsername() == null) {
      return;
    }
    Notice notice =
        new Notice(
            title,
            p.getAssured().assuredName(),
            SubmittedCodes.link(p.getId()),
            SubmittedCodes.ENTITY,
            p.getId().toString());
    if (event == null) {
      notifications.notifyUser(p.getHandlerUsername(), notice);
    } else {
      notifications.notifyUser(p.getHandlerUsername(), notice, event);
    }
  }

  /**
   * What the watch did.
   *
   * @param alerted hand-offs alerted
   * @param closed renewals closed as not renewed
   */
  public record Watch(int alerted, int closed) {}
}
