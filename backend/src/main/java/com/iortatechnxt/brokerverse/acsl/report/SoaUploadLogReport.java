package com.iortatechnxt.brokerverse.acsl.report;

import com.iortatechnxt.brokerverse.acsl.domain.SoaLine;
import com.iortatechnxt.brokerverse.acsl.domain.SoaUpload;
import com.iortatechnxt.brokerverse.acsl.service.AcslQueryService;
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
import org.springframework.stereotype.Component;

/**
 * SOA upload log (ACSL-SOA-UPLOAD-LOG, ACSL 2.4.0 Addendum 1): every row of an insurer SOA upload
 * with its status, loaded or failed with the reason, proving that every row of the file was loaded
 * or accounted for.
 */
@Component
public class SoaUploadLogReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "ACSL-SOA-UPLOAD-LOG";

  private final AcslQueryService queries;

  /**
   * Creates the report.
   *
   * @param queries ACSL reads
   */
  public SoaUploadLogReport(AcslQueryService queries) {
    this.queries = queries;
  }

  @Override
  public ReportMetadata metadata() {
    return ReportMetadata.acsl(
        CODE,
        "SOA Upload Log",
        "Every row of an insurer SOA upload, loaded or failed with the reason (ACSL 2.4.0)",
        List.of(
            ParameterSpec.required("companyId", "Company", ParameterType.COMPANY),
            ParameterSpec.required(SoaReconReport.UPLOAD, "SOA Upload No.", ParameterType.TEXT)));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    SoaUpload upload = queries.upload(p.text(SoaReconReport.UPLOAD).strip());
    List<Map<String, Object>> rows =
        queries.uploadLog(upload.getId()).stream().map(SoaUploadLogReport::row).toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.count("rowNo", "Row"),
            ReportColumn.text("status", "Status"),
            ReportColumn.text("invoiceNo", "Invoice No."),
            ReportColumn.text("policyNo", "Policy No."),
            ReportColumn.text("assured", "Assured"),
            ReportColumn.amount("balance", "Balance"),
            ReportColumn.text("error", "Reason"))
        .rows(rows)
        .presorted()
        .note(
            upload.getFileName()
                + ": "
                + upload.getRowsRead()
                + " read, "
                + upload.getRowsLoaded()
                + " loaded, "
                + upload.getRowsFailed()
                + " failed")
        .build();
  }

  private static Map<String, Object> row(SoaLine l) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("rowNo", l.getRowNo());
    m.put("status", l.getStatus());
    m.put("invoiceNo", l.getInvoiceNo());
    m.put("policyNo", l.getPolicyNo());
    m.put("assured", l.getAssuredName());
    m.put("balance", l.getBalance());
    m.put("error", l.getError());
    return m;
  }
}
