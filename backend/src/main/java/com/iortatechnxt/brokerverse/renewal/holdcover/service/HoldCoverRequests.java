package com.iortatechnxt.brokerverse.renewal.holdcover.service;

import com.iortatechnxt.brokerverse.account.domain.HoldCoverStatus;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.renewal.allocation.service.InsurerAllocationService;
import com.iortatechnxt.brokerverse.renewal.domain.HoldCoverAsk;
import com.iortatechnxt.brokerverse.renewal.domain.HoldCoverAskRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hold cover requests of BDOI's FRS (FRRN.036, FRRN.037): the duration by segment (Non-CBG 30 days,
 * CBG 90 days, a CBG account moved to another insurer after a rejection 30 days for Home and 60 for
 * Motor), one request per insurer of the renewal sent by MFT or e-mail, an extension of 15 or 30
 * days from 7 days before the end, and the insurer's response: an approval confirms the hold cover
 * (the end becomes the effective expiry date), a rejection returns the account to its user with the
 * insurer's remarks. The automatic requests are in {@link HoldCoverBatches}.
 */
@Service
@Transactional
public class HoldCoverRequests {

  private static final String CBG_HOME = "PROPERTY";
  private static final String MOTOR = "MOTOR";
  private static final int DEFAULT_DAYS = 30;
  private static final int CBG_DAYS = 90;
  private static final int MOTOR_TRANSFER_DAYS = 60;
  private static final int WINDOW = 7;

  private final RenewalRecords records;
  private final HoldCoverAskRepository asks;
  private final InsurerAllocationService allocations;
  private final HoldCoverFiles files;
  private final RenewalHoldCoverService holdCovers;
  private final SystemParameterService parameters;
  private final RenewalParameters renewal;
  private final DocumentNumberService numbers;
  private final RenewalNotices notices;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param asks requests
   * @param allocations insurers of a renewal
   * @param files request files and their delivery
   * @param holdCovers hold cover of the renewal account
   * @param parameters durations and windows
   * @param renewal CBG segments
   * @param numbers reference numbers
   * @param notices notifications
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public HoldCoverRequests(
      RenewalRecords records,
      HoldCoverAskRepository asks,
      InsurerAllocationService allocations,
      HoldCoverFiles files,
      RenewalHoldCoverService holdCovers,
      SystemParameterService parameters,
      RenewalParameters renewal,
      DocumentNumberService numbers,
      RenewalNotices notices,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.asks = asks;
    this.allocations = allocations;
    this.files = files;
    this.holdCovers = holdCovers;
    this.parameters = parameters;
    this.renewal = renewal;
    this.numbers = numbers;
    this.notices = notices;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The default duration of the hold cover of a renewal (FRRN.036.01).
   *
   * @param c renewal
   * @return days
   */
  @Transactional(readOnly = true)
  public int defaultDays(RenewalCandidate c) {
    var p = c.getSnapshot().product();
    String segment = p == null ? null : p.segment();
    if (!cbg(segment)) {
      return parameters.intValue("RNW_HOLD_COVER_DAYS_NON_CBG", DEFAULT_DAYS);
    }
    boolean rejected =
        c.getExpiry().getHoldCoverStatus() == HoldCoverStatus.DECLINED
            || asks.findByCandidateIdOrderByIdDesc(c.getId()).stream()
                .anyMatch(a -> HoldCoverAsk.REJECTED.equals(a.getStatus()));
    if (rejected) {
      return p != null && MOTOR.equals(p.lineCode())
          ? parameters.intValue("RNW_HOLD_COVER_DAYS_CBG_MOTOR_TRANSFER", MOTOR_TRANSFER_DAYS)
          : parameters.intValue("RNW_HOLD_COVER_DAYS_CBG_HOME_TRANSFER", DEFAULT_DAYS);
    }
    return parameters.intValue("RNW_HOLD_COVER_DAYS_CBG", CBG_DAYS);
  }

  /**
   * Requests the hold cover of a renewal: one request per insurer, sent at once.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param days duration, the default of the renewal when null
   * @param start first day, the policy expiry when null
   * @return the requests
   */
  public List<HoldCoverAsk> request(
      Long companyId, String renewalRef, Integer days, LocalDate start) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    return requestFor(c, HoldCoverAsk.REQUEST, days == null ? defaultDays(c) : days, start);
  }

