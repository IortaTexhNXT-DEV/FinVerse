package com.iortatechnxt.brokerverse.placement.api.dto;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.placement.domain.PaymentEvidence;
import com.iortatechnxt.brokerverse.placement.service.PaymentGateService.GateView;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * The payment gate of an account: rule, position and evidence (BRD 2.3.1, BRNB.068/114).
 *
 * @param accountId account id
 * @param arn Account Reference Number
 * @param status account status
 * @param marketSegment market segment
 * @param lineCode product line
 * @param paymentStatus payment gate position
 * @param paymentSource source recorded on the account
 * @param directPayment paid directly to the insurer
 * @param rule rule
 * @param ruleDescription rule description
 * @param open gate open
 * @param evidence evidence, oldest first
 */
public record GateResponse(
    Long accountId,
    String arn,
    String status,
    String marketSegment,
    String lineCode,
    String paymentStatus,
    String paymentSource,
    boolean directPayment,
    String rule,
    String ruleDescription,
    boolean open,
    List<Evidence> evidence) {

  /**
   * Maps a gate view.
   *
   * @param v view
   * @return response
   */
  public static GateResponse from(GateView v) {
    Account a = v.account();
    return new GateResponse(
        a.getId(),
        a.getArn(),
        a.getStatus().name(),
        a.getMarketSegment(),
        a.getLineCode(),
        a.getLifecycle().getPaymentStatus().name(),
        a.getLifecycle().getPaymentSource(),
        a.isDirectPayment(),
        v.rule().name(),
        v.ruleDescription(),
        v.open(),
        v.evidence().stream().map(Evidence::from).toList());
  }

  /**
   * One piece of evidence.
   *
   * @param id id
   * @param kind kind
   * @param source source
   * @param reference source reference
   * @param amount amount
   * @param paidOn payment date
   * @param channel confirmation channel
   * @param remarks remarks
   * @param attachmentId supporting document
   * @param gateOpened whether it opened the gate
   * @param createdAt recorded at
   * @param createdBy recorded by
   */
  public record Evidence(
      Long id,
      String kind,
      String source,
      String reference,
      BigDecimal amount,
      LocalDate paidOn,
      String channel,
      String remarks,
      Long attachmentId,
      boolean gateOpened,
      Instant createdAt,
      String createdBy) {

    /**
     * Maps evidence.
     *
     * @param e evidence
     * @return response
     */
    public static Evidence from(PaymentEvidence e) {
      return new Evidence(
          e.getId(),
          e.getKind().name(),
          e.getSource(),
          e.getReference(),
          e.getAmount(),
          e.getPaidOn(),
          e.getChannel(),
          e.getRemarks(),
          e.getAttachmentId(),
          e.isGateOpened(),
          e.getCreatedAt(),
          e.getCreatedBy());
    }
  }
}
