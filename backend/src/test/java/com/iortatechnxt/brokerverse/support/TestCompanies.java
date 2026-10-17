package com.iortatechnxt.brokerverse.support;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalRequest;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalService;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import com.iortatechnxt.brokerverse.period.domain.FiscalYear;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates isolated test companies (copy of the seed chart of accounts and dimensions, one head
 * office branch) so tests of period-end processes do not interfere with each other or the seed
 * company.
 */
@Component
public class TestCompanies {

  private static final String SEED = "FVI";

  private final JdbcTemplate jdbc;
  private final CompanyRepository companies;
  private final PeriodService periods;
  private final OrganizationService organization;
  private final SystemJournalService journals;

  TestCompanies(
      JdbcTemplate jdbc,
      CompanyRepository companies,
      PeriodService periods,
      OrganizationService organization,
      SystemJournalService journals) {
    this.jdbc = jdbc;
    this.companies = companies;
    this.periods = periods;
    this.organization = organization;
    this.journals = journals;
  }

  /**
   * Creates a company with a head office branch, the seed chart of accounts and dimensions.
   *
   * @param code unique company code
   * @param baseCurrency base currency
   * @return company
   */
  public Company create(String code, String baseCurrency) {
    jdbc.update(
        """
        insert into org_company (code, name, base_currency, fiscal_year_start_month, back_value_days,
            forward_value_days, retained_earnings_account, record_status, authorized_by,
            authorized_at, created_at, created_by)
        values (?, ?, ?, 1, 45, 5, '3500', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM')
        """,
        code,
        "Test company " + code,
        baseCurrency);
    Long id = companies.findByCode(code).orElseThrow().getId();
    jdbc.update(
        """
        insert into org_branch (company_id, code, name, opening_date, head_office, forex_authorized,
            record_status, authorized_by, authorized_at, created_at, created_by)
        values (?, 'HO', 'Head Office', date '2010-01-01', true, true, 'ACTIVE', 'SYSTEM', now(),
            now(), 'SYSTEM')
        """,
        id);
    copyChart(id);
    return companies.findById(id).orElseThrow();
  }

  /**
   * Creates a fiscal year and opens all its periods.
   *
   * @param companyId company
   * @param yearCode year
   * @return fiscal year
   */
  public FiscalYear openYear(Long companyId, int yearCode) {
    FiscalYear year = periods.createFiscalYear(companyId, yearCode);
    periods.listPeriods(year.getId()).forEach(p -> periods.open(p.getId()));
    return year;
  }

  /**
   * Finds a period by name (e.g. "2026-03").
   *
   * @param companyId company
   * @param date any date in the period
   * @return period
   */
  public AccountingPeriod period(Long companyId, LocalDate date) {
    return periods.listPeriods(periods.yearContaining(companyId, date).getId()).stream()
        .filter(p -> p.contains(date))
        .findFirst()
        .orElseThrow();
  }

  /**
   * Head office branch id.
   *
   * @param companyId company
   * @return branch id
   */
  public Long headOffice(Long companyId) {
    return organization.listBranches(companyId).get(0).getId();
  }

  /**
   * Posts a system journal at the head office.
   *
   * @param companyId company
   * @param date value date
   * @param currency header currency
   * @param lines lines
   * @return posted batch
   */
  public JournalBatch post(
      Long companyId, LocalDate date, String currency, List<JournalLineRequest> lines) {
    String key = "TEST-" + UUID.randomUUID();
    return journals.post(
        new SystemJournalRequest(
            companyId,
            headOffice(companyId),
            JournalType.PAYMENT,
            date,
            currency,
            "Test posting",
            null,
            "TEST",
            key,
            lines));
  }

  /**
   * Journal line.
   *
   * @param account account code
   * @param side side
   * @param amount amount
   * @return line in header currency
   */
  public static JournalLineRequest line(String account, BalanceSide side, String amount) {
    return line(account, side, amount, null, null);
  }

  /**
   * Journal line with dimensions.
   *
   * @param account account code
   * @param side side
   * @param amount amount
   * @param costCenter cost centre
   * @param businessLine line of business
   * @return line in header currency
   */
  public static JournalLineRequest line(
      String account, BalanceSide side, String amount, String costCenter, String businessLine) {
    return new JournalLineRequest(
        account,
        side,
        new BigDecimal(amount),
        null,
        null,
        null,
        costCenter,
        businessLine,
        null,
        null,
        null);
  }

  private void copyChart(Long targetId) {
    Long seedId = companies.findByCode(SEED).orElseThrow().getId();
    jdbc.update(
        """
        insert into coa_account (company_id, code, name, short_name, account_class, level, parent_id,
            category_id, postable, control_account, sub_ledger_type, allow_manual_posting,
            cost_center_required, business_line_required, revaluation_required, reconcilable,
            inter_branch, contra_account_code, report_group, frozen, opened_on, record_status,
            authorized_by, authorized_at, created_at, created_by)
        select ?, code, name, short_name, account_class, level, null, category_id, postable,
            control_account, sub_ledger_type, allow_manual_posting, cost_center_required,
            business_line_required, revaluation_required, reconcilable, inter_branch,
            contra_account_code, report_group, false, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(),
            now(), 'SYSTEM'
        from coa_account where company_id = ?
        """,
        targetId,
        seedId);
    jdbc.update(
        """
        update coa_account t set parent_id = tp.id
        from coa_account s join coa_account sp on sp.id = s.parent_id, coa_account tp
        where s.company_id = ? and t.company_id = ? and t.code = s.code
          and tp.company_id = t.company_id and tp.code = sp.code
        """,
        seedId,
        targetId);
    jdbc.update(
        """
        insert into coa_account_currency (account_id, currency_code)
        select t.id, ac.currency_code from coa_account_currency ac
        join coa_account s on s.id = ac.account_id and s.company_id = ?
        join coa_account t on t.code = s.code and t.company_id = ?
        """,
        seedId,
        targetId);
    jdbc.update(
        """
        insert into dim_value (company_id, dimension_type, code, name, created_at, created_by)
        select ?, dimension_type, code, name, now(), 'SYSTEM' from dim_value where company_id = ?
        """,
        targetId,
        seedId);
  }
}
