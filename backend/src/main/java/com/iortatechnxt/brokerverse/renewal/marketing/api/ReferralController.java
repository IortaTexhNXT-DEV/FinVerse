package com.iortatechnxt.brokerverse.renewal.marketing.api;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.renewal.domain.ReferralStatus;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalReferral;
import com.iortatechnxt.brokerverse.renewal.marketing.service.ReferralNewBusiness;
import com.iortatechnxt.brokerverse.renewal.marketing.service.ReferralService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.TransferModes;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transfer requests for a New Business opportunity and the Transfer Request Monitoring screen (BDOI
 * Renewal FRS FRRN.011.01 to FRRN.011.05).
 */
@RestController
@RequestMapping("/api/v1/renewal/referrals")
@PreAuthorize("hasAnyAuthority('RNW_ASSIGN','RNW_DISPOSE')")
public class ReferralController {

  private static final String RECEIVER = "hasAuthority('RNW_ASSIGN')";

  private final ReferralService referrals;
  private final ReferralNewBusiness newBusiness;
  private final TransferModes modes;
  private final RenewalCandidateRepository candidates;

  /**
   * Creates the controller.
   *
   * @param referrals transfer requests
   * @param newBusiness New Business accounts of the requests
   * @param modes referral and transfer in use
   * @param candidates renewal accounts (list columns)
   */
  public ReferralController(
      ReferralService referrals,
      ReferralNewBusiness newBusiness,
      TransferModes modes,
      RenewalCandidateRepository candidates) {
    this.referrals = referrals;
    this.newBusiness = newBusiness;
    this.modes = modes;
    this.candidates = candidates;
  }

  /**
   * Whether the referral and the ownership transfer are in use.
   *
   * @param companyId company
   * @return the two switches
   */
  @GetMapping("/modes")
  public Map<String, Boolean> modes(@RequestParam Long companyId) {
    return Map.of("referral", modes.referral(), "transfer", modes.transfer());
  }

  /**
   * Transfer Request Monitoring.
   *
   * @param companyId company
   * @return requests, newest first
   */
  @GetMapping
  @Transactional(readOnly = true)
  public List<ReferralView> list(@RequestParam Long companyId) {
    return referrals.list(companyId).stream().map(this::view).toList();
  }

  /**
   * The requests of a renewal account.
   *
   * @param companyId company
   * @param renewalRef renewal account
   * @return requests
   */
  @GetMapping("/of/{renewalRef}")
  @Transactional(readOnly = true)
  public List<ReferralView> ofRenewal(
      @RequestParam Long companyId, @PathVariable String renewalRef) {
    return referrals.ofRenewal(companyId, renewalRef).stream().map(this::view).toList();
  }

  /**
   * Creates a request (Transfer to Other Unit).
   *
   * @param companyId company
   * @param body renewal account, receiving unit, justification, submit or save as draft
   * @return the request
   */
  @PostMapping
  public ReferralView request(@RequestParam Long companyId, @RequestBody ReferralBody body) {
    return view(
        referrals.request(
            companyId,
            body.renewalRef(),
            new ReferralService.Request(body.toUnit(), body.justification(), body.submit())));
  }

  /**
   * Submits a draft or a request returned for clarification.
   *
   * @param companyId company
   * @param id request
   * @param body justification
   * @return the request
   */
  @PostMapping("/{id}/submit")
  public ReferralView submit(
      @RequestParam Long companyId, @PathVariable Long id, @RequestBody TextBody body) {
    return view(referrals.submit(companyId, id, body.text()));
  }

  /**
   * Accepts, rejects or returns a request (the receiving unit).
   *
   * @param companyId company
   * @param id request
   * @param body outcome and remarks
   * @return the request
   */
  @PostMapping("/{id}/decision")
  @PreAuthorize(RECEIVER)
  public ReferralView decide(
      @RequestParam Long companyId, @PathVariable Long id, @RequestBody DecisionBody body) {
    return view(
        referrals.decide(
            companyId, id, new ReferralService.Decision(body.outcome(), body.remarks())));
  }

