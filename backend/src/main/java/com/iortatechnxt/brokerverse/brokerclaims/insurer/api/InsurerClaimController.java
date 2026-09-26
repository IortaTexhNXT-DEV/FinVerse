package com.iortatechnxt.brokerverse.brokerclaims.insurer.api;

import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimQueryService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto.InsurerDtos.AdjusterRequest;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto.InsurerDtos.InsurerLineRequest;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto.InsurerDtos.InsurerLineResponse;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto.InsurerDtos.NumberRequest;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto.InsurerDtos.ReserveChangeResponse;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto.InsurerDtos.ReserveRequest;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto.InsurerDtos.ShareRequest;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerClaim;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerClaimService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The insurers of a claim (BRCLM.018/023/024/043; FR-CL-021/031/032): insurer lines with their
 * names, add a line or an insurer claim number ({@code BCL_RECORD}), change a share, amend the
 * reserve ({@code BCL_RESERVE_AMEND}), assign the adjuster of a line ({@code BCL_ADJUSTER_ASSIGN})
 * and the reserve history. Every answer is the claim's lines after the change.
 */
@RestController
@RequestMapping("/api/v1/broker-claims/{claimId}/insurers")
public class InsurerClaimController {

  private static final String RECORD = "hasAuthority('BCL_RECORD')";

  private final ClaimQueryService claims;
  private final InsurerClaimService lines;
  private final InsurerService insurers;
  private final LovService lovs;

  /**
   * Creates the controller.
   *
   * @param claims claims
   * @param lines insurer lines
   * @param insurers insurer names
   * @param lovs adjuster labels
   */
  public InsurerClaimController(
      ClaimQueryService claims,
      InsurerClaimService lines,
      InsurerService insurers,
      LovService lovs) {
    this.claims = claims;
    this.lines = lines;
    this.insurers = insurers;
    this.lovs = lovs;
  }

  /**
   * The insurer lines of a claim.
   *
   * @param claimId claim
   * @param companyId company
   * @return lines
   */
  @GetMapping
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public List<InsurerLineResponse> list(@PathVariable Long claimId, @RequestParam Long companyId) {
    return responses(claims.require(companyId, claimId));
  }

  /**
   * The reserve history of the claim's lines.
   *
   * @param claimId claim
   * @param companyId company
   * @return amendments, newest first
   */
  @GetMapping("/reserve-history")
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public List<ReserveChangeResponse> reserveHistory(
      @PathVariable Long claimId, @RequestParam Long companyId) {
    Claim claim = claims.require(companyId, claimId);
    List<Long> ids = lines.ofClaim(claim.getId()).stream().map(InsurerClaim::getId).toList();
    return lines.reserveHistory(ids).stream().map(ReserveChangeResponse::from).toList();
  }

  /**
   * Adds an insurer line.
   *
   * @param claimId claim
   * @param companyId company
   * @param confirmReuse confirmation of a number found on another claim
   * @param request line
   * @return lines
   */
  @PostMapping
  @PreAuthorize(RECORD)
  public List<InsurerLineResponse> add(
      @PathVariable Long claimId,
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "false") boolean confirmReuse,
      @Valid @RequestBody InsurerLineRequest request) {
    Claim claim = claims.requireOpen(companyId, claimId);
    lines.add(claim, request.toLine(), confirmReuse);
    return responses(claim);
  }

  /**
   * Records an insurer claim number on a line.
   *
   * @param claimId claim
   * @param lineId line
   * @param companyId company
   * @param request number, date reported and confirmation
   * @return lines
   */
  @PostMapping("/{lineId}/number")
  @PreAuthorize(RECORD)
  public List<InsurerLineResponse> number(
      @PathVariable Long claimId,
      @PathVariable Long lineId,
      @RequestParam Long companyId,
      @Valid @RequestBody NumberRequest request) {
    Claim claim = claims.requireOpen(companyId, claimId);
    lines.number(
        claim,
        lineId,
        request.insurerClaimNo(),
        request.reportedToInsurerOn(),
        request.confirmReuse());
    return responses(claim);
  }

  /**
   * Changes the share of a line.
   *
   * @param claimId claim
   * @param lineId line
   * @param companyId company
   * @param request share
   * @return lines
   */
  @PutMapping("/{lineId}/share")
  @PreAuthorize(RECORD)
  public List<InsurerLineResponse> share(
      @PathVariable Long claimId,
      @PathVariable Long lineId,
      @RequestParam Long companyId,
      @RequestBody ShareRequest request) {
    Claim claim = claims.requireOpen(companyId, claimId);
    lines.changeShare(claim, lineId, request.sharePct());
    return responses(claim);
  }

  /**
   * Amends the insurer reserve of a line.
   *
   * @param claimId claim
   * @param lineId line
   * @param companyId company
   * @param request amount and reason
   * @return lines
   */
  @PostMapping("/{lineId}/reserve")
  @PreAuthorize("hasAuthority('BCL_RESERVE_AMEND')")
  public List<InsurerLineResponse> reserve(
      @PathVariable Long claimId,
      @PathVariable Long lineId,
      @RequestParam Long companyId,
      @Valid @RequestBody ReserveRequest request) {
    Claim claim = claims.requireOpen(companyId, claimId);
    lines.amendReserve(claim, lineId, request.amount(), request.reason());
    return responses(claim);
  }

  /**
   * Assigns the adjuster of a line.
   *
   * @param claimId claim
   * @param lineId line
   * @param companyId company
   * @param request adjuster
   * @return lines
   */
  @PostMapping("/{lineId}/adjuster")
  @PreAuthorize("hasAuthority('BCL_ADJUSTER_ASSIGN')")
  public List<InsurerLineResponse> adjuster(
      @PathVariable Long claimId,
      @PathVariable Long lineId,
      @RequestParam Long companyId,
      @RequestBody AdjusterRequest request) {
    Claim claim = claims.requireOpen(companyId, claimId);
    lines.assignAdjuster(claim, lineId, request.adjusterCode());
    return responses(claim);
  }

  private List<InsurerLineResponse> responses(Claim claim) {
    List<InsurerClaim> all = lines.ofClaim(claim.getId());
    Map<String, String> names =
        insurers.insurers(claim.getCompanyId()).stream()
            .collect(
                Collectors.toMap(
                    InsurerProfile::getPartyCode, InsurerProfile::getName, (a, b) -> a));
    Map<String, String> adjusters = new HashMap<>();
    all.stream()
        .map(InsurerClaim::getAdjusterCode)
        .filter(Objects::nonNull)
        .distinct()
        .forEach(code -> adjusters.put(code, lovs.label(ClaimCodes.LOV_ADJUSTER, code)));
    return all.stream().map(l -> InsurerLineResponse.from(l, names, adjusters)).toList();
  }
}
