package com.iortatechnxt.finverse.consolidation.report;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationRun;
import com.iortatechnxt.finverse.consolidation.service.ConsolidatedBalance;
import com.iortatechnxt.finverse.consolidation.service.ConsolidationRunService;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Shared parameters and run lookup of the consolidation reports. */
@Component
public class ConsolidationReportSupport {

  /** Group code parameter. */
  public static final String GROUP = "groupCode";

  /** Account code column key. */
  static final String CODE = "code";

  /** Account name column key. */
  static final String NAME = "name";

  /** Account class grouping key. */
  static final String CLASS = "accountClass";

  private final ConsolidationRunService runs;
  private final OrganizationService organization;

  /**
   * Creates the helper.
   *
   * @param runs run service
   * @param organization organization service
   */
  public ConsolidationReportSupport(
      ConsolidationRunService runs, OrganizationService organization) {
    this.runs = runs;
    this.organization = organization;
  }

  /**
   * Parameters: group code (mandatory) and as-of date (optional; latest run when blank).
   *
   * @return specs
   */
  static List<ParameterSpec> parameters() {
    return List.of(
        ParameterSpec.required(GROUP, "Consolidation group", ParameterType.TEXT),
        ParameterSpec.optional(
            GlReportSupport.AS_OF, "Run as of (latest when blank)", ParameterType.DATE));
  }

  /**
   * Finds the run a report is about: the latest DRAFT or FINAL run on or before the date.
   *
   * @param p parameters
   * @return run with lines
   */
  ConsolidationRun run(ReportParameters p) {
    String group = p.text(GROUP).trim();
    return runs.latest(group, p.optionalDate(GlReportSupport.AS_OF).orElse(null))
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "NO_CONSOLIDATION_RUN", "No consolidation run exists for group " + group));
  }

  /**
   * Consolidated trial balance of a run.
   *
   * @param run run
   * @return balances by code
   */
  static Map<String, ConsolidatedBalance> balances(ConsolidationRun run) {
    return ConsolidationRunService.trialBalance(run);
  }

  /**
   * Company code for display.
   *
   * @param companyId company or null (group level)
   * @return code
   */
  String companyCode(Long companyId) {
    return companyId == null ? "GROUP" : organization.getCompany(companyId).getCode();
  }

  /**
   * Footnote describing the run.
   *
   * @param run run
   * @return note
   */
  static String runNote(ConsolidationRun run) {
    return "Run "
        + run.getRunNo()
        + " as of "
        + run.getAsOfDate()
        + " ("
        + run.getStatus()
        + "), amounts in "
        + run.getCurrency()
        + ".";
  }

  /**
   * Natural-sign amount (credit balances positive for liabilities, equity and income).
   *
   * @param accountClass class
   * @param netDebit net debit
   * @return natural amount
   */
  static BigDecimal natural(AccountClass accountClass, BigDecimal netDebit) {
    return GlReportSupport.natural(accountClass, netDebit);
  }
}
