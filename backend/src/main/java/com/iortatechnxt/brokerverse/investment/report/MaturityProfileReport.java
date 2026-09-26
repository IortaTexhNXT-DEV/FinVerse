package com.iortatechnxt.brokerverse.investment.report;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.report.gl.GlReportSupport;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** FIN-INV-MAT: holdings on a date bucketed by remaining time to maturity (liquidity profile). */
@Component
public class MaturityProfileReport implements ReportDefinition {

  private static final String BUCKET = "bucket";
  private static final String ORDER = "bucketOrder";
  private static final String DAYS = "days";
  private static final int[] LIMITS = {30, 90, 180, 365, 1095, 1826};
  private static final String[] LABELS = {
    "Due / overdue",
    "Within 1 month",
    "1 - 3 months",
    "3 - 6 months",
    "6 - 12 months",
    "1 - 3 years",
    "3 - 5 years",
    "Over 5 years",
    "No maturity"
  };

  private final InvestmentPositions positions;

  /**
   * Creates the report.
   *
   * @param positions position query
   */
  public MaturityProfileReport(InvestmentPositions positions) {
    this.positions = positions;
  }

  /**
   * Maturity bucket of a holding.
   *
   * @param asOf reporting date
   * @param maturity maturity date (null for equities)
   * @return bucket index into the bucket labels (0 = due / overdue, last = no maturity)
   */
  public static int bucket(LocalDate asOf, LocalDate maturity) {
    if (maturity == null) {
      return LABELS.length - 1;
    }
    long days = ChronoUnit.DAYS.between(asOf, maturity);
    if (days <= 0) {
      return 0;
    }
    int index = 0;
    while (index < LIMITS.length && days > LIMITS[index]) {
      index++;
    }
    return index + 1;
  }

  /**
   * Label of a bucket.
   *
   * @param index bucket index
   * @return label
   */
  public static String label(int index) {
    return LABELS[index];
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-INV-MAT",
        "Investment Maturity Profile",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Holdings bucketed by remaining term to maturity on a date",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.branchParam(),
            GlReportSupport.asOfParam()),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters params) {
    LocalDate asOf = params.date(GlReportSupport.AS_OF);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> position : positions.asOf(params, false)) {
      Object maturityText = position.get(InvestmentPositions.MATURITY);
      LocalDate maturity = maturityText == null ? null : LocalDate.parse(maturityText.toString());
      int index = bucket(asOf, maturity);
      position.put(ORDER, index);
      position.put(BUCKET, label(index));
      position.put(DAYS, maturity == null ? null : ChronoUnit.DAYS.between(asOf, maturity));
      rows.add(position);
    }
    rows.sort(
        Comparator.comparingInt((Map<String, Object> r) -> (Integer) r.get(ORDER))
            .thenComparing(r -> String.valueOf(r.get(InvestmentPositions.MATURITY))));
    return TabularReportBuilder.of(params)
        .columns(
            ReportColumn.text("holdingNo", "Holding No"),
            ReportColumn.text("description", "Description"),
            ReportColumn.text("instrumentType", "Type"),
            ReportColumn.date(InvestmentPositions.MATURITY, "Maturity"),
            ReportColumn.text(DAYS, "Days"),
            ReportColumn.amount(InvestmentPositions.FACE, "Face Value"),
            ReportColumn.amount(InvestmentPositions.CARRYING, "Carrying Amount"),
            ReportColumn.amount(InvestmentPositions.ACCRUED, "Accrued Interest"))
        .groupBy(BUCKET, "Term to Maturity")
        .rows(rows)
        .presorted()
        .build();
  }
}
