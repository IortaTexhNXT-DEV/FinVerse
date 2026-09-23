package com.iortatechnxt.finverse.report.gl;

import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.journal.domain.JournalSearchCriteria;
import com.iortatechnxt.finverse.journal.domain.JournalStatus;
import com.iortatechnxt.finverse.journal.domain.JournalType;
import com.iortatechnxt.finverse.journal.service.JournalEntryService;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Journal register / transaction checklist with filters for batch number, inputter, authorizer,
 * status, type and cut-off amount.
 */
@Component
public class JournalRegisterReport implements ReportDefinition {

  private static final int MAX_ROWS = 20_000;
  private static final String ALL = "ALL";
  private static final String STATUS = "status";
  private static final String TYPE = "journalType";
  private static final String BATCH_NO = "batchNo";
  private static final String INPUTTER = "inputter";
  private static final String AUTHORIZER = "authorizer";
  private static final String MIN_AMOUNT = "minAmount";

  private final JournalEntryService journals;

  /**
   * Creates the report.
   *
   * @param journals journal service
   */
  public JournalRegisterReport(JournalEntryService journals) {
    this.journals = journals;
  }

  @Override
  public ReportMetadata metadata() {
    List<String> statuses = new ArrayList<>(List.of(ALL));
    Arrays.stream(JournalStatus.values()).forEach(s -> statuses.add(s.name()));
    List<String> types = new ArrayList<>(List.of(ALL));
    Arrays.stream(JournalType.values()).forEach(t -> types.add(t.name()));
    return new ReportMetadata(
        "GL-JRNL",
        "Journal Register / Transaction Checklist",
        ReportCategory.GENERAL_LEDGER,
        "Journals by date with inputter, authorizer and status",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.branchParam(),
            GlReportSupport.fromParam(),
            GlReportSupport.toParam(),
            ParameterSpec.select(STATUS, "Status", statuses, ALL),
            ParameterSpec.select(TYPE, "Journal Type", types, ALL),
            ParameterSpec.optional(BATCH_NO, "Batch No", ParameterType.TEXT),
            ParameterSpec.optional(INPUTTER, "Inputter", ParameterType.TEXT),
            ParameterSpec.optional(AUTHORIZER, "Authorizer", ParameterType.TEXT),
            ParameterSpec.optional(MIN_AMOUNT, "Cut-off Amount", ParameterType.NUMBER)),
        Permission.REPORT_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    String status = p.text(STATUS);
    String type = p.text(TYPE);
    var criteria =
        new JournalSearchCriteria(
            p.longValue(GlReportSupport.COMPANY),
            p.optionalLong(GlReportSupport.BRANCH).orElse(null),
            ALL.equals(status) ? null : JournalStatus.valueOf(status),
            ALL.equals(type) ? null : JournalType.valueOf(type),
            p.date(GlReportSupport.FROM),
            p.date(GlReportSupport.TO),
            p.optionalText(BATCH_NO).orElse(null),
            p.optionalText(INPUTTER).orElse(null),
            p.optionalText(AUTHORIZER).orElse(null),
            p.optionalDecimal(MIN_AMOUNT).orElse(null),
            null);
    var page =
        journals.search(criteria, PageRequest.of(0, MAX_ROWS, Sort.by("valueDate", BATCH_NO)));
    List<Map<String, Object>> rows =
        page.getContent().stream().map(JournalRegisterReport::row).toList();
    TabularReportBuilder builder =
        TabularReportBuilder.of(p)
            .columns(
                ReportColumn.text(BATCH_NO, "Batch No"),
                ReportColumn.text("type", "Type"),
                ReportColumn.text(STATUS, "Status"),
                ReportColumn.text("narration", "Narration"),
                ReportColumn.text(INPUTTER, "Inputter"),
                ReportColumn.text(AUTHORIZER, "Authorizer"),
                ReportColumn.amount("debit", "Total Debit"),
                ReportColumn.amount("credit", "Total Credit"))
            .groupBy("valueDate", "Value Date")
            .rows(rows);
    if (page.getTotalElements() > MAX_ROWS) {
      builder.note("Output limited to " + MAX_ROWS + " journals; narrow the filters.");
    }
    return builder.build();
  }

  private static Map<String, Object> row(JournalBatch b) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("valueDate", b.getValueDate().toString());
    m.put(BATCH_NO, b.getBatchNo());
    m.put("type", b.getJournalType().name());
    m.put(STATUS, b.getStatus().name());
    m.put("narration", b.getNarration());
    m.put(INPUTTER, b.getCreatedBy());
    m.put(AUTHORIZER, b.getAuthorizedBy());
    m.put("debit", b.getTotalDebit());
    m.put("credit", b.getTotalCredit());
    return m;
  }
}
