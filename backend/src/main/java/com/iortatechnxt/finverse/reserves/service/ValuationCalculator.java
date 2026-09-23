package com.iortatechnxt.finverse.reserves.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.reserves.domain.PremiumAmounts;
import com.iortatechnxt.finverse.reserves.domain.ReserveKey;
import com.iortatechnxt.finverse.reserves.domain.ReserveLineValues;
import com.iortatechnxt.finverse.reserves.domain.ReserveParameterTerms;
import com.iortatechnxt.finverse.reserves.domain.ReserveType;
import com.iortatechnxt.finverse.reserves.domain.TakafulItem;
import com.iortatechnxt.finverse.reserves.domain.UprItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calculates every technical reserve of a company at a valuation date (nothing is stored).
 *
 * <ol>
 *   <li>UPR and DAC / UCR per premium transaction ({@link UprMath}) summed per reporting unit;
 *   <li>OSLR from the claims kernel ({@link OslrCalculator});
 *   <li>IBNR by rate or chain-ladder ({@link IbnrCalculator});
 *   <li>ULAE = ULAE % × (OSLR + IBNR) gross; MfAD = MfAD % × (OSLR + IBNR), gross and RI share;
 *   <li>premium deficiency per line of business (liability adequacy test): expected claims = net
 *       UPR × expected loss ratio; deficiency = max(0, expected claims − (net UPR − net DAC)),
 *       allocated to the line's units by net UPR;
 *   <li>takaful surplus of policies expiring in the valuation month when enabled for the company.
 * </ol>
 */
@Component
@Transactional(readOnly = true)
public class ValuationCalculator {

  private static final Comparator<ReserveLineValues> LINE_ORDER =
      Comparator.comparing(ReserveLineValues::type)
          .thenComparing(ReserveLineValues::key, ReserveKey.ORDER);

  private final ReserveParameterService parameters;
  private final TakafulSettingService takafulSettings;
  private final PremiumPortfolio portfolio;
  private final OslrCalculator oslr;
  private final IbnrCalculator ibnr;
  private final TakafulCalculator takaful;
  private final ReservePorts ports;

  /**
   * Creates the calculator.
   *
   * @param parameters reserve parameters
   * @param takafulSettings takaful settings
   * @param portfolio premium portfolio loader
   * @param oslr OSLR calculator
   * @param ibnr IBNR calculator
   * @param takaful takaful surplus calculator
   * @param ports claims / reinsurance availability
   */
  public ValuationCalculator(
      ReserveParameterService parameters,
      TakafulSettingService takafulSettings,
      PremiumPortfolio portfolio,
      OslrCalculator oslr,
      IbnrCalculator ibnr,
      TakafulCalculator takaful,
      ReservePorts ports) {
    this.parameters = parameters;
    this.takafulSettings = takafulSettings;
    this.portfolio = portfolio;
    this.oslr = oslr;
    this.ibnr = ibnr;
    this.takaful = takaful;
    this.ports = ports;
  }

  /**
   * Calculates the reserves.
   *
   * @param companyId company
   * @param valuationDate valuation date
   * @return result
   */
  public ValuationResult calculate(Long companyId, LocalDate valuationDate) {
    ReserveParameterSet params = parameters.inForce(companyId, valuationDate);
    Portfolio book = portfolio.load(companyId, valuationDate, params);
    List<UprItem> upr = book.uprAt(valuationDate);
    Map<ReserveKey, PremiumAmounts> unearned = book.unearnedByKey(valuationDate);
    Map<ReserveKey, PremiumAmounts> earned =
        book.earnedByKey(valuationDate.minusYears(1), valuationDate);
    Map<ReserveKey, GrossRi> outstanding = oslr.calculate(companyId, valuationDate);
    IbnrCalculator.Result incurredButNotReported =
        ibnr.calculate(companyId, valuationDate, params, earned, outstanding.keySet());

    List<ReserveLineValues> lines = new ArrayList<>();
    unearned.forEach(
        (k, u) -> {
          lines.add(ReserveLineValues.of(ReserveType.UPR, k, u.premium(), u.cededPremium()));
          lines.add(ReserveLineValues.of(ReserveType.DAC, k, u.commission(), u.riCommission()));
        });
    outstanding.forEach(
        (k, a) -> lines.add(ReserveLineValues.of(ReserveType.OSLR, k, a.gross(), a.ri())));
    lines.addAll(incurredButNotReported.lines());
    lines.addAll(margins(params, outstanding, incurredButNotReported.lines()));
    lines.addAll(premiumDeficiency(params, unearned));

    LocalDate monthStart = YearMonth.from(valuationDate).atDay(1);
    List<TakafulItem> surplus =
        takafulSettings
            .inForce(companyId)
            .map(s -> takaful.calculate(companyId, s, monthStart, valuationDate))
            .orElse(List.of());
    return new ValuationResult(
        lines.stream().filter(ValuationCalculator::nonZero).sorted(LINE_ORDER).toList(),
        upr.stream()
            .filter(
                i ->
                    i.unearned().premium().signum() != 0
                        || i.unearned().commission().signum() != 0
                        || !i.approvalDate().isBefore(monthStart))
            .toList(),
        surplus,
        incurredButNotReported.analyses(),
        remarks(params, lines));
  }

