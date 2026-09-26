package com.iortatechnxt.brokerverse.adjustment.report;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.service.DocText;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Adjustment Report (ADJ-REGISTER, ADJID.019): endorsement requests of the period with their type,
 * amount and reason, filterable by account (ARN), market segment, AO and risk type. Draft layout
 * (OQ42).
 */
@Component
public class AdjustmentRegisterReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "ADJ-REGISTER";

  private static final String ARN = "arn";
  private static final String SEGMENT = "segment";
  private static final String AO = "ao";
  private static final String RISK = "riskType";

  private final AdjustmentReportSupport support;

  /**
   * Creates the report.
   *
   * @param support shared parameters and rows
   */
  public AdjustmentRegisterReport(AdjustmentReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> specs = new ArrayList<>(AdjustmentReportSupport.period("MONTH_START"));
    specs.add(ParameterSpec.optional(ARN, "Account (ARN)", ParameterType.TEXT));
    specs.add(ParameterSpec.optional(SEGMENT, "Market Segment", ParameterType.TEXT));
    specs.add(ParameterSpec.optional(AO, "Account Officer", ParameterType.TEXT));
    specs.add(ParameterSpec.optional(RISK, "Risk Type (product line)", ParameterType.TEXT));
    return ReportMetadata.operations(
        CODE,
        "Adjustment Report",
        "Endorsement requests with type, amount and reason by account, segment, AO and risk type"
            + " (ADJID.019; layout to confirm, OQ42)",
        specs);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Predicate<EndorsementRequest> filter =
        matches(p, ARN, r -> r.getSubject().arn())
            .and(matches(p, SEGMENT, r -> r.getSubject().segment()))
            .and(matches(p, AO, r -> r.getSubject().aoUsername()))
            .and(matches(p, RISK, r -> r.getSubject().productLine()));
    List<Map<String, Object>> rows =
        support.created(p, filter).stream().map(AdjustmentRegisterReport::row).toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("requestNo", "Request No."),
            ReportColumn.date("date", "Date"),
            ReportColumn.text("type", "Endorsement Type"),
            ReportColumn.text("requestType", "Request Type"),
            ReportColumn.amount("premium", "Premium Change"),
            ReportColumn.amount("commission", "Commission Change"),
            ReportColumn.text("reason", "Reason"),
            ReportColumn.text("arn", "ARN"),
            ReportColumn.text("invoice", "Invoice No."),
            ReportColumn.text("segment", "Segment"),
            ReportColumn.text("ao", "AO"),
            ReportColumn.text("risk", "Risk Type"),
            ReportColumn.text("stage", "Status"))
        .rows(rows)
        .presorted()
        .build();
  }

  private static Predicate<EndorsementRequest> matches(
      ReportParameters p, String name, Function<EndorsementRequest, String> field) {
    String wanted = p.optionalText(name).map(AdjustmentReportSupport::normal).orElse(null);
    return r -> wanted == null || wanted.equals(AdjustmentReportSupport.normal(field.apply(r)));
  }

  private static Map<String, Object> row(EndorsementRequest r) {
    Map<String, Object> m = AdjustmentReportSupport.requestRow(r);
    m.put("reason", DocText.text(r.getTerms().reasonCode()));
    m.put("segment", DocText.text(r.getSubject().segment()));
    m.put("ao", DocText.text(r.getSubject().aoUsername()));
    m.put("risk", DocText.text(r.getSubject().productLine()));
    return m;
  }
}
