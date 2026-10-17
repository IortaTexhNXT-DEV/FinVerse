package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.tax.domain.Certificate2307;
import com.iortatechnxt.brokerverse.tax.domain.Certificate2307Line;
import com.iortatechnxt.brokerverse.tax.domain.CertificateStatus;
import com.iortatechnxt.brokerverse.tax.domain.Taxpayer;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Prints BIR Form 2307 certificates in the layout of the official form (one certificate per page):
 * the period, Part I payee and payor information, Part II income payments by ATC for each month of
 * the quarter with the total and the tax withheld, the declaration and the payor / payee signature
 * blocks. It is a faithful reproduction for issuance to payees, not the BIR-printed form itself
 * (assumption: BIR accepts computer-generated 2307 that follow the official layout).
 */
@Component
public class Certificate2307Pdf {

  private static final float MARGIN = 30f;
  private static final float PADDING = 3f;
  private static final float SMALL = 7f;
  private static final float BODY_SIZE = 8f;
  private static final float TITLE_SIZE = 12f;
  private static final float SPACE = 6f;
  private static final float SIGNATURE_SPACE = 28f;
  private static final float[] PART_I_WIDTHS = {1.2f, 4f};
  private static final float[] PART_II_WIDTHS = {3.2f, 0.9f, 1.2f, 1.2f, 1.2f, 1.3f, 1.4f};
  private static final int PART_II_COLUMNS = 7;
  private static final int MONTHS = 3;
  private static final Color SHADE = new Color(0xE8, 0xEE, 0xF8);
  private static final Font TITLE = new Font(Font.HELVETICA, TITLE_SIZE, Font.BOLD);
  private static final Font BOLD = new Font(Font.HELVETICA, BODY_SIZE, Font.BOLD);
  private static final Font BODY = new Font(Font.HELVETICA, BODY_SIZE, Font.NORMAL);
  private static final Font NOTE = new Font(Font.HELVETICA, SMALL, Font.NORMAL);
  private static final Font STAMP = new Font(Font.HELVETICA, TITLE_SIZE, Font.BOLD, Color.RED);
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  private static final String DECLARATION =
      "We declare under the penalties of perjury that this certificate has been made in good"
          + " faith, verified by us, and to the best of our knowledge and belief, is true and"
          + " correct, pursuant to the provisions of the National Internal Revenue Code, as"
          + " amended, and the regulations issued under authority thereof. Further, we give our"
          + " consent to the processing of our information as contemplated under the Data Privacy"
          + " Act of 2012 (R.A. No. 10173) for legitimate and lawful purposes.";

