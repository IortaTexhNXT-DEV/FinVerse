package com.iortatechnxt.finverse.reserves.service;

import com.iortatechnxt.finverse.reserves.domain.DevelopmentPeriod;
import com.iortatechnxt.finverse.reserves.domain.IbnrMethod;
import com.iortatechnxt.finverse.reserves.domain.ReserveParameterTerms;
import com.iortatechnxt.finverse.reserves.domain.TriangleBasis;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Reserve parameters in force at a valuation date, by line of business. A line without authorized
 * parameters gets zero IBNR, margins and commission rates ({@link #DEFAULTS}); the valuation run
 * lists such lines in its remarks.
 */
public final class ReserveParameterSet {

  /** Values used for a line of business without authorized parameters. */
  public static final ReserveParameterTerms DEFAULTS =
      new ReserveParameterTerms(
          IbnrMethod.RATE,
          BigDecimal.ZERO,
          TriangleBasis.INCURRED,
          DevelopmentPeriod.YEAR,
          5,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          null);

  private final Map<String, ReserveParameterTerms> byLine;

  /**
   * Creates the set.
   *
   * @param byLine parameters by line of business
   */
  public ReserveParameterSet(Map<String, ReserveParameterTerms> byLine) {
    this.byLine = Map.copyOf(byLine);
  }

  /**
   * Parameters of a line of business.
   *
   * @param businessLine line of business
   * @return parameters, {@link #DEFAULTS} when none is in force
   */
  public ReserveParameterTerms terms(String businessLine) {
    return byLine.getOrDefault(businessLine, DEFAULTS);
  }

  /**
   * Whether a line of business has parameters in force.
   *
   * @param businessLine line of business
   * @return true when configured
   */
  public boolean isConfigured(String businessLine) {
    return byLine.containsKey(businessLine);
  }

  /**
   * Lines of business with parameters in force.
   *
   * @return lines
   */
  public Set<String> lines() {
    return byLine.keySet();
  }
}
