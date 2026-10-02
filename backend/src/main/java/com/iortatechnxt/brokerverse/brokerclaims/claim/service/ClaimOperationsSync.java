package com.iortatechnxt.brokerverse.brokerclaims.claim.service;

import com.iortatechnxt.brokerverse.brokerclaims.cover.service.CoverService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.BrokerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimLovAttribute;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimLovAttributeRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.brokerclaims.domain.CoverSnapshot;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps open claims in step with the Operations ledger (BRCLM.001/039, OQ46; CLAIMS_BROKING_DESIGN
 * 3.1 and 9.4). Each method runs in its own transaction after the ledger change committed: a
 * payment, reversal or cancellation of an invoice re-runs the premium check of the open claims of
 * its cover and policy year; a full remittance tells the handlers of claims awaiting premium
 * remittance ({@code BCL_PREMIUM_REMITTED}); an endorsement booked on the cover tells the handlers
 * of a newer cover version ({@code BCL_NEWER_COVER_VERSION}).
 */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ClaimOperationsSync {

  private final BrokerClaimRepository claims;
  private final ClaimLovAttributeRepository attributes;
  private final InvoiceLedgerQueryService ledger;
  private final CoverService covers;
  private final PremiumCheckService premiums;
  private final NotificationService notifications;

  /**
   * Creates the synchroniser.
   *
   * @param claims claims
   * @param attributes status attributes
   * @param ledger invoice ledger
   * @param covers cover versions
   * @param premiums premium check
   * @param notifications handler notifications
   */
  public ClaimOperationsSync(
      BrokerClaimRepository claims,
      ClaimLovAttributeRepository attributes,
      InvoiceLedgerQueryService ledger,
      CoverService covers,
      PremiumCheckService premiums,
      NotificationService notifications) {
    this.claims = claims;
    this.attributes = attributes;
    this.ledger = ledger;
    this.covers = covers;
    this.premiums = premiums;
    this.notifications = notifications;
  }

  /**
   * Re-runs the premium check of the open claims of an invoice's cover and policy year.
   *
   * @param invoiceNo invoice that moved
   * @return claims checked
   */
  public int invoiceMoved(String invoiceNo) {
    List<Claim> open = openClaimsOf(invoiceNo);
    open.forEach(premiums::check);
    return open.size();
  }

  /**
   * Tells the handlers of claims awaiting premium remittance that the invoice is fully remitted.
   *
   * @param invoiceNo invoice
   * @return handlers told
   */
  public int invoiceRemitted(String invoiceNo) {
    Set<String> awaiting =
        attributes
            .findByTypeCodeAndAttributeAndValueIgnoreCase(
                ClaimCodes.LOV_STATUS, ClaimCodes.ATTR_AWAITING_PREMIUM_REMITTANCE, "true")
            .stream()
            .map(ClaimLovAttribute::getCode)
            .collect(Collectors.toSet());
    List<Claim> waiting =
        openClaimsOf(invoiceNo).stream()
            .filter(c -> awaiting.contains(c.getProgress().getStatusCode()))
            .toList();
    waiting.forEach(
        c ->
            notify(
                c,
                "Premium of claim " + c.getClaimNo() + " remitted",
                "Invoice " + invoiceNo + " is fully remitted to the insurer",
                "BCL_PREMIUM_REMITTED"));
    return waiting.size();
  }

  /**
   * Re-checks the premium and tells the handlers when an endorsement of the cover was booked.
   *
   * @param invoiceNo endorsement invoice
   * @return claims told
   */
  public int endorsementBooked(String invoiceNo) {
    List<Claim> open = openClaimsOf(invoiceNo);
    int told = 0;
    for (Claim c : open) {
      premiums.check(c);
      CoverSnapshot cover = c.getCover();
      int latest = covers.version(cover.getArn(), cover.getPolicyYear(), null).no();
      if (cover.getCoverVersionNo() == null || latest > cover.getCoverVersionNo()) {
        notify(
            c,
            "Newer cover version for claim " + c.getClaimNo(),
            "Endorsement invoice " + invoiceNo + " was booked on " + cover.getArn(),
            "BCL_NEWER_COVER_VERSION");
        told++;
      }
    }
    return told;
  }

  /**
   * Re-runs the premium check of one claim (daily job {@code BCL_PREMIUM_RECHECK}).
   *
   * @param claimId claim
   * @return true when the claim was found
   */
  public boolean recheck(Long claimId) {
    return claims
        .findById(claimId)
        .map(
            c -> {
              premiums.check(c);
              return true;
            })
        .orElse(false);
  }

  /**
   * Open claims without an authorization code, a batch at a time (daily re-check).
   *
   * @param page page number
   * @param size page size
   * @return claim ids
   */
  @Transactional(readOnly = true)
  public List<Long> unauthorized(int page, int size) {
    return claims
        .findByProgressPhaseNotAndCoverAuthorizationCodeIsNullOrderByIdAsc(
            ClaimPhase.CLOSED, PageRequest.of(page, size))
        .stream()
        .map(Claim::getId)
        .toList();
  }

  private List<Claim> openClaimsOf(String invoiceNo) {
    Optional<OpsInvoice> invoice = ledger.find(invoiceNo);
    if (invoice.isEmpty()) {
      return List.of();
    }
    OpsInvoice i = invoice.get();
    return claims
        .findByCompanyIdAndCoverArnAndProgressPhaseNot(
            i.getCompanyId(), i.getArn(), ClaimPhase.CLOSED)
        .stream()
        .filter(c -> c.getCover().getPolicyYear() == i.getPolicyYear())
        .toList();
  }

  private void notify(Claim c, String title, String body, String event) {
    notifications.notifyUser(
        c.getHandler(),
        new Notice(
            title,
            body,
            "/claims-handling/" + c.getId(),
            ClaimCodes.ENTITY_TYPE,
            String.valueOf(c.getId())),
        event);
  }
}
