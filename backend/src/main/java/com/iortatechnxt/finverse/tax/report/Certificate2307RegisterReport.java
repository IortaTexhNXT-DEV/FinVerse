package com.iortatechnxt.finverse.tax.report;

import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import com.iortatechnxt.finverse.tax.domain.Certificate2307;
import com.iortatechnxt.finverse.tax.domain.CertificateStatus;
import com.iortatechnxt.finverse.tax.domain.TaxPeriod;
import com.iortatechnxt.finverse.tax.service.Certificate2307Service;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * TAX-2307-REG – Register of BIR Form 2307 certificates issued in a year, by quarter. Cancelled
 * certificates are listed with zero amounts so the totals equal the tax certified.
 */
@Component
public class Certificate2307RegisterReport implements ReportDefinition {

  private final Certificate2307Service certificates;
  private final Clock clock;

  /**
   * Creates the report.
   *
   * @param certificates certificates
   * @param clock clock (parameter defaults)
   */
  public Certificate2307RegisterReport(Certificate2307Service certificates, Clock clock) {
    this.certificates = certificates;
    this.clock = clock;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "TAX-2307-REG",
        "BIR Form 2307 Certificate Register",
        ReportCategory.TAX_STATUTORY,
        "Certificates of creditable tax withheld issued per quarter, with batch and status",
        List.of(TaxReportSupport.company(), TaxReportSupport.year(clock)),
        Permission.TAX_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Certificate2307 c :
        certificates.register(TaxReportSupport.companyId(p), TaxReportSupport.year(p))) {
      boolean issued = c.getStatus() == CertificateStatus.ISSUED;
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("quarter", TaxPeriod.quarterOf(c.getPeriodStart()).label());
      row.put("certificateNo", c.getCertificateNo());
      row.put("batchNo", c.getBatch().getBatchNo());
      row.put(TaxReportSupport.TIN, c.payee().formattedTin());
      row.put(TaxReportSupport.PARTY, c.payee().name());
      row.put("status", c.getStatus().name());
      row.put(TaxReportSupport.BASE, issued ? c.getTotalIncome() : BigDecimal.ZERO);
      row.put(TaxReportSupport.TAX, issued ? c.getTotalTax() : BigDecimal.ZERO);
      rows.add(row);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("certificateNo", "Certificate"),
            ReportColumn.text("batchNo", "Batch"),
            ReportColumn.text(TaxReportSupport.TIN, "Payee TIN"),
            ReportColumn.text(TaxReportSupport.PARTY, "Payee"),
            ReportColumn.text("status", "Status"),
            ReportColumn.amount(TaxReportSupport.BASE, "Income payments"),
            ReportColumn.amount(TaxReportSupport.TAX, "Tax withheld"))
        .groupBy("quarter", "Quarter")
        .rows(rows)
        .presorted()
        .build();
  }
}
