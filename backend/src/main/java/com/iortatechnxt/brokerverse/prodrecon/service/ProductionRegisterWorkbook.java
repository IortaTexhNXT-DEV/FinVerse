package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.common.excel.SheetColumnWidths;
import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.messaging.service.DocumentPasswordPolicy;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtractLine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Writes the production register workbook sent to an insurer (PRCID.002/006): the Annex IV columns,
 * every cell locked by a sheet protection except the insurer's columns (remarks and incentive), and
 * blank unlocked rows below the register where the insurer adds production BDOI did not send. The
 * statuses are written as their labels ("Fully Remitted"), dates as dd-MMM-yyyy, and every column
 * is as wide as its content. Opening the file is protected separately by the e-mail password
 * (PRCID.007).
 */
@Component
public class ProductionRegisterWorkbook {

  /** MIME type of the workbook. */
  public static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private static final int BLANK_ROWS = 200;
  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
  private static final List<String> EDITABLE =
      List.of(RegisterLayout.INCENTIVE, RegisterLayout.REMARKS);

  private final DocumentPasswordPolicy passwords;

  /**
   * Creates the writer.
   *
   * @param passwords source of the sheet protection password (never shared)
   */
  public ProductionRegisterWorkbook(DocumentPasswordPolicy passwords) {
    this.passwords = passwords;
  }

  /**
   * The workbook of a register.
   *
   * @param month production month
   * @param lines register lines
   * @return xlsx bytes
   */
  public byte[] write(LocalDate month, List<ReconExtractLine> lines) {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      Styles styles = new Styles(wb);
      XSSFSheet sheet = wb.createSheet("Production Register");
      Row header = sheet.createRow(0);
      for (int c = 0; c < RegisterLayout.HEADERS.size(); c++) {
        Cell cell = header.createCell(c);
        cell.setCellValue(RegisterLayout.HEADERS.get(c));
        cell.setCellStyle(styles.header);
      }
      int r = 1;
      for (ReconExtractLine line : lines) {
        Row row = sheet.createRow(r++);
        List<Object> values = values(month, line);
        for (int c = 0; c < values.size(); c++) {
          write(row.createCell(c), values.get(c), styles, c);
        }
      }
      SheetColumnWidths.fit(sheet, 0, RegisterLayout.HEADERS.size());
      for (int i = 0; i < BLANK_ROWS; i++) {
        Row row = sheet.createRow(r++);
        for (int c = 0; c < RegisterLayout.HEADERS.size(); c++) {
          row.createCell(c).setCellStyle(styles.open);
        }
      }
      sheet.createFreezePane(0, 1);
      sheet.protectSheet(passwords.newPassword());
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      wb.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static List<Object> values(LocalDate month, ReconExtractLine l) {
    List<Object> v = new ArrayList<>();
    v.add(MONTH.format(month));
    v.add(l.getInsurerCode());
    v.add(l.getInvoiceNo());
    v.add(l.getBookingDate());
    v.add(l.getInceptionDate());
    v.add(l.getExpiryDate());
    v.add(l.getPolicyNo());
    v.add(l.getEndorsementNo());
    v.add(l.getPnNos());
    v.add(l.getAssuredName());
    v.add(l.getRiskCode());
    v.add(l.getBasicPremium());
    v.add(l.getGrossCommission());
    v.add(l.getClientCode());
    v.add(l.getGrossPremium());
    v.add(l.getBookedVat());
    v.add(l.getAmountPaid());
    v.add(l.getDatePaid());
    v.add(l.getArNumber());
    v.add(label(l.getKind()));
    v.add(label(l.getRemittanceStatus()));
    v.add(null);
    v.add(null);
    return v;
  }

  /** A status code as its label (WITH_OUTSTANDING_BALANCE as "With Outstanding Balance"). */
  private static String label(String code) {
    return code == null ? null : DisplayFormat.label(code);
  }

  private static void write(Cell cell, Object value, Styles styles, int column) {
    boolean editable = EDITABLE.contains(RegisterLayout.HEADERS.get(column));
    switch (value) {
      case null -> cell.setCellStyle(editable ? styles.open : styles.locked);
      case BigDecimal n -> {
        cell.setCellValue(n.doubleValue());
        cell.setCellStyle(styles.amount);
      }
      case LocalDate d -> {
        cell.setCellValue(d);
        cell.setCellStyle(styles.date);
      }
      default -> {
        cell.setCellValue(value.toString());
        cell.setCellStyle(styles.locked);
      }
    }
  }

  /** Cell styles of the workbook. */
  private static final class Styles {

    private final CellStyle header;
    private final CellStyle locked;
    private final CellStyle open;
    private final CellStyle date;
    private final CellStyle amount;

    Styles(XSSFWorkbook wb) {
      XSSFCellStyle head = wb.createCellStyle();
      header = head;
      Font font = wb.createFont();
      font.setBold(true);
      font.setColor(IndexedColors.WHITE.getIndex());
      header.setFont(font);
      java.awt.Color brand = BrandAssets.color(BrandAssets.HEADER);
      head.setFillForegroundColor(
          new XSSFColor(
              new byte[] {(byte) brand.getRed(), (byte) brand.getGreen(), (byte) brand.getBlue()},
              new DefaultIndexedColorMap()));
      header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      header.setWrapText(true);
      header.setLocked(true);
      header.setAlignment(HorizontalAlignment.LEFT);
      header.setIndention(SheetColumnWidths.TEXT_INDENT);
      // Texts start one indent from the cell border, so a date or amount of the column before
      // never runs into them.
      locked = wb.createCellStyle();
      locked.setLocked(true);
      locked.setAlignment(HorizontalAlignment.LEFT);
      locked.setIndention(SheetColumnWidths.TEXT_INDENT);
      open = wb.createCellStyle();
      open.setLocked(false);
      open.setAlignment(HorizontalAlignment.LEFT);
      open.setIndention(SheetColumnWidths.TEXT_INDENT);
      date = wb.createCellStyle();
      date.setLocked(true);
      date.setDataFormat(
          wb.getCreationHelper().createDataFormat().getFormat(DisplayFormat.SHEET_DATE_FORMAT));
      amount = wb.createCellStyle();
      amount.setLocked(true);
      amount.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
    }
  }
}
