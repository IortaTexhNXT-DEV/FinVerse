package com.iortatechnxt.brokerverse.closing.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.closing.domain.FxRevaluationHeader;
import com.iortatechnxt.brokerverse.closing.domain.FxRevaluationRun;
import com.iortatechnxt.brokerverse.closing.domain.FxRevaluationRunRepository;
import com.iortatechnxt.brokerverse.closing.domain.RevaluationItem;
import com.iortatechnxt.brokerverse.closing.service.ClosingLines.Dims;
import com.iortatechnxt.brokerverse.closing.service.ClosingLines.Value;
import com.iortatechnxt.brokerverse.closing.service.OpenItemRevaluation.OpenItemLine;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalRequest;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalService;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriodRepository;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Period-end FX revaluation: preview, posting of the REVALUATION journal and auto-reversal.
 *
 * <p>Posting method ("restatement"): per revalued balance the booked base amount is taken out at
 * the booked average rate and the foreign currency amount is put back at the CLOSING rate, both in
 * the foreign currency bucket of the account, so the foreign currency balance is unchanged and the
 * base balance becomes FC × closing rate. The difference goes to the unrealized FX gain/loss
 * account (seed chart: 4602) on the same branch. Idempotent per period: posting a period that was
 * already revalued returns the existing run.
 */
@Service
@Transactional
public class FxRevaluationService {

  /** Source module of revaluation journals. */
  public static final String SOURCE = "FX_REVALUATION";

  /** Default unrealized FX gain/loss account of the standard chart. */
  public static final String DEFAULT_GAIN_LOSS_ACCOUNT = "4602";

  private static final String ENTITY = "FxRevaluationRun";

  private final FxRevaluationRunRepository runs;
  private final FxRevaluationCalculator calculator;
  private final OpenItemRevaluation openItems;
  private final PeriodService periods;
  private final AccountingPeriodRepository periodRepository;
  private final SystemJournalService journals;
  private final JournalBatchRepository batches;
  private final OrganizationService organization;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param runs run repository
   * @param calculator revaluation calculator
   * @param openItems open item revaluation
   * @param periods period service
   * @param periodRepository period lookups that must not raise errors
   * @param journals system journal service
   * @param batches journal batches (posted lines for auto-reversal)
   * @param organization organization service
   * @param audit audit trail
   */
  public FxRevaluationService(
      FxRevaluationRunRepository runs,
      FxRevaluationCalculator calculator,
      OpenItemRevaluation openItems,
      PeriodService periods,
      AccountingPeriodRepository periodRepository,
      SystemJournalService journals,
      JournalBatchRepository batches,
      OrganizationService organization,
      AuditTrailService audit) {
    this.runs = runs;
    this.calculator = calculator;
    this.openItems = openItems;
    this.periods = periods;
    this.periodRepository = periodRepository;
    this.journals = journals;
    this.batches = batches;
    this.organization = organization;
    this.audit = audit;
  }

  /**
   * Previews the revaluation of a period (nothing is posted).
   *
   * @param companyId company
   * @param periodId period
   * @return preview
   */
  @Transactional(readOnly = true)
  public Preview preview(Long companyId, Long periodId) {
    AccountingPeriod period = period(companyId, periodId);
    LocalDate date = period.getEndDate();
    FxRevaluationCalculator.Result result = calculator.calculate(companyId, date);
    return new Preview(
        period.getId(),
        period.getName(),
        date,
        result.items(),
        List.copyOf(result.missingRates()),
        openItems.revalue(companyId, date),
        runs.findByCompanyIdAndPeriodId(companyId, periodId)
            .map(FxRevaluationRun::getId)
            .orElse(null));
  }

