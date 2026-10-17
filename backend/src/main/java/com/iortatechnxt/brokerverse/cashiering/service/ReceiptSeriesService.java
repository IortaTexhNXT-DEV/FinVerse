package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptSeries;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptSeriesRepository;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Receipt series master (CSHID.006/015, OQ05): AR series per branch and OR series for Head Office
 * only, maker-checker, number allocation under a row lock, refusal of a depleted series and the
 * {@code RECEIPT_SERIES_LOW} alert at the warning threshold.
 */
@Service
@Transactional
public class ReceiptSeriesService {

  /** Audit entity. */
  public static final String ENTITY = "ReceiptSeries";

  private static final String LOW_ALERT = "RECEIPT_SERIES_LOW";
  private static final String NUMBER_FORMAT = "CASH_RECEIPT_NO_FORMAT";

  private final ReceiptSeriesRepository series;
  private final CashieringSettings settings;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final SystemParameterService parameters;

  /**
   * Creates the service.
   *
   * @param series series
   * @param settings settings
   * @param alerts alerts
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   * @param parameters number format proposed for a new series (C4)
   */
  public ReceiptSeriesService(
      ReceiptSeriesRepository series,
      CashieringSettings settings,
      AlertService alerts,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock,
      SystemParameterService parameters) {
    this.series = series;
    this.settings = settings;
    this.alerts = alerts;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
    this.parameters = parameters;
  }

  /**
   * Series of a company.
   *
   * @param companyId company
   * @return series
   */
  @Transactional(readOnly = true)
  public List<ReceiptSeries> list(Long companyId) {
    return series.findByCompanyIdOrderByBranchIdAscKindAscIdAsc(companyId);
  }

  /**
   * Creates a series, pending authorization.
   *
   * @param request branch, kind, prefix, range, ATP and warning threshold
   * @return series
   */
  public ReceiptSeries create(SeriesRequest request) {
    Branch branch = settings.branch(request.companyId(), request.branchId());
    if (request.kind() == ReceiptKind.OR && !branch.isHeadOffice()) {
      throw new BusinessRuleException(
          "OR_HEAD_OFFICE_ONLY", "Official receipt series belong to Head Office only");
    }
    ReceiptSeries created =
        new ReceiptSeries(
            request.companyId(),
            branch.getId(),
            request.kind(),
            request.prefix().strip(),
            request.fromNo(),
            request.toNo());
    created.update(request.atpNo(), request.toNo(), request.warnAt());
    Numbering numbering = request.numbering() == null ? Numbering.PLAIN : request.numbering();
    String format = numbering.format() == null ? defaultFormat() : numbering.format().strip();
    Integer year = numbering.year();
    if (year == null && format.contains("{YEAR}")) {
      year = BusinessClock.today(clock).getYear();
    }
    created.numbering(year, resolve(format, created.getPrefix(), year, branch.getCode()));
    ReceiptSeries saved = series.save(created);
    audit.record(
        ENTITY,
        saved.getPrefix(),
        AuditAction.CREATE,
        saved.getKind() + " " + saved.getFromNo() + "-" + saved.getToNo() + " " + branch.getCode());
    return saved;
  }

  private String defaultFormat() {
    return parameters.text(NUMBER_FORMAT, "").strip();
  }

  /**
   * Resolves a number format for a series: the prefix, the year and the branch indicator are
   * written in, the sequence stays a token.
   *
   * @param format format, blank for prefix and sequence
   * @param prefix prefix of the series
   * @param year year, may be null
   * @param branchCode branch indicator
   * @return resolved format, null for prefix and sequence
   */
  static String resolve(String format, String prefix, Integer year, String branchCode) {
    if (format == null || format.isBlank()) {
      return null;
    }
    if (!format.contains(ReceiptSeries.SEQ)) {
      throw new BusinessRuleException(
          "SERIES_FORMAT_INVALID", "The number format must hold the sequence {SEQ}");
    }
    String stem = prefix.endsWith("-") ? prefix.substring(0, prefix.length() - 1) : prefix;
    return format
        .replace("{PREFIX}", stem)
        .replace("{YEAR}", year == null ? "" : year.toString())
        .replace("{BRANCH}", branchCode == null ? "" : branchCode);
  }

