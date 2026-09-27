package com.iortatechnxt.brokerverse.renewal.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.renewal.insurer.service.InsurerExtract;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Extract per insurer (RNW-INSURER-EXTRACT; FR-RN-070, BRD 3.009.1.4, 4.007.1.4): the 28 columns of
 * the insurer batches, as frozen when each batch was built, per insurer and batch.
 */
@Component
public class InsurerExtractReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "RNW-INSURER-EXTRACT";

  private static final String BATCH = "batchNo";
  private static final TypeReference<List<String>> ROW = new TypeReference<>() {};

  private static final String SQL =
      "select b.batch_no, b.insurer_code, l.snapshot"
          + " from rnw_insurer_batch_line l join rnw_insurer_batch b on b.id = l.batch_id"
          + " join rnw_candidate c on c.id = l.candidate_id"
          + " where (cast(:batch as varchar) is null or b.batch_no = :batch)"
          + RenewalReportSupport.FILTERS
          + " order by b.batch_no, l.id";

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;
  private final ObjectMapper json;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   * @param support filters and scope
   * @param json JSON
   */
  public InsurerExtractReport(NbReportJdbc jdbc, RenewalReportSupport support, ObjectMapper json) {
    this.jdbc = jdbc;
    this.support = support;
    this.json = json;
  }

  @Override
  public ReportMetadata metadata() {
    return RenewalReportSupport.metadata(
        CODE,
        "Renewal Extract per Insurer",
        "The 28 columns sent to the insurers, per batch",
        ParameterSpec.optional(BATCH, "Batch No.", ParameterType.TEXT));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    var args =
        support.args(p).with("batch", p.optionalText(BATCH).filter(v -> !v.isBlank()).orElse(null));
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> r : jdbc.rows(SQL, args.map())) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("batch_no", r.get("batch_no"));
      List<String> values = read((String) r.get("snapshot"));
      for (int i = 0; i < InsurerExtract.HEADERS.size(); i++) {
        row.put("c" + i, i < values.size() ? values.get(i) : null);
      }
      rows.add(row);
    }
    List<ReportColumn> columns = new ArrayList<>();
    for (int i = 0; i < InsurerExtract.HEADERS.size(); i++) {
      columns.add(ReportColumn.text("c" + i, InsurerExtract.HEADERS.get(i)));
    }
    return TabularReportBuilder.of(p)
        .columns(columns)
        .groupBy("batch_no", "Batch")
        .rows(rows)
        .presorted()
        .build();
  }

  private List<String> read(String snapshot) {
    try {
      return json.readValue(snapshot, ROW);
    } catch (JsonProcessingException e) {
      return List.of();
    }
  }
}
