package com.iortatechnxt.brokerverse.submitted.renewal.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmInsurerRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmInsurerRuleRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRenewal;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRenewalRepository;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import com.iortatechnxt.brokerverse.submitted.service.SbmHistoryService;
import com.iortatechnxt.brokerverse.submitted.service.SbmPolicyFlow;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff.Contact;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff.HandOffRequest;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff.HandOffResult;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff.Loan;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff.Period;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff.PolicyData;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff.RenewalTerms;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff.Risk;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The renewal hand-off of a masterlist record (BRIDSP-15, 23, 25; FRS FR-SP-060): the insurer is
 * assigned by the insurer rules (by vehicle type or occupancy, never the expiring insurer where the
 * rule says so), the record is handed to the Renewal module through {@link RenewalHandOff} with its
 * policy, risk and loan data and the RA template, the hand-off is recorded and the record moves to
 * Renewal in Progress. A hand-off the Renewal module has not taken yet (PENDING) is offered again
 * by the next expiry scan. "Renew with BDOI" starts it by hand for the segments renewed by hand.
 */
@Service
@Transactional
public class HandOffService {

  private final SbmRenewalRepository renewals;
  private final SbmInsurerRuleRepository insurerRules;
  private final RenewalHandOff port;
  private final MasterlistService masterlist;
  private final SbmPolicyFlow flow;
  private final SbmHistoryService history;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param renewals hand-offs
   * @param insurerRules insurer rules
   * @param port Renewal module (or the pending default)
   * @param masterlist masterlist
   * @param flow work case
   * @param history record history
   * @param notifications notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the hand-off
  public HandOffService(
      SbmRenewalRepository renewals,
      SbmInsurerRuleRepository insurerRules,
      RenewalHandOff port,
      MasterlistService masterlist,
      SbmPolicyFlow flow,
      SbmHistoryService history,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.renewals = renewals;
    this.insurerRules = insurerRules;
    this.port = port;
    this.masterlist = masterlist;
    this.flow = flow;
    this.history = history;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * "Renew with BDOI": the AO starts the renewal of a For Renewal record by hand; the renewal
   * starts unassigned for a manual disposition.
   *
   * @param policyId record
   * @return the hand-off
   */
  public SbmRenewal renewWithBdoi(Long policyId) {
    SbmPolicy p = masterlist.get(policyId);
    if (p.getStatus() != SbmPolicyStatus.FOR_RENEWAL) {
      throw new BusinessRuleException(
          "SBM_NOT_FOR_RENEWAL", "Policy " + p.getSbmNo() + " is not For Renewal");
    }
    return handOff(p, assignInsurer(p).orElse(null), true);
  }

  /**
   * Hands a record over (expiry scan or by hand).
   *
   * @param p record For Renewal
   * @param insurer insurer assigned, may be null for a manual renewal
   * @param manual Renew with BDOI
   * @return the hand-off
   */
  public SbmRenewal handOff(SbmPolicy p, String insurer, boolean manual) {
    if (renewals.findByPolicyId(p.getId()).isPresent()) {
      throw new BusinessRuleException(
          "SBM_ALREADY_HANDED_OFF", "Policy " + p.getSbmNo() + " was already handed to Renewal");
    }
    String template = p.getRaTemplate() == null ? "GENERIC" : p.getRaTemplate();
    SbmRenewal r =
        renewals.save(
            new SbmRenewal(
                p.getCompanyId(),
                p.getId(),
                new SbmRenewal.Terms(insurer, template, manual),
                clock.instant()));
    offer(p, r);
    if (manual && currentUser.optionalUsername().isPresent()) {
      flow.act(p, "renew", TransitionNote.comment("Renew with BDOI"));
    } else {
      flow.system(p, "renew", "Handed to Renewal");
    }
    history.note(
        p,
        "Renewal",
        "Handed to Renewal" + (insurer == null ? "" : " with insurer " + insurer),
        SbmHistorySource.RENEWAL,
        r.getRenewalRef());
    notifyStarted(p);
    audit.record(
        SubmittedCodes.ENTITY,
        p.getSbmNo(),
        AuditAction.SUBMIT,
        "Renewal hand-off " + r.getHandoffStatus());
    return r;
  }

  /**
   * Offers a hand-off to the Renewal module (again): the answer is recorded.
   *
   * @param p record
   * @param r hand-off
   * @return the answer
   */
  public HandOffResult offer(SbmPolicy p, SbmRenewal r) {
    HandOffResult answer =
        port.handOff(
            new HandOffRequest(
                p.getCompanyId(),
                p.getSbmNo(),
                data(p),
                new RenewalTerms(
                    r.getRaTemplate(),
                    r.getInsurerAssigned(),
                    p.getHandlerUsername(),
                    p.getAoUsername(),
                    r.isManual()),
                currentUser.username()));
    r.answered(answer.outcome().name(), answer.renewalRef(), answer.arn(), answer.message());
    p.renewal(answer.renewalRef(), answer.arn());
    if (answer.outcome() == RenewalHandOff.Outcome.HANDED_OFF) {
      p.convert("PROCESS_PLACEMENT");
    }
    return answer;
  }

  /**
   * The insurer the rules assign to a record.
   *
   * @param p record
   * @return insurer, empty when no rule applies
   */
  @Transactional(readOnly = true)
  public Optional<String> assignInsurer(SbmPolicy p) {
    String expiring = p.getTerms().insurerCode();
    for (SbmInsurerRule rule :
        insurerRules.findByCompanyIdAndSegmentAndRecordStatusOrderByPriorityDesc(
            p.getCompanyId(), p.getSegment(), RecordStatus.ACTIVE)) {
      boolean vehicleOk =
          rule.getVehicleType() == null || same(rule.getVehicleType(), p.getRisk().vehicleType());
      boolean occupancyOk =
          rule.getOccupancy() == null || same(rule.getOccupancy(), p.getRisk().occupancy());
      boolean insurerOk =
          !rule.isExcludeExpiring() || !Objects.equals(rule.getInsurerCode(), expiring);
      if (vehicleOk && occupancyOk && insurerOk) {
        return Optional.of(rule.getInsurerCode());
      }
    }
    return Optional.empty();
  }

  private static boolean same(String a, String b) {
    return b != null
        && a.strip().toLowerCase(Locale.ROOT).equals(b.strip().toLowerCase(Locale.ROOT));
  }

  private void notifyStarted(SbmPolicy p) {
    Notice notice =
        new Notice(
            "Renewal of " + p.getSbmNo() + " started",
            p.getAssured().assuredName(),
            SubmittedCodes.link(p.getId()),
            SubmittedCodes.ENTITY,
            p.getId().toString());
    for (String user : new String[] {p.getHandlerUsername(), p.getAoUsername()}) {
      if (user != null) {
        notifications.notifyUser(user, notice, "SBM_RENEWAL_STARTED");
      }
    }
  }

  /**
   * The policy data of a record for the hand-off.
   *
   * @param p record
   * @return data
   */
  static PolicyData data(SbmPolicy p) {
    return new PolicyData(
        p.getSegment(),
        p.getBusinessType().name(),
        p.getAssured().assuredName(),
        p.getLoan().borrowerName(),
        p.getLoan().cif(),
        new Contact(
            p.getAssured().mailingAddress(), p.getAssured().email(), p.getAssured().mobile()),
        p.getTerms().insurerCode(),
        p.getTerms().policyNo(),
        new Period(p.getTerms().inceptionDate(), p.getTerms().expiryDate()),
        p.getTerms().sumInsured(),
        p.getTerms().totalPremium(),
        new Risk(
            p.getRisk().unitDescription() != null
                ? p.getRisk().unitDescription()
                : p.getRisk().propertyLocation(),
            p.getRisk().serialNo(),
            p.getRisk().motorNo(),
            p.getRisk().plateNo(),
            p.getRisk().occupancy()),
        new Loan(p.getLoan().pnNo(), p.getLoanStatus(), p.getRisk().mortgagee()));
  }
}
