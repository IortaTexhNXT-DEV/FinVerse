package com.iortatechnxt.brokerverse.renewal.holdcover.service;

import com.iortatechnxt.brokerverse.account.domain.HoldCoverStatus;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.placement.domain.HoldCover;
import com.iortatechnxt.brokerverse.placement.service.HoldCoverService;
import com.iortatechnxt.brokerverse.placement.service.HoldCoverService.HoldCoverConfirmation;
import com.iortatechnxt.brokerverse.placement.service.HoldCoverService.RenewalHoldCoverRequest;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hold cover of a renewal (Walkthrough addendum BRRN.042, R37-HC-01 to 04; FR-RN-086): the request
 * to the insurer from the renewal with the duration (30 or 60 days), the coverage start (the policy
 * expiry by default), the expiring policy number and remarks; the insurer's confirmation with its
 * reference, validity and conditions, or its decline; the cancellation with a reason. The hold
 * cover itself is that of the renewal account (Placement, FR-NB-082, 083); the renewal keeps its
 * status and the confirmed end, which is its effective expiry date.
 */
@Service
@Transactional
public class RenewalHoldCoverService {

  private final RenewalRecords records;
  private final HoldCoverService holdCovers;
  private final RenewalParameters parameters;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param holdCovers hold covers of the accounts
   * @param parameters durations
   * @param audit audit trail
   */
  public RenewalHoldCoverService(
      RenewalRecords records,
      HoldCoverService holdCovers,
      RenewalParameters parameters,
      AuditTrailService audit) {
    this.records = records;
    this.holdCovers = holdCovers;
    this.parameters = parameters;
    this.audit = audit;
  }

  /**
   * The current hold cover of a renewal.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @return hold cover of the renewal account, empty when none was requested
   */
  @Transactional(readOnly = true)
  public Optional<HoldCover> current(Long companyId, String renewalRef) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    return c.getRenewalArn() == null ? Optional.empty() : holdCovers.current(c.getRenewalArn());
  }

  /**
   * The durations offered.
   *
   * @return days, the default first
   */
  @Transactional(readOnly = true)
  public List<Integer> durations() {
    return parameters.holdCoverDays();
  }

  /**
   * Requests the hold cover of a renewal from its insurer.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param request start (the policy expiry when null), duration, remarks
   * @return hold cover
   */
  public HoldCover request(Long companyId, String renewalRef, Request request) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    String arn = requireAccount(c);
    if (request.durationDays() == null || !durations().contains(request.durationDays())) {
      throw new BusinessRuleException(
          "HOLD_COVER_DURATION", "Select the duration of the hold cover");
    }
    LocalDate start = request.startDate() == null ? c.getExpiryDate() : request.startDate();
    HoldCover cover =
        holdCovers.requestForRenewal(
            arn,
            new RenewalHoldCoverRequest(
                start,
                request.durationDays(),
                c.getSnapshot().policyNo(),
                blankToNull(request.remarks()),
                List.of()));
    c.getExpiry().holdCover(cover.getStatus(), cover.getExpiryDate());
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.UPDATE,
        "Hold cover of "
            + request.durationDays()
            + " days requested from "
            + DisplayFormat.date(start));
    return cover;
  }

  /**
   * Records the insurer's confirmation: the end of the hold cover becomes the effective expiry date
   * of the renewal.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param confirmation reference, date, validity and conditions
   * @return hold cover
   */
  public HoldCover confirm(Long companyId, String renewalRef, Confirmation confirmation) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    String arn = requireAccount(c);
    if (blankToNull(confirmation.reference()) == null || confirmation.validTo() == null) {
      throw new BusinessRuleException(
          "HOLD_COVER_CONFIRMATION", "Enter the insurer reference and the validity period");
    }
    HoldCover cover =
        holdCovers.confirm(
            arn,
            new HoldCoverConfirmation(
                null,
                confirmation.reference(),
                confirmation.confirmedOn(),
                confirmation.validTo()));
    cover.conditions(confirmation.conditions());
    c.getExpiry().holdCover(cover.getStatus(), cover.getExpiryDate());
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.UPDATE,
        "Hold cover confirmed until "
            + DisplayFormat.date(cover.getExpiryDate())
            + ": effective expiry date moved");
    return cover;
  }

  /**
   * Records the insurer's decline: the effective expiry date stays the policy expiry date.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param reference insurer reference, may be null
   * @return hold cover
   */
  public HoldCover decline(Long companyId, String renewalRef, String reference) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    HoldCover cover = holdCovers.decline(requireAccount(c), blankToNull(reference));
    c.getExpiry().holdCover(cover.getStatus(), null);
    audit.record(RenewalCodes.ENTITY, c.getRenewalRef(), AuditAction.UPDATE, "Hold cover declined");
    return cover;
  }

  /**
   * Cancels the hold cover with a reason; a new request is then allowed.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param reason reason
   * @return hold cover
   */
  public HoldCover cancel(Long companyId, String renewalRef, String reason) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    HoldCover cover = holdCovers.cancel(requireAccount(c), reason == null ? "" : reason);
    c.getExpiry().holdCover(cover.getStatus(), null);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.UPDATE,
        "Hold cover cancelled: " + reason);
    return cover;
  }

  /**
   * Copies the hold cover of the renewal account onto the renewal (a confirmation recorded from
   * Placement, an expiry), so that the lists and the letter jobs read the effective expiry date.
   *
   * @param c renewal
   */
  public void refresh(RenewalCandidate c) {
    if (c.getRenewalArn() == null) {
      return;
    }
    holdCovers
        .current(c.getRenewalArn())
        .ifPresent(h -> c.getExpiry().holdCover(h.getStatus(), h.getExpiryDate()));
  }

  /**
   * The effective expiry date of a renewal read from the hold cover of its renewal account, without
   * changing the renewal (the letter jobs select on it).
   *
   * @param c renewal
   * @return end of a confirmed hold cover, otherwise the policy expiry date
   */
  @Transactional(readOnly = true)
  public LocalDate effectiveExpiry(RenewalCandidate c) {
    if (c.getRenewalArn() == null) {
      return c.effectiveExpiry();
    }
    return holdCovers
        .current(c.getRenewalArn())
        .filter(h -> h.getStatus() == HoldCoverStatus.CONFIRMED)
        .map(HoldCover::getExpiryDate)
        .filter(end -> end.isAfter(c.getExpiryDate()))
        .orElse(c.getExpiryDate());
  }

  private static String requireAccount(RenewalCandidate c) {
    if (c.getRenewalArn() == null) {
      throw new BusinessRuleException(
          "RNW_HOLD_COVER_NO_ACCOUNT",
          "Renewal " + c.getRenewalRef() + " has no renewal account yet: create it first");
    }
    return c.getRenewalArn();
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }

  /**
   * A hold cover request of a renewal.
   *
   * @param startDate coverage start, the policy expiry date when null
   * @param durationDays duration (one of {@code RNW_HOLD_COVER_DAYS})
   * @param remarks remarks, may be null
   */
  public record Request(LocalDate startDate, Integer durationDays, String remarks) {}

  /**
   * The insurer's confirmation.
   *
   * @param reference insurer reference
   * @param confirmedOn confirmation date, today when null
   * @param validTo last day of the hold cover
   * @param conditions conditions of the insurer, may be null
   */
  public record Confirmation(
      String reference, LocalDate confirmedOn, LocalDate validTo, String conditions) {}
}
