package com.iortatechnxt.brokerverse.acsl.report;

import com.iortatechnxt.brokerverse.acsl.domain.BookFigures;
import com.iortatechnxt.brokerverse.acsl.domain.ReconBucket;
import com.iortatechnxt.brokerverse.acsl.domain.ReconResult;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * SOA reconciliation report (ACSL-SOA-RECON, ACSL 2.14.0-2.14.1, Appendix C): the latest
 * reconciliation of an insurer SOA upload per invoice, grouped by bucket (Outstanding, For
 * remittance, Remitted, Cancelled, Direct billed, Not found) with BDOI's figures, the remittance
 * and 2307 batches and dates, the cancellation reference, the SOA balance and the variances. The
 * screen downloads it as "Insurer_Covered period" (ACSL 2.14.1).
 */
@Component
public class SoaReconReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "ACSL-SOA-RECON";

  /** Upload parameter. */
  public static final String UPLOAD = "uploadNo";

  private static final String BUCKET = "bucket";
  private static final String ALL = "ALL";

  private final AcslQueryService queries;

  /**
   * Creates the report.
   *
   * @param queries ACSL reads
   */
  public SoaReconReport(AcslQueryService queries) {
    this.queries = queries;
  }

  @Override
  public ReportMetadata metadata() {
    List<String> buckets = new ArrayList<>(List.of(ALL));
    for (ReconBucket b : ReconBucket.values()) {
      buckets.add(b.name());
    }
    return ReportMetadata.acsl(
        CODE,
        "SOA Reconciliation",
        "Insurer statement of account reconciled per invoice against BDOI's books (ACSL 2.14.1)",
        List.of(
            ParameterSpec.required("companyId", "Company", ParameterType.COMPANY),
            ParameterSpec.required(UPLOAD, "SOA Upload No.", ParameterType.TEXT),
            ParameterSpec.select(BUCKET, "Bucket", buckets, ALL)));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    SoaUpload upload = queries.upload(p.text(UPLOAD).strip());
    String bucket = p.optionalText(BUCKET).orElse(ALL);
    List<Map<String, Object>> rows =
        queries.results(upload, ALL.equals(bucket) ? null : ReconBucket.valueOf(bucket)).stream()
            .map(SoaReconReport::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.count("rowNo", "Row"),
            ReportColumn.text("invoiceNo", "Invoice No."),
            ReportColumn.text("policyNo", "Policy No."),
            ReportColumn.text("assured", "Assured"),
            ReportColumn.amount("bookPremium", "Premium (Books)"),
            ReportColumn.amount("outstanding", "Outstanding"),
            ReportColumn.amount("forRemittance", "For Remittance"),
            ReportColumn.amount("remitted", "Remitted"),
            ReportColumn.text("remittance", "Remittance Batch / Date"),
            ReportColumn.amount("pr2307", "2307 Amount"),
            ReportColumn.text("batch2307", "2307 Batch / Date"),
            ReportColumn.text("cancellation", "Cancellation Ref."),
            ReportColumn.text("directBilled", "Direct Billed"),
            ReportColumn.amount("soaPremium", "Premium (SOA)"),
            ReportColumn.amount("soaBalance", "SOA Balance"),
            ReportColumn.amount("premiumVariance", "Premium Variance"),
            ReportColumn.amount("outstandingVariance", "Outstanding Variance"))
        .groupBy(BUCKET, "Bucket")
        .rows(rows)
        .note(
            upload.getInsurerCode() + " " + upload.getPeriodFrom() + " to " + upload.getPeriodTo())
        .build();
  }

  private static Map<String, Object> row(ReconResult r) {
    BookFigures b = r.bookOrNone();
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(BUCKET, r.getBucket().name());
    m.put("rowNo", r.getRowNo());
    m.put("invoiceNo", r.getInvoiceNo());
    m.put("policyNo", r.getPolicyNo());
    m.put("assured", r.getAssuredName());
    m.put("bookPremium", b.premium());
    m.put("outstanding", b.outstanding());
    m.put("forRemittance", b.forRemittance());
    m.put("remitted", b.remitted());
    m.put("remittance", joined(b.remittanceBatchNo(), b.remittanceDate()));
    m.put("pr2307", b.pr2307Amount());
    m.put("batch2307", joined(b.pr2307BatchNo(), b.pr2307Date()));
    m.put("cancellation", b.cancellationRef());
    m.put("directBilled", b.directBilled() ? "Y" : "N");
    m.put("soaPremium", r.getSoaPremium());
    m.put("soaBalance", r.getSoaBalance());
    m.put("premiumVariance", r.getPremiumVariance());
    m.put("outstandingVariance", r.getOutstandingVariance());
    return m;
  }

  private static String joined(String batch, Object date) {
    if (batch == null && date == null) {
      return null;
    }
    return (batch == null ? "" : batch) + (date == null ? "" : " / " + date);
  }
}