  /**
   * Revalues a period and posts the REVALUATION journal (idempotent per period).
   *
   * @param companyId company
   * @param periodId period
   * @param autoReverse reverse on the first day of the next period
   * @param gainLossAccount unrealized gain/loss account (default 4602)
   * @return run
   */
  public FxRevaluationRun post(
      Long companyId, Long periodId, boolean autoReverse, String gainLossAccount) {
    AccountingPeriod period = period(companyId, periodId);
    Optional<FxRevaluationRun> existing = runs.findByCompanyIdAndPeriodId(companyId, periodId);
    if (existing.isPresent()) {
      return existing.get();
    }
    postPendingReversals(companyId);
    String account =
        gainLossAccount == null || gainLossAccount.isBlank()
            ? DEFAULT_GAIN_LOSS_ACCOUNT
            : gainLossAccount.trim();
    LocalDate date = period.getEndDate();
    Company company = organization.getCompany(companyId);
    FxRevaluationCalculator.Result result = calculator.calculate(companyId, date);
    result.requireRates();
    List<RevaluationItem> items = result.items();
    List<JournalLineRequest> lines = new ArrayList<>();
    List<Boolean> posted = new ArrayList<>();
    for (RevaluationItem item : items) {
      List<JournalLineRequest> itemLines = lines(item, account, company.getBaseCurrency());
      lines.addAll(itemLines);
      posted.add(!itemLines.isEmpty());
    }
    String reference = "FXR-" + period.getName();
    String batchNo =
        lines.isEmpty()
            ? null
            : postJournal(companyId, date, "FX revaluation " + period.getName(), reference, lines);
    FxRevaluationRun run =
        new FxRevaluationRun(
            new FxRevaluationHeader(
                companyId,
                periodId,
                period.getName(),
                date,
                account,
                autoReverse,
                autoReverse ? date.plusDays(1) : null));
    run.record(items, posted, batchNo);
    FxRevaluationRun saved = runs.save(run);
    if (saved.isReversalPending()) {
      reverse(saved);
    }
    audit.record(
        ENTITY,
        companyId + "/" + period.getName(),
        AuditAction.POST,
        "FX revaluation "
            + period.getName()
            + ": gain "
            + saved.getTotalGain()
            + ", loss "
            + saved.getTotalLoss()
            + (batchNo == null ? " (nothing to post)" : ", journal " + batchNo));
    return saved;
  }

  /**
   * Lists the runs of a company.
   *
   * @param companyId company
   * @return runs, newest first
   */
  @Transactional(readOnly = true)
  public List<FxRevaluationRun> list(Long companyId) {
    return runs.findByCompanyIdOrderByRevaluationDateDesc(companyId);
  }

  /**
   * Gets a run with its lines.
   *
   * @param id id
   * @return run
   */
  @Transactional(readOnly = true)
  public FxRevaluationRun get(Long id) {
    FxRevaluationRun run =
        runs.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    Hibernate.initialize(run.getLines());
    return run;
  }

  /**
   * Run of a period, if revalued.
   *
   * @param companyId company
   * @param periodId period
   * @return run
   */
  @Transactional(readOnly = true)
  public Optional<FxRevaluationRun> forPeriod(Long companyId, Long periodId) {
    Optional<FxRevaluationRun> run = runs.findByCompanyIdAndPeriodId(companyId, periodId);
    run.ifPresent(r -> Hibernate.initialize(r.getLines()));
    return run;
  }

  /**
   * Data of the FX Revaluation Register: the posted run of the period containing a date (or a live
   * preview when the period was not revalued) and the revalued foreign currency open items.
   *
   * @param companyId company
   * @param asOf date
   * @return register data
   */
  @Transactional(readOnly = true)
  public Register register(Long companyId, LocalDate asOf) {
    Optional<FxRevaluationRun> run =
        periodRepository
            .findContaining(companyId, asOf)
            .flatMap(p -> runs.findByCompanyIdAndPeriodId(companyId, p.getId()));
    run.ifPresent(r -> Hibernate.initialize(r.getLines()));
    LocalDate date = run.map(FxRevaluationRun::getRevaluationDate).orElse(asOf);
    List<RevaluationItem> live =
        run.isPresent() ? List.of() : calculator.calculate(companyId, asOf).items();
    return new Register(run.orElse(null), live, openItems.revalue(companyId, date), date);
  }

  /**
   * Posts auto-reversals whose reversal date has become postable (next period opened).
   *
   * @param companyId company
   */
  public void postPendingReversals(Long companyId) {
    runs.findByCompanyIdOrderByRevaluationDateDesc(companyId).stream()
        .filter(FxRevaluationRun::isReversalPending)
        .forEach(this::reverse);
  }

  private void reverse(FxRevaluationRun run) {
    boolean open =
        periodRepository
            .findContaining(run.getCompanyId(), run.getReversalDate())
            .filter(p -> p.acceptsPosting(true))
            .isPresent();
    if (!open) {
      return;
    }
    JournalBatch original =
        batches
            .findByCompanyIdAndBatchNo(run.getCompanyId(), run.getJournalBatchNo())
            .orElseThrow(() -> new ResourceNotFoundException("Journal", run.getJournalBatchNo()));
    String batchNo =
        postJournal(
            run.getCompanyId(),
            run.getReversalDate(),
            "Reversal of FX revaluation " + run.getPeriodName(),
            "FXR-" + run.getPeriodName() + "-REV",
            original.getLines().stream().map(ClosingLines::reversed).toList());
    run.markReversed(batchNo);
  }

