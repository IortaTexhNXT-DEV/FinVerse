package com.iortatechnxt.brokerverse.adjustment.report;

import com.iortatechnxt.brokerverse.adjustment.domain.ComponentChange;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequestRepository;
import com.iortatechnxt.brokerverse.adjustment.service.DocText;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Shared parameters and row values of the adjustment reports (OPERATIONS_DESIGN section 11). The
 * layouts the BRD does not give are drafts to confirm (OQ42).
 */
@Component
public class AdjustmentReportSupport {

  /** Company parameter. */
  public static final String COMPANY = "companyId";

  /** Period start parameter. */
  public static final String FROM = "from";

  /** Period end parameter. */
  public static final String TO = "to";

  /** Endorsement type filter. */
  public static final String TYPE = "endorsementType";

  /** Requester / processor filter. */
  public static final String USER = "user";

  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private final EndorsementRequestRepository requests;

  /**
   * Creates the support.
   *
   * @param requests requests
   */
  public AdjustmentReportSupport(EndorsementRequestRepository requests) {
    this.requests = requests;
  }

  /**
   * Company and period parameters.
   *
   * @param fromDefault default of the period start (TODAY or MONTH_START)
   * @return parameters
   */
  public static List<ParameterSpec> period(String fromDefault) {
    return List.of(
        ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY),
        ParameterSpec.required(FROM, "From", ParameterType.DATE).withDefault(fromDefault),
        ParameterSpec.required(TO, "To", ParameterType.DATE).withDefault("TODAY"));
  }

  /**
   * Company, period, type and user parameters.
   *
   * @param fromDefault default of the period start
   * @return parameters
   */
  public static List<ParameterSpec> periodTypeUser(String fromDefault) {
    List<ParameterSpec> specs = new ArrayList<>(period(fromDefault));
    specs.add(ParameterSpec.optional(TYPE, "Endorsement Type", ParameterType.TEXT));
    specs.add(ParameterSpec.optional(USER, "User", ParameterType.TEXT));
    return specs;
  }

  /**
   * Requests created in the period, filtered.
   *
   * @param p parameters
   * @param filter extra filter
   * @return requests
   */
  public List<EndorsementRequest> created(
      ReportParameters p, Predicate<EndorsementRequest> filter) {
    return requests.createdBetween(p.longValue(COMPANY), start(p), end(p)).stream()
        .filter(filter)
        .toList();
  }

  /**
   * Requests posted in the period.
   *
   * @param p parameters
   * @return requests
   */
  public List<EndorsementRequest> posted(ReportParameters p) {
    return requests.postedBetween(p.longValue(COMPANY), start(p), end(p));
  }

  /**
   * The type and user filter of a report.
   *
   * @param p parameters
   * @return filter
   */
  public static Predicate<EndorsementRequest> typeAndUser(ReportParameters p) {
    String type = p.optionalText(TYPE).map(AdjustmentReportSupport::normal).orElse(null);
    String user = p.optionalText(USER).map(String::strip).orElse(null);
    return r ->
        (type == null || type.equals(r.getTerms().endorsementType()))
            && (user == null
                || CurrentUser.sameUser(user, r.getCreatedBy())
                || CurrentUser.sameUser(user, r.trail().postedBy()));
  }

  /**
   * A filter value as compared (trimmed; codes are compared exactly).
   *
   * @param value value, may be null
   * @return trimmed value, null when none
   */
  public static String normal(String value) {
    return value == null ? null : value.strip();
  }

  /**
   * Start of the period.
   *
   * @param p parameters
   * @return first instant of the start date (Manila)
   */
  public static Instant start(ReportParameters p) {
    return p.date(FROM).atStartOfDay(MANILA).toInstant();
  }

  /**
   * End of the period.
   *
   * @param p parameters
   * @return first instant after the end date (Manila)
   */
  public static Instant end(ReportParameters p) {
    return p.date(TO).plusDays(1).atStartOfDay(MANILA).toInstant();
  }

  /**
   * The change of a component of a request.
   *
   * @param r request
   * @param component component
   * @return change, zero when none
   */
  public static BigDecimal change(EndorsementRequest r, LedgerComponent component) {
    return r.getChanges().stream()
        .filter(c -> c.component() == component)
        .map(ComponentChange::delta)
        .findFirst()
        .orElse(BigDecimal.ZERO);
  }

  /**
   * The common columns of a request row.
   *
   * @param r request
   * @return values by key
   */
  public static Map<String, Object> requestRow(EndorsementRequest r) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("requestNo", r.getRequestNo());
    m.put("date", DocText.date(r.getCreatedAt()));
    m.put("class", r.getRequestClass().name());
    m.put("type", r.getTerms().endorsementType());
    m.put("requestType", DocText.text(r.getTerms().requestType()));
    m.put("invoice", r.getSubject().invoiceNo());
    m.put("arn", r.getSubject().arn());
    m.put("assured", r.getSubject().assuredName());
    m.put("insurer", r.getSubject().insurerCode());
    m.put("stage", r.getStage().name());
    m.put("premium", change(r, LedgerComponent.DTIP));
    m.put("commission", change(r, LedgerComponent.COMMISSION));
    return m;
  }

  /**
   * A date of a request trail, as a Manila date.
   *
   * @param at instant
   * @return date or null
   */
  public static LocalDate day(Instant at) {
    return DocText.date(at);
  }
}
