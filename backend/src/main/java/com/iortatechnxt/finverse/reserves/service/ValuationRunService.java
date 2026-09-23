package com.iortatechnxt.finverse.reserves.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.period.domain.AccountingPeriodRepository;
import com.iortatechnxt.finverse.reserves.domain.RunStatus;
import com.iortatechnxt.finverse.reserves.domain.TakafulItem;
import com.iortatechnxt.finverse.reserves.domain.TakafulLine;
import com.iortatechnxt.finverse.reserves.domain.TakafulLineRepository;
import com.iortatechnxt.finverse.reserves.domain.UprDetail;
import com.iortatechnxt.finverse.reserves.domain.UprDetailRepository;
import com.iortatechnxt.finverse.reserves.domain.UprItem;
import com.iortatechnxt.finverse.reserves.domain.ValuationRun;
import com.iortatechnxt.finverse.reserves.domain.ValuationRunRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valuation runs: preview (calculate and store), submit, approve / reject (maker-checker), post the
 * movement journals and cancel (reversing the journals of a posted run).
 *
 * <ul>
 *   <li>One live run per company and valuation month (the valuation date is the month end); a
 *       cancelled run frees the month for a new run.
 *   <li>Posting is idempotent: posting a posted run returns it unchanged. Runs are posted in date
 *       order: a run cannot be posted while a later valuation is posted.
 *   <li>Only the latest posted run can be cancelled. The reversal is dated the valuation date while
 *       its period accepts postings, else the first day of the next period (which must be open).
 * </ul>
 */
@Service
@Transactional
public class ValuationRunService {

  /** Audit entity name. */
  public static final String ENTITY = "ValuationRun";

