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
import com.iortatechnxt.brokerverse.organization.domain.Branch;
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

  private final ReceiptSeriesRepository series;
  private final CashieringSettings settings;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param series series
   * @param settings settings
   * @param alerts alerts
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ReceiptSeriesService(
      ReceiptSeriesRepository series,
      CashieringSettings settings,
      AlertService alerts,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.series = series;
    this.settings = settings;
    this.alerts = alerts;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
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
          "OR_HEAD_OFFICE_ONLY", "Official receipt series belong to Head Office only (CSHID.006)");
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
    ReceiptSeries saved = series.save(created);
    audit.record(
        ENTITY,
        saved.getPrefix(),
        AuditAction.CREATE,
        saved.getKind() + " " + saved.getFromNo() + "-" + saved.getToNo() + " " + branch.getCode());
    return saved;
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
    List<ReceiptSeries> usable = series.lockUsable(companyId, branchId, kind, RecordStatus.ACTIVE);
    if (usable.isEmpty()) {
      throw new BusinessRuleException(
          "RECEIPT_SERIES_DEPLETED",
          "No authorized " + kind + " series with numbers left for this branch (CSHID.015)");
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
   */
  public record SeriesRequest(
      Long companyId,
      Long branchId,
      ReceiptKind kind,
      String prefix,
      long fromNo,
      long toNo,
      String atpNo,
      int warnAt) {}

  /**
   * An allocated number.
   *
   * @param seriesId series
   * @param number receipt number
   */
  public record Allocation(Long seriesId, String number) {}
}
