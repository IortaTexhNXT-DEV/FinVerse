package com.iortatechnxt.brokerverse.closing.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.closing.domain.YearEndClose;
import com.iortatechnxt.brokerverse.closing.domain.YearEndCloseRepository;
import com.iortatechnxt.brokerverse.closing.domain.YearEndCloseValues;
import com.iortatechnxt.brokerverse.closing.service.ClosingBalanceQuery.PnlBalance;
import com.iortatechnxt.brokerverse.closing.service.ClosingLines.Dims;
import com.iortatechnxt.brokerverse.closing.service.ClosingLines.Value;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalRequest;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalService;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import com.iortatechnxt.brokerverse.period.domain.FiscalYear;
import com.iortatechnxt.brokerverse.period.domain.PeriodStatus;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Year-end closing (spec 15.4).
 *
 * <ol>
 *   <li>Run the pre-close checklist; any failed control blocks the close.
 *   <li>Post one CLOSING journal per branch, dated the last day of the year, that zeroes every
 *       income and expense balance (per account, cost centre and line of business) against the
 *       company's retained earnings account.
 *   <li>Close the periods still in soft close and mark the fiscal year CLOSED ({@link
 *       PeriodService#closeFiscalYear}).
 *   <li>Create the next fiscal year if missing and open its first period.
 *   <li>Verify the close (FRBS 2.7.1): nominal balances and trial balance difference as of the year
 *       end, both expected to be zero, stored on the close record.
 * </ol>
 *
 * <p>Carry forward is implicit: balance sheet balances are cumulative (the ledger is never reset),
 * so the closing balance of every permanent account is automatically the opening balance of the
 * next year and no opening balance journal is needed. Reversal of a year-end close is not
 * supported: a closed fiscal year cannot be reopened (see {@code PeriodService#reopen}).
 */
@Service
@Transactional
public class YearEndService {

  /** Source module of closing journals. */
  public static final String SOURCE = "YEAR_END";

  private final ClosingChecklistService checklist;
  private final ClosingBalanceQuery balances;
  private final PeriodService periods;
  private final SystemJournalService journals;
  private final OrganizationService organization;
  private final YearEndCloseRepository closes;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param checklist checklist service
   * @param balances income and expense balances
   * @param periods period service
   * @param journals system journal service
   * @param organization organization service
   * @param closes close records
   * @param audit audit trail
   * @param clock clock
   */
  public YearEndService(
      ClosingChecklistService checklist,
      ClosingBalanceQuery balances,
      PeriodService periods,
      SystemJournalService journals,
      OrganizationService organization,
      YearEndCloseRepository closes,
      AuditTrailService audit,
      Clock clock) {
    this.checklist = checklist;
    this.balances = balances;
    this.periods = periods;
    this.journals = journals;
    this.organization = organization;
    this.closes = closes;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Preview of the closing entries: income and expense balances that will be transferred.
   *
   * @param companyId company
   * @param fiscalYearId year
   * @return balances (net debit) per branch, account and dimension
   */
  @Transactional(readOnly = true)
  public List<PnlBalance> preview(Long companyId, Long fiscalYearId) {
    FiscalYear year = year(companyId, fiscalYearId);
    return balances.balances(companyId, year.getEndDate());
  }

  /**
   * Close record of a fiscal year, if closed.
   *
   * @param fiscalYearId year
   * @return record
   */
  @Transactional(readOnly = true)
  public Optional<YearEndClose> closeRecord(Long fiscalYearId) {
    return closes.findByFiscalYearId(fiscalYearId);
  }

  /**
   * Closes a fiscal year.
   *
   * @param companyId company
   * @param fiscalYearId year
   * @return close record
   */
  public YearEndClose close(Long companyId, Long fiscalYearId) {
    FiscalYear year = year(companyId, fiscalYearId);
    List<String> failures =
        checklist.yearEnd(companyId, fiscalYearId).stream()
            .filter(CheckItem::blocks)
            .map(c -> c.label() + ": " + c.detail())
            .toList();
    if (!failures.isEmpty()) {
      throw new BusinessRuleException("YEAR_END_CHECKLIST_FAILED", String.join("; ", failures));
    }
    Company company = organization.getCompany(companyId);
    String retained = company.getRetainedEarningsAccount();
    Map<Long, List<PnlBalance>> byBranch =
        balances.balances(companyId, year.getEndDate()).stream()
            .collect(
                Collectors.groupingBy(
                    PnlBalance::branchId, LinkedHashMap::new, Collectors.toList()));
    List<String> batchNos = new ArrayList<>();
    BigDecimal netDebit = BigDecimal.ZERO;
    for (Map.Entry<Long, List<PnlBalance>> e : byBranch.entrySet()) {
      Branch branch = organization.getBranch(e.getKey());
      netDebit = netDebit.add(sum(e.getValue()));
      batchNos.add(postClosing(company, year, branch, e.getValue()));
    }
    for (AccountingPeriod p : periods.listPeriods(fiscalYearId)) {
      if (p.getStatus() != PeriodStatus.CLOSED) {
        periods.close(p.getId());
      }
    }
    periods.closeFiscalYear(fiscalYearId);
    Integer next = prepareNextYear(companyId, year.getYearCode() + 1);
    YearEndClose record =
        closes.save(
            new YearEndClose(
                new YearEndCloseValues(
                    companyId,
                    fiscalYearId,
                    year.getYearCode(),
                    year.getEndDate(),
                    netDebit.negate(),
                    retained,
                    String.join(",", batchNos),
                    next)));
    verify(record);
    audit.record(
        "YearEndClose",
        company.getCode() + "/" + year.getYearCode(),
        AuditAction.CLOSE,
        "Year-end close FY "
            + year.getYearCode()
            + ": net result "
            + netDebit.negate()
            + " to "
            + retained
            + ", journals "
            + batchNos);
    return record;
  }

  /**
   * Verifies a closed year again (FRBS 2.7.1): nominal balances and trial balance difference as of
   * the year end, stored on the close record.
   *
   * @param fiscalYearId year
   * @return close record with the verification
   */
  public YearEndClose verify(Long fiscalYearId) {
    YearEndClose record =
        closes
            .findByFiscalYearId(fiscalYearId)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "YEAR_NOT_CLOSED", "The fiscal year has no year-end close to verify"));
    verify(record);
    return record;
  }

  private void verify(YearEndClose record) {
    BigDecimal nominal = balances.nominalBalance(record.getCompanyId(), record.getClosingDate());
    BigDecimal difference =
        balances.trialBalanceDifference(record.getCompanyId(), record.getClosingDate());
    record.verify(Money.round(nominal), Money.round(difference), clock.instant());
    audit.record(
        "YearEndClose",
        record.getYearCode(),
        AuditAction.UPDATE,
        "Post-close verification: nominal balance "
            + nominal
            + ", trial balance difference "
            + difference);
  }

  private String postClosing(
      Company company, FiscalYear year, Branch branch, List<PnlBalance> branchBalances) {
    String base = company.getBaseCurrency();
    List<JournalLineRequest> lines = new ArrayList<>();
    for (PnlBalance b : branchBalances) {
      lines.add(
          ClosingLines.line(
              b.accountCode(),
              b.netDebit().signum() > 0 ? BalanceSide.CREDIT : BalanceSide.DEBIT,
              new Value(b.netDebit().abs(), base, null),
              branch.getId(),
              new Dims(b.costCenter(), b.businessLine()),
              "Close " + b.accountCode() + " to retained earnings"));
    }
    BigDecimal net = sum(branchBalances);
    if (net.signum() != 0) {
      lines.add(
          ClosingLines.line(
              company.getRetainedEarningsAccount(),
              net.signum() > 0 ? BalanceSide.DEBIT : BalanceSide.CREDIT,
              new Value(net.abs(), base, null),
              branch.getId(),
              Dims.NONE,
              (net.signum() > 0 ? "Net loss" : "Net profit") + " FY " + year.getYearCode()));
    }
    String key = "FY" + year.getYearCode() + ":" + branch.getCode();
    return journals
        .post(
            new SystemJournalRequest(
                company.getId(),
                branch.getId(),
                JournalType.CLOSING,
                year.getEndDate(),
                base,
                "Year-end closing FY " + year.getYearCode() + " - " + branch.getCode(),
                "YEC-" + year.getYearCode(),
                SOURCE,
                key,
                lines))
        .getBatchNo();
  }

  private Integer prepareNextYear(Long companyId, int nextCode) {
    boolean exists =
        periods.listYears(companyId).stream().anyMatch(y -> y.getYearCode() == nextCode);
    if (!exists) {
      FiscalYear created = periods.createFiscalYear(companyId, nextCode);
      periods.listPeriods(created.getId()).stream()
          .findFirst()
          .ifPresent(first -> periods.open(first.getId()));
    }
    return nextCode;
  }

  private FiscalYear year(Long companyId, Long fiscalYearId) {
    FiscalYear year = periods.getYear(fiscalYearId);
    if (!year.getCompanyId().equals(companyId)) {
      throw new BusinessRuleException(
          "PERIOD_OTHER_COMPANY", "Fiscal year belongs to another company");
    }
    return year;
  }

  private static BigDecimal sum(List<PnlBalance> list) {
    return list.stream().map(PnlBalance::netDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
