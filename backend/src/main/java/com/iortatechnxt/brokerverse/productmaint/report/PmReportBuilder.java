package com.iortatechnxt.brokerverse.productmaint.report;

import com.iortatechnxt.brokerverse.report.core.CodeSet;
import com.iortatechnxt.brokerverse.report.core.NamedExport;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * The Product Maintenance report builder (BDOI FRS FRPM.020.01, PM-REPORT-BUILDER): a custom report
 * of quotation requests, package requests or products with the columns, filters and sort the user
 * chooses; previewed, printed and exported like every report, and saved as a report template (a
 * report variant) that the user may share with the other authorized users, who then get the same
 * result.
 */
@Component
public class PmReportBuilder implements ReportDefinition, NamedExport {

  /** Report code. */
  public static final String CODE = "PM-REPORT-BUILDER";

  /** Source parameter. */
  public static final String SOURCE = "source";

  /** Columns parameter. */
  public static final String COLUMNS = "columns";

  private static final String FROM = "from";
  private static final String TO = "to";
  private static final String LINE = "lineCode";
  private static final String STATUS = "status";
  private static final String SORT = "sortBy";

  private final BuilderRows rows;

  /**
   * Creates the report.
   *
   * @param rows source records
   */
  public PmReportBuilder(BuilderRows rows) {
    this.rows = rows;
  }

  @Override
  public String exportName() {
    return "Custom Report";
  }

  @Override
  public ReportMetadata metadata() {
    return PmReportSupport.metadata(
        CODE,
        "Product Maintenance Report Builder",
        "Custom report of quotation requests, package requests or products with chosen columns",
        ParameterSpec.select(
            SOURCE,
            "Source",
            List.of(BuilderRows.QUOTATION, BuilderRows.PACKAGE, BuilderRows.PRODUCT),
            BuilderRows.QUOTATION),
        ParameterSpec.codeSet(COLUMNS, "Columns", BuilderColumnCodes.SOURCE),
        ParameterSpec.optional(FROM, "Request Date From", ParameterType.DATE),
        ParameterSpec.optional(TO, "Request Date To", ParameterType.DATE),
        ParameterSpec.optional(LINE, "Line of Insurance", ParameterType.BUSINESS_LINE),
        ParameterSpec.optional(STATUS, "Status Contains", ParameterType.TEXT),
        ParameterSpec.select(
            SORT,
            "Sort By",
            BuilderField.all().stream().map(Enum::name).toList(),
            BuilderField.REFERENCE.name()));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<BuilderField> columns = columns(p.codeSet(COLUMNS));
    BuilderField sort =
        BuilderField.valueOf(p.optionalText(SORT).orElse(BuilderField.REFERENCE.name()));
    LocalDate from = p.optionalDate(FROM).orElse(null);
    LocalDate to = p.optionalDate(TO).orElse(null);
    Pattern status =
        p.optionalText(STATUS)
            .filter(s -> !s.isBlank())
            .map(s -> Pattern.compile(Pattern.quote(s.strip()), Pattern.CASE_INSENSITIVE))
            .orElse(null);
    List<Map<String, Object>> shown =
        rows
            .of(
                p.optionalText(SOURCE).orElse(BuilderRows.QUOTATION),
                p.longValue(PmReportSupport.COMPANY),
                PmReportSupport.upper(p, LINE))
            .stream()
            .filter(r -> within(r.get(BuilderField.REQUEST_DATE), from, to))
            .filter(r -> status == null || matches(status, r.get(BuilderField.STATUS)))
            .sorted(Comparator.comparing(r -> key(r.get(sort))))
            .map(r -> cells(r, columns))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(columns.stream().map(BuilderField::column).toList())
        .rows(shown)
        .presorted()
        .withoutGrandTotal()
        .build();
  }

  /**
   * The chosen columns in field order; the default columns when none is chosen.
   *
   * @param chosen code set of the columns parameter
   * @return fields
   */
  static List<BuilderField> columns(CodeSet chosen) {
    if (chosen.isAll()) {
      return BuilderField.defaults();
    }
    return BuilderField.all().stream().filter(f -> chosen.matches(f.name())).toList();
  }

  private static boolean within(Object date, LocalDate from, LocalDate to) {
    if (from == null && to == null) {
      return true;
    }
    if (!(date instanceof LocalDate d)) {
      return false;
    }
    return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
  }

  private static boolean matches(Pattern status, Object value) {
    return value != null && status.matcher(value.toString()).find();
  }

  private static String key(Object value) {
    if (value instanceof Number n) {
      return String.format(java.util.Locale.ROOT, "%020d", n.longValue());
    }
    return value == null ? "￿" : value.toString();
  }

  private static Map<String, Object> cells(Map<BuilderField, Object> r, List<BuilderField> cols) {
    Map<String, Object> row = new LinkedHashMap<>();
    cols.forEach(f -> row.put(f.name(), r.get(f) instanceof Number n ? n.toString() : r.get(f)));
    return row;
  }
}
