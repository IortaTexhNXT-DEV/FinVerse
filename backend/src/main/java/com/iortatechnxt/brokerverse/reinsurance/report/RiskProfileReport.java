package com.iortatechnxt.brokerverse.reinsurance.report;

import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.insurance.ClaimMovementType;
import com.iortatechnxt.brokerverse.reinsurance.domain.Cession;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionBasis;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionLine;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiClaimMovement;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiClaimMovementRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * RISK-PROFILE Risk Profile (RI, UW year wise): risks of the underwriting years selected, banded by
 * company sum insured per risk, with the number of risks, the sum insured and premium split Risk =
 * Retention + QS + Surplus + FAC, and the number and split of paid and outstanding claims. Sum
 * insured comes from the latest full allocation of each policy; premium includes endorsements;
 * claims are banded by the largest risk of their policy (BrokerVerse rule).
 */
@Component
public class RiskProfileReport implements ReportDefinition {

  private static final String YEAR_FROM = "uwYearFrom";
  private static final String YEAR_TO = "uwYearTo";
  private static final long MILLION = 1_000_000L;
  private static final long[] BANDS = {
    0, MILLION, 5 * MILLION, 10 * MILLION, 25 * MILLION, 50 * MILLION, 100 * MILLION
  };
  private static final List<RiLayer> SPLIT =
      List.of(RiLayer.RETENTION, RiLayer.QUOTA_SHARE, RiLayer.SURPLUS, RiLayer.FAC);
  private static final String BAND = "band";
  private static final String RISKS = "risks";
  private static final String PAID_COUNT = "paidCount";
  private static final String OS_COUNT = "osCount";

  private final CessionRepository cessions;
  private final RiClaimMovementRepository movements;

  /**
   * Creates the report.
   *
   * @param cessions cessions
   * @param movements claim movements
   */
  public RiskProfileReport(CessionRepository cessions, RiClaimMovementRepository movements) {
    this.cessions = cessions;
    this.movements = movements;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(RiReportSupport.COMPANY, "Company", ParameterType.COMPANY));
    params.add(ParameterSpec.required(YEAR_FROM, "UW Year From", ParameterType.NUMBER));
    params.add(ParameterSpec.required(YEAR_TO, "UW Year To", ParameterType.NUMBER));
    params.addAll(RiReportSupport.dateRange("Date From", "Date To"));
    return new ReportMetadata(
        "RISK-PROFILE",
        "Risk Profile (RI, UW year wise)",
        ReportCategory.REINSURANCE,
        "Risks, sums insured, premium and claims by SI band with the reinsurance split",
        params,
        Permission.REINSURANCE_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(RiReportSupport.COMPANY);
    int yearFrom = new BigDecimal(p.text(YEAR_FROM)).intValue();
    int yearTo = new BigDecimal(p.text(YEAR_TO)).intValue();
    LocalDate from = p.date(RiReportSupport.FROM);
    LocalDate to = p.date(RiReportSupport.TO);
    List<Cession> list =
        cessions
            .findByCompanyIdAndRiDateBetweenOrderByRiDateAscCessionNoAsc(companyId, from, to)
            .stream()
            .filter(c -> c.getUwYear() >= yearFrom && c.getUwYear() <= yearTo)
            .toList();
    Map<Integer, Map<String, Object>> bands = new LinkedHashMap<>();
    for (int i = 0; i < BANDS.length; i++) {
      bands.put(i, emptyRow(i));
    }
    Map<String, Integer> riskBand = new HashMap<>();
    Map<Long, Integer> policyBand = new HashMap<>();
    exposures(list, bands, riskBand, policyBand);
    premiums(list, bands, riskBand);
    claims(companyId, from, to, bands, policyBand);
    return TabularReportBuilder.of(p)
        .columns(columns())
        .rows(new ArrayList<>(bands.values()))
        .presorted()
        .note("Company share, base currency. Risk = Retention + QS + Surplus + FAC.")
        .note("Claims are banded by the largest risk of their policy.")
        .build();
  }

  private static void exposures(
      List<Cession> list,
      Map<Integer, Map<String, Object>> bands,
      Map<String, Integer> riskBand,
      Map<Long, Integer> policyBand) {
    Map<Long, Cession> latest = new LinkedHashMap<>();
    list.stream()
        .filter(c -> c.getBasis() == CessionBasis.FULL)
        .forEach(c -> latest.put(c.getPolicyId(), c));
    for (Cession c : latest.values()) {
      Map<Long, List<CessionLine>> risks =
          c.getLines().stream()
              .collect(
                  Collectors.groupingBy(
                      CessionLine::getRiskId, LinkedHashMap::new, Collectors.toList()));
      for (List<CessionLine> risk : risks.values()) {
        BigDecimal si = c.toBase(risk.get(0).getRiskSi());
        int band = bandOf(si);
        riskBand.put(c.getPolicyId() + ":" + risk.get(0).getRiskId(), band);
        policyBand.merge(c.getPolicyId(), band, Math::max);
        Map<String, Object> row = bands.get(band);
        row.merge(RISKS, BigDecimal.ONE, RiskProfileReport::plus);
        add(row, "siRisk", si);
        for (CessionLine l : risk) {
          add(row, "si" + l.getLayer().name(), c.toBase(l.getSumInsured()));
        }
      }
    }
  }