  /**
   * Requests the hold cover of a renewal already loaded, one request per insurer.
   *
   * @param c renewal
   * @param kind REQUEST, CBG_BATCH or MONTHLY_EXTENSION
   * @param days duration
   * @param start first day, the policy expiry when null
   * @return the requests
   */
  public List<HoldCoverAsk> requestFor(RenewalCandidate c, String kind, int days, LocalDate start) {
    if (days <= 0) {
      throw new BusinessRuleException(
          "HOLD_COVER_DURATION", "Select the duration of the hold cover");
    }
    if (HoldCoverAsk.REQUEST.equals(kind) && open(c).isPresent()) {
      throw new BusinessRuleException(
          "HOLD_COVER_OPEN",
          "Renewal " + c.getRenewalRef() + " already has a hold cover requested or confirmed");
    }
    LocalDate from = start == null ? c.getExpiryDate() : start;
    List<HoldCoverAsk> created = new ArrayList<>();
    for (InsurerAllocationService.Share share : allocations.of(c)) {
      HoldCoverAsk a =
          asks.save(
              new HoldCoverAsk(
                  c,
                  numbers.next("HCR-" + BusinessClock.today(clock).getYear()),
                  kind,
                  new HoldCoverAsk.Insurer(share.insurerCode(), share(share.percent())),
                  new HoldCoverAsk.Period(from, days)));
      created.add(a);
    }
    if (!HoldCoverAsk.CBG_BATCH.equals(kind)) {
      created.forEach(a -> files.sendOne(c, a));
    }
    c.getExpiry().holdCover(HoldCoverStatus.REQUESTED, null);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.UPDATE,
        "Hold cover of "
            + days
            + " days requested from "
            + DisplayFormat.date(from)
            + " ("
            + created.size()
            + " insurer(s))");
    return created;
  }

  /**
   * Requests the extension of a confirmed hold cover (FRRN.036.03), from the window before its end.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param days 15 or 30 by the setting
   * @return the extension requests
   */
  public List<HoldCoverAsk> extend(Long companyId, String renewalRef, int days) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    List<Integer> offered =
        parameters.items("RNW_HOLD_COVER_EXTENSION_DAYS").stream()
            .map(String::strip)
            .filter(v -> v.matches("\\d{1,3}"))
            .map(Integer::valueOf)
            .toList();
    if (!offered.contains(days)) {
      throw new BusinessRuleException(
          "HOLD_COVER_EXTENSION_DAYS", "An extension is of " + offered + " days");
    }
    LocalDate end = c.getExpiry().getHoldCoverUntil();
    if (c.getExpiry().getHoldCoverStatus() != HoldCoverStatus.CONFIRMED || end == null) {
      throw new BusinessRuleException(
          "HOLD_COVER_NOT_CONFIRMED", "Renewal " + renewalRef + " has no confirmed hold cover");
    }
    LocalDate from = end.minusDays(parameters.intValue("RNW_HOLD_COVER_EXTENSION_WINDOW", WINDOW));
    if (BusinessClock.today(clock).isBefore(from)) {
      throw new BusinessRuleException(
          "HOLD_COVER_EXTENSION_EARLY",
          "The extension of " + renewalRef + " can be requested from " + DisplayFormat.date(from));
    }
    return requestFor(c, HoldCoverAsk.EXTENSION, days, end);
  }

  /**
   * Records the insurer's response to a request (FRRN.037.02).
   *
   * @param companyId company
   * @param requestNo reference number of the request
   * @param response approval or rejection
   * @return the request
   */
  public HoldCoverAsk respond(Long companyId, String requestNo, Response response) {
    HoldCoverAsk a =
        asks.findByCompanyIdAndRequestNo(companyId, requestNo.strip())
            .orElseThrow(() -> new ResourceNotFoundException("Hold cover request", requestNo));
    RenewalCandidate c = records.byId(a.getCandidateId());
    if (response.end() != null && response.end().isBefore(a.getStartDate())) {
      throw new BusinessRuleException(
          "RNW_HOLD_COVER_END", "The hold cover end date cannot be before its start date");
    }
    a.respond(
        response.approved(),
        response.end(),
        response.reference(),
        response.remarks(),
        clock.instant());
    if (response.approved()) {
      c.getExpiry().holdCover(HoldCoverStatus.CONFIRMED, a.getEndDate());
      confirmAccountHoldCover(c, a);
      audit.record(
          RenewalCodes.ENTITY,
          c.getRenewalRef(),
          AuditAction.UPDATE,
          "Hold cover "
              + a.getRequestNo()
              + " approved until "
              + DisplayFormat.date(a.getEndDate()));
    } else {
      c.getExpiry().holdCover(HoldCoverStatus.DECLINED, null);
      c.getFlags().setReturned(true);
      List<String> owners = new ArrayList<>();
      owners.add(c.getAssignedPo());
      owners.add(c.getAssignedAo());
      notices.users(
          owners,
          RenewalCodes.EVENT_RETURNED,
          c,
          new RenewalNotices.Text(
              c.getRenewalRef() + ": hold cover rejected by " + a.getInsurerCode(),
              response.remarks() == null ? "Rejected by the insurer" : response.remarks()));
      audit.record(
          RenewalCodes.ENTITY,
          c.getRenewalRef(),
          AuditAction.REJECT,
          "Hold cover " + a.getRequestNo() + " rejected: " + response.remarks());
    }
    return a;
  }

  /**
   * Cancels the open requests of a renewal; a new request is then accepted.
   *
   * @param companyId company
   * @param renewalRef renewal
   */
  public void cancel(Long companyId, String renewalRef) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    asks.findByCandidateIdOrderByIdDesc(c.getId()).forEach(HoldCoverAsk::cancel);
    c.getExpiry().holdCover(HoldCoverStatus.CANCELLED, null);
    audit.record(
        RenewalCodes.ENTITY, renewalRef, AuditAction.UPDATE, "Hold cover requests cancelled");
  }

  /**
   * The requests of a renewal, newest first.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @return requests
   */
  @Transactional(readOnly = true)
  public List<HoldCoverAsk> of(Long companyId, String renewalRef) {
    return asks.findByCandidateIdOrderByIdDesc(records.get(companyId, renewalRef).getId());
  }

  private Optional<HoldCoverAsk> open(RenewalCandidate c) {
    LocalDate today = BusinessClock.today(clock);
    return asks.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .filter(
            a ->
                HoldCoverAsk.REQUESTED.equals(a.getStatus())
                    || HoldCoverAsk.APPROVED.equals(a.getStatus())
                        && !a.getEndDate().isBefore(today))
        .findFirst();
  }

  private void confirmAccountHoldCover(RenewalCandidate c, HoldCoverAsk a) {
    if (c.getRenewalArn() == null) {
      return;
    }
    holdCovers
        .current(c.getCompanyId(), c.getRenewalRef())
        .filter(
            h ->
                h.getStatus() == HoldCoverStatus.REQUESTED
                    || h.getStatus() == HoldCoverStatus.CONFIRMED)
        .ifPresent(
            h ->
                holdCovers.confirm(
                    c.getCompanyId(),
                    c.getRenewalRef(),
                    new RenewalHoldCoverService.Confirmation(
                        a.getInsurerRef() == null ? a.getRequestNo() : a.getInsurerRef(),
                        BusinessClock.today(clock),
                        a.getEndDate(),
                        a.getResponseRemarks())));
  }

  private boolean cbg(String segment) {
    return renewal.cbgSegment(segment);
  }

  /**
   * Whether a renewal is a CBG Home account.
   *
   * @param c renewal
   * @return true for the home line of a CBG segment
   */
  boolean cbgHome(RenewalCandidate c) {
    var p = c.getSnapshot().product();
    return p != null && renewal.cbgSegment(p.segment()) && CBG_HOME.equals(p.lineCode());
  }

  private static BigDecimal share(BigDecimal percent) {
    return percent == null || percent.compareTo(BigDecimal.valueOf(100)) == 0 ? null : percent;
  }

  /**
   * An insurer's response.
   *
   * @param approved approved or rejected
   * @param end end of the hold cover approved, null for the requested one
   * @param reference insurer reference
   * @param remarks insurer remarks
   */
  public record Response(boolean approved, LocalDate end, String reference, String remarks) {}
}
