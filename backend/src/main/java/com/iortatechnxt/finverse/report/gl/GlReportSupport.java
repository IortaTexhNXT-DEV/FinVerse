package com.iortatechnxt.finverse.report.gl;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.coa.domain.BalanceSide;
import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Shared helpers and parameter declarations for general ledger reports. */
@Component
public class GlReportSupport {

  /** Company parameter name. */
  public static final String COMPANY = "companyId";

  /** Branch parameter name. */
  public static final String BRANCH = "branchId";

  /** As-of date parameter name. */
  public static final String AS_OF = "asOfDate";

  /** From date parameter name. */
  public static final String FROM = "fromDate";

  /** To date parameter name. */
  public static final String TO = "toDate";

  private final ChartOfAccountsService accounts;

  /**
   * Creates the helper.
   *
   * @param accounts chart of accounts
   */
  public GlReportSupport(ChartOfAccountsService accounts) {
    this.accounts = accounts;
  }

  /**
   * Company parameter (mandatory).
   *
   * @return spec
   */
  public static ParameterSpec companyParam() {
    return ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY);
  }

  /**
   * Branch parameter (optional; blank = all branches).
   *
   * @return spec
   */
  public static ParameterSpec branchParam() {
    return ParameterSpec.optional(BRANCH, "Branch", ParameterType.BRANCH);
  }

  /**
   * As-of date parameter defaulting to today.
   *
   * @return spec
   */
  public static ParameterSpec asOfParam() {
    return ParameterSpec.required(AS_OF, "As of Date", ParameterType.DATE).withDefault("TODAY");
  }

  /**
   * From date parameter defaulting to the first day of the month.
   *
   * @return spec
   */
  public static ParameterSpec fromParam() {
    return ParameterSpec.required(FROM, "From Date", ParameterType.DATE).withDefault("MONTH_START");
  }

  /**
   * To date parameter defaulting to today.
   *
   * @return spec
   */
  public static ParameterSpec toParam() {
    return ParameterSpec.required(TO, "To Date", ParameterType.DATE).withDefault("TODAY");
  }

  /**
   * Loads the chart of accounts indexed by id.
   *
   * @param companyId company
   * @return accounts by id
   */
  public Map<Long, GlAccount> accountsById(Long companyId) {
    return accounts.list(companyId).stream()
        .collect(Collectors.toMap(GlAccount::getId, Function.identity()));
  }

  /**
   * Converts a net (debit positive) balance into the account's natural sign, so income, liabilities
   * and equity show positive when in credit.
   *
   * @param accountClass class
   * @param netDebit debit minus credit
   * @return natural balance
   */
  public static BigDecimal natural(AccountClass accountClass, BigDecimal netDebit) {
    return accountClass.normalBalance() == BalanceSide.DEBIT ? netDebit : netDebit.negate();
  }
}
