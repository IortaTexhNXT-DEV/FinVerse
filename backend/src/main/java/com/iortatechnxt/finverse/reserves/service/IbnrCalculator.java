package com.iortatechnxt.finverse.reserves.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.insurance.ClaimMovement;
import com.iortatechnxt.finverse.reserves.domain.IbnrMethod;
import com.iortatechnxt.finverse.reserves.domain.PremiumAmounts;
import com.iortatechnxt.finverse.reserves.domain.ReserveKey;
import com.iortatechnxt.finverse.reserves.domain.ReserveLineValues;
import com.iortatechnxt.finverse.reserves.domain.ReserveParameterTerms;
import com.iortatechnxt.finverse.reserves.domain.ReserveType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * IBNR per reporting unit with the method configured for each line of business.
 *
 * <ul>
 *   <li><b>Rate</b> (PGIBR079): IBNR = base × rate %, base = earned premium (gross of reinsurance)
 *       of the twelve months ending at the valuation date.
 *   <li><b>Chain-ladder</b>: IBNR of the line from {@link TriangleAnalysis}, allocated to the
 *       line's reporting units in proportion to their earned premium (or equally over the units
 *       with open claims when no premium was earned).
 * </ul>
 *
 * The reinsurers' portion = IBNR × ceded share, ceded share = earned ceded premium / earned premium
 * of the unit.
 */
@Component
public class IbnrCalculator {

  private final ReservePorts ports;

  /**
   * Creates the calculator.
   *
   * @param ports claims movements (optional port)
   */
  public IbnrCalculator(ReservePorts ports) {
    this.ports = ports;
  }

  /**
   * Calculates IBNR.
   *
   * @param companyId company
   * @param valuationDate valuation date
   * @param params parameters in force
   * @param earned earned amounts of the last twelve months by unit
   * @param claimUnits units with open claims (allocation fallback)
   * @return IBNR lines and the triangle analyses of chain-ladder lines
   */
  public Result calculate(
      Long companyId,
      LocalDate valuationDate,
      ReserveParameterSet params,
      Map<ReserveKey, PremiumAmounts> earned,
      Collection<ReserveKey> claimUnits) {
    Set<String> lines = new TreeSet<>(params.lines());
    earned.keySet().forEach(k -> lines.add(k.businessLine()));
    claimUnits.forEach(k -> lines.add(k.businessLine()));
    Map<String, List<ClaimMovement>> movements =
        chainLadderMovements(companyId, valuationDate, params, lines);
    List<ReserveLineValues> out = new ArrayList<>();
    List<TriangleAnalysis> analyses = new ArrayList<>();
    for (String line : lines) {
      ReserveParameterTerms terms = params.terms(line);
      Map<ReserveKey, PremiumAmounts> units = unitsOf(line, earned);
      if (terms.ibnrMethod() == IbnrMethod.RATE) {
        units.forEach((k, e) -> out.add(rateLine(k, e, terms.ibnrRate())));
      } else {
        TriangleAnalysis analysis =
            TriangleAnalysis.of(
                line,
                movements.getOrDefault(line, List.of()),
                terms.triangleBasis(),
                terms.developmentPeriod(),
                terms.accidentPeriods(),
                valuationDate);
        analyses.add(analysis);
        out.addAll(allocate(analysis.ibnr(), units, line, claimUnits));
      }
    }
    return new Result(out, analyses);
  }

  /**
   * Chain-ladder analysis of one line (triangle viewer and report).
   *
   * @param companyId company
   * @param valuationDate valuation date
   * @param line line of business
   * @param terms parameters (basis, period, number of accident periods)
   * @return analysis
   */
  public TriangleAnalysis analyse(
      Long companyId, LocalDate valuationDate, String line, ReserveParameterTerms terms) {
    LocalDate from =
        ClaimsTriangleBuilder.firstDate(
            terms.developmentPeriod(), valuationDate, terms.accidentPeriods());
    List<ClaimMovement> movements =
        ports.movements(companyId, from, valuationDate).stream()
            .filter(m -> line.equals(m.lineOfBusiness()))
            .toList();
    return TriangleAnalysis.of(
        line,
        movements,
        terms.triangleBasis(),
        terms.developmentPeriod(),
        terms.accidentPeriods(),
        valuationDate);
  }

