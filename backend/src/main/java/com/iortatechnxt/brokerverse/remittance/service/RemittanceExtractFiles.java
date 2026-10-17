package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine.MallAssurance;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceAmounts;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RemittanceType;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceSettingChange.Values;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Component;

/**
 * The remittance extract files of a batch (FRS BRD-02 FRID.RMT.001.03/001.04/001.08/001.09, ICD
 * IF-RMT-01 and IF-RMT-02): the Excel 97-2003 extract in the layout of the Normal - Peso and Normal
 * - Dollar report with the Mall Assurance columns, totals and closing lines, and the pipe-delimited
 * text extract with an H record carrying the control totals and one D record per account. The names
 * follow the settings: YYYYMMDD + insurer code + 8-digit settlement number + type suffix for the
 * Excel file, the text prefix + YYMMDD + insurer code + settlement number for the text file.
 */
@SuppressWarnings(
    "PMD.GodClass") // one layout of IF-RMT-01: heading, columns, totals, closing lines
@Component
public class RemittanceExtractFiles {

  /** Excel 97-2003 content type. */
  public static final String XLS = "application/vnd.ms-excel";

  /** Text content type. */
  public static final String TEXT = "text/plain";

  /** Column headers of the Excel extract, by column (1-based), merged columns once. */
  static final List<String> HEADERS =
      List.of(
          "Invoice Number",
          "Policy Number",
          "Endorsement Number",
          "With COI",
          "Name of Assured",
          "Product Line",
          "Record Status",
          "",
          "",
          "Date Last Paid",
          "Date Applied",
          "",
          "Date of Inception",
          "Date Booked",
          "Date of Expiry",
          "OR Delivery",
          "Payment Status",
          "Paid AR",
          "Realized Commission",
          "Realized VAT",
          "WTAX",
          "DTIP",
          "Commission Receivable",
          "Net Due",
          "Mall Assurance Mode of Payment",
          "Mall Assurance Service Charge",
          "Mall Assurance Credit / Debit Card Charge",
          "Mall Assurance Total Deductions",
          "Mall Assurance VAT",
          "Mall Assurance WTAX",
          "Net Due After Mall Assurance",
          "Amount Due for Remittance",
          "Date of Official Receipt",
          "Official Receipt Number",
          "OR Amount",
          "Extracted Date",
          "Batch Number",
          "Processor");

  /** Header row of the Excel extract (row 9). */
  static final int HEADER_ROW = 8;

  /** First amount column (Paid AR), 0-based. */
  private static final int FIRST_AMOUNT = 17;

  /** Last summed amount column (OR Amount), 0-based. */
  private static final int LAST_AMOUNT = 34;

  private static final int MALL_MODE = 24;
  private static final int STATUS_FROM = 6;
  private static final int STATUS_TO = 8;
  private static final int APPLIED_FROM = 10;
  private static final int APPLIED_TO = 11;
  private static final int RUN_COLUMN = 8;
  private static final int CHECKED_BY_COLUMN = 3;
  private static final int TITLE_ROW = 3;
  private static final int EXTRACTED_ROW = 6;
  private static final int LABEL_COLUMN = 4;
  private static final int CLOSING_AMOUNT_COLUMN = 5;
  private static final int OR_DATE = 32;
  private static final int OR_NO = 33;
  private static final int SETTLEMENT_DIGITS = 8;
  private static final String YES = "Yes";
  private static final String NO = "No";
  private static final DateTimeFormatter SHEET_DATE =
      DateTimeFormatter.ofPattern("M/d/uuuu", Locale.ENGLISH);
  private static final DateTimeFormatter RUN_TIME =
      DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

  private final OrganizationService organization;
  private final RemittanceSettingsService settings;
  private final Clock clock;

  /**
   * Creates the files.
   *
   * @param organization company name, address and TIN of the heading
   * @param settings file naming
   * @param clock run date and time of the heading
   */
  public RemittanceExtractFiles(
      OrganizationService organization, RemittanceSettingsService settings, Clock clock) {
    this.organization = organization;
    this.settings = settings;
    this.clock = clock;
  }

