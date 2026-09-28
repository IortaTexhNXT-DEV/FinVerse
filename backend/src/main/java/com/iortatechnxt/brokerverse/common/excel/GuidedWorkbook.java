package com.iortatechnxt.brokerverse.common.excel;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * A guided template being written: the workbook with its data sheets laid out (title block, guide
 * band, header, examples), so a caller can add data rows below (e.g. the error file of an upload,
 * which keeps the guide so the corrected file is uploaded again in the same layout).
 */
public final class GuidedWorkbook implements AutoCloseable {

  private final XSSFWorkbook wb;
  private final GuidedStyles styles;
  private final List<Placed> sheets;

  /**
   * Where a data sheet's header and first free row are.
   *
   * @param sheet sheet
   * @param headerRow 0-based header row
   * @param firstDataRow 0-based first row after the examples
   * @param columns columns
   */
  record Placed(Sheet sheet, int headerRow, int firstDataRow, List<GuideColumn> columns) {}

  GuidedWorkbook(XSSFWorkbook wb, GuidedStyles styles, List<Placed> sheets) {
    this.wb = wb;
    this.styles = styles;
    this.sheets = List.copyOf(sheets);
  }

  /**
   * The workbook.
   *
   * @return workbook
   */
  public XSSFWorkbook workbook() {
    return wb;
  }

  /**
   * A data sheet.
   *
   * @param index data sheet index (0 = first data sheet, "Start here" not counted)
   * @return sheet
   */
  public Sheet sheet(int index) {
    return sheets.get(index).sheet();
  }

  /**
   * The 0-based header row of a data sheet.
   *
   * @param index data sheet index
   * @return row index
   */
  public int headerRow(int index) {
    return sheets.get(index).headerRow();
  }

  /**
   * The 0-based first row after the header and examples of a data sheet.
   *
   * @param index data sheet index
   * @return row index
   */
  public int firstDataRow(int index) {
    return sheets.get(index).firstDataRow();
  }

  /**
   * The 0-based sheet column of a template column (column A holds the guide labels).
   *
   * @param column template column index
   * @return sheet column index
   */
  public static int sheetColumn(int column) {
    return column + GuidedTemplateWriter.FIRST_COLUMN;
  }

  /**
   * The style of a data cell of a kind.
   *
   * @param kind kind
   * @return style
   */
  public CellStyle dataStyle(Kind kind) {
    return styles.data(kind, false);
  }

  /**
   * The style of an offending cell (error file).
   *
   * @param date true for a date cell
   * @return style
   */
  public CellStyle markedStyle(boolean date) {
    return date ? styles.markedDate : styles.marked;
  }

  /**
   * The workbook as XLSX bytes.
   *
   * @return bytes
   */
  public byte[] bytes() {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      wb.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void close() {
    try {
      wb.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
