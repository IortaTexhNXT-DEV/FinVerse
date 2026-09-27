package com.iortatechnxt.brokerverse.renewal.acceptance.api;

import com.iortatechnxt.brokerverse.renewal.acceptance.service.AcceptanceService;
import com.iortatechnxt.brokerverse.renewal.acceptance.service.FollowupService;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAcceptance;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalFollowup;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Client acceptance and Contact Center follow-ups of a renewal (FR-RN-084, 085). */
@RestController
@RequestMapping("/api/v1/renewal/candidates/{ref}")
public class AcceptanceController {

  private final AcceptanceService acceptances;
  private final FollowupService followups;

  /**
   * Creates the controller.
   *
   * @param acceptances acceptances
   * @param followups follow-ups
   */
  public AcceptanceController(AcceptanceService acceptances, FollowupService followups) {
    this.acceptances = acceptances;
    this.followups = followups;
  }

  /**
   * Records the client's acceptance.
   *
   * @param companyId company
   * @param ref renewal
   * @param input method and evidence
   * @return acceptance
   */
  @PostMapping("/acceptance")
  @PreAuthorize("hasAuthority('RNW_ACCEPT')")
  public AcceptanceView accept(
      @RequestParam Long companyId,
      @PathVariable String ref,
      @RequestBody AcceptanceService.Input input) {
    return AcceptanceView.of(acceptances.accept(companyId, ref, input, "USER"));
  }

  /**
   * The acceptances of a renewal.
   *
   * @param companyId company
   * @param ref renewal
   * @return acceptances
   */
  @GetMapping("/acceptance")
  @PreAuthorize("hasAuthority('RNW_VIEW')")
  public List<AcceptanceView> acceptances(@RequestParam Long companyId, @PathVariable String ref) {
    return acceptances.of(companyId, ref).stream().map(AcceptanceView::of).toList();
  }

  /**
   * Records a follow-up.
   *
   * @param companyId company
   * @param ref renewal
   * @param input follow-up
   * @return follow-up
   */
  @PostMapping("/followups")
  @PreAuthorize("hasAuthority('RNW_FOLLOWUP')")
  public FollowupView followup(
      @RequestParam Long companyId,
      @PathVariable String ref,
      @RequestBody FollowupService.Input input) {
    return FollowupView.of(followups.record(companyId, ref, input));
  }

  /**
   * The follow-ups of a renewal.
   *
   * @param companyId company
   * @param ref renewal
   * @return follow-ups, newest first
   */
  @GetMapping("/followups")
  @PreAuthorize("hasAuthority('RNW_VIEW')")
  public List<FollowupView> followups(@RequestParam Long companyId, @PathVariable String ref) {
    return followups.of(companyId, ref).stream().map(FollowupView::of).toList();
  }

  /**
   * An acceptance.
   *
   * @param method method
   * @param attachmentId evidence file
   * @param reference evidence reference
   * @param acceptedOn date
   * @param source USER, UPLOAD or SYSTEM
   * @param financialImpactAck changed terms acknowledged
   * @param remarks remarks
   * @param by user
   * @param at time
   */
  public record AcceptanceView(
      String method,
      Long attachmentId,
      String reference,
      LocalDate acceptedOn,
      String source,
      boolean financialImpactAck,
      String remarks,
      String by,
      Instant at) {

    static AcceptanceView of(RenewalAcceptance a) {
      return new AcceptanceView(
          a.getMethod().name(),
          a.getAttachmentId(),
          a.getEvidenceRef(),
          a.getAcceptedOn(),
          a.getSource(),
          a.isFinancialImpactAck(),
          a.getRemarks(),
          a.getCreatedBy(),
          a.getCreatedAt());
    }
  }

  /**
   * A follow-up.
   *
   * @param channel channel
   * @param outcome outcome
   * @param remarks remarks
   * @param nextActionDate next action
   * @param by user
   * @param at time
   */
  public record FollowupView(
      String channel,
      String outcome,
      String remarks,
      LocalDate nextActionDate,
      String by,
      Instant at) {

    static FollowupView of(RenewalFollowup f) {
      return new FollowupView(
          f.getChannel(),
          f.getOutcome(),
          f.getRemarks(),
          f.getNextActionDate(),
          f.getCreatedBy(),
          f.getCreatedAt());
    }
  }
}