  /**
   * The 8-digit settlement number of an allocated sequence number (RMS-insurer-000123).
   *
   * @param allocated number from the document numbering
   * @return the running number with 8 digits
   */
  public static String settlementNo(String allocated) {
    String digits = allocated.substring(allocated.lastIndexOf('-') + 1);
    String value = Long.toString(Long.parseLong(digits));
    if (value.length() > SETTLEMENT_DIGITS) {
      throw new IllegalStateException("Settlement number above 8 digits: " + allocated);
    }
    return "0".repeat(SETTLEMENT_DIGITS - value.length()) + value;
  }

  /**
   * Suffix of a remittance type in the file names.
   *
   * @param type type
   * @return NormalPeso, NormalDollar, WithIncentives or Special
   */
  public static String typeSuffix(RemittanceType type) {
    return switch (type) {
      case NORMAL_PHP -> "NormalPeso";
      case NORMAL_USD -> "NormalDollar";
      case WITH_INCENTIVES -> "WithIncentives";
      case SPECIAL -> "Special";
    };
  }

  /**
   * Date of the extraction of a batch: its creation date in the Philippines.
   *
   * @param batch batch
   * @return date
   */
  public LocalDate extractedOn(RemittanceBatch batch) {
    Instant created = batch.getCreatedAt();
    return created == null
        ? BusinessClock.today(clock)
        : created.atZone(BusinessClock.zone()).toLocalDate();
  }

  /**
   * Name of the Excel extract.
   *
   * @param batch batch
   * @return name with .xls
   */
  public String excelName(RemittanceBatch batch) {
    Values values = settings.current(batch.getCompanyId());
    return name(values.filePattern(), batch, extractedOn(batch)) + ".xls";
  }

  /**
   * Name of the text extract.
   *
   * @param batch batch
   * @return name with .txt
   */
  public String textName(RemittanceBatch batch) {
    Values values = settings.current(batch.getCompanyId());
    return values.textPrefix()
        + extractedOn(batch).format(DateTimeFormatter.ofPattern("uuMMdd", Locale.ENGLISH))
        + batch.getInsurerCode()
        + settlement(batch)
        + ".txt";
  }

  static String name(String pattern, RemittanceBatch batch, LocalDate date) {
    return pattern
        .replace("{date}", date.format(DateTimeFormatter.BASIC_ISO_DATE))
        .replace("{insurer}", batch.getInsurerCode())
        .replace("{settlement}", settlement(batch))
        .replace("{type}", typeSuffix(batch.getRemittanceType()))
        .replaceAll("[^A-Za-z0-9._-]", "_");
  }

  private static String settlement(RemittanceBatch batch) {
    return batch.getSettlementNo() == null ? batch.getBatchNo() : batch.getSettlementNo();
  }

