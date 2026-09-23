package com.iortatechnxt.finverse.reserves;

import com.iortatechnxt.finverse.reserves.domain.DevelopmentPeriod;
import com.iortatechnxt.finverse.reserves.domain.IbnrMethod;
import com.iortatechnxt.finverse.reserves.domain.ReserveParameter;
import com.iortatechnxt.finverse.reserves.domain.ReserveParameterTerms;
import com.iortatechnxt.finverse.reserves.domain.ReserveType;
import com.iortatechnxt.finverse.reserves.domain.RunLine;
import com.iortatechnxt.finverse.reserves.domain.TriangleBasis;
import com.iortatechnxt.finverse.reserves.domain.ValuationRun;
import com.iortatechnxt.finverse.reserves.service.ReserveParameterService;
import com.iortatechnxt.finverse.reserves.service.ValuationRunService;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Reserve test helpers: parameters, the run life cycle (maker accountant, checker fmanager). */
@Component
public class ReserveFixtures {

  public static final String MAKER = "accountant";
  public static final String CHECKER = "fmanager";

  private final ReserveParameterService parameters;
  private final ValuationRunService runs;
  private final AsUser as;
  private final TestData data;
  private final JdbcTemplate jdbc;

  ReserveFixtures(
      ReserveParameterService parameters,
      ValuationRunService runs,
      AsUser as,
      TestData data,
      JdbcTemplate jdbc) {
    this.parameters = parameters;
    this.runs = runs;
    this.as = as;
    this.data = data;
    this.jdbc = jdbc;
  }

  public Long companyId() {
    return data.company().getId();
  }

  public static ReserveParameterTerms terms(IbnrMethod method, String rate, String lossRatio) {
    return new ReserveParameterTerms(
        method,
        new BigDecimal(rate),
        TriangleBasis.INCURRED,
        DevelopmentPeriod.YEAR,
        3,
        new BigDecimal("10"),
        new BigDecimal("5"),
        new BigDecimal(lossRatio),
        new BigDecimal("30"),
        new BigDecimal("20"),
        "test");
  }

  /** Creates (accountant) and authorizes (fmanager) parameters of a line from 1 January 2026. */
  public ReserveParameter parameters(String line, ReserveParameterTerms terms) {
    ReserveParameter p =
        as.run(MAKER, () -> parameters.create(companyId(), line, LocalDate.of(2026, 1, 1), terms));
    return as.run(CHECKER, () -> parameters.authorize(p.getId()));
  }

  /** Prepares and submits a run as the accountant. */
  public ValuationRun submitted(LocalDate date) {
    ValuationRun run = as.run(MAKER, () -> runs.create(companyId(), date));
    return as.run(MAKER, () -> runs.submit(run.getId()));
  }

  /** Prepares, approves and posts a run. */
  public ValuationRun posted(LocalDate date) {
    ValuationRun run = submitted(date);
    as.run(CHECKER, () -> runs.approve(run.getId()));
    return as.run(CHECKER, () -> runs.post(run.getId()));
  }

  /** Total of a reserve type of a run, gross or reinsurers' share. */
  public BigDecimal total(ValuationRun run, ReserveType type, Function<RunLine, BigDecimal> part) {
    return runs.get(run.getId()).getLines().stream()
        .filter(l -> l.getReserveType() == type)
        .map(part)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Ledger balance (debit − credit, base currency) of an account of the test company. */
  public BigDecimal balance(String accountCode, LocalDate asOf) {
    BigDecimal value =
        jdbc.queryForObject(
            "select coalesce(sum(e.debit_base - e.credit_base), 0) from gl_ledger_entry e"
                + " join coa_account a on a.id = e.account_id"
                + " where e.company_id = ? and a.code = ? and e.value_date <= ?",
            BigDecimal.class,
            companyId(),
            accountCode,
            asOf);
    return value == null ? BigDecimal.ZERO : value;
  }

  /** Number of reserve journals of the test company. */
  public int reserveJournals() {
    Integer n =
        jdbc.queryForObject(
            "select count(*) from jnl_batch where company_id = ? and source_module = 'RESERVES'",
            Integer.class,
            companyId());
    return n == null ? 0 : n;
  }
}
