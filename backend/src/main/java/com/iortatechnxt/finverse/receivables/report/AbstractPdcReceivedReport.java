package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.receivables.domain.PostDatedCheque;
import com.iortatechnxt.finverse.receivables.service.PdcQueries;
import com.iortatechnxt.finverse.receivables.service.PdcQueries.PdcAsOf;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Engine of the PDC (received) reports (Src FPD001-FPD003, FR2581-FR2583): cheques on hand as of a
 * date, received during a period, or due to be banked, optionally grouped by division (branch),
 * department and bank with range selections. Status tests use the status history as of the date.
 */
public abstract class AbstractPdcReceivedReport implements ReportDefinition {

  /** Which cheques a report lists. */
  public enum Selection {
    /** Held (on hand or due) as of the date. */
    ON_HAND,
    /** Received during the period, any status. */
    PERIOD,
    /** Held as of the date with a cheque date on or before it. */
    DUE_TO_BANK
  }

  private static final String[][] RANGES = {
    {"division", "Division"},
    {"department", "Department"},
    {"party", "Customer Account"},
    {"bank", "Bank Account"}
  };
  private static final String FROM_SUFFIX = "From";
  private static final String TO_SUFFIX = "To";

  private final PdcQueries pdcs;
  private final OrganizationService organization;
  private final String code;
  private final String title;
  private final Selection selection;
  private final boolean byDivision;

  /**
   * Creates the report.
   *
   * @param pdcs PDC register
   * @param organization organization (branch codes)
   * @param code report code
   * @param title title
   * @param selection cheques to list
   * @param byDivision group by division, department and bank (with range parameters)
   */
  protected AbstractPdcReceivedReport(
      PdcQueries pdcs,
      OrganizationService organization,
      String code,
      String title,
      Selection selection,
      boolean byDivision) {
    this.pdcs = pdcs;
    this.organization = organization;
    this.code = code;
    this.title = title;
    this.selection = selection;
    this.byDivision = byDivision;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(GlReportSupport.companyParam());
    if (selection == Selection.PERIOD) {
      params.add(GlReportSupport.fromParam());
      params.add(GlReportSupport.toParam());
    } else {
      params.add(GlReportSupport.asOfParam());
    }
    if (byDivision) {
      for (String[] range : RANGES) {
        params.add(
            ParameterSpec.optional(range[0] + FROM_SUFFIX, range[1] + " From", ParameterType.TEXT));
        params.add(
            ParameterSpec.optional(range[0] + TO_SUFFIX, range[1] + " To", ParameterType.TEXT));
      }
    }
    return new ReportMetadata(
        code,
        title,
        ReportCategory.RECEIVABLES_PAYABLES,
        "Post-dated cheques received (" + selection.name().toLowerCase(Locale.ROOT) + ")",
        params,
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(GlReportSupport.COMPANY);
    Map<Long, String> branches =
        organization.listBranches(companyId).stream()
            .collect(Collectors.toMap(Branch::getId, Branch::getCode));
    List<Map<String, Object>> rows =
        select(companyId, p).stream()
            .map(a -> cells(a, branches))
            .filter(r -> !byDivision || inRanges(r, p))
            .toList();
    TabularReportBuilder builder = TabularReportBuilder.of(p).columns(columns());
    if (byDivision) {
      builder
          .groupBy("division", "Division")
          .groupBy("department", "Department")
          .groupBy("bank", "Bank");
    }
    return builder
        .rows(rows)
        .note("PDCs are memorandum items until banked; LC value at the rate of the received date")
        .build();
  }

  private List<PdcAsOf> select(Long companyId, ReportParameters p) {
    return switch (selection) {
      case PERIOD ->
          pdcs.receivedBetween(companyId, p.date(GlReportSupport.FROM), p.date(GlReportSupport.TO));
      case ON_HAND -> pdcs.heldAsOf(companyId, p.date(GlReportSupport.AS_OF));
      case DUE_TO_BANK -> {
        LocalDate asOf = p.date(GlReportSupport.AS_OF);
        yield pdcs.heldAsOf(companyId, asOf).stream()
            .filter(a -> !a.pdc().getChequeDate().isAfter(asOf))
            .toList();
      }
    };
  }

  private static Map<String, Object> cells(PdcAsOf a, Map<Long, String> branches) {
    PostDatedCheque c = a.pdc();
    Map<String, Object> cells = new LinkedHashMap<>();
    cells.put("division", branches.getOrDefault(c.getBranchId(), "-"));
    cells.put("department", c.getDepartment() == null ? "-" : c.getDepartment());
    cells.put("bank", c.getBankAccountCode());
    cells.put("party", c.getPartyCode());
    cells.put("dueDate", c.getChequeDate());
    cells.put("chequeNo", c.getChequeNo());
    cells.put("customer", c.getPayerName() + " / " + c.getDraweeBank());
    cells.put("currency", c.getCurrency());
    cells.put("amount", c.getAmount());
    cells.put("lcValue", c.getBaseAmount());
    cells.put("pdcNo", c.getPdcNo());
    cells.put("receivedDate", c.getReceivedDate());
    cells.put("status", a.status().name());
    return cells;
  }

  private static boolean inRanges(Map<String, Object> row, ReportParameters p) {
    for (String[] range : RANGES) {
      String value = String.valueOf(row.get(range[0]));
      String from = p.optionalText(range[0] + FROM_SUFFIX).orElse("");
      String to = p.optionalText(range[0] + TO_SUFFIX).orElse("￿");
      if (value.compareTo(from) < 0 || value.compareTo(to) > 0) {
        return false;
      }
    }
    return true;
  }

  private List<ReportColumn> columns() {
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.date("dueDate", "Due Date"));
    columns.add(ReportColumn.text("chequeNo", "Cheque No."));
    columns.add(ReportColumn.text("party", "Customer Account"));
    columns.add(ReportColumn.text("customer", "Customer Name / Bank Name"));
    if (byDivision) {
      columns.add(ReportColumn.text("bank", "Bank Account"));
    }
    columns.add(ReportColumn.text("currency", "Currency"));
    columns.add(ReportColumn.amount("amount", "Cheque Amount"));
    columns.add(ReportColumn.amount("lcValue", "LC Value"));
    columns.add(ReportColumn.text("pdcNo", "Receipt No."));
    columns.add(ReportColumn.date("receivedDate", "Date"));
    columns.add(ReportColumn.text("status", "Status"));
    return columns;
  }
}