  /**
   * Renders certificates, one per page.
   *
   * @param certificates certificates with lines loaded
   * @return PDF bytes
   */
  public byte[] render(List<Certificate2307> certificates) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (Document doc = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN)) {
      PdfWriter.getInstance(doc, out);
      doc.open();
      boolean first = true;
      for (Certificate2307 c : certificates) {
        if (!first) {
          doc.newPage();
        }
        first = false;
        page(doc, c);
      }
    }
    return out.toByteArray();
  }

  private static void page(Document doc, Certificate2307 c) {
    doc.add(
        block(
            "Republic of the Philippines  |  Department of Finance  |  Bureau of Internal"
                + " Revenue",
            NOTE,
            Element.ALIGN_CENTER));
    doc.add(block("BIR Form No. 2307", BOLD, Element.ALIGN_RIGHT));
    doc.add(block("Certificate of Creditable Tax Withheld at Source", TITLE, Element.ALIGN_CENTER));
    doc.add(
        block(
            "1  For the Period   From  "
                + DATE.format(c.getPeriodStart())
                + "   To  "
                + DATE.format(c.getPeriodEnd())
                + "        Certificate No. "
                + c.getCertificateNo(),
            BOLD,
            Element.ALIGN_LEFT));
    if (c.getStatus() == CertificateStatus.CANCELLED) {
      doc.add(block("CANCELLED - " + c.getStatusReason(), STAMP, Element.ALIGN_CENTER));
    }
    doc.add(section("Part I - Payee Information"));
    doc.add(
        party(
            c.payee(),
            "2",
            "Payee's Name (Last Name, First Name, Middle Name for"
                + " Individual OR Registered Name for Non-Individual)"));
    doc.add(section("Payor Information"));
    doc.add(party(c.payor(), "6", "Payor's Name (Registered Name for Non-Individual)"));
    doc.add(section("Part II - Details of Monthly Income Payments and Taxes Withheld"));
    doc.add(incomeTable(c));
    doc.add(block(DECLARATION, NOTE, Element.ALIGN_JUSTIFIED));
    doc.add(signatures());
  }

  private static PdfPTable party(Taxpayer t, String nameItem, String nameLabel) {
    PdfPTable table = new PdfPTable(PART_I_WIDTHS);
    table.setWidthPercentage(100);
    boolean payee = "2".equals(nameItem);
    row(table, (payee ? "1" : "5") + "  Taxpayer Identification Number (TIN)", t.formattedTin());
    row(table, nameItem + "  " + nameLabel, t.name());
    row(table, (payee ? "3" : "7") + "  Registered Address", nz(t.address()));
    row(table, (payee ? "3A" : "7A") + "  ZIP Code", nz(t.zipCode()));
    if (payee) {
      row(table, "4  Foreign Address, if applicable", "");
    }
    return table;
  }

  private static PdfPTable incomeTable(Certificate2307 c) {
    PdfPTable table = new PdfPTable(PART_II_WIDTHS);
    table.setWidthPercentage(100);
    table.setHeaderRows(1);
    LocalDate start = c.getPeriodStart();
    head(table, "Income Payments Subject to Expanded Withholding Tax");
    head(table, "ATC");
    for (int m = 0; m < MONTHS; m++) {
      head(table, ordinal(m) + " Month of the Quarter (" + monthName(start.plusMonths(m)) + ")");
    }
    head(table, "Total");
    head(table, "Tax Withheld for the Quarter");
    for (Certificate2307Line l : c.getLines()) {
      text(table, l.getIncomeNature(), BODY);
      text(table, l.getAtc(), BODY);
      amount(table, l.getMonth1Amount(), BODY);
      amount(table, l.getMonth2Amount(), BODY);
      amount(table, l.getMonth3Amount(), BODY);
      amount(table, l.getTotalAmount(), BODY);
      amount(table, l.getTaxWithheld(), BODY);
    }
    text(table, "Total", BOLD);
    text(table, "", BOLD);
    amount(table, sum(c, Certificate2307Line::getMonth1Amount), BOLD);
    amount(table, sum(c, Certificate2307Line::getMonth2Amount), BOLD);
    amount(table, sum(c, Certificate2307Line::getMonth3Amount), BOLD);
    amount(table, c.getTotalIncome(), BOLD);
    amount(table, c.getTotalTax(), BOLD);
    PdfPCell business =
        cell(
            "Money Payments Subject to Withholding of Business Tax (Government & Private): none",
            NOTE,
            Element.ALIGN_LEFT);
    business.setColspan(PART_II_COLUMNS);
    table.addCell(business);
    return table;
  }

  private static PdfPTable signatures() {
    PdfPTable table = new PdfPTable(1);
    table.setWidthPercentage(100);
    table.setSpacingBefore(SPACE);
    signature(
        table,
        "Signature over Printed Name of Payor / Payor's Authorized Representative / Tax Agent"
            + "   |   TIN of Signatory   |   Title / Designation");
    signature(
        table,
        "CONFORME: Signature over Printed Name of Payee / Payee's Authorized Representative /"
            + " Tax Agent   |   TIN of Signatory   |   Title / Designation");
    return table;
  }

  private static void signature(PdfPTable table, String caption) {
    PdfPCell line = cell("", BODY, Element.ALIGN_LEFT);
    line.setBorder(Rectangle.BOTTOM);
    line.setFixedHeight(SIGNATURE_SPACE);
    table.addCell(line);
    PdfPCell text = cell(caption, NOTE, Element.ALIGN_CENTER);
    text.setBorder(Rectangle.NO_BORDER);
    table.addCell(text);
  }

  private static PdfPTable block(String text, Font font, int align) {
    PdfPTable table = new PdfPTable(1);
    table.setWidthPercentage(100);
    table.setSpacingBefore(2f);
    PdfPCell cell = cell(text, font, align);
    cell.setBorder(Rectangle.NO_BORDER);
    table.addCell(cell);
    return table;
  }

  private static PdfPTable section(String title) {
    PdfPTable table = block(title, BOLD, Element.ALIGN_LEFT);
    table.setSpacingBefore(SPACE);
    table.getRow(0).getCells()[0].setBackgroundColor(SHADE);
    return table;
  }

  private static void row(PdfPTable table, String label, String value) {
    table.addCell(cell(label, NOTE, Element.ALIGN_LEFT));
    table.addCell(cell(value, BODY, Element.ALIGN_LEFT));
  }

  private static void head(PdfPTable table, String text) {
    PdfPCell cell = cell(text, BOLD, Element.ALIGN_CENTER);
    cell.setBackgroundColor(SHADE);
    table.addCell(cell);
  }

  private static void text(PdfPTable table, String text, Font font) {
    table.addCell(cell(text, font, Element.ALIGN_LEFT));
  }

  private static void amount(PdfPTable table, BigDecimal value, Font font) {
    table.addCell(cell(money(value), font, Element.ALIGN_RIGHT));
  }

  private static PdfPCell cell(String text, Font font, int align) {
    PdfPCell cell = new PdfPCell(new Phrase(text, font));
    cell.setHorizontalAlignment(align);
    cell.setPadding(PADDING);
    return cell;
  }

  private static BigDecimal sum(
      Certificate2307 c, Function<Certificate2307Line, BigDecimal> field) {
    return c.getLines().stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static String money(BigDecimal value) {
    DecimalFormat format =
        new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    return format.format(value);
  }

  private static String ordinal(int index) {
    return switch (index) {
      case 0 -> "1st";
      case 1 -> "2nd";
      default -> "3rd";
    };
  }

  private static String monthName(LocalDate date) {
    return date.getMonth().getDisplayName(TextStyle.SHORT, Locale.US);
  }

  private static String nz(String s) {
    return s == null ? "" : s;
  }
}
