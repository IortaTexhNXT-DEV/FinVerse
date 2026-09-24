package com.iortatechnxt.brokerverse.crm.report;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import com.iortatechnxt.brokerverse.crm.service.KycDueQuery;
import com.iortatechnxt.brokerverse.crm.service.KycReviewService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
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
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * KYC reviews due (NB-KYC-DUE, BRNB.110): clients whose periodic KYC review is overdue or falls due
 * by a date, by default non-bank clients (bank clients are reviewed by BDO). The report gives the
 * KYC Reviews Due screen its download and print.
 */
@Component
public class KycReviewDueReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "NB-KYC-DUE";

  private static final int MAX_ROWS = 20_000;
  private static final String BANK = "bankClients";
  private static final String NON_BANK = "NON_BANK";
  private static final String BANK_ONLY = "BANK";
  private static final String ALL = "ALL";
  private static final String RISK = "riskRating";

  private final KycReviewService reviews;
  private final LovService lovs;

  /**
   * Creates the report.
   *
   * @param reviews KYC review service
   * @param lovs lists of values (labels)
   */
  public KycReviewDueReport(KycReviewService reviews, LovService lovs) {
    this.reviews = reviews;
    this.lovs = lovs;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        CODE,
        "KYC Reviews Due",
        ReportCategory.CONTROL,
        "Clients whose periodic KYC review is overdue or falls due by the date (BRNB.110)",
        List.of(
            ParameterSpec.required("companyId", "Company", ParameterType.COMPANY),
            ParameterSpec.optional("dueBy", "Due By", ParameterType.DATE),
            ParameterSpec.select(BANK, "Clients", List.of(NON_BANK, BANK_ONLY, ALL), NON_BANK),
            ParameterSpec.optional(RISK, "Risk Rating", ParameterType.TEXT)),
        Permission.CLIENT_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String bank = p.text(BANK);
    Boolean bankClient = ALL.equals(bank) ? null : BANK_ONLY.equals(bank);
    KycDueQuery query =
        new KycDueQuery(
            p.longValue("companyId"),
            p.optionalDate("dueBy").orElse(null),
            bankClient,
            p.optionalText(RISK).orElse(null),
            null);
    List<Map<String, Object>> rows =
        reviews
            .due(query, PageRequest.of(0, MAX_ROWS, Sort.by("kycReviewDue", "displayName")))
            .getContent()
            .stream()
            .map(this::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("code", "Client Code"),
            ReportColumn.text("name", "Client"),
            ReportColumn.text("type", "Type"),
            ReportColumn.text("segment", "Segment"),
            ReportColumn.text("bank", "Bank Client"),
            ReportColumn.text("risk", "Risk Rating"),
            ReportColumn.date("verified", "Last Verified"),
            ReportColumn.date("due", "Review Due"),
            ReportColumn.text("state", "KYC"))
        .rows(rows)
        .withoutGrandTotal()
        .presorted()
        .build();
  }

  private Map<String, Object> row(Client c) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("code", c.getCode());
    m.put("name", c.getDisplayName());
    m.put("type", c.getClientType().name());
    m.put("segment", lovs.label("MARKET_SEGMENT", c.getMarketSegment()));
    m.put("bank", c.isBankClient() ? "Yes" : "No");
    m.put("risk", lovs.label("KYC_RISK_RATING", c.getRiskRating()));
    m.put(
        "verified",
        c.getKycVerifiedAt() == null
            ? null
            : c.getKycVerifiedAt().atZone(ZoneOffset.UTC).toLocalDate());
    m.put("due", c.getKycReviewDue());
    m.put("state", c.getKycStatus() == KycStatus.EXPIRED ? "Overdue (expired)" : "Due");
    return m;
  }
}
