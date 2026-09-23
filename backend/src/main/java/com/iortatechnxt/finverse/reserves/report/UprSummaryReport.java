package com.iortatechnxt.finverse.reserves.report;

import static com.iortatechnxt.finverse.reserves.report.ReserveReportSupport.K_BRANCH;
import static com.iortatechnxt.finverse.reserves.report.ReserveReportSupport.K_CLASS;
import static com.iortatechnxt.finverse.reserves.report.ReserveReportSupport.K_PRODUCT;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.reserves.domain.PremiumAmounts;
import com.iortatechnxt.finverse.reserves.domain.ReserveParameterTerms;
import com.iortatechnxt.finverse.reserves.domain.UprItem;
import com.iortatechnxt.finverse.reserves.service.Percent;
import com.iortatechnxt.finverse.reserves.service.PremiumPortfolio;
import com.iortatechnxt.finverse.reserves.service.ReserveParameterService;
import com.iortatechnxt.finverse.reserves.service.ReserveParameterSet;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR072 UPR Summary Report: per premium transaction (Detail) or per product (Summary), the
 * earning units and the earned or unearned premium and commission, gross and for treaty and FAC
 * cessions, at the UPR processed (valuation) date. Unearned = amount × unexpired units / total
 * units on the product's basis (1/365 daily pro-rata by default, 1/24 or 1/8). Branch &gt; Class.
 */
@Component
public class UprSummaryReport implements ReportDefinition {

  private static final String OPTION = "option";
  private static final String LEVEL = "level";
  private static final String UNEARNED = "UNEARNED";
  private static final String DETAIL = "DETAIL";
  private static final String[] AMOUNTS = {
    "gross", "commission", "premium", "comm", "treatyPrem", "treatyComm", "facPrem", "facComm"
  };

  private final ReserveReportSupport support;
  private final PremiumPortfolio portfolio;
  private final ReserveParameterService parameters;

  /**
   * Creates the report.
   *
   * @param support report support
   * @param portfolio premium portfolio
   * @param parameters reserve parameters (RI commission rates)
   */
  public UprSummaryReport(
      ReserveReportSupport support,
      PremiumPortfolio portfolio,
      ReserveParameterService parameters) {
    this.support = support;
    this.portfolio = portfolio;
    this.parameters = parameters;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = ReserveReportSupport.baseParams("UPR Processed Date");
    params.add(
        ParameterSpec.select(OPTION, "Earned / Unearned", List.of(UNEARNED, "EARNED"), UNEARNED));
    params.add(ParameterSpec.select(LEVEL, "Detail / Summary", List.of(DETAIL, "SUMMARY"), DETAIL));
    return new ReportMetadata(
        "PGIBR072",
        "UPR Summary Report",
        ReportCategory.ACTUARIAL,
        "Earned and unearned premium and commission per policy, gross, treaty and FAC",
        params,
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(ReserveReportSupport.COMPANY);
    LocalDate date = p.date(ReserveReportSupport.DATE);
    boolean unearned = UNEARNED.equals(p.text(OPTION));
    boolean detail = DETAIL.equals(p.text(LEVEL));
    ReserveParameterSet params = parameters.inForce(companyId, date);
    Map<Long, String> branches = support.branchCodes(companyId);
    Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
    for (UprItem i : portfolio.load(companyId, date, params).uprAt(date)) {
      if (!ReserveReportSupport.matches(p, i.key())) {
        continue;
      }
      Map<String, Object> row = new LinkedHashMap<>();
      ReserveReportSupport.putKey(row, i.key(), branches);
      BigDecimal[] values = values(i, unearned, params.terms(i.key().businessLine()));
      if (detail) {
        putDetail(row, i);
        putAmounts(row, values);
        rows.put(i.policyId() + "|" + i.endorsementNo(), row);
      } else {
        Map<String, Object> total =
            rows.computeIfAbsent(
                row.get(K_BRANCH) + "|" + i.key().businessLine() + "|" + i.key().productCode(),
                k -> row);
        for (int n = 0; n < AMOUNTS.length; n++) {
          total.merge(AMOUNTS[n], values[n], ReserveReportSupport::add);
        }
      }
    }
    String label = unearned ? "Unearned" : "Earned";
    return TabularReportBuilder.of(p)
        .columns(columns(detail, label))
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .rows(new ArrayList<>(rows.values()))
        .note(
            "Company share, base currency. Units: days (1/365), twenty-fourths (1/24) or eighths"
                + " (1/8) per the product's UPR basis. Treaty = quota share + surplus.")
        .build();
  }

  private static void putDetail(Map<String, Object> row, UprItem i) {
    row.put("policy", i.documentNo());
    row.put("period", i.coverFrom() + " - " + i.coverTo());
    row.put("approval", i.approvalDate());
    row.put("basis", i.basis());
    row.put("total", BigDecimal.valueOf(i.units().total()));
    row.put("earned", BigDecimal.valueOf(i.units().earned()));
    row.put("unearned", BigDecimal.valueOf(i.units().unearned()));
  }

  private static void putAmounts(Map<String, Object> row, BigDecimal... values) {
    for (int n = 0; n < AMOUNTS.length; n++) {
      row.put(AMOUNTS[n], values[n]);
    }
  }

  /** Written gross / commission, then the earned or unearned premium and commission splits. */
  private static BigDecimal[] values(UprItem i, boolean unearned, ReserveParameterTerms t) {
    PremiumAmounts w = i.written();
    PremiumAmounts part = unearned ? i.unearned() : i.earned();
    BigDecimal fraction =
        unearned
            ? i.units().unearnedFraction()
            : BigDecimal.ONE.subtract(i.units().unearnedFraction());
    return new BigDecimal[] {
      w.premium(),
      w.commission(),
      part.premium(),
      part.commission(),
      part.treatyPremium(),
      Money.round(Percent.of(w.treatyPremium(), t.treatyCommissionPct()).multiply(fraction)),
      part.facPremium(),
      Money.round(Percent.of(w.facPremium(), t.facCommissionPct()).multiply(fraction))
    };
  }

  private static List<ReportColumn> columns(boolean detail, String label) {
    List<ReportColumn> cols = new ArrayList<>();
    if (detail) {
      cols.add(ReportColumn.text("policy", "Policy / Endorsement"));
      cols.add(ReportColumn.text("period", "Period"));
      cols.add(ReportColumn.date("approval", "Approval Date"));
      cols.add(ReportColumn.text("basis", "Basis"));
      cols.add(ReportColumn.count("total", "Total Units"));
      cols.add(ReportColumn.count("earned", "Earned Units"));
      cols.add(ReportColumn.count("unearned", "Unearned Units"));
    } else {
      cols.add(ReportColumn.text(K_PRODUCT, "Product"));
    }
    cols.add(ReportColumn.amount("gross", "Gross Premium"));
    cols.add(ReportColumn.amount("commission", "Commission"));
    cols.add(ReportColumn.amount("premium", label + " Premium"));
    cols.add(ReportColumn.amount("comm", label + " Commission"));
    cols.add(ReportColumn.amount("treatyPrem", "Treaty " + label + " Premium"));
    cols.add(ReportColumn.amount("treatyComm", "Treaty " + label + " Commission"));
    cols.add(ReportColumn.amount("facPrem", "FAC " + label + " Premium"));
    cols.add(ReportColumn.amount("facComm", "FAC " + label + " Commission"));
    return cols;
  }
}
