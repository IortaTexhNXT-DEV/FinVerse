package com.iortatechnxt.brokerverse.brokerclaims.feed.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.BrokerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimLovAttribute;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimLovAttributeRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimProgress;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.port.ClaimsFeed;
import com.iortatechnxt.brokerverse.opsledger.service.port.FeedItem;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The in-app adapter of the Operations port {@link ClaimsFeed} (OQ46, OQ25, MKTID.009;
 * CLAIMS_BROKING_DESIGN 3.1): feed {@value #SPECIAL_REMIT} lists, for each open claim whose status
 * carries the attribute {@code awaiting_premium_remittance} (status 11 "With BDOI - For Premium
 * Remittance") and was set on or after {@code since}, one item per invoice of the claim's cover and
 * policy year that is not fully remitted, keyed by invoice number, with the fields {@code claimNo},
 * {@code arn}, {@code policyYear}, {@code status}, {@code handler} and {@code statusSince}. The
 * special remittance service confirms a claims-condition request with it. The claim status is read
 * only through {@link ClaimProgress} (wave CL1-B owns the status engine).
 */
@Service
@Transactional(readOnly = true)
public class InAppClaimsFeed implements ClaimsFeed {

  /** Feed of the claims special remittance (same code as the remittance module's). */
  public static final String SPECIAL_REMIT = "CLAIMS_SPECIAL_REMIT";

  private static final Set<RemittanceStatus> DONE =
      Set.of(RemittanceStatus.FULLY_REMITTED, RemittanceStatus.NOT_APPLICABLE);

  private final BrokerClaimRepository claims;
  private final ClaimLovAttributeRepository attributes;
  private final InvoiceLedgerQueryService ledger;

  /**
   * Creates the feed.
   *
   * @param claims claims
   * @param attributes status attributes
   * @param ledger invoice ledger
   */
  public InAppClaimsFeed(
      BrokerClaimRepository claims,
      ClaimLovAttributeRepository attributes,
      InvoiceLedgerQueryService ledger) {
    this.claims = claims;
    this.attributes = attributes;
    this.ledger = ledger;
  }

  @Override
  public List<FeedItem> fetch(Long companyId, String feedCode, LocalDate since) {
    if (!SPECIAL_REMIT.equals(feedCode)) {
      return List.of();
    }
    Set<String> awaiting =
        attributes
            .findByTypeCodeAndAttributeAndValueIgnoreCase(
                ClaimCodes.LOV_STATUS, ClaimCodes.ATTR_AWAITING_PREMIUM_REMITTANCE, "true")
            .stream()
            .map(ClaimLovAttribute::getCode)
            .collect(Collectors.toSet());
    if (awaiting.isEmpty()) {
      return List.of();
    }
    LocalDate from = since == null ? LocalDate.EPOCH : since;
    List<FeedItem> items = new ArrayList<>();
    for (Claim claim :
        claims
            .findByCompanyIdAndProgressStatusCodeInAndProgressPhaseNotAndProgressStatusSinceGreaterThanEqual(
                companyId,
                awaiting,
                ClaimPhase.CLOSED,
                from.atStartOfDay(ZoneOffset.UTC).toInstant())) {
      items.addAll(items(claim));
    }
    return items;
  }

  private List<FeedItem> items(Claim claim) {
    ClaimProgress progress = claim.getProgress();
    return ledger.forArn(claim.getCover().getArn()).stream()
        .filter(i -> i.getPolicyYear() == claim.getCover().getPolicyYear())
        .filter(i -> !i.isCancelled() && !DONE.contains(i.getRemittanceStatus()))
        .map(i -> item(claim, progress, i))
        .toList();
  }

  private static FeedItem item(Claim claim, ClaimProgress progress, OpsInvoice invoice) {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("claimNo", claim.getClaimNo());
    fields.put("arn", claim.getCover().getArn());
    fields.put("policyYear", String.valueOf(claim.getCover().getPolicyYear()));
    fields.put("status", progress.getStatusCode());
    fields.put("handler", claim.getHandler());
    fields.put("statusSince", String.valueOf(progress.getStatusSince()));
    return new FeedItem(invoice.getInvoiceNo(), fields);
  }
}
