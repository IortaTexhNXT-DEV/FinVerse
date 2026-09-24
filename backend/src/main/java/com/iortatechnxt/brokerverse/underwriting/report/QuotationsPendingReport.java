package com.iortatechnxt.brokerverse.underwriting.report;

import static com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport.K_CLASS;
import static com.iortatechnxt.brokerverse.underwriting.report.UwReportSupport.K_SHARE;

import com.iortatechnxt.brokerverse.common.util.Money;
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
import com.iortatechnxt.brokerverse.underwriting.domain.Quotation;
import com.iortatechnxt.brokerverse.underwriting.domain.QuotationIteration;
import com.iortatechnxt.brokerverse.underwriting.domain.QuotationStatus;
import com.iortatechnxt.brokerverse.underwriting.service.QuotationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR042 List of Quotations Pending Approval, at 100 % and our share. Pending days 0 lists all
 * pending quotations, otherwise those pending at least that many days at the as-of date
 * (BrokerVerse rule: as-of − issue date ≥ pending days). Branch &gt; Class &gt; Customer.
 */
@Component
public class QuotationsPendingReport implements ReportDefinition {

  private static final String AS_OF = "asOfDate";
  private static final String PENDING_DAYS = "pendingDays";
  private static final String ISSUE_FROM = "issueFrom";
  private static final String ISSUE_TO = "issueTo";
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final QuotationService quotations;
  private final UwReportSupport support;

  /**
   * Creates the report.
   *
   * @param quotations quotation service
   * @param support shared report support
   */
  public QuotationsPendingReport(QuotationService quotations, UwReportSupport support) {
    this.quotations = quotations;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = UwReportSupport.rangeParams();
    params.add(ParameterSpec.optional(ISSUE_FROM, "Issue Date From", ParameterType.DATE));
    params.add(ParameterSpec.optional(ISSUE_TO, "Issue Date To", ParameterType.DATE));
    params.add(
        ParameterSpec.required(AS_OF, "As of Date", ParameterType.DATE).withDefault("TODAY"));
    params.add(
        ParameterSpec.required(PENDING_DAYS, "Pending Days", ParameterType.NUMBER)
            .withDefault("0"));
    return new ReportMetadata(
        "PGIBR042",
        "List of Quotations Pending Approval",
        ReportCategory.UNDERWRITING,
        "Quotations awaiting approval with 100% and our share figures",
        params,
        Permission.POLICY_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(UwReportSupport.COMPANY);
    LocalDate asOf = p.date(AS_OF);
    long pendingDays = p.optionalDecimal(PENDING_DAYS).orElse(BigDecimal.ZERO).longValue();
    UwFilters f = UwReportSupport.filters(p);
    Map<Long, String> branches = support.branchCodes(companyId);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Quotation q :
        quotations.search(
            companyId,
            QuotationStatus.PENDING_APPROVAL,
            p.optionalDate(ISSUE_FROM).orElse(null),
            p.optionalDate(ISSUE_TO).orElse(null))) {
      boolean pendingLongEnough =
          pendingDays == 0 || ChronoUnit.DAYS.between(q.getIssueDate(), asOf) >= pendingDays;
      if (pendingLongEnough && f.test(q)) {
        rows.add(row(q, branches));
      }
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("quotationNo", "Quotation No"),
            ReportColumn.count("iteration", "Iteration"),
            ReportColumn.date("issueDate", "Issue Date"),
            ReportColumn.text("customer", "Customer"),
            ReportColumn.date("periodFrom", "Period From"),
            ReportColumn.date("periodTo", "Period To"),
            ReportColumn.percent(K_SHARE, "Our Share %"),
            ReportColumn.amount("si100", "100% SI"),
            ReportColumn.amount("ourSi", "Our SI"),
            ReportColumn.amount("gross100", "100% Gross"),
            ReportColumn.amount("ourGross", "Our Gross"),
            ReportColumn.amount("disc100", "100% Disc"),
            ReportColumn.amount("ourDisc", "Our Disc"),
            ReportColumn.amount("load100", "100% Loading"),
            ReportColumn.amount("ourLoad", "Our Loading"),
            ReportColumn.amount("charges", "Charges"),
            ReportColumn.amount("net100", "100% Net"),
            ReportColumn.amount("ourNet", "Our Net"),
            ReportColumn.amount("brokerage", "Brokerage"))
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Class")
        .groupBy("customerGroup", "Customer")
        .rows(rows)
        .build();
  }

  private static Map<String, Object> row(Quotation q, Map<Long, String> branches) {
    QuotationIteration it = q.current();
    BigDecimal share = q.getSharePct();
    BigDecimal ourNet = our(it.netPremium(), share);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(K_BRANCH, branches.getOrDefault(q.getBranchId(), ""));
    m.put(K_CLASS, q.getProduct().getBusinessLine());
    m.put("customerGroup", q.getCustomer().getCode() + " " + q.getCustomer().getName());
    m.put("quotationNo", q.getQuotationNo());
    m.put("iteration", it.getIterationNo());
    m.put("issueDate", q.getIssueDate());
    m.put("customer", q.getCustomer().getName());
    m.put("periodFrom", q.getPeriodFrom());
    m.put("periodTo", q.getPeriodTo());
    m.put(K_SHARE, share);
    m.put("si100", it.getSumInsured());
    m.put("ourSi", our(it.getSumInsured(), share));
    m.put("gross100", it.getGrossPremium());
    m.put("ourGross", our(it.getGrossPremium(), share));
    m.put("disc100", it.getDiscount());
    m.put("ourDisc", our(it.getDiscount(), share));
    m.put("load100", it.getLoading());
    m.put("ourLoad", our(it.getLoading(), share));
    m.put("charges", it.getCharges());
    m.put("net100", it.netPremium());
    m.put("ourNet", ourNet);
    m.put("brokerage", our(ourNet, q.getCommissionRate()));
    return m;
  }

  private static BigDecimal our(BigDecimal amount, BigDecimal pct) {
    return Money.round(
        amount.multiply(pct).divide(HUNDRED, Money.RATE_SCALE, RoundingMode.HALF_EVEN));
  }
}
