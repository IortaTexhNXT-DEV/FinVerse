package com.iortatechnxt.brokerverse.renewal.candidate.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A renewal list exported as PDF or CSV (FRRN.029.01), next to the Excel export: the same columns
 * and rows, dates as dd-MMM-yyyy and amounts formatted.
 */
@Component
public class CandidateExports {

  private final CandidateDocuments documents;
  private final DocumentComposer composer;

  /**
   * Creates the exports.
   *
   * @param documents rows of the export
   * @param composer PDF
   */
  public CandidateExports(CandidateDocuments documents, DocumentComposer composer) {
    this.documents = documents;
    this.composer = composer;
  }

  /**
   * A renewal list as a file.
   *
   * @param filter criteria
   * @param format pdf or csv
   * @return file
   */
  @Transactional(readOnly = true)
  public MessageFile export(CandidateFilter filter, String format) {
    List<List<String>> rows =
        documents.rows(filter).stream()
            .map(r -> r.stream().map(CandidateExports::text).toList())
            .toList();
    if ("csv".equals(format)) {
      String csv =
          line(CandidateDocuments.headers())
              + rows.stream().map(CandidateExports::line).collect(Collectors.joining());
      return new MessageFile("renewals.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
    }
    if ("pdf".equals(format)) {
      byte[] pdf =
          composer.pdf(
              new DocumentSpec(
                  "",
                  "Renewal Accounts",
                  rows.size() + " account(s)",
                  List.of(new Table(null, CandidateDocuments.headers(), rows, null)),
                  List.of(),
                  null));
      return new MessageFile("renewals.pdf", "application/pdf", pdf);
    }
    throw new BusinessRuleException("RNW_EXPORT_FORMAT", "Export as xlsx, pdf or csv");
  }

  private static String line(List<String> cells) {
    return cells.stream().map(CandidateExports::quote).collect(Collectors.joining(",")) + "\r\n";
  }

  private static String quote(String cell) {
    String v = cell == null ? "" : cell;
    return v.contains(",") || v.contains("\"") || v.contains("\n")
        ? "\"" + v.replace("\"", "\"\"") + "\""
        : v;
  }

  private static String text(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof LocalDate d) {
      return DisplayFormat.date(d);
    }
    return value instanceof BigDecimal b ? DisplayFormat.value(b) : value.toString();
  }
}
