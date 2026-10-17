package com.iortatechnxt.brokerverse.placement.api.dto;

import com.iortatechnxt.brokerverse.placement.domain.PaymentGateRule;

/**
 * A payment gate rule (BRD 2.3.1).
 *
 * @param marketSegment segment, null = any
 * @param lineCode product line, null = any
 * @param rule rule
 * @param priority priority (lowest first)
 * @param description description
 */
public record GateRuleResponse(
    String marketSegment, String lineCode, String rule, int priority, String description) {

  /**
   * Maps a rule.
   *
   * @param r rule
   * @return response
   */
  public static GateRuleResponse from(PaymentGateRule r) {
    return new GateRuleResponse(
        r.getMarketSegment(),
        r.getLineCode(),
        r.getRule().name(),
        r.getPriority(),
        r.getDescription());
  }
}
