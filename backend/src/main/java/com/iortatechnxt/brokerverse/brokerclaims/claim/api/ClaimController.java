package com.iortatechnxt.brokerverse.brokerclaims.claim.api;

import com.iortatechnxt.brokerverse.brokerclaims.claim.api.dto.ClaimRequests.AuthorizeRequest;
import com.iortatechnxt.brokerverse.brokerclaims.claim.api.dto.ClaimRequests.ClaimantRequest;
import com.iortatechnxt.brokerverse.brokerclaims.claim.api.dto.ClaimRequests.LossRequest;
import com.iortatechnxt.brokerverse.brokerclaims.claim.api.dto.ClaimRequests.RecordClaimRequest;
import com.iortatechnxt.brokerverse.brokerclaims.claim.api.dto.ClaimRequests.ReportedDateRequest;
import com.iortatechnxt.brokerverse.brokerclaims.claim.api.dto.ClaimResponse;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimDetailsService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimQueryService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimRecordingService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimViewService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.PremiumCheckService;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.CoverClaimDto;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.PremiumDto;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The claim record (BRCLM.001/003/004/006/016/036/039; FR-CL-011..016/030/033): record a claim on a
 * cover ({@code BCL_RECORD}), read it ({@code BCL_VIEW}), change its loss data, correct the
 * reported date ({@code BCL_STATUS_UPDATE}), override the claimant ({@code BCL_CLAIMANT_OVERRIDE}),
 * refresh the cover data, use the latest cover version, re-check the premium and generate the
 * claims authorization code ({@code BCL_AUTHORIZE}). Every call is scoped to the company.
 */
@RestController
@RequestMapping("/api/v1/broker-claims")
public class ClaimController {

  private static final String RECORD = "hasAuthority('BCL_RECORD')";

  private final ClaimRecordingService recording;
  private final ClaimViewService views;
  private final ClaimQueryService claims;
  private final ClaimDetailsService details;
  private final PremiumCheckService premiums;

  /**
   * Creates the controller.
   *
   * @param recording recording
   * @param views claim views
   * @param claims claim reads
   * @param details detail changes
   * @param premiums premium check and authorization
   */
  public ClaimController(
      ClaimRecordingService recording,
      ClaimViewService views,
      ClaimQueryService claims,
      ClaimDetailsService details,
      PremiumCheckService premiums) {
    this.recording = recording;
    this.views = views;
    this.claims = claims;
    this.details = details;
    this.premiums = premiums;
  }

  /**
   * Records a claim.
   *
   * @param request cover, loss, locations and insurers
   * @return the claim
   */
  @PostMapping
  @PreAuthorize(RECORD)
  @ResponseStatus(HttpStatus.CREATED)
  public ClaimResponse record(@Valid @RequestBody RecordClaimRequest request) {
    Claim claim = recording.record(request.companyId(), request.toClaim());
    return view(request.companyId(), claim.getId());
  }

  /**
   * Claims by number, ARN, policy number or assured.
   *
   * @param companyId company
   * @param q text
   * @return claims, newest first
   */
  @GetMapping("/search")
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public List<CoverClaimDto> search(
      @RequestParam Long companyId, @RequestParam(required = false) String q) {
    return claims.search(companyId, q).stream().map(CoverClaimDto::from).toList();
  }

  /**
   * One claim.
   *
   * @param id claim
   * @param companyId company
   * @return the claim
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public ClaimResponse get(@PathVariable Long id, @RequestParam Long companyId) {
    return view(companyId, id);
  }

  /**
   * Changes the loss data.
   *
   * @param id claim
   * @param companyId company
   * @param request loss data
   * @return the claim
   */
  @PutMapping("/{id}/loss")
  @PreAuthorize(RECORD)
  public ClaimResponse amendLoss(
      @PathVariable Long id, @RequestParam Long companyId, @Valid @RequestBody LossRequest request) {
    details.amendLoss(companyId, id, request.toLoss(), request.toAmounts());
    return view(companyId, id);
  }

  /**
   * Corrects the reported date.
   *
   * @param id claim
   * @param companyId company
   * @param request date, reason and remark
   * @return the claim
   */
  @PostMapping("/{id}/reported-date")
  @PreAuthorize("hasAuthority('BCL_STATUS_UPDATE')")
  public ClaimResponse correctReportedDate(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @Valid @RequestBody ReportedDateRequest request) {
    details.correctReportedDate(
        companyId, id, request.reportedDate(), request.reason(), request.remark());
    return view(companyId, id);
  }

  /**
   * Overrides the claimant.
   *
   * @param id claim
   * @param companyId company
   * @param request name and reason
   * @return the claim
   */
  @PostMapping("/{id}/claimant")
  @PreAuthorize("hasAuthority('BCL_CLAIMANT_OVERRIDE')")
  public ClaimResponse overrideClaimant(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @Valid @RequestBody ClaimantRequest request) {
    details.overrideClaimant(companyId, id, request.claimantName(), request.reason());
    return view(companyId, id);
  }

  /**
   * Reloads the cover data from the account.
   *
   * @param id claim
   * @param companyId company
   * @return the changes made
   */
  @PostMapping("/{id}/refresh-cover")
  @PreAuthorize(RECORD)
  public List<String> refreshCover(@PathVariable Long id, @RequestParam Long companyId) {
    return details.refreshCover(companyId, id);
  }

  /**
   * Switches to the latest cover version.
   *
   * @param id claim
   * @param companyId company
   * @return the claim
   */
  @PostMapping("/{id}/latest-version")
  @PreAuthorize(RECORD)
  public ClaimResponse useLatestVersion(@PathVariable Long id, @RequestParam Long companyId) {
    details.useLatestVersion(companyId, id);
    return view(companyId, id);
  }

  /**
   * Checks the premium again and records the result.
   *
   * @param id claim
   * @param companyId company
   * @return the result
   */
  @PostMapping("/{id}/premium-check")
  @PreAuthorize("hasAnyAuthority('BCL_RECORD', 'BCL_AUTHORIZE')")
  public PremiumDto premiumCheck(@PathVariable Long id, @RequestParam Long companyId) {
    return PremiumDto.from(premiums.recheck(companyId, id));
  }

  /**
   * Generates the claims authorization code.
   *
   * @param id claim
   * @param companyId company
   * @param request insurer payment evidence (direct payment)
   * @return the claim
   */
  @PostMapping("/{id}/authorize")
  @PreAuthorize("hasAuthority('BCL_AUTHORIZE')")
  public ClaimResponse authorize(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @RequestBody(required = false) AuthorizeRequest request) {
    premiums.authorize(companyId, id, request == null ? null : request.evidenceAttachmentId());
    return view(companyId, id);
  }

  private ClaimResponse view(Long companyId, Long id) {
    return ClaimResponse.from(views.view(companyId, id));
  }
}
