package com.iortatechnxt.finverse.reinsurance.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.reinsurance.domain.CessionBasis;
import com.iortatechnxt.finverse.reinsurance.domain.CessionHeader;
import com.iortatechnxt.finverse.reinsurance.domain.Participation;
import com.iortatechnxt.finverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.finverse.reinsurance.domain.RiskRef;
import java.math.BigDecimal;
import java.util.List;

/**
 * Allocation of one premium transaction computed by {@link CessionPlanner}, before it is stored:
 * used both for the preview of an allocation run and to create the cession.
 *
 * @param header transaction identification and totals
 * @param basis allocation basis
 * @param lines planned shares (policy currency)
 */
public record CessionPlan(CessionHeader header, CessionBasis basis, List<PlannedLine> lines) {

  /** Canonical constructor copying the lines. */
  public CessionPlan {
    lines = List.copyOf(lines);
  }

  /**
   * Premium of a layer.
   *
   * @param layer layer
   * @return premium in the policy currency
   */
  public BigDecimal premiumOf(RiLayer layer) {
    return lines.stream()
        .filter(l -> l.who().layer() == layer)
        .map(PlannedLine::premium)
        .reduce(Money.zero(), BigDecimal::add);
  }

  /**
   * Sum insured of a layer.
   *
   * @param layer layer
   * @return sum insured in the policy currency
   */
  public BigDecimal siOf(RiLayer layer) {
    return lines.stream()
        .filter(l -> l.who().layer() == layer)
        .map(PlannedLine::sumInsured)
        .reduce(Money.zero(), BigDecimal::add);
  }

  /**
   * One planned share of a risk.
   *
   * @param risk risk
   * @param who who takes the share (for a new facultative requirement: FAC without placement)
   * @param sumInsured sum insured
   * @param premium premium
   */
  public record PlannedLine(
      RiskRef risk, Participation who, BigDecimal sumInsured, BigDecimal premium) {}
}
