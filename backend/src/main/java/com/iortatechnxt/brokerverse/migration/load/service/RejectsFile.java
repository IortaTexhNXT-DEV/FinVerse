package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.migration.common.service.Workbooks;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigIssue;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigIssueRepository;
import com.iortatechnxt.brokerverse.migration.intake.domain.RowStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRowRepository;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rejection file of a batch (MIG-REJECTS in Excel; DATA_MIGRATION_DESIGN section 15.1): one
 * line per failing rule of each rejected or invalid row - row number, legacy key, column, value,
 * rule and message - with blank columns for the maker's correction and the checker's review, so the
 * corrected rows can be resubmitted.
 */
@Component
public class RejectsFile {

  private static final List<String> HEADERS =
      List.of(
          "row_no",
          "legacy_key",
          "status",
          "column",
          "value",
          "rule",
          "message",
          "correction",
          "corrected_by",
          "corrected_on",
          "checked_by");

  private final StageRowRepository rows;
  private final MigIssueRepository issues;

  /**
   * Creates the file builder.
   *
   * @param rows staged rows
   * @param issues issues
   */
  public RejectsFile(StageRowRepository rows, MigIssueRepository issues) {
    this.rows = rows;
    this.issues = issues;
  }

  /**
   * The lines of the rejection file of a batch.
   *
   * @param batch batch
   * @return lines
   */
  @Transactional(readOnly = true)
  public List<List<String>> lines(MigBatch batch) {
    List<StageRow> failing =
        rows.findByBatchIdAndStatusInOrderByIdAsc(
            batch.getId(), EnumSet.of(RowStatus.REJECTED, RowStatus.INVALID));
    Map<Long, List<MigIssue>> byRow =
        failing.isEmpty()
            ? Map.of()
            : issues.findByStageRowIdIn(failing.stream().map(StageRow::getId).toList()).stream()
                .filter(i -> i.getSeverity() == MigIssue.Severity.ERROR)
                .collect(Collectors.groupingBy(MigIssue::getStageRowId));
    List<List<String>> out = new ArrayList<>();
    for (StageRow r : failing) {
      List<MigIssue> list = byRow.getOrDefault(r.getId(), List.of());
      if (list.isEmpty()) {
        out.add(line(r, "", "", "", r.getMessage()));
      }
      for (MigIssue i : list) {
        out.add(line(r, i.getField(), i.getValue(), i.getRuleCode(), i.getMessage()));
      }
    }
    return out;
  }

  private static List<String> line(
      StageRow r, String column, String value, String rule, String message) {
    return List.of(
        String.valueOf(r.getRowNo()),
        r.getLegacyKey(),
        r.getStatus().name(),
        text(column),
        text(value),
        text(rule),
        text(message),
        "",
        "",
        "",
        "");
  }

  private static String text(String v) {
    return v == null ? "" : v;
  }

  /**
   * The rejection file as a workbook.
   *
   * @param batch batch
   * @return XLSX bytes
   */
  @Transactional(readOnly = true)
  public byte[] workbook(MigBatch batch) {
    try (Workbooks wb = Workbooks.create()) {
      return wb.sheet("Rejected rows", HEADERS, lines(batch)).bytes();
    }
  }
}