  /**
   * The Excel 97-2003 extract (IF-RMT-01).
   *
   * @param batch batch with lines
   * @param runBy user who runs the extract
   * @return .xls bytes
   */
  public byte[] excel(RemittanceBatch batch, String runBy) {
    try (HSSFWorkbook wb = new HSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Styles styles = new Styles(wb);
      Sheet sheet = wb.createSheet("RemittanceExtract" + typeSuffix(batch.getRemittanceType()));
      heading(sheet, styles, batch, runBy);
      Row header = sheet.createRow(HEADER_ROW);
      for (int c = 0; c < HEADERS.size(); c++) {
        text(header, c, HEADERS.get(c), styles.bold);
      }
      sheet.addMergedRegion(new CellRangeAddress(HEADER_ROW, HEADER_ROW, STATUS_FROM, STATUS_TO));
      sheet.addMergedRegion(new CellRangeAddress(HEADER_ROW, HEADER_ROW, APPLIED_FROM, APPLIED_TO));
      List<BatchLine> lines = batch.included();
      int r = accounts(sheet, styles, batch, lines);
      r = closingLines(sheet, styles, r, lines);
      Row sign = sheet.createRow(r + 1);
      text(sign, 0, "Printed By:", null);
      text(sign, CHECKED_BY_COLUMN, "Checked By:", null);
      wb.write(out);
      return out.toByteArray();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private int accounts(Sheet sheet, Styles styles, RemittanceBatch batch, List<BatchLine> lines) {
    int r = HEADER_ROW + 1;
    BigDecimal[] totals = new BigDecimal[HEADERS.size()];
    for (BatchLine l : lines) {
      Row row = sheet.createRow(r);
      List<Object> cells = cells(batch, l);
      for (int c = 0; c < cells.size(); c++) {
        cell(row, c, cells.get(c), styles, totals);
      }
      sheet.addMergedRegion(new CellRangeAddress(r, r, STATUS_FROM, STATUS_TO));
      sheet.addMergedRegion(new CellRangeAddress(r, r, APPLIED_FROM, APPLIED_TO));
      r++;
    }
    Row totalRow = sheet.createRow(r);
    for (int c = FIRST_AMOUNT; c <= LAST_AMOUNT; c++) {
      if (totals[c] != null) {
        number(totalRow, c, totals[c], styles.amountBold);
      }
    }
    return r;
  }

  private static void cell(Row row, int c, Object v, Styles styles, BigDecimal[] totals) {
    if (v instanceof BigDecimal amount) {
      number(row, c, amount, styles.amount);
      totals[c] = totals[c] == null ? amount : totals[c].add(amount);
    } else {
      text(row, c, v == null ? "" : v.toString(), null);
    }
  }

  private static int closingLines(Sheet sheet, Styles styles, int totalRow, List<BatchLine> lines) {
    int r = totalRow + 2;
    Row count = sheet.createRow(r);
    text(count, 0, "Number of Accounts:", styles.bold);
    text(count, 1, String.valueOf(lines.size()), null);
    r += 2;
    Closing closing = closing(lines);
    r = closingLine(sheet, r, "Total Remittance", closing.totalRemittance(), styles);
    r = closingLine(sheet, r, "Less: Mall Assurance Deductions", closing.deductions(), styles);
    r = closingLine(sheet, r, "Less: Mall Assurance VAT", closing.vat(), styles);
    r = closingLine(sheet, r, "Less: Mall Assurance WTAX", closing.wtax(), styles);
    return closingLine(
        sheet, r, "Grand Total After Mall Assurance Charges", closing.grandTotal(), styles);
  }

  private void heading(Sheet sheet, Styles styles, RemittanceBatch batch, String runBy) {
    ZonedDateTime now = clock.instant().atZone(BusinessClock.zone());
    Company company = organization.getCompany(batch.getCompanyId());
    String[] run = {
      "Run Date : " + now.toLocalDate().format(SHEET_DATE),
      "Run Time : " + now.format(RUN_TIME),
      "Run By : " + (runBy == null ? "SYSTEM" : runBy),
      "Page : 1 of 1"
    };
    for (int i = 0; i < run.length; i++) {
      text(sheet.createRow(i), RUN_COLUMN, run[i], null);
    }
    text(sheet.getRow(1), 0, company.getName(), styles.bold);
    String address = company.getAddress() == null ? "" : company.getAddress();
    String tin = company.getTaxId() == null ? "" : "VAT Reg. TIN " + company.getTaxId();
    text(sheet.getRow(2), 0, (address + " " + tin).strip(), null);
    text(
        sheet.getRow(TITLE_ROW),
        0,
        "REMITTANCE EXTRACT "
            + BatchDocuments.typeLabel(batch.getRemittanceType())
                .replace(" - ", " ")
                .toUpperCase(Locale.ROOT)
            + ": "
            + batch.getSettlementRef(),
        styles.bold);
    text(
        sheet.createRow(EXTRACTED_ROW),
        0,
        "DATE EXTRACTED: " + extractedOn(batch).format(SHEET_DATE),
        styles.bold);
  }

  private List<Object> cells(RemittanceBatch batch, BatchLine l) {
    RemittanceAmounts a = l.getAmounts();
    MallAssurance m = l.getMallAssurance();
    BigDecimal afterMall = a.netDue().subtract(m.totalDeductions());
    List<Object> c = new ArrayList<>(HEADERS.size());
    c.add(l.getInvoiceNo());
    c.add(nz(l.getPolicyNo()));
    c.add(nz(l.getEndorsementNo()));
    c.add(NO);
    c.add(l.getAssuredName());
    c.add(nz(l.getProductLine()));
    c.add(l.getInsurerOrNo() == null ? "For Remittance" : "OR Received");
    c.add("");
    c.add("");
    c.add(date(l.getLastPaidOn()));
    c.add(date(l.getLastPaidOn()));
    c.add("");
    c.add(date(l.getInceptionDate()));
    c.add(date(l.getBookingDate()));
    c.add(date(l.getExpiryDate()));
    c.add("");
    c.add(a.dtip().compareTo(a.paidAr()) > 0 ? "Partially Paid" : "Fully Paid");
    c.add(a.paidAr());
    c.add(a.commission());
    c.add(a.commissionVat());
    c.add(a.wtax());
    c.add(a.dtip());
    c.add(BigDecimal.ZERO.setScale(2));
    c.add(a.netDue());
    c.add(nz(m.paymentMode()));
    c.add(m.serviceCharge());
    c.add(m.cardCharge());
    c.add(m.totalDeductions());
    c.add(m.vat());
    c.add(m.wtax());
    c.add(afterMall);
    c.add(amountDue(l));
    c.add(date(l.getInsurerOrDate()));
    c.add(nz(l.getInsurerOrNo()));
    c.add(l.getInsurerOrAmount() == null ? "" : l.getInsurerOrAmount());
    c.add(extractedOn(batch).format(SHEET_DATE));
    c.add(batch.getSettlementRef());
    c.add(nz(batch.getProcessor()));
    return c;
  }

  /**
   * Amount due for remittance of an account: net due less the Mall Assurance deductions, VAT and
   * withholding.
   *
   * @param l line
   * @return amount
   */
  static BigDecimal amountDue(BatchLine l) {
    MallAssurance m = l.getMallAssurance();
    return l.getAmounts()
        .netDue()
        .subtract(m.totalDeductions())
        .subtract(m.vat())
        .subtract(m.wtax());
  }

  static Closing closing(List<BatchLine> lines) {
    BigDecimal total = BigDecimal.ZERO;
    BigDecimal deductions = BigDecimal.ZERO;
    BigDecimal vat = BigDecimal.ZERO;
    BigDecimal wtax = BigDecimal.ZERO;
    for (BatchLine l : lines) {
      MallAssurance m = l.getMallAssurance();
      total = total.add(l.getAmounts().netDue());
      deductions = deductions.add(m.totalDeductions());
      vat = vat.add(m.vat());
      wtax = wtax.add(m.wtax());
    }
    return new Closing(total, deductions, vat, wtax);
  }

  /**
   * Closing lines of the Excel extract.
   *
   * @param totalRemittance sum of the net due
   * @param deductions Mall Assurance deductions
   * @param vat Mall Assurance VAT
   * @param wtax Mall Assurance WTAX
   */
  record Closing(
      BigDecimal totalRemittance, BigDecimal deductions, BigDecimal vat, BigDecimal wtax) {

    BigDecimal grandTotal() {
      return totalRemittance.subtract(deductions).subtract(vat).subtract(wtax);
    }
  }

  private static int closingLine(
      Sheet sheet, int r, String label, BigDecimal amount, Styles styles) {
    Row row = sheet.createRow(r);
    text(row, LABEL_COLUMN, label, styles.bold);
    number(row, CLOSING_AMOUNT_COLUMN, amount, styles.amount);
    return r + 1;
  }

  /**
   * The text extract (IF-RMT-02): H record with the control totals, then one D record per account.
   *
   * @param batch batch with lines
   * @return UTF-8 bytes with a byte order mark, lines ended by CR LF
   */
  public byte[] text(RemittanceBatch batch) {
    return RemittanceTextExtract.write(batch, extractedOn(batch));
  }

  private static String nz(String value) {
    return value == null ? "" : value;
  }

  private static String date(LocalDate value) {
    return value == null ? "" : value.format(SHEET_DATE);
  }

  private static void text(Row row, int column, String value, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellValue(value);
    if (style != null) {
      cell.setCellStyle(style);
    }
  }

  private static void number(Row row, int column, BigDecimal value, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellValue(value.doubleValue());
    cell.setCellStyle(style);
  }

  /** Column indexes the OR return reads (0-based). */
  static int orDateColumn() {
    return OR_DATE;
  }

  static int orNoColumn() {
    return OR_NO;
  }

  static int mallModeColumn() {
    return MALL_MODE;
  }

  /** Cell styles of the workbook. */
  private static final class Styles {
    private final CellStyle bold;
    private final CellStyle amount;
    private final CellStyle amountBold;

    Styles(HSSFWorkbook wb) {
      Font font = wb.createFont();
      font.setBold(true);
      bold = wb.createCellStyle();
      bold.setFont(font);
      short format = wb.createDataFormat().getFormat("#,##0.00;(#,##0.00)");
      amount = wb.createCellStyle();
      amount.setDataFormat(format);
      amountBold = wb.createCellStyle();
      amountBold.setDataFormat(format);
      amountBold.setFont(font);
    }
  }
}
