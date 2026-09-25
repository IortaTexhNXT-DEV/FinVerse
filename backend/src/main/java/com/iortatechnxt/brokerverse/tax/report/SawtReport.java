package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificate;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificateLine;
import com.iortatechnxt.brokerverse.tax.domain.ReturnFigures;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.service.ReceivedCertificateService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * TAX-SAWT - Summary Alphalist of Withholding Taxes (FRBS 3.2.0, Appendix A VII, list #30): the
 * creditable taxes withheld from BDOI by its withholding agents (insurers) in a quarter, from the
 * received-certificate register (DIS 2.11, V702), one line per agent and ATC. Attached to the
 * income tax return; DAT format to confirm (AQ07).
 */
@Component
public class SawtReport implements ReportDefinition {

  private static final String CERTIFICATES = "certificates";

  private final ReceivedCertificateService certificates;
  private final Clock clock;

  /**
   * Creates the report.
   *
   * @param certificates received certificates
   * @param clock clock (parameter defaults)
   */
  public SawtReport(ReceivedCertificateService certificates, Clock clock) {
    this.certificates = certificates;
    this.clock = clock;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "TAX-SAWT",
        "Summary Alphalist of Withholding Taxes (SAWT)",
        ReportCategory.TAX_STATUTORY,
        "Creditable taxes withheld from BDOI per withholding agent and ATC, from the certificates"
            + " received (FRBS 3.2.0, App. A VII)",
        List.of(
            TaxReportSupport.company(),
            TaxReportSupport.year(clock),
            TaxReportSupport.quarter(clock)),
        Permission.TAX_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    TaxPeriod quarter = TaxReportSupport.quarterPeriod(p);
    Map<String, Map<String, Object>> byAgent = new LinkedHashMap<>();
    for (ReceivedCertificate c : certificates.inPeriod(TaxReportSupport.companyId(p), quarter)) {
      for (ReceivedCertificateLine l : c.getLines()) {
        Map<String, Object> row =
            byAgent.computeIfAbsent(
                c.getAgentCode() + "|" + l.atc(),
                k -> {
                  Map<String, Object> r = new LinkedHashMap<>();
                  r.put(TaxReportSupport.TIN, c.getAgentTin());
                  r.put(TaxReportSupport.PARTY, c.getAgentName());
                  r.put("atc", l.atc());
                  r.put("nature", l.incomeNature());
                  r.put(TaxReportSupport.BASE, BigDecimal.ZERO);
                  r.put(TaxReportSupport.TAX, BigDecimal.ZERO);
                  r.put(CERTIFICATES, 0);
                  return r;
                });
        row.put(
            TaxReportSupport.BASE, ((BigDecimal) row.get(TaxReportSupport.BASE)).add(l.income()));
        row.put(TaxReportSupport.TAX, ((BigDecimal) row.get(TaxReportSupport.TAX)).add(l.tax()));
        row.put(CERTIFICATES, (Integer) row.get(CERTIFICATES) + 1);
      }
    }
    List<Map<String, Object>> rows = new ArrayList<>();
    int seq = 0;
    for (Map<String, Object> row : byAgent.values()) {
      seq++;
      row.put("seq", seq);
      row.put(
          "rate",
          ReturnFigures.effectiveRate(
              (BigDecimal) row.get(TaxReportSupport.BASE),
              (BigDecimal) row.get(TaxReportSupport.TAX)));
      rows.add(row);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("seq", "Seq"),
            ReportColumn.text(TaxReportSupport.TIN, "TIN"),
            ReportColumn.text(TaxReportSupport.PARTY, "Withholding agent"),
            ReportColumn.text("atc", "ATC"),
            ReportColumn.text("nature", "Nature of income"),
            ReportColumn.count(CERTIFICATES, "Certificates"),
            ReportColumn.percent("rate", "Rate"),
            ReportColumn.amount(TaxReportSupport.BASE, "Income payment"),
            ReportColumn.amount(TaxReportSupport.TAX, "Tax withheld"))
        .rows(rows)
        .presorted()
        .note(
            "Quarter "
                + quarter.label()
                + "; certificates whose period covered ends in the quarter; DAT format to confirm"
                + " (AQ07).")
        .build();
  }
}
