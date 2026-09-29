package com.iortatechnxt.brokerverse.common.excel;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationConstraint.OperatorType;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;

/**
 * The cell checks of a guided sheet: drop-downs of list and Y/N columns, date and number checks, an
 * input message on every column, and the note of every header cell.
 */
final class GuidedChecks {

  private static final int PROMPT_TITLE = 32;
  private static final int PROMPT_TEXT = 255;
  private static final int NOTE_WIDTH = 4;
  private static final int NOTE_CHARS_PER_ROW = 45;
  private static final int NOTE_MAX_ROWS = 18;
  private static final String BIG_NUMBER = "999999999999999";

  private GuidedChecks() {}

  /**
   * Adds the checks of every column on the data rows.
   *
   * @param sheet sheet
   * @param columns columns
   * @param lists lists sheet
   * @param first first data row (the example rows included)
   * @param last last data row
   */
  static void apply(
      Sheet sheet, List<GuideColumn> columns, GuidedLists lists, int first, int last) {
    DataValidationHelper helper = sheet.getDataValidationHelper();
    for (int c = 0; c < columns.size(); c++) {
      GuideColumn col = columns.get(c);
      DataValidationConstraint constraint = constraint(helper, col, lists);
      int column = GuidedWorkbook.sheetColumn(c);
      DataValidation v =
          helper.createValidation(
              constraint, new CellRangeAddressList(first, last, column, column));
      v.setShowPromptBox(true);
      v.createPromptBox(cut(col.header(), PROMPT_TITLE), cut(prompt(col), PROMPT_TEXT));
      boolean checked =
          col.kind() != Kind.TEXT && !(col.kind() == Kind.LIST && col.choices().isEmpty());
      v.setShowErrorBox(checked);
      if (checked) {
        v.setErrorStyle(DataValidation.ErrorStyle.STOP);
        v.createErrorBox(cut(col.header(), PROMPT_TITLE), cut(error(col), PROMPT_TEXT));
      }
      if (constraint.getValidationType() == DataValidationConstraint.ValidationType.LIST) {
        v.setSuppressDropDownArrow(true);
      }
      sheet.addValidationData(v);
    }
  }

  private static DataValidationConstraint constraint(
      DataValidationHelper helper, GuideColumn col, GuidedLists lists) {
    return switch (col.kind()) {
      case YES_NO -> helper.createExplicitListConstraint(new String[] {"Y", "N"});
      case DATE ->
          helper.createDateConstraint(
              OperatorType.BETWEEN, "DATE(1900,1,1)", "DATE(2099,12,31)", null);
      case NUMBER, AMOUNT ->
          helper.createDecimalConstraint(OperatorType.BETWEEN, "-" + BIG_NUMBER, BIG_NUMBER);
      case INTEGER ->
          helper.createIntegerConstraint(OperatorType.BETWEEN, "-2147483647", "2147483647");
      case LIST -> {
        String ref = lists.reference(col.choices());
        yield ref == null ? anyText(helper) : helper.createFormulaListConstraint(ref);
      }
      case TEXT -> anyText(helper);
    };
  }

  private static DataValidationConstraint anyText(DataValidationHelper helper) {
    return helper.createTextLengthConstraint(OperatorType.LESS_OR_EQUAL, "32767", null);
  }

  private static String prompt(GuideColumn col) {
    StringBuilder out = new StringBuilder(col.whatToEnter());
    append(out, "Mandatory: " + col.needText() + ".");
    append(out, "Format: " + col.formatText() + ".");
    return out.toString();
  }

  private static String error(GuideColumn col) {
    return switch (col.kind()) {
      case YES_NO -> "Enter Y or N.";
      case DATE -> "Enter a date, e.g. 15-Jan-2026.";
      case NUMBER, AMOUNT, INTEGER -> "Enter a number without thousands separators.";
      default -> "Choose a value from the drop-down (the list is on the Lists sheet).";
    };
  }

  /**
   * Adds the note of a header cell: the full description, format and allowed values.
   *
   * @param sheet sheet
   * @param cell header cell
   * @param col column
   */
  static void note(Sheet sheet, Cell cell, GuideColumn col) {
    StringBuilder text = new StringBuilder(col.header()).append('\n').append(col.noteText());
    append(text, "\nMandatory: " + col.needText());
    append(text, "\nFormat: " + col.formatText());
    String allowed = col.allowedText(GuidedLists.SHOWN_IN_BAND);
    if (!"Any".equals(allowed)) {
      append(text, "\nAllowed values: " + allowed);
    }
    Drawing<?> drawing = sheet.createDrawingPatriarch();
    ClientAnchor anchor = sheet.getWorkbook().getCreationHelper().createClientAnchor();
    int rows = Math.min(NOTE_MAX_ROWS, 2 + text.length() / NOTE_CHARS_PER_ROW);
    anchor.setCol1(cell.getColumnIndex());
    anchor.setCol2(cell.getColumnIndex() + NOTE_WIDTH);
    anchor.setRow1(cell.getRowIndex());
    anchor.setRow2(cell.getRowIndex() + rows);
    Comment comment = drawing.createCellComment(anchor);
    comment.setString(
        sheet.getWorkbook().getCreationHelper().createRichTextString(text.toString()));
    comment.setAuthor("BIBS");
    cell.setCellComment(comment);
  }

  private static void append(StringBuilder out, String part) {
    if (!out.isEmpty() && !Character.isWhitespace(part.charAt(0))) {
      out.append(' ');
    }
    out.append(part);
  }

  private static String cut(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max - 1) + "…";
  }
}