  private static List<ReserveLineValues> margins(
      ReserveParameterSet params,
      Map<ReserveKey, GrossRi> outstanding,
      List<ReserveLineValues> ibnrLines) {
    Map<ReserveKey, GrossRi> claims = new HashMap<>(outstanding);
    ibnrLines.forEach(l -> claims.merge(l.key(), new GrossRi(l.gross(), l.ri()), GrossRi::plus));
    List<ReserveLineValues> out = new ArrayList<>();
    claims.forEach(
        (k, base) -> {
          ReserveParameterTerms t = params.terms(k.businessLine());
          out.add(
              new ReserveLineValues(
                  ReserveType.ULAE,
                  k,
                  Percent.of(base.gross(), t.ulaePct()),
                  Money.zero(),
                  Money.round(base.gross()),
                  t.ulaePct(),
                  null));
          GrossRi margin = base.percent(t.mfadPct());
          out.add(
              new ReserveLineValues(
                  ReserveType.MFAD,
                  k,
                  margin.gross(),
                  margin.ri(),
                  Money.round(base.gross()),
                  t.mfadPct(),
                  null));
        });
    return out;
  }

  private static List<ReserveLineValues> premiumDeficiency(
      ReserveParameterSet params, Map<ReserveKey, PremiumAmounts> unearned) {
    Map<String, Map<ReserveKey, BigDecimal>> netUprByLine = new HashMap<>();
    Map<String, BigDecimal> netDacByLine = new HashMap<>();
    unearned.forEach(
        (k, u) -> {
          netUprByLine
              .computeIfAbsent(k.businessLine(), l -> new HashMap<>())
              .put(k, u.premium().subtract(u.cededPremium()));
          netDacByLine.merge(
              k.businessLine(), u.commission().subtract(u.riCommission()), BigDecimal::add);
        });
    List<ReserveLineValues> out = new ArrayList<>();
    netUprByLine.forEach(
        (line, weights) -> {
          BigDecimal netUpr = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
          BigDecimal expected = Percent.of(netUpr, params.terms(line).expectedLossRatio());
          BigDecimal available = netUpr.subtract(netDacByLine.getOrDefault(line, BigDecimal.ZERO));
          BigDecimal deficiency = expected.subtract(available);
          if (deficiency.signum() > 0) {
            Percent.allocate(deficiency, weights)
                .forEach(
                    (k, part) ->
                        out.add(ReserveLineValues.of(ReserveType.PDR, k, part, Money.zero())));
          }
        });
    return out;
  }

  private List<String> remarks(ReserveParameterSet params, List<ReserveLineValues> lines) {
    List<String> out = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    lines.forEach(l -> seen.add(l.key().businessLine()));
    Set<String> missing = new TreeSet<>();
    seen.stream().filter(l -> !params.isConfigured(l)).forEach(missing::add);
    if (!missing.isEmpty()) {
      out.add(
          "No authorized reserve parameters for "
              + String.join(", ", missing)
              + ": IBNR, ULAE, MfAD, LAT and RI commission taken as zero");
    }
    if (!ports.hasClaims()) {
      out.add("Claims module not available: OSLR and chain-ladder IBNR are zero");
    }
    if (!ports.hasClaimReinsurance()) {
      out.add("Reinsurers' share of outstanding claims not available: OSLR RI share is zero");
    }
    return out;
  }

  private static boolean nonZero(ReserveLineValues l) {
    return l.gross().signum() != 0 || l.ri().signum() != 0;
  }
}
