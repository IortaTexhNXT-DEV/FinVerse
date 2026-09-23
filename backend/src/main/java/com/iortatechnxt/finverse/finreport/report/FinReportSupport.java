package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.finverse.finreport.service.AccountNode;
import com.iortatechnxt.finverse.finreport.service.LedgerQuery;
import com.iortatechnxt.finverse.journal.domain.JournalType;
import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Shared look-ups and parameter interpretation for the finance (FIN-*) reports.
 *
 * <p>Dimension mapping of the PREMIA reports onto FinVerse: <b>Division</b> = branch,
 * <b>Department</b> = cost centre of the line, <b>Activity</b> = line of business (head 1) or cost
 * centre (head 2), <b>Transaction Code</b> = voucher number prefix (journal type code, e.g. JV).
 */
@Component
public class FinReportSupport {

  private final ChartOfAccountsService accounts;
  private final OrganizationService organization;

  /**
   * Creates the helper.
   *
   * @param accounts chart of accounts
   * @param organization organization service
   */
  public FinReportSupport(ChartOfAccountsService accounts, OrganizationService organization) {
    this.accounts = accounts;
    this.organization = organization;
  }

  /**
   * Chart of accounts hierarchy of a company.
   *
   * @param companyId company
   * @return hierarchy
   */
  public AccountHierarchy hierarchy(Long companyId) {
    return new AccountHierarchy(accounts.list(companyId).stream().map(AccountNode::of).toList());
  }

  /**
   * Branch codes by id.
   *
   * @param companyId company
   * @return code by id
   */
  public Map<Long, String> branchCodes(Long companyId) {
    return organization.listBranches(companyId).stream()
        .collect(Collectors.toMap(Branch::getId, Branch::getCode));
  }

  /**
   * First day of the fiscal year containing a date (from the company's fiscal year start month).
   *
   * @param companyId company
   * @param date date
   * @return fiscal year start
   */
  public LocalDate fiscalYearStart(Long companyId, LocalDate date) {
    int startMonth = organization.getCompany(companyId).getFiscalYearStartMonth();
    LocalDate start = LocalDate.of(date.getYear(), startMonth, 1);
    return start.isAfter(date) ? start.minusYears(1) : start;
  }

  /**
   * Calendar month selected by the month and year parameters.
   *
   * @param p parameters
   * @return month
   */
  public static YearMonth month(ReportParameters p) {
    return YearMonth.of(
        Integer.parseInt(p.text(FinParams.YEAR)), Integer.parseInt(p.text(FinParams.MONTH)));
  }

  /**
   * Postable accounts selected by the main and sub account code ranges.
   *
   * @param h hierarchy
   * @param p parameters
   * @return account ids
   */
  public static Set<Long> selectedAccounts(AccountHierarchy h, ReportParameters p) {
    return h.postableIds(
        range(p, FinParams.MAIN_FROM, FinParams.MAIN_TO),
        range(p, FinParams.SUB_FROM, FinParams.SUB_TO));
  }

  /**
   * Ledger selection from the standard parameters: company, division, department, document date
   * range and main account range.
   *
   * @param p parameters
   * @param h chart of accounts
   * @return query
   */
  public static LedgerQuery dimensionQuery(ReportParameters p, AccountHierarchy h) {
    return new LedgerQuery(
        p.longValue(FinParams.COMPANY),
        FinParams.branch(p),
        FinParams.costCenter(p),
        p.date(FinParams.FROM),
        p.date(FinParams.TO),
        h.postableIds(range(p, FinParams.MAIN_FROM, FinParams.MAIN_TO), a -> true),
        null,
        null);
  }

  /**
   * Account code range filter (blank bound = open).
   *
   * @param p parameters
   * @param fromKey lower bound parameter
   * @param toKey upper bound parameter
   * @return predicate on account codes
   */
  public static Predicate<AccountNode> range(ReportParameters p, String fromKey, String toKey) {
    Predicate<String> codes = codeRange(p, fromKey, toKey);
    return n -> codes.test(n.code());
  }

  /**
   * Text range filter (blank bound = open).
   *
   * @param p parameters
   * @param fromKey lower bound parameter
   * @param toKey upper bound parameter
   * @return predicate
   */
  public static Predicate<String> codeRange(ReportParameters p, String fromKey, String toKey) {
    String low = p.optionalText(fromKey).orElse(null);
    String high = p.optionalText(toKey).orElse(null);
    return code ->
        code != null
            && (low == null || code.compareTo(low) >= 0)
            && (high == null || code.compareTo(high) <= 0);
  }

  /**
   * Journal types whose transaction code (voucher prefix) lies in the transaction code range.
   *
   * @param p parameters
   * @return journal type names
   */
  public static List<String> journalTypes(ReportParameters p) {
    Predicate<String> codes = codeRange(p, FinParams.TXN_FROM, FinParams.TXN_TO);
    return Arrays.stream(JournalType.values())
        .filter(t -> codes.test(t.prefix()))
        .map(JournalType::name)
        .toList();
  }

  /**
   * Transaction code description, e.g. "JV - Manual".
   *
   * @param transactionCode voucher prefix
   * @return description
   */
  public static String transactionCodeLabel(String transactionCode) {
    return Arrays.stream(JournalType.values())
        .filter(t -> t.prefix().equals(transactionCode))
        .findFirst()
        .map(t -> transactionCode + " - " + title(t.name()))
        .orElse(transactionCode);
  }

  private static String title(String enumName) {
    String lower = enumName.replace('_', ' ').toLowerCase(Locale.ROOT);
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }
}
