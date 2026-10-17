package com.iortatechnxt.brokerverse.common.excel;

import java.util.List;

/**
 * One data sheet of a guided template: its tab name, title, what the sheet holds, its columns and
 * the example rows (values in column order; an ISO date for a date column).
 *
 * @param name tab name (at most 31 characters, no \ / ? * [ ] :)
 * @param title title shown on the sheet
 * @param intro what one row of the sheet is
 * @param columns columns in order
 * @param examples example rows, marked on the sheet as examples to overwrite or delete
 */
public record GuidedSheet(
    String name,
    String title,
    String intro,
    List<GuideColumn> columns,
    List<List<String>> examples) {

  /** Copies of the lists, null-safe texts. */
  public GuidedSheet {
    title = title == null ? name : title;
    intro = intro == null ? "" : intro;
    columns = List.copyOf(columns);
    examples = examples == null ? List.of() : examples.stream().map(List::copyOf).toList();
  }

  /**
   * A sheet whose one example row is taken from the columns' example values.
   *
   * @param name tab name
   * @param title title
   * @param intro what one row is
   * @param columns columns
   * @return sheet
   */
  public static GuidedSheet of(String name, String title, String intro, List<GuideColumn> columns) {
    boolean anyExample = columns.stream().anyMatch(c -> !c.example().isEmpty());
    List<List<String>> examples =
        anyExample ? List.of(columns.stream().map(GuideColumn::example).toList()) : List.of();
    return new GuidedSheet(name, title, intro, columns, examples);
  }

  /**
   * This sheet with other example rows.
   *
   * @param rows example rows (none for an error file or a sheet without examples)
   * @return sheet
   */
  public GuidedSheet withExamples(List<List<String>> rows) {
    return new GuidedSheet(name, title, intro, columns, rows);
  }
}
