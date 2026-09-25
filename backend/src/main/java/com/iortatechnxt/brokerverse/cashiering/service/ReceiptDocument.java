package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptLine;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Section;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The printed AR and OR (CSHID.019) and the Certificate of Payment (Annex II report 19): PDF from
 * the platform document composer, with the payor, amounts, tender and, for an AR, the invoices the
 * payment was applied to, or for an OR, its lines. The BIR-registered receipt format and ATP
 * details are parked (OQ05). Also merges several PDFs into one batch file.
 */
@Component
@Transactional(readOnly = true)
public class ReceiptDocument {

  private static final List<Integer> OR_AMOUNT_COLUMNS = List.of(1, 2, 3);

  private static final List<Integer> AMOUNT_COLUMN = List.of(1);
  private static final String DASH = "-";

  private final DocumentComposer composer;
  private final CompanyRepository companies;
  private final ApplicationRepository applications;

  /**
   * Creates the document builder.
   *
   * @param composer PDF composer
   * @param companies companies (letterhead)
   * @param applications applications
   */
  public ReceiptDocument(
      DocumentComposer composer, CompanyRepository companies, ApplicationRepository applications) {
    this.composer = composer;
    this.companies = companies;
    this.applications = applications;
  }

  /**
   * The receipt as a PDF.
   *
   * @param r receipt
   * @return PDF
   */
  public byte[] pdf(Receipt r) {
    List<Section> sections = new ArrayList<>();
    sections.add(
        new Fields(
            "Received from",
            List.of(
                new Field("Payor", r.getPayorName()),
                new Field("Payor code", nz(r.getPayorCode())),
                new Field("Assured", nz(r.getAssuredName())),
                new Field("Date", r.getReceiptDate().toString()),
                new Field("Class", r.getReceiptClass()),
                new Field("Amount", r.getCurrency() + " " + amount(r.getAmount())),
                new Field("Mode of payment", r.getMode().name()),
                new Field("Check", nz(r.getCheckNo()) + " " + nz(r.getCheckBank())),
                new Field("Status", r.getStatus().name()))));
    sections.add(details(r));
    return composer.pdf(
        new DocumentSpec(
            companyName(r.getCompanyId()),
            r.getKind() == ReceiptKind.OR ? "OFFICIAL RECEIPT" : "ACKNOWLEDGEMENT RECEIPT",
            r.getReceiptNo(),
            sections,
            List.of("Cashier"),
            "Receipt format and BIR ATP details to be confirmed (OQ05)"));
  }

  /**
   * A Certificate of Payment of a receipt (Annex II report 19).
   *
   * @param r receipt
   * @param policyNo policy number
   * @param requestingUnit requesting marketing unit
   * @return PDF
   */
  public byte[] certificateOfPayment(Receipt r, String policyNo, String requestingUnit) {
    return composer.pdf(
        new DocumentSpec(
            companyName(r.getCompanyId()),
            "CERTIFICATION OF PAYMENT",
            r.getReceiptNo(),
            List.of(
                new Fields(
                    "This certifies that payment was received",
                    List.of(
                        new Field("From", r.getPayorName()),
                        new Field("AR number", r.getReceiptNo()),
                        new Field("Date paid", r.getReceiptDate().toString()),
                        new Field("Amount", r.getCurrency() + " " + amount(r.getAmount())),
                        new Field("Policy number", nz(policyNo)),
                        new Field("Requested by", requestingUnit))),
                details(r)),
            List.of("Cashiering"),
            "Certificate of Payment layout to be confirmed (OQ42)"));
  }

  /**
   * Merges PDFs into one file (batch printing).
   *
   * @param pdfs documents
   * @return merged PDF
   */
  public static byte[] merge(List<byte[]> pdfs) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (Document document = new Document();
        PdfCopy copy = new PdfCopy(document, out)) {
      document.open();
      for (byte[] pdf : pdfs) {
        try (PdfReader reader = new PdfReader(pdf)) {
          for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            copy.addPage(copy.getImportedPage(reader, page));
          }
        }
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("Batch print merge failed", ex);
    }
    return out.toByteArray();
  }

  private Table details(Receipt r) {
    List<List<String>> rows = new ArrayList<>();
    if (r.getKind() == ReceiptKind.OR) {
      for (ReceiptLine l : r.getLines()) {
        rows.add(
            List.of(
                nz(l.getInvoiceNo()) + " " + nz(l.getDescription()),
                amount(l.getNet()),
                amount(l.getVat()),
                amount(l.getWtax())));
      }
      return new Table("Lines", List.of("Item", "Net", "VAT", "WTAX"), rows, OR_AMOUNT_COLUMNS);
    }
    for (Application a : applications.findByReceiptIdOrderByIdAsc(r.getId())) {
      if (a.isActive()) {
        rows.add(List.of(a.getInvoiceNo(), amount(a.getAmount())));
      }
    }
    rows.add(List.of("Unapplied", amount(r.unappliedAmount())));
    return new Table("Application", List.of("Invoice", "Amount"), rows, AMOUNT_COLUMN);
  }

  private String companyName(Long companyId) {
    return companies.findById(companyId).map(c -> c.getName()).orElse(DASH);
  }

  private static String nz(String value) {
    return value == null ? DASH : value;
  }

  private static String amount(BigDecimal value) {
    return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
  }
}
