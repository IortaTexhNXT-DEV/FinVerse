package com.iortatechnxt.brokerverse.renewal.placement.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.renewal.domain.CandidatePlacement;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPlacement;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPlacementRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The insurer's placement response (FRRN.030.01): Approved records the response and its date and,
 * once every insurer of the account approved, books and locks the renewal account when {@value
 * #BOOK_ON_APPROVAL} is on (otherwise it stays For Booking for the Processing Officer); Rejected
 * records one of the seven rejection reasons and makes the account Rejected Placement, to be
 * returned to Marketing or its placement cancelled.
 */
@Service
public class PlacementResponses {

  /** Parameter: the insurer's approval books the account. */
  public static final String BOOK_ON_APPROVAL = "RNW_BOOK_ON_PLACEMENT_APPROVAL";

  /** List of rejection reasons. */
  public static final String LOV_REJECT = "RNW_PLACEMENT_REJECT_REASON";

  /** Notification: the insurer answered the placement. */
  public static final String EVENT = "RNW_PLACEMENT_RESPONDED";

  private final RenewalRecords records;
  private final RenewalCandidateRepository candidates;
  private final RenewalPlacementRepository placements;
  private final LovService lovs;
  private final SystemParameterService parameters;
  private final RenewalNotices notices;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param candidates renewal lookup by reference
   * @param placements placements
   * @param lovs rejection reasons
   * @param parameters booking on approval
   * @param notices notifications
   * @param audit audit trail
   * @param clock clock
   */
  public PlacementResponses(
      RenewalRecords records,
      RenewalCandidateRepository candidates,
      RenewalPlacementRepository placements,
      LovService lovs,
      SystemParameterService parameters,
      RenewalNotices notices,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.candidates = candidates;
    this.placements = placements;
    this.lovs = lovs;
    this.parameters = parameters;
    this.notices = notices;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The problem with a response, or null when it can be applied.
   *
   * @param companyId company
   * @param response the response
   * @return refusal reason, null when valid
   */
  @Transactional(readOnly = true)
  public String problem(Long companyId, Response response) {
    Optional<RenewalCandidate> found =
        candidates.findByCompanyIdAndRenewalRef(companyId, response.renewalRef());
    if (found.isEmpty()) {
      return "Record not found";
    }
    RenewalCandidate c = found.get();
    if (waiting(c, response.insurerCode()).isEmpty()) {
      return "No placement of the account is waiting for the insurer's response";
    }
    if (!response.approved() && reasonCode(response.reason()) == null) {
      return "Enter a valid rejection reason";
    }
    return null;
  }

  /**
   * Applies a response.
   *
   * @param companyId company
   * @param response the response
   * @return the placement status of the account
   */
  @Transactional
  public String apply(Long companyId, Response response) {
    String problem = problem(companyId, response);
    if (problem != null) {
      throw new BusinessRuleException("RNW_PLACEMENT_RESPONSE", problem);
    }
    RenewalCandidate c = records.get(companyId, response.renewalRef());
    LocalDate date = response.date() == null ? BusinessClock.today(clock) : response.date();
    String reason = response.approved() ? null : reasonCode(response.reason());
    waiting(c, response.insurerCode())
        .forEach(p -> p.respond(response.approved(), date, reason, response.remarks()));
    String status = status(c, response.approved());
    c.getPlacement().status(status);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.UPDATE,
        "Placement " + (response.approved() ? "approved" : "rejected") + " by the insurer");
    notices.users(
        Collections.singletonList(c.getAssignedPo()),
        EVENT,
        c,
        new RenewalNotices.Text(
            c.getRenewalRef() + " placement " + (response.approved() ? "approved" : "rejected"),
            reason == null ? "" : lovs.label(LOV_REJECT, reason)));
    return status;
  }

  private String status(RenewalCandidate c, boolean approved) {
    if (!approved) {
      return CandidatePlacement.REJECTED_PLACEMENT;
    }
    boolean allApproved =
        placements.findByCandidateIdOrderByIdDesc(c.getId()).stream()
            .filter(RenewalPlacement::isCurrent)
            .allMatch(p -> RenewalPlacement.APPROVED.equals(p.getStatus()));
    if (allApproved && "true".equals(parameters.text(BOOK_ON_APPROVAL, "false").strip())) {
      c.lockMarketing(clock.instant());
      return CandidatePlacement.BOOKED;
    }
    return CandidatePlacement.FOR_BOOKING;
  }

  private List<RenewalPlacement> waiting(RenewalCandidate c, String insurerCode) {
    return placements.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .filter(p -> RenewalPlacement.SENT.equals(p.getStatus()))
        .filter(
            p ->
                insurerCode == null
                    || insurerCode.isBlank()
                    || insurerCode.equals(p.getInsurerCode()))
        .toList();
  }

  /**
   * The code of a rejection reason given as its code or its label.
   *
   * @param reason code or label
   * @return code, null when unknown
   */
  public String reasonCode(String reason) {
    if (reason == null || reason.isBlank()) {
      return null;
    }
    String value = reason.strip();
    return lovs.activeValues(LOV_REJECT, BusinessClock.today(clock)).stream()
        .filter(v -> v.getCode().equals(value) || same(v.getLabel(), value))
        .map(v -> v.getCode())
        .findFirst()
        .orElse(null);
  }

  private static boolean same(String label, String value) {
    return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE).matcher(label).matches();
  }

  /**
   * A placement response.
   *
   * @param renewalRef renewal reference
   * @param insurerCode insurer, null for every insurer of the account
   * @param approved Approved or Rejected
   * @param date response date, today when null
   * @param reason rejection reason (code or label)
   * @param remarks insurer remarks
   */
  public record Response(
      String renewalRef,
      String insurerCode,
      boolean approved,
      LocalDate date,
      String reason,
      String remarks) {}
}
