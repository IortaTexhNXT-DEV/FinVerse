package com.iortatechnxt.brokerverse.payrequest.report;

import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestKind;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestQueryService;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Parameters and rows shared by the request reports (MKT 1.18.1): company, request-date range and
 * an optional kind; viewed and exported by the holders of {@code PRQ_VIEW}, archived.
 */
@Component
public class PayRequestReports {

  /** Company parameter. */
  public static final String COMPANY = "companyId";

  /** Start of the period. */
  public static final String FROM = "from";

  /** End of the period. */
  public static final String TO = "to";

  /** Request kind parameter. */
  public static final String KIND = "kind";

  private static final String ALL = "ALL";

  private final PayRequestQueryService queries;

  /**
   * Creates the helper.
   *
   * @param queries requests
   */
  public PayRequestReports(PayRequestQueryService queries) {
    this.queries = queries;
  }

  /**
   * Catalogue entry of a request report.
   *
   * @param code report code
   * @param title title
   * @param description one line purpose
   * @return metadata in the category Refund and Cash Advance Requests
   */
  public static ReportMetadata metadata(String code, String title, String description) {
    List<ParameterSpec> specs = new ArrayList<>();
    specs.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    specs.add(ParameterSpec.required(FROM, "From", ParameterType.DATE).withDefault("MONTH_START"));
    specs.add(ParameterSpec.required(TO, "To", ParameterType.DATE).withDefault("TODAY"));
    specs.add(
        ParameterSpec.select(
            KIND,
            "Request Kind",
            List.of(
                ALL,
                RequestKind.REFUND.name(),
                RequestKind.CASH_ADVANCE.name(),
                RequestKind.CHECK_CANCELLATION.name()),
            ALL));
    return new ReportMetadata(
        code,
        title,
        ReportCategory.PAYMENT_REQUESTS,
        description,
        specs,
        Permission.PRQ_VIEW,
        Permission.PRQ_VIEW,
        true);
  }

  /**
   * Requests of the period and kind.
   *
   * @param p parameters
   * @return requests, oldest first
   */
  public List<PaymentRequest> requests(ReportParameters p) {
    Optional<String> kind = p.optionalText(KIND).filter(k -> !ALL.equals(k));
    return queries.period(p.longValue(COMPANY), p.date(FROM), p.date(TO)).stream()
        .filter(r -> kind.isEmpty() || r.getKind().name().equals(kind.get()))
        .toList();
  }

  /**
   * The columns every request row has.
   *
   * @param r request
   * @return row
   */
  public static Map<String, Object> row(PaymentRequest r) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("requestNo", r.getRequestNo());
    m.put("date", r.getRequestDate());
    m.put(KIND, r.getKind().name());
    m.put("payee", r.getPayee().name());
    m.put("amount", r.getAmount());
    m.put("currency", r.getContent().currency());
    m.put("stage", r.getStage().name());
    return m;
  }
}