  private final ValuationRunRepository runs;
  private final UprDetailRepository uprDetails;
  private final TakafulLineRepository takafulLines;
  private final ValuationCalculator calculator;
  private final ReservePosting posting;
  private final TakafulSettingService takafulSettings;
  private final OrganizationService organization;
  private final AccountingPeriodRepository periods;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param runs run repository
   * @param uprDetails policy-level UPR repository
   * @param takafulLines takaful surplus repository
   * @param calculator reserve calculator
   * @param posting journal posting
   * @param takafulSettings takaful settings (cost centre of the surplus journal)
   * @param organization companies (base currency)
   * @param periods accounting periods (reversal date)
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ValuationRunService(
      ValuationRunRepository runs,
      UprDetailRepository uprDetails,
      TakafulLineRepository takafulLines,
      ValuationCalculator calculator,
      ReservePosting posting,
      TakafulSettingService takafulSettings,
      OrganizationService organization,
      AccountingPeriodRepository periods,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.runs = runs;
    this.uprDetails = uprDetails;
    this.takafulLines = takafulLines;
    this.calculator = calculator;
    this.posting = posting;
    this.takafulSettings = takafulSettings;
    this.organization = organization;
    this.periods = periods;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Runs of a company, latest first.
   *
   * @param companyId company
   * @return runs
   */
  @Transactional(readOnly = true)
  public List<ValuationRun> list(Long companyId) {
    return runs.findByCompanyIdOrderByValuationDateDescIdDesc(companyId);
  }

  /**
   * Gets a run with its lines.
   *
   * @param id id
   * @return run
   */
  @Transactional(readOnly = true)
  public ValuationRun get(Long id) {
    return runs.findWithLinesById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * The live run of a valuation month.
   *
   * @param companyId company
   * @param valuationDate any date of the month
   * @return run if any
   */
  @Transactional(readOnly = true)
  public Optional<ValuationRun> forMonth(Long companyId, LocalDate valuationDate) {
    return runs.findFirstByCompanyIdAndValuationDateAndStatusNot(
        companyId, monthEnd(valuationDate), RunStatus.CANCELLED);
  }

  /**
   * Latest posted run on or before a date.
   *
   * @param companyId company
   * @param asOf date
   * @return run if any
   */
  @Transactional(readOnly = true)
  public Optional<ValuationRun> latestPosted(Long companyId, LocalDate asOf) {
    return runs.findFirstByCompanyIdAndStatusAndValuationDateLessThanEqualOrderByValuationDateDesc(
        companyId, RunStatus.POSTED, asOf);
  }

  /**
   * Posted run before a date (the comparison run of a valuation).
   *
   * @param companyId company
   * @param before date (exclusive)
   * @return run if any
   */
  @Transactional(readOnly = true)
  public Optional<ValuationRun> postedBefore(Long companyId, LocalDate before) {
    return runs.findFirstByCompanyIdAndStatusAndValuationDateBeforeOrderByValuationDateDesc(
        companyId, RunStatus.POSTED, before);
  }

  /**
   * The run whose balances the movements of a run are measured against: for a posted run the one it
   * was posted against, otherwise the latest posted run before its valuation date.
   *
   * @param run run
   * @return baseline run, empty for the first valuation
   */
  @Transactional(readOnly = true)
  public Optional<ValuationRun> baseline(ValuationRun run) {
    if (run.isPosted()) {
      return Optional.ofNullable(run.getPreviousRunId()).map(this::get);
    }
    return postedBefore(run.getCompanyId(), run.getValuationDate());
  }

  /**
   * Policy-level UPR of a run.
   *
   * @param runId run
   * @param businessLine optional line of business filter
   * @param pageable page
   * @return page of rows
   */
  @Transactional(readOnly = true)
  public Page<UprDetail> uprDetail(Long runId, String businessLine, Pageable pageable) {
    return businessLine == null || businessLine.isBlank()
        ? uprDetails.findByRunId(runId, pageable)
        : uprDetails.findByRunIdAndBusinessLine(runId, businessLine, pageable);
  }

  /**
   * Every policy-level UPR row of a run.
   *
   * @param runId run
   * @return items
   */
  @Transactional(readOnly = true)
  public List<UprItem> uprItems(Long runId) {
    return uprDetails.findByRunId(runId).stream().map(UprDetail::toItem).toList();
  }

  /**
   * Takaful surplus lines of a run.
   *
   * @param runId run
   * @return items
   */
  @Transactional(readOnly = true)
  public List<TakafulItem> takaful(Long runId) {
    return takafulLines.findByRunIdOrderByPolicyNo(runId).stream()
        .map(TakafulLine::toItem)
        .toList();
  }

  /**
   * Calculates and stores a new run in preview.
   *
   * @param companyId company
   * @param valuationDate any date of the valuation month
   * @return run
   */
  public ValuationRun create(Long companyId, LocalDate valuationDate) {
    LocalDate date = monthEnd(valuationDate);
    forMonth(companyId, date)
        .ifPresent(
            r -> {
              throw new BusinessRuleException(
                  "RUN_EXISTS",
                  "A valuation run for "
                      + r.getPeriodName()
                      + " already exists ("
                      + r.getStatus()
                      + ")");
            });
    ValuationRun run =
        runs.save(
            new ValuationRun(
                companyId,
                date,
                organization.getCompany(companyId).getBaseCurrency(),
                clock.instant()));
    store(run);
    audit.record(ENTITY, run.getPeriodName(), AuditAction.RUN, "Valuation run calculated");
    return run;
  }

  /**
   * Recalculates a run in preview (e.g. after late transactions or parameter changes).
   *
   * @param id run
   * @return run
   */
  public ValuationRun recalculate(Long id) {
    ValuationRun run = get(id);
    store(run);
    audit.record(ENTITY, run.getPeriodName(), AuditAction.RUN, "Valuation run recalculated");
    return run;
  }

  /**
   * Submits a run for approval (maker).
   *
   * @param id run
   * @return run
   */
  public ValuationRun submit(Long id) {
    ValuationRun run = get(id);
    run.submit(currentUser.username(), clock.instant());
    audit.record(ENTITY, run.getPeriodName(), AuditAction.SUBMIT, "Submitted for approval");
    return run;
  }

  /**
   * Approves a run (checker, not the preparer).
   *
   * @param id run
   * @return run
   */
  public ValuationRun approve(Long id) {
    ValuationRun run = get(id);
    run.approve(currentUser.username(), clock.instant());
    audit.record(ENTITY, run.getPeriodName(), AuditAction.AUTHORIZE, "Valuation run approved");
    return run;
  }

  /**
   * Rejects a submitted run back to preview.
   *
   * @param id run
   * @param reason reason
   * @return run
   */
  public ValuationRun reject(Long id, String reason) {
    ValuationRun run = get(id);
    run.reject(reason);
    audit.record(ENTITY, run.getPeriodName(), AuditAction.REJECT, "Rejected: " + reason);
    return run;
  }

  /**
   * Posts the movement journals of an approved run (idempotent).
   *
   * @param id run
   * @return run
   */
  public ValuationRun post(Long id) {
    ValuationRun run = get(id);
    if (run.isPosted()) {
      return run;
    }
    if (run.getStatus() != RunStatus.APPROVED) {
      throw new BusinessRuleException(
          "RUN_NOT_APPROVED",
          "Valuation run " + run.getPeriodName() + " is " + run.getStatus() + ": approve it first");
    }
    if (runs.existsByCompanyIdAndStatusAndValuationDateAfter(
        run.getCompanyId(), RunStatus.POSTED, run.getValuationDate())) {
      throw new BusinessRuleException(
          "LATER_RUN_POSTED",
          "A later valuation is already posted: cancel it before posting " + run.getPeriodName());
    }
    ValuationRun previous = postedBefore(run.getCompanyId(), run.getValuationDate()).orElse(null);
    int journals = posting.post(run, previous, surplus(run));
    run.posted(
        currentUser.username(),
        clock.instant(),
        journals,
        previous == null ? null : previous.getId());
    audit.record(
        ENTITY, run.getPeriodName(), AuditAction.POST, journals + " reserve journal(s) posted");
    return run;
  }

  /**
   * Cancels a run; a posted run's journals are reversed.
   *
   * @param id run
   * @param reason reason
   * @return run
   */
  public ValuationRun cancel(Long id, String reason) {
    ValuationRun run = get(id);
    LocalDate reversalDate = null;
    if (run.isPosted()) {
      if (runs.existsByCompanyIdAndStatusAndValuationDateAfter(
          run.getCompanyId(), RunStatus.POSTED, run.getValuationDate())) {
        throw new BusinessRuleException(
            "LATER_RUN_POSTED", "Only the latest posted valuation run can be cancelled");
      }
      reversalDate = reversalDate(run);
      ValuationRun previous = run.getPreviousRunId() == null ? null : get(run.getPreviousRunId());
      posting.reverse(run, previous, surplus(run), reversalDate);
    }
    run.cancel(currentUser.username(), clock.instant(), reason, reversalDate);
    audit.record(ENTITY, run.getPeriodName(), AuditAction.REVERSE, "Cancelled: " + reason);
    return run;
  }

  private ReservePosting.Surplus surplus(ValuationRun run) {
    return new ReservePosting.Surplus(
        takaful(run.getId()),
        takafulSettings.find(run.getCompanyId()).map(s -> s.terms().costCenter()).orElse(null));
  }

  private void store(ValuationRun run) {
    ValuationResult result = calculator.calculate(run.getCompanyId(), run.getValuationDate());
    Long previous =
        postedBefore(run.getCompanyId(), run.getValuationDate())
            .map(ValuationRun::getId)
            .orElse(null);
    run.calculated(result.lines(), previous, clock.instant(), String.join("; ", result.remarks()));
    uprDetails.deleteByRunId(run.getId());
    takafulLines.deleteByRunId(run.getId());
    uprDetails.saveAll(result.uprItems().stream().map(i -> new UprDetail(run.getId(), i)).toList());
    takafulLines.saveAll(
        result.takafulItems().stream().map(i -> new TakafulLine(run.getId(), i)).toList());
  }

  /** The valuation date while its period accepts postings, else the next period's first day. */
  private LocalDate reversalDate(ValuationRun run) {
    boolean open =
        periods
            .findContaining(run.getCompanyId(), run.getValuationDate())
            .map(p -> p.acceptsPosting(true))
            .orElse(false);
    return open ? run.getValuationDate() : run.getValuationDate().plusDays(1);
  }

  /**
   * Last day of the month of a date (the valuation date of that month).
   *
   * @param date date
   * @return month end
   */
  public static LocalDate monthEnd(LocalDate date) {
    return YearMonth.from(date).atEndOfMonth();
  }
}
