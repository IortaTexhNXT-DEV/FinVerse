package com.iortatechnxt.brokerverse.adjustment.api.dto;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.PostingOutcome;
import com.iortatechnxt.brokerverse.adjustment.domain.ProcessingTrail;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;
import com.iortatechnxt.brokerverse.adjustment.service.RequestAging;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * One endorsement request (ADJID.020-022): invoice, terms, recompute before / after and per
 * insurer, processing trail, posting outcome and aging.
 *
 * @param id id
 * @param requestNo request number
 * @param stage stage
 * @param requestClass class
 * @param computation computation
 * @param invoice invoice and account
 * @param terms what is asked
 * @param amounts amounts entered
 * @param control approval, sign, overrides, quotation link and return
 * @param trail processing trail
 * @param outcome posting outcome
 * @param changes before / after per component
 * @param shares change per insurer
 * @param journals journal batches posted
 * @param createdBy requester
 * @param createdAt raised at
 * @param agingDays aging in days
 */
public record RequestResponse(
    Long id,
    String requestNo,
    String stage,
    String requestClass,
    String computation,
    InvoiceView invoice,
    RequestTerms terms,
    AmountInput amounts,
    ControlView control,
    ProcessingTrail trail,
    PostingOutcome outcome,
    List<ChangeView> changes,
    List<ShareView> shares,
    List<String> journals,
    String createdBy,
    Instant createdAt,
    long agingDays) {

  /** Defensive copies. */
  public RequestResponse {
    changes = List.copyOf(changes);
    shares = List.copyOf(shares);
    journals = List.copyOf(journals);
  }

  /**
   * Maps a request (collections loaded).
   *
   * @param r request
   * @param now current time (aging)
   * @return response
   */
  public static RequestResponse from(EndorsementRequest r, Instant now) {
    var s = r.getSubject();
    return new RequestResponse(
        r.getId(),
        r.getRequestNo(),
        r.getStage().name(),
        r.getRequestClass().name(),
        r.getComputation().name(),
        new InvoiceView(
            s.invoiceNo(),
            s.arn(),
            s.policyNo(),
            s.clientCode(),
            s.assuredName(),
            s.insurerCode(),
            s.currency(),
            s.segment(),
            s.aoUsername(),
            s.productLine()),
        r.getTerms(),
        r.getAmounts(),
        new ControlView(
            r.isNeedsApproval(),
            r.isNegative(),
            r.getDuplicateOverride(),
            r.getBaselineOverride(),
            r.isQuotationRequired(),
            r.getQuotationRef(),
            r.getReturnReason(),
            r.getReturnComment(),
            r.getSlipNo()),
        r.trail(),
        r.outcome(),
        r.getChanges().stream()
            .map(c -> new ChangeView(c.component().name(), c.before(), c.delta(), c.after()))
            .toList(),
        r.getShares().stream()
            .map(
                x ->
                    new ShareView(
                        x.insurerCode(),
                        x.sharePct(),
                        x.lead(),
                        x.premiumDelta(),
                        x.commissionDelta(),
                        x.vatDelta()))
            .toList(),
        r.getJournals(),
        r.getCreatedBy(),
        r.getCreatedAt(),
        RequestAging.days(r, now));
  }

  /**
   * Invoice and account of the request.
   *
   * @param invoiceNo invoice
   * @param arn ARN
   * @param policyNo policy
   * @param clientCode client
   * @param assuredName assured
   * @param insurerCode lead insurer
   * @param currency currency
   * @param segment market segment
   * @param aoUsername requesting AO
   * @param productLine product line
   */
  public record InvoiceView(
      String invoiceNo,
      String arn,
      String policyNo,
      String clientCode,
      String assuredName,
      String insurerCode,
      String currency,
      String segment,
      String aoUsername,
      String productLine) {}

  /**
   * Controls of the request.
   *
   * @param needsApproval team leader approval needed
   * @param negative reduces the invoice
   * @param duplicateOverride duplicate justification
   * @param baselineOverride over-adjustment justification
   * @param quotationRequired TSI increase above the package limit
   * @param quotationRef quotation linked
   * @param returnReason last return reason
   * @param returnComment last return comment
   * @param slipNo endorsement slip number
   */
  public record ControlView(
      boolean needsApproval,
      boolean negative,
      String duplicateOverride,
      String baselineOverride,
      boolean quotationRequired,
      String quotationRef,
      String returnReason,
      String returnComment,
      String slipNo) {}

  /**
   * Before / after of a component.
   *
   * @param component component
   * @param before before
   * @param delta change
   * @param after after
   */
  public record ChangeView(
      String component, BigDecimal before, BigDecimal delta, BigDecimal after) {}

  /**
   * Change of an insurer share.
   *
   * @param insurerCode insurer
   * @param sharePct share
   * @param lead lead insurer
   * @param premiumDelta premium change
   * @param commissionDelta commission change
   * @param vatDelta VAT change
   */
  public record ShareView(
      String insurerCode,
      BigDecimal sharePct,
      boolean lead,
      BigDecimal premiumDelta,
      BigDecimal commissionDelta,
      BigDecimal vatDelta) {}
}
