package com.iortatechnxt.brokerverse.brokerclaims.location.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.CoverService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.location.domain.ClaimLocation;
import com.iortatechnxt.brokerverse.brokerclaims.location.domain.ClaimLocationRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The insured locations of a claim (BRCLM.037; FR-CL-020): several locations of the claim's own
 * cover under one claim reference, each linked once with a snapshot of its address and location key
 * and the damage there. Links and removals are allowed while the claim is open and are audited on
 * the claim.
 */
@Service
@Transactional
public class ClaimLocationService {

  private final ClaimLocationRepository locations;
  private final CoverService covers;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param locations claim locations
   * @param covers covers
   * @param audit audit trail
   */
  public ClaimLocationService(
      ClaimLocationRepository locations, CoverService covers, AuditTrailService audit) {
    this.locations = locations;
    this.covers = covers;
    this.audit = audit;
  }

  /**
   * Links locations of the claim's cover (R1, R2).
   *
   * @param claim open claim
   * @param picks items and damage descriptions
   * @return the new links
   */
  public List<ClaimLocation> link(Claim claim, List<LocationPick> picks) {
    if (picks.isEmpty()) {
      return List.of();
    }
    Account account = covers.account(claim.getCompanyId(), claim.getCover().getArn());
    Set<Integer> seen = new HashSet<>();
    return picks.stream()
        .map(
            pick -> {
              RiskItem item = LocationRefService.requireLocation(account, pick.itemNo());
              if (!seen.add(pick.itemNo())
                  || locations
                      .findByClaimIdAndAccountItemNo(claim.getId(), pick.itemNo())
                      .isPresent()) {
                throw new BusinessRuleException(
                    "BCL_LOCATION_LINKED",
                    "Location " + item.label() + " is already on this claim");
              }
              ClaimLocation saved =
                  locations.save(
                      new ClaimLocation(claim.getId(), place(item), blankToNull(pick.description())));
              audit.record(
                  ClaimCodes.ENTITY_TYPE,
                  claim.getClaimNo(),
                  AuditAction.UPDATE,
                  "Location " + item.getItemNo() + " linked: " + item.label());
              return saved;
            })
        .toList();
  }

  /**
   * Removes a location from an open claim; the removal is audited (FR-CL-020).
   *
   * @param claim open claim
   * @param itemNo item number
   */
  public void remove(Claim claim, int itemNo) {
    ClaimLocation location = require(claim, itemNo);
    locations.delete(location);
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.UPDATE,
        "Location " + itemNo + " removed: " + location.getAddress());
  }

  /**
   * Changes the damage description of a linked location.
   *
   * @param claim open claim
   * @param itemNo item number
   * @param description description
   * @return the location
   */
  public ClaimLocation describe(Claim claim, int itemNo, String description) {
    ClaimLocation location = require(claim, itemNo);
    location.describe(blankToNull(description));
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.UPDATE,
        "Location " + itemNo + " damage: " + description);
    return location;
  }

  /**
   * The locations of a claim.
   *
   * @param claimId claim
   * @return locations by item number
   */
  @Transactional(readOnly = true)
  public List<ClaimLocation> ofClaim(Long claimId) {
    return locations.findByClaimIdOrderByAccountItemNoAsc(claimId);
  }

  private ClaimLocation require(Claim claim, int itemNo) {
    return locations
        .findByClaimIdAndAccountItemNo(claim.getId(), itemNo)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "BCL_LOCATION_NOT_LINKED", "Location " + itemNo + " is not on this claim"));
  }

  private static ClaimLocation.Place place(RiskItem item) {
    return new ClaimLocation.Place(
        item.getItemNo(),
        item.location().address(),
        item.location().city(),
        item.location().province(),
        item.getLocationKey());
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * A location of the cover to link.
   *
   * @param itemNo item number on the account
   * @param description damage at the location, may be null
   */
  public record LocationPick(int itemNo, String description) {}
}
