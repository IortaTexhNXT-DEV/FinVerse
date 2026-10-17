package com.iortatechnxt.brokerverse.placement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * One row of the payment gate rule table (BRD 2.3.1, BRNB.068): the rule for a market segment and
 * product line; a blank segment or line matches any. The first active rule by priority applies.
 */
@Entity
@Table(name = "plc_payment_gate_rule")
public class PaymentGateRule extends BaseEntity {

  @Column(name = "market_segment", length = 40)
  private String marketSegment;

  @Column(name = "line_code", length = 30)
  private String lineCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "gate_rule", nullable = false, length = 30)
  private GateRule rule;

  @Column(nullable = false)
  private int priority;

  @Column(nullable = false, length = 200)
  private String description;

  @Column(nullable = false)
  private boolean active;

  protected PaymentGateRule() {}

  /**
   * Whether the rule applies to an account's segment and line.
   *
   * @param segment market segment of the account
   * @param line product line of the account
   * @return true when both match (blank = any)
   */
  public boolean matches(String segment, String line) {
    return (marketSegment == null || marketSegment.equals(segment))
        && (lineCode == null || lineCode.equals(line));
  }

  public String getMarketSegment() {
    return marketSegment;
  }

  public String getLineCode() {
    return lineCode;
  }

  public GateRule getRule() {
    return rule;
  }

  public int getPriority() {
    return priority;
  }

  public String getDescription() {
    return description;
  }

  public boolean isActive() {
    return active;
  }
}
