package com.iortatechnxt.brokerverse.brokerclaims.claim.service;

import com.iortatechnxt.brokerverse.brokerclaims.cover.service.CoverService;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.PremiumRule.PremiumCheck;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimLovAttribute;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimLovAttributeRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.CoverSnapshot;
import com.iortatechnxt.brokerverse.brokerclaims.domain.LossDetails;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerClaim;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerClaimService;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.ClaimLocationService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The claim record as the screen shows it (BRCLM.001/016/037/039/043; design 11): the claim with
 * its live premium check and the invoices not fully paid, the latest cover version, the flags
 * (unpaid premium, awaiting premium remittance, newer cover version, multi-location, multi-insurer,
 * CAT, claimant overridden), the total insurer reserve and the labels of its codes.
 */
@Service
@Transactional(readOnly = true)
public class ClaimViewService {

  private static final Set<RemittanceStatus> SETTLED =
      Set.of(RemittanceStatus.FULLY_REMITTED, RemittanceStatus.NOT_APPLICABLE);

  private final BrokerClaimQueryService claims;
  private final CoverService covers;
  private final ClaimLocationService locations;
  private final InsurerClaimService insurers;
  private final ClaimLovAttributeRepository attributes;
  private final LovService lovs;
  private final SystemParameterService parameters;

  /**
   * Creates the service.
   *
   * @param claims claims
   * @param covers premium check and versions
   * @param locations claim locations
   * @param insurers insurer lines
   * @param attributes status attributes
   * @param lovs labels
   * @param parameters direct-payment policy
   */
  public ClaimViewService(
      BrokerClaimQueryService claims,
      CoverService covers,
      ClaimLocationService locations,
      InsurerClaimService insurers,
      ClaimLovAttributeRepository attributes,
      LovService lovs,
      SystemParameterService parameters) {
    this.claims = claims;
    this.covers = covers;
    this.locations = locations;
    this.insurers = insurers;
    this.attributes = attributes;
    this.lovs = lovs;
    this.parameters = parameters;
  }

  /**
   * One claim with everything the record page shows in its header.
   *
   * @param companyId company
   * @param claimId claim
   * @return the view
   */
  public ClaimView view(Long companyId, Long claimId) {
    Claim claim = claims.require(companyId, claimId);
    CoverSnapshot cover = claim.getCover();
    PremiumCheck premium = covers.premium(cover.getArn(), cover.getPolicyYear());
    int latest = covers.version(cover.getArn(), cover.getPolicyYear(), null).no();
    List<InsurerClaim> lines = insurers.ofClaim(claim.getId());
    String status = claim.getProgress().getStatusCode();
    boolean awaiting =
        status != null
            && attributes
                .findByTypeCodeAndCodeAndAttribute(
                    ClaimCodes.LOV_STATUS, status, ClaimCodes.ATTR_AWAITING_PREMIUM_REMITTANCE)
                .map(ClaimLovAttribute::isTrue)
                .orElse(false);
    BigDecimal reserve =
        lines.stream()
            .map(InsurerClaim::getReserveAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    List<String> unremitted =
        covers.invoices(cover.getArn(), cover.getPolicyYear()).stream()
            .filter(i -> !i.isCancelled() && !SETTLED.contains(i.getRemittanceStatus()))
            .map(OpsInvoice::getInvoiceNo)
            .toList();
    return new ClaimView(
        claim,
        premium,
        latest,
        awaiting && !unremitted.isEmpty(),
        locations.ofClaim(claim.getId()).size(),
        (int) lines.stream().map(InsurerClaim::getInsurerCode).distinct().count(),
        reserve,
        labels(claim),
        parameters.text(ClaimCodes.PARAM_AUTH_DP_POLICY, "CONFIRM"),
        unremitted);
  }

  private Map<String, String> labels(Claim claim) {
    LossDetails loss = claim.getLoss();
    Map<String, String> labels = new HashMap<>();
    put(labels, ClaimCodes.LOV_LOSS_NATURE, loss.getLossNature());
    put(labels, ClaimCodes.LOV_CLAIM_TYPE, loss.getClaimType());
    put(labels, ClaimCodes.LOV_CATASTROPHE, loss.getCatastropheCode());
    put(labels, ClaimCodes.LOV_STATUS, claim.getProgress().getStatusCode());
    put(labels, ClaimCodes.LOV_ADJUSTER, claim.getProgress().getAdjusterCode());
    put(labels, ClaimCodes.LOV_SETTLEMENT_TYPE, claim.getProgress().getSettlementTypeCode());
    put(labels, ClaimCodes.LOV_UNIT, claim.getUnitCode());
    return labels;
  }

  private void put(Map<String, String> labels, String type, String code) {
    if (code != null) {
      labels.put(type, lovs.label(type, code));
    }
  }

  /**
   * A claim as the record page shows it.
   *
   * @param claim claim
   * @param premium live premium check
   * @param latestVersionNo latest cover version of the policy year
   * @param awaitingRemittance the status waits for the premium remittance and an invoice of the
   *     cover and policy year is not yet fully remitted (the flag drops once the premium is
   *     remitted, wave CL2)
   * @param locationCount linked locations
   * @param insurerCount distinct insurers
   * @param totalReserve sum of the insurer reserves
   * @param labels labels of the claim's codes by list type
   * @param dpPolicy parameter {@code BCL_AUTH_DP_POLICY}
   * @param unremittedInvoices invoices of the cover and policy year not fully remitted (special
   *     remittance link)
   */
  public record ClaimView(
      Claim claim,
      PremiumCheck premium,
      int latestVersionNo,
      boolean awaitingRemittance,
      int locationCount,
      int insurerCount,
      BigDecimal totalReserve,
      Map<String, String> labels,
      String dpPolicy,
      List<String> unremittedInvoices) {

    /** Defensive copy. */
    public ClaimView {
      labels = Map.copyOf(labels);
      unremittedInvoices = List.copyOf(unremittedInvoices);
    }
  }
}