  /**
   * Withdraws a request (its requester).
   *
   * @param companyId company
   * @param id request
   * @return the request
   */
  @PostMapping("/{id}/cancel")
  public ReferralView cancel(@RequestParam Long companyId, @PathVariable Long id) {
    return view(referrals.cancel(companyId, id));
  }

  /**
   * The data copied to the New Business account, to review before saving.
   *
   * @param companyId company
   * @param id accepted request
   * @return proposed data
   */
  @GetMapping("/{id}/new-business")
  @PreAuthorize(RECEIVER)
  public ReferralNewBusiness.NewBusinessInput proposal(
      @RequestParam Long companyId, @PathVariable Long id) {
    return newBusiness.proposal(companyId, id);
  }

  /**
   * Creates the New Business account of an accepted request.
   *
   * @param companyId company
   * @param id accepted request
   * @param input reviewed data
   * @return the request with the New Business account
   */
  @PostMapping("/{id}/new-business")
  @PreAuthorize(RECEIVER)
  public ReferralView createNewBusiness(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @RequestBody ReferralNewBusiness.NewBusinessInput input) {
    Account account = newBusiness.createNewBusiness(companyId, id, input);
    return referrals.list(companyId).stream()
        .filter(r -> account.getArn().equals(r.getNbArn()))
        .findFirst()
        .map(this::view)
        .orElseThrow();
  }

  private ReferralView view(RenewalReferral r) {
    RenewalCandidate c = candidates.findById(r.getCandidateId()).orElseThrow();
    return new ReferralView(
        r.getId(),
        r.getReferralNo(),
        c.getRenewalRef(),
        c.getSnapshot().client().assuredName() == null
            ? c.getSnapshot().clientName()
            : c.getSnapshot().client().assuredName(),
        r.getFromUnit(),
        r.getToUnit(),
        r.getJustification(),
        r.getStatus().name(),
        r.getStatus().label(),
        r.getCreatedBy(),
        r.getCreatedAt(),
        r.getDecidedBy(),
        r.getDecidedAt(),
        r.getDecisionRemarks(),
        r.getNbArn(),
        r.getNbCreatedAt());
  }

  /**
   * A new request.
   *
   * @param renewalRef renewal account
   * @param toUnit receiving Marketing unit
   * @param justification business justification
   * @param submit true to submit, false to save as Draft
   */
  public record ReferralBody(
      String renewalRef, String toUnit, String justification, boolean submit) {}

  /**
   * A justification.
   *
   * @param text text
   */
  public record TextBody(String text) {}

  /**
   * A decision.
   *
   * @param outcome ACCEPTED, REJECTED or RETURNED
   * @param remarks remarks
   */
  public record DecisionBody(ReferralStatus outcome, String remarks) {}

  /**
   * A request as listed.
   *
   * @param id id
   * @param referralNo Transfer Request Number
   * @param renewalRef renewal account
   * @param assuredName assured's name
   * @param fromUnit source Marketing unit
   * @param toUnit receiving Marketing unit
   * @param justification justification
   * @param status status code
   * @param statusLabel status
   * @param requestedBy requester
   * @param requestedAt request date
   * @param decidedBy accepting or deciding user
   * @param decidedAt decision time
   * @param decisionRemarks remarks of the decision
   * @param nbArn New Business account
   * @param nbCreatedAt New Business account creation time
   */
  public record ReferralView(
      Long id,
      String referralNo,
      String renewalRef,
      String assuredName,
      String fromUnit,
      String toUnit,
      String justification,
      String status,
      String statusLabel,
      String requestedBy,
      Instant requestedAt,
      String decidedBy,
      Instant decidedAt,
      String decisionRemarks,
      String nbArn,
      Instant nbCreatedAt) {}
}
