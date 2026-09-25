package com.iortatechnxt.brokerverse.acsl.report;

import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.LedgerSearch;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * List of booked accounts with financial details (ACSL-BOOKED-FIN-DETAILS, ACSL 2.14.2, Appendix
 * C): the invoices booked in the period, optionally of one insurer, with their system references
 * (invoice, root invoice, ARN, policy), premium, collected, balance, due to insurer, remitted,
 * commission and the direct-billed indicator.
 */
@Component
public class BookedFinancialDetailsReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "ACSL-BOOKED-FIN-DETAILS";

  private static final String COMPANY = "companyId";
  private static final String FROM = "from";
  private static final String TO = "to";
  private static final String INSURER = "insurer";
  private static final int MAX_ROWS = 20_000;

  private final InvoiceLedgerQueryService ledger;

  /**
   * Creates the report.
   *
   * @param ledger Operations invoice ledger
   */
  public BookedFinancialDetailsReport(InvoiceLedgerQueryService ledger) {
    this.ledger = ledger;
  }

  @Override
  public ReportMetadata metadata() {
    return ReportMetadata.acsl(
        CODE,
        "List of Booked Accounts with Financial Details",
        "Invoices booked in the period with references, premium, payments, remittance and commission"
            + " (ACSL 2.14.2)",
        List.of(
            ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.required(FROM, "From", ParameterType.DATE).withDefault("MONTH_START"),
            ParameterSpec.required(TO, "To", ParameterType.DATE).withDefault("TODAY"),
            ParameterSpec.optional(INSURER, "Insurer", ParameterType.TEXT)));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    LedgerSearch search =
        new LedgerSearch(
            p.longValue(COMPANY),
            null,
            p.optionalText(INSURER).map(String::strip).orElse(null),
            null,
            null,
            null,
            null,
            null,
            null,
            p.date(FROM),
            p.date(TO));
    List<Map<String, Object>> rows =
        ledger.searchLoaded(search, PageRequest.of(0, MAX_ROWS)).stream()
            .map(BookedFinancialDetailsReport::row)
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("insurer", "Insurer"),
            ReportColumn.text("invoiceNo", "Invoice No."),
            ReportColumn.text("rootInvoiceNo", "Root Invoice"),
            ReportColumn.text("arn", "ARN"),
            ReportColumn.text("policyNo", "Policy No."),
            ReportColumn.text("assured", "Assured"),
            ReportColumn.date("booked", "Booked"),
            ReportColumn.text("kind", "Account Status"),
            ReportColumn.amount("premium", "Gross Premium"),
            ReportColumn.amount("collected", "Collected"),
            ReportColumn.amount("balance", "Premium Balance"),
            ReportColumn.amount("dtip", "Due to Insurer"),
            ReportColumn.amount("remitted", "Remitted"),
            ReportColumn.amount("commission", "Commission"),
            ReportColumn.text("directBilled", "Direct Billed"))
        .groupBy("insurer", "Insurer")
        .rows(rows)
        .build();
  }

  private static Map<String, Object> row(OpsInvoice i) {
    Map<String, Object> m = new LinkedHashMap<>();
    BigDecimal balance = i.premiumBalance();
    m.put("insurer", i.getInsurerCode());
    m.put("invoiceNo", i.getInvoiceNo());
    m.put("rootInvoiceNo", i.getRootInvoiceNo());
    m.put("arn", i.getArn());
    m.put("policyNo", i.getPolicyNo());
    m.put("assured", i.getAssuredName());
    m.put("booked", i.getBookingDate());
    m.put("kind", i.getKind().name());
    m.put("premium", i.getGrossPremium());
    m.put("collected", i.getGrossPremium().subtract(balance));
    m.put("balance", balance);
    m.put("dtip", amount(i, LedgerComponent.DTIP, false));
    m.put("remitted", amount(i, LedgerComponent.DTIP, true));
    m.put("commission", i.getCommission());
    m.put("directBilled", i.isDpFlag() ? "Y" : "N");
    return m;
  }

  private static BigDecimal amount(OpsInvoice i, LedgerComponent component, boolean remitted) {
    return i.getComponents().stream()
        .filter(c -> c.getComponent() == component)
        .findFirst()
        .map(remitted ? OpsInvoiceComponent::getRemitted : OpsInvoiceComponent::getBalance)
        .orElse(BigDecimal.ZERO);
  }
}