  private String postJournal(
      Long companyId,
      LocalDate valueDate,
      String narration,
      String reference,
      List<JournalLineRequest> lines) {
    Company company = organization.getCompany(companyId);
    return journals
        .post(
            new SystemJournalRequest(
                companyId,
                ClosingLines.headOffice(organization.listBranches(companyId)).getId(),
                JournalType.REVALUATION,
                valueDate,
                company.getBaseCurrency(),
                narration,
                reference,
                SOURCE,
                reference,
                lines))
        .getBatchNo();
  }

  /**
   * Journal lines revaluing one balance: out at the booked rate, in at the closing rate, and the
   * difference to the gain/loss account. Empty when the balance cannot or need not be revalued.
   */
  private static List<JournalLineRequest> lines(
      RevaluationItem item, String gainLossAccount, String baseCurrency) {
    if (!item.postable()) {
      return List.of();
    }
    BigDecimal fc = item.fcBalance().abs();
    BigDecimal bookedRate =
        item.bookedBase().abs().divide(fc, Money.RATE_SCALE, RoundingMode.HALF_EVEN);
    boolean debitBalance = item.fcBalance().signum() > 0;
    BigDecimal atClosing = Money.convert(fc, item.closingRate());
    BigDecimal atBooked = Money.convert(fc, bookedRate);
    BigDecimal delta = debitBalance ? atClosing.subtract(atBooked) : atBooked.subtract(atClosing);
    if (delta.signum() == 0) {
      return List.of();
    }
    BalanceSide in = debitBalance ? BalanceSide.DEBIT : BalanceSide.CREDIT;
    String text = "FX revaluation " + item.accountCode() + " " + item.currency();
    return List.of(
        ClosingLines.line(
            item.accountCode(),
            in,
            new Value(fc, item.currency(), item.closingRate()),
            item.branchId(),
            Dims.NONE,
            text + " at closing rate"),
        ClosingLines.line(
            item.accountCode(),
            in.opposite(),
            new Value(fc, item.currency(), bookedRate),
            item.branchId(),
            Dims.NONE,
            text + " at booked rate"),
        ClosingLines.line(
            gainLossAccount,
            delta.signum() > 0 ? BalanceSide.CREDIT : BalanceSide.DEBIT,
            new Value(delta.abs(), baseCurrency, null),
            item.branchId(),
            Dims.NONE,
            text + (delta.signum() > 0 ? " unrealized gain" : " unrealized loss")));
  }

  private AccountingPeriod period(Long companyId, Long periodId) {
    AccountingPeriod period = periods.getPeriod(periodId);
    if (!period.getCompanyId().equals(companyId)) {
      throw new BusinessRuleException("PERIOD_OTHER_COMPANY", "Period belongs to another company");
    }
    return period;
  }

  /**
   * Revaluation preview.
   *
   * @param periodId period
   * @param periodName period name
   * @param revaluationDate period end
   * @param items GL balances revalued
   * @param missingRates currencies without a CLOSING rate (posting is refused)
   * @param openItems foreign currency open items revalued (information only)
   * @param existingRunId run already posted for the period, if any
   */
  public record Preview(
      Long periodId,
      String periodName,
      LocalDate revaluationDate,
      List<RevaluationItem> items,
      List<String> missingRates,
      List<OpenItemLine> openItems,
      Long existingRunId) {

    /** Canonical constructor copying lists. */
    public Preview {
      items = List.copyOf(items);
      missingRates = List.copyOf(missingRates);
      openItems = List.copyOf(openItems);
    }
  }

  /**
   * FX Revaluation Register data.
   *
   * @param run posted run of the period, or null
   * @param preview live revaluation when no run exists
   * @param openItems revalued open items
   * @param date revaluation date
   */
  public record Register(
      FxRevaluationRun run,
      List<RevaluationItem> preview,
      List<OpenItemLine> openItems,
      LocalDate date) {

    /** Canonical constructor copying lists. */
    public Register {
      preview = List.copyOf(preview);
      openItems = List.copyOf(openItems);
    }
  }
}