  private Map<String, List<ClaimMovement>> chainLadderMovements(
      Long companyId, LocalDate valuationDate, ReserveParameterSet params, Set<String> lines) {
    LocalDate from = null;
    for (String line : lines) {
      ReserveParameterTerms t = params.terms(line);
      if (t.ibnrMethod() == IbnrMethod.CHAIN_LADDER) {
        LocalDate first =
            ClaimsTriangleBuilder.firstDate(
                t.developmentPeriod(), valuationDate, t.accidentPeriods());
        from = from == null || first.isBefore(from) ? first : from;
      }
    }
    if (from == null) {
      return Map.of();
    }
    return ports.movements(companyId, from, valuationDate).stream()
        .filter(m -> m.lineOfBusiness() != null)
        .collect(Collectors.groupingBy(ClaimMovement::lineOfBusiness));
  }

  private static Map<ReserveKey, PremiumAmounts> unitsOf(
      String line, Map<ReserveKey, PremiumAmounts> earned) {
    Map<ReserveKey, PremiumAmounts> out = new LinkedHashMap<>();
    earned.entrySet().stream()
        .filter(e -> e.getKey().businessLine().equals(line))
        .sorted(Map.Entry.comparingByKey(ReserveKey.ORDER))
        .forEach(e -> out.put(e.getKey(), e.getValue()));
    return out;
  }

  private static ReserveLineValues rateLine(
      ReserveKey key, PremiumAmounts earned, BigDecimal rate) {
    BigDecimal base = Money.round(earned.premium());
    BigDecimal gross = base.signum() > 0 ? Percent.of(base, rate) : Money.zero();
    return new ReserveLineValues(
        ReserveType.IBNR, key, gross, ceded(gross, earned), base, rate, IbnrMethod.RATE.name());
  }

  private static List<ReserveLineValues> allocate(
      BigDecimal ibnr,
      Map<ReserveKey, PremiumAmounts> units,
      String line,
      Collection<ReserveKey> claimUnits) {
    Map<ReserveKey, BigDecimal> weights = new HashMap<>();
    units.forEach((k, e) -> weights.put(k, e.premium()));
    Map<ReserveKey, BigDecimal> parts = Percent.allocate(ibnr, weights);
    if (parts.isEmpty()) {
      claimUnits.stream()
          .filter(k -> k.businessLine().equals(line))
          .forEach(k -> weights.put(k, BigDecimal.ONE));
      parts = Percent.allocate(ibnr, weights);
    }
    List<ReserveLineValues> out = new ArrayList<>();
    parts.forEach(
        (k, gross) -> {
          PremiumAmounts e = units.getOrDefault(k, PremiumAmounts.zero());
          out.add(
              new ReserveLineValues(
                  ReserveType.IBNR,
                  k,
                  gross,
                  ceded(gross, e),
                  Money.round(e.premium()),
                  null,
                  IbnrMethod.CHAIN_LADDER.name()));
        });
    return out;
  }

  private static BigDecimal ceded(BigDecimal gross, PremiumAmounts earned) {
    return Money.round(gross.multiply(Percent.ratio(earned.cededPremium(), earned.premium())));
  }

  /**
   * IBNR of a valuation.
   *
   * @param lines IBNR lines by reporting unit
   * @param analyses triangle analyses of the chain-ladder lines of business
   */
  public record Result(List<ReserveLineValues> lines, List<TriangleAnalysis> analyses) {

    /** Canonical constructor copying the lists. */
    public Result {
      lines = List.copyOf(lines);
      analyses = List.copyOf(analyses);
    }
  }
}
