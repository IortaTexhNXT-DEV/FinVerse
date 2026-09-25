package com.iortatechnxt.brokerverse.acsl.report;

import com.iortatechnxt.brokerverse.acsl.domain.GlSlRecon;
import com.iortatechnxt.brokerverse.acsl.domain.GlSlRun;
import com.iortatechnxt.brokerverse.acsl.service.GlSlReconciliationService;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * GL-SL reconciliation report (ACSL-GL-SL-RECON, ACSL 2.13.2): per control account, the GL balance,
 * the sub-ledger balance and the difference, from the latest reconciliation run on or before the
 * date (the daily job {@code ACSL_GL_SL_RECON}, or a run started on the screen).
 */
@Component
public class GlSlReconReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "ACSL-GL-SL-RECON";

  private static final String COMPANY = "companyId";
  private static final String AS_OF = "asOf";

  private final GlSlReconciliationService reconciliation;

  /**
   * Creates the report.
   *
   * @param reconciliation GL-SL runs
   */
  public GlSlReconReport(GlSlReconciliationService reconciliation) {
    this.reconciliation = reconciliation;
  }

  @Override
  public ReportMetadata metadata() {
    return ReportMetadata.acsl(
        CODE,
        "GL-SL Reconciliation",
        "General ledger against sub-ledger per control account, with the difference (ACSL 2.13.2)",
        List.of(
            ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.required(AS_OF, "As Of", ParameterType.DATE).withDefault("TODAY")));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Optional<GlSlRun> run = reconciliation.latest(p.longValue(COMPANY), p.date(AS_OF));
    List<Map<String, Object>> rows =
        run.map(r -> reconciliation.rows(r.getId())).orElse(List.of()).stream()
            .map(GlSlReconReport::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("account", "Control Account"),
            ReportColumn.text("name", "Account Name"),
            ReportColumn.text("source", "Sub-ledger"),
            ReportColumn.amount("gl", "GL Balance"),
            ReportColumn.amount("sl", "SL Balance"),
            ReportColumn.amount("difference", "Difference"))
        .rows(rows)
        .presorted()
        .note(
            run.map(r -> "Run of " + r.getAsOf() + " at " + r.getRunAt())
                .orElse("No reconciliation run on or before this date"))
        .build();
  }

  private static Map<String, Object> row(GlSlRecon r) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("account", r.getAccountCode());
    m.put("name", r.getAccountName());
    m.put("source", r.getSource().name());
    m.put("gl", r.getGlBalance());
    m.put("sl", r.getSlBalance());
    m.put("difference", r.getDifference());
    return m;
  }
}