  private static void premiums(
      List<Cession> list, Map<Integer, Map<String, Object>> bands, Map<String, Integer> riskBand) {
    for (Cession c : list) {
      for (CessionLine l : c.getLines()) {
        int band = riskBand.getOrDefault(c.getPolicyId() + ":" + l.getRiskId(), 0);
        Map<String, Object> row = bands.get(band);
        BigDecimal premium = c.toBase(l.getPremium());
        add(row, "prRisk", premium);
        add(row, "pr" + l.getLayer().name(), premium);
      }
    }
  }

  private void claims(
      Long companyId,
      LocalDate from,
      LocalDate to,
      Map<Integer, Map<String, Object>> bands,
      Map<Long, Integer> policyBand) {
    List<RiClaimMovement> all =
        movements.findByCompanyIdAndMovementTypeInAndMovementDateLessThanEqual(
            companyId, EnumSet.allOf(ClaimMovementType.class), to);
    Map<Long, List<RiClaimMovement>> byClaim =
        all.stream()
            .filter(m -> policyBand.containsKey(m.getPolicyId()))
            .collect(
                Collectors.groupingBy(
                    RiClaimMovement::getClaimId, LinkedHashMap::new, Collectors.toList()));
    for (List<RiClaimMovement> claim : byClaim.values()) {
      Map<String, Object> row = bands.get(policyBand.get(claim.get(0).getPolicyId()));
      List<RiClaimMovement> paid =
          claim.stream()
              .filter(m -> m.getMovementType() != ClaimMovementType.RESERVE_CHANGE)
              .filter(m -> !m.getMovementDate().isBefore(from))
              .toList();
      List<RiClaimMovement> os =
          claim.stream()
              .filter(m -> m.getMovementType() == ClaimMovementType.RESERVE_CHANGE)
              .toList();
      addClaim(row, "paid", PAID_COUNT, paid);
      addClaim(row, "os", OS_COUNT, os);
    }
  }

  private static void addClaim(
      Map<String, Object> row, String prefix, String countKey, List<RiClaimMovement> movements) {
    BigDecimal total =
        movements.stream().map(RiClaimMovement::signedLoss).reduce(Money.zero(), BigDecimal::add);
    if (total.signum() == 0) {
      return;
    }
    row.merge(countKey, BigDecimal.ONE, RiskProfileReport::plus);
    Map<RiLayer, BigDecimal> split =
        ClaimReportData.byLayer(movements.stream().flatMap(m -> m.getShares().stream()).toList());
    BigDecimal ceded = Money.zero();
    for (RiLayer layer : List.of(RiLayer.QUOTA_SHARE, RiLayer.SURPLUS, RiLayer.FAC)) {
      add(row, prefix + layer.name(), split.get(layer));
      ceded = ceded.add(split.get(layer));
    }
    add(row, prefix + "Risk", total);
    add(row, prefix + RiLayer.RETENTION.name(), total.subtract(ceded));
  }

  private static Map<String, Object> emptyRow(int band) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(BAND, label(band));
    row.put(RISKS, BigDecimal.ZERO);
    for (String prefix : List.of("si", "pr", "paid", "os")) {
      add(row, prefix + "Risk", Money.zero());
      SPLIT.forEach(l -> add(row, prefix + l.name(), Money.zero()));
    }
    row.put(PAID_COUNT, BigDecimal.ZERO);
    row.put(OS_COUNT, BigDecimal.ZERO);
    return row;
  }

  private static List<ReportColumn> columns() {
    List<ReportColumn> out = new ArrayList<>();
    out.add(ReportColumn.text(BAND, "SI Band"));
    out.add(ReportColumn.count(RISKS, "No of Risks"));
    splitColumns(out, "si", "SI");
    splitColumns(out, "pr", "Premium");
    out.add(ReportColumn.count(PAID_COUNT, "Paid Claims"));
    splitColumns(out, "paid", "Paid");
    out.add(ReportColumn.count(OS_COUNT, "O/S Claims"));
    splitColumns(out, "os", "O/S");
    return out;
  }

  private static void splitColumns(List<ReportColumn> out, String prefix, String label) {
    out.add(ReportColumn.amount(prefix + "Risk", label + " Risk"));
    out.add(ReportColumn.amount(prefix + RiLayer.RETENTION.name(), label + " Retention"));
    out.add(ReportColumn.amount(prefix + RiLayer.QUOTA_SHARE.name(), label + " QS"));
    out.add(ReportColumn.amount(prefix + RiLayer.SURPLUS.name(), label + " Surplus"));
    out.add(ReportColumn.amount(prefix + RiLayer.FAC.name(), label + " FAC"));
  }

  private static int bandOf(BigDecimal si) {
    int band = 0;
    for (int i = 0; i < BANDS.length; i++) {
      if (si.compareTo(BigDecimal.valueOf(BANDS[i])) >= 0) {
        band = i;
      }
    }
    return band;
  }

  private static String label(int band) {
    String lower = String.format(Locale.ROOT, "%,d", BANDS[band]);
    return band == BANDS.length - 1
        ? lower + " and above"
        : lower + " - " + String.format(Locale.ROOT, "%,d", BANDS[band + 1]);
  }

  private static void add(Map<String, Object> row, String key, BigDecimal value) {
    row.merge(key, value, RiskProfileReport::plus);
  }

  private static Object plus(Object a, Object b) {
    return ((BigDecimal) a).add((BigDecimal) b);
  }
}