  /**
   * Extends or changes a series; it must be authorized again.
   *
   * @param id series
   * @param atpNo BIR authority to print
   * @param toNo last number
   * @param warnAt warning threshold
   * @return series
   */
  public ReceiptSeries update(Long id, String atpNo, long toNo, int warnAt) {
    ReceiptSeries s = get(id);
    s.update(atpNo, toNo, warnAt);
    audit.record(ENTITY, s.getPrefix(), AuditAction.UPDATE, "To " + toNo + ", warn at " + warnAt);
    return s;
  }

  /**
   * Authorizes a series (checker, not the maker).
   *
   * @param id series
   * @return series
   */
  public ReceiptSeries authorize(Long id) {
    ReceiptSeries s = get(id);
    s.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, s.getPrefix(), AuditAction.AUTHORIZE, "Authorized");
    return s;
  }

  /**
   * Deactivates a series.
   *
   * @param id series
   * @return series
   */
  public ReceiptSeries deactivate(Long id) {
    ReceiptSeries s = get(id);
    s.deactivate();
    audit.record(ENTITY, s.getPrefix(), AuditAction.DEACTIVATE, "Deactivated");
    return s;
  }

  /**
   * One series.
   *
   * @param id id
   * @return series
   */
  @Transactional(readOnly = true)
  public ReceiptSeries get(Long id) {
    return series.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Allocates the next receipt number of a branch (CSHID.006/015) in the caller's transaction.
   *
   * @param companyId company
   * @param branchId branch
   * @param kind AR or OR
   * @return series and number
   */
  public Allocation allocate(Long companyId, Long branchId, ReceiptKind kind) {
    int year = BusinessClock.today(clock).getYear();
    List<ReceiptSeries> usable =
        series.lockUsable(companyId, branchId, kind, RecordStatus.ACTIVE).stream()
            .filter(r -> r.getSeriesYear() == null || r.getSeriesYear() == year)
            .toList();
    if (usable.isEmpty()) {
      throw new BusinessRuleException(
          "RECEIPT_SERIES_DEPLETED",
          "No authorized " + kind + " series with numbers left for this branch");
    }
    ReceiptSeries s = usable.get(0);
    String number = s.allocate();
    if (s.isLow()) {
      alerts.raise(
          LOW_ALERT,
          new AlertFacts(
              companyId,
              branchId,
              ENTITY,
              String.valueOf(s.getId()),
              kind + " series " + s.getPrefix() + " has " + s.remaining() + " numbers left",
              BigDecimal.valueOf(s.remaining()),
              LOW_ALERT + ":" + s.getId()));
    }
    return new Allocation(s.getId(), number);
  }

  /**
   * A new series.
   *
   * @param companyId company
   * @param branchId branch
   * @param kind AR or OR
   * @param prefix number prefix
   * @param fromNo first number
   * @param toNo last number
   * @param atpNo BIR authority to print, may be null
   * @param warnAt remaining numbers that raise the alert
   * @param numbering year of the series and number format
   */
  public record SeriesRequest(
      Long companyId,
      Long branchId,
      ReceiptKind kind,
      String prefix,
      long fromNo,
      long toNo,
      String atpNo,
      int warnAt,
      Numbering numbering) {}

  /**
   * The year a series serves and its number format (FRS.CSH.02.03.02 / 02.03.03; Appendix R, C4).
   *
   * @param year year, null for every year (the current year when the format holds {YEAR})
   * @param format format with the tokens {PREFIX}, {YEAR}, {BRANCH} (branch indicator) and {SEQ};
   *     null for the format of the setting {@code CASH_RECEIPT_NO_FORMAT}, blank for the prefix
   *     followed by the sequence
   */
  public record Numbering(Integer year, String format) {

    /** Prefix followed by the sequence, every year (series in use at go-live). */
    public static final Numbering PLAIN = new Numbering(null, "");
  }

  /**
   * An allocated number.
   *
   * @param seriesId series
   * @param number receipt number
   */
  public record Allocation(Long seriesId, String number) {}
}
