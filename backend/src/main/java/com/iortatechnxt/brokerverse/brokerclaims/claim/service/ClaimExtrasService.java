package com.iortatechnxt.brokerverse.brokerclaims.claim.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimExtras;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPremiumStatus;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimEvent;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimField;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimEventRecorder;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyRepository;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Details of a claim of the FRS BRD-07 Claims v1.0 in the BRD scope: the plate number and the
 * preferred repair shop of a motor claim (FRCLM.008.02, FRCLM.007.02: the accredited repair shops
 * are the active parties of type Garage), and the return of the claim of a partly paid or unpaid
 * policy to the Account Officer for the collection of the premium (FRCLM.008.04).
 */
@Service
@Transactional
public class ClaimExtrasService {

  private static final int MAX_PLATE = 20;
  private static final Set<ClaimPremiumStatus> UNPAID =
      Set.of(ClaimPremiumStatus.UNPAID, ClaimPremiumStatus.PARTIALLY_PAID);

  private final BrokerClaimQueryService claims;
  private final PartyRepository parties;
  private final ClaimEventRecorder recorder;
  private final NotificationService notifications;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param claims claims
   * @param parties repair shops
   * @param recorder claim timeline
   * @param notifications notice to the Account Officer
   * @param currentUser signed-in user
   * @param clock clock
   */
  public ClaimExtrasService(
      BrokerClaimQueryService claims,
      PartyRepository parties,
      ClaimEventRecorder recorder,
      NotificationService notifications,
      CurrentUser currentUser,
      Clock clock) {
    this.claims = claims;
    this.parties = parties;
    this.recorder = recorder;
    this.notifications = notifications;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The accredited repair shops: the active parties of type Garage.
   *
   * @param companyId company
   * @return shops by name
   */
  @Transactional(readOnly = true)
  public List<Party> repairShops(Long companyId) {
    return parties
        .findByCompanyIdAndPartyTypeInOrderByName(companyId, Set.of(PartyType.GARAGE))
        .stream()
        .filter(Party::isActive)
        .toList();
  }

  /**
   * Sets the plate number and the preferred repair shop of a claim.
   *
   * @param companyId company
   * @param claimId claim
   * @param plateNo plate number, blank clears it
   * @param shopCode repair shop party code, blank clears it
   * @return the claim
   */
  public Claim motor(Long companyId, Long claimId, String plateNo, String shopCode) {
    Claim claim = claims.requireOpen(companyId, claimId);
    String plate = blankToNull(plateNo);
    if (plate != null && plate.length() > MAX_PLATE) {
      throw new BusinessRuleException(
          "BCL_PLATE_INVALID", "The plate number can have up to 20 characters");
    }
    String shop = blankToNull(shopCode);
    if (shop != null) {
      String code = shop;
      Party party =
          parties
              .findByCompanyIdAndCode(companyId, code)
              .filter(p -> p.getPartyType() == PartyType.GARAGE && p.isActive())
              .orElseThrow(
                  () ->
                      new BusinessRuleException(
                          "BCL_SHOP_INVALID", code + " is not an active accredited repair shop"));
      shop = party.getCode();
    }
    ClaimExtras extras = claim.getExtras();
    String before = describe(extras.getPlateNo(), extras.getPreferredShopCode());
    extras.motor(plate, shop);
    String after = describe(plate, shop);
    if (!Objects.equals(before, after)) {
      recorder.record(claim, ClaimField.MOTOR, new ClaimEvent.Values(before, after), null);
    }
    return claim;
  }

  /**
   * Returns the claim of a partly paid or unpaid policy to the Account Officer of the cover, who is
   * notified to collect the premium; the authorization code stays disabled until it is paid.
   *
   * @param companyId company
   * @param claimId claim
   * @param note note to the Account Officer
   * @return the claim
   */
  public Claim returnToAccountOfficer(Long companyId, Long claimId, String note) {
    Claim claim = claims.requireOpen(companyId, claimId);
    String officer = requireReturnable(claim);
    String text = blankToNull(note);
    if (text != null && text.length() > ClaimExtras.MAX_RETURN_NOTE) {
      throw new BusinessRuleException(
          "BCL_RETURN_NOTE_TOO_LONG", "The note can have up to 500 characters");
    }
    claim.getExtras().returnedToAccountOfficer(currentUser.username(), clock.instant(), text);
    recorder.record(
        claim,
        ClaimField.RETURNED_TO_AO,
        new ClaimEvent.Values(null, officer),
        text == null ? "Premium to collect" : text);
    notifications.notifyUser(
        officer,
        new Notice(
            "Claim " + claim.getClaimNo() + " returned for premium collection",
            "The premium of "
                + claim.getCover().getPolicyNo()
                + " is "
                + claim.getCover().getPremiumStatus()
                + "; collect it so the claim can proceed."
                + (text == null ? "" : " " + text),
            "/claims-handling/" + claim.getId(),
            ClaimCodes.ENTITY_TYPE,
            claim.getId().toString()));
    return claim;
  }

  private static String requireReturnable(Claim claim) {
    ClaimPremiumStatus premium = claim.getCover().getPremiumStatus();
    if (premium == null || !UNPAID.contains(premium)) {
      throw new BusinessRuleException(
          "BCL_RETURN_NOT_UNPAID",
          "Only the claim of a partly paid or unpaid policy is returned to the Account Officer");
    }
    String officer = claim.getCover().getAccountOfficer();
    if (officer == null || officer.isBlank()) {
      throw new BusinessRuleException(
          "BCL_RETURN_NO_OFFICER", "The cover of the claim has no Account Officer");
    }
    return officer;
  }

  private static String describe(String plate, String shop) {
    if (plate == null && shop == null) {
      return null;
    }
    return "Plate " + (plate == null ? "-" : plate) + ", shop " + (shop == null ? "-" : shop);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
