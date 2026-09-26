package com.iortatechnxt.brokerverse.brokerclaims.location.api;

import com.iortatechnxt.brokerverse.brokerclaims.claim.service.BrokerClaimQueryService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.location.api.dto.LocationDtos.ClaimLocationResponse;
import com.iortatechnxt.brokerverse.brokerclaims.location.api.dto.LocationDtos.DescribeRequest;
import com.iortatechnxt.brokerverse.brokerclaims.location.api.dto.LocationDtos.LinkRequest;
import com.iortatechnxt.brokerverse.brokerclaims.location.api.dto.LocationDtos.LocationPickRequest;
import com.iortatechnxt.brokerverse.brokerclaims.location.domain.LocationRef;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.ClaimLocationService;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.LocationRefService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The locations of a claim (BRCLM.037/042; FR-CM-020): list with the insurer references valid
 * today, link locations of the cover, describe and remove them ({@code BCL_RECORD}).
 */
@RestController
@RequestMapping("/api/v1/broker-claims/{claimId}/locations")
public class ClaimLocationController {

  private static final String RECORD = "hasAuthority('BCL_RECORD')";

  private final BrokerClaimQueryService claims;
  private final ClaimLocationService locations;
  private final LocationRefService refs;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param claims claims
   * @param locations claim locations
   * @param refs insurer references
   * @param clock clock
   */
  public ClaimLocationController(
      BrokerClaimQueryService claims,
      ClaimLocationService locations,
      LocationRefService refs,
      Clock clock) {
    this.claims = claims;
    this.locations = locations;
    this.refs = refs;
    this.clock = clock;
  }

  /**
   * The locations of a claim with their insurer references.
   *
   * @param claimId claim
   * @param companyId company
   * @return locations
   */
  @GetMapping
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public List<ClaimLocationResponse> list(
      @PathVariable Long claimId, @RequestParam Long companyId) {
    return responses(claims.require(companyId, claimId));
  }

  /**
   * Links locations of the cover.
   *
   * @param claimId claim
   * @param companyId company
   * @param request locations
   * @return the claim's locations
   */
  @PostMapping
  @PreAuthorize(RECORD)
  public List<ClaimLocationResponse> link(
      @PathVariable Long claimId,
      @RequestParam Long companyId,
      @Valid @RequestBody LinkRequest request) {
    Claim claim = claims.requireOpen(companyId, claimId);
    locations.link(claim, request.locations().stream().map(LocationPickRequest::toPick).toList());
    return responses(claim);
  }

  /**
   * Changes the damage description of a location.
   *
   * @param claimId claim
   * @param itemNo item number
   * @param companyId company
   * @param request description
   * @return the claim's locations
   */
  @PutMapping("/{itemNo}")
  @PreAuthorize(RECORD)
  public List<ClaimLocationResponse> describe(
      @PathVariable Long claimId,
      @PathVariable int itemNo,
      @RequestParam Long companyId,
      @Valid @RequestBody DescribeRequest request) {
    Claim claim = claims.requireOpen(companyId, claimId);
    locations.describe(claim, itemNo, request.description());
    return responses(claim);
  }

  /**
   * Removes a location from the claim.
   *
   * @param claimId claim
   * @param itemNo item number
   * @param companyId company
   * @return the claim's locations
   */
  @DeleteMapping("/{itemNo}")
  @PreAuthorize(RECORD)
  public List<ClaimLocationResponse> remove(
      @PathVariable Long claimId, @PathVariable int itemNo, @RequestParam Long companyId) {
    Claim claim = claims.requireOpen(companyId, claimId);
    locations.remove(claim, itemNo);
    return responses(claim);
  }

  private List<ClaimLocationResponse> responses(Claim claim) {
    List<LocationRef> valid =
        refs.validOn(claim.getCompanyId(), claim.getCover().getArn(), LocalDate.now(clock));
    return locations.ofClaim(claim.getId()).stream()
        .map(l -> ClaimLocationResponse.from(l, valid))
        .toList();
  }
}
