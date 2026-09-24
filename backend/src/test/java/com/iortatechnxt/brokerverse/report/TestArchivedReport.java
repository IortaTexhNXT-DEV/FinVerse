package com.iortatechnxt.brokerverse.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** An Operations report for tests: view / export permission split and archive (CSHID.017/018). */
@Component
public class TestArchivedReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "OPS-TEST-ARCHIVED";

  @Override
  public ReportMetadata metadata() {
    return ReportMetadata.operations(
        CODE,
        "Test Operations report",
        "Archived test report",
        List.of(ParameterSpec.optional("note", "Note", ParameterType.TEXT)));
  }

  @Override
  public ReportResult generate(ReportParameters params) {
    return new ReportResult(
        CODE,
        "Test Operations report",
        params.echo(),
        List.of(ReportColumn.text("item", "Item")),
        List.of(ReportRow.detail(Map.of("item", "One"))),
        List.of());
  }
}
