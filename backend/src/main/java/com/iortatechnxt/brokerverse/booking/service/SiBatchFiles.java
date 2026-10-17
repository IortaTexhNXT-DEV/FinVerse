package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.SiBatch;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The files of a service-invoice batch (G09): one PDF with the service invoices of the batch one
 * after the other, and a manifest (CSV) that lists each service invoice with its invoice, issue
 * date, amounts and its pages in the PDF, with the batch totals.
 */
@Component
public class SiBatchFiles {

  /** Content type of the manifest. */
  public static final String CSV = "text/csv";

  private static final String SEPARATOR = ",";
  private static final int MANIFEST_CAPACITY = 1024;

  private final ServiceInvoiceDocument documents;

  /**
   * Creates the builder.
   *
   * @param documents service invoice PDFs
   */
  public SiBatchFiles(ServiceInvoiceDocument documents) {
    this.documents = documents;
  }

  /**
   * The files of a batch.
   *
   * @param pdf merged PDF
   * @param manifest manifest (CSV, UTF-8)
   * @param pdfName file name of the PDF
   * @param manifestName file name of the manifest
   */
  public record Files(byte[] pdf, byte[] manifest, String pdfName, String manifestName) {}

  /**
   * Builds the merged PDF and the manifest of a batch.
   *
   * @param batch batch
   * @param items its service invoices
   * @return files
   */
  @SuppressWarnings("PMD.CloseResource") // the copy writer is closed with its document
  public Files of(SiBatch batch, List<ServiceInvoice> items) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    List<PdfReader> readers = new ArrayList<>();
    List<int[]> pages = new ArrayList<>();
    try (Document doc = new Document()) {
      PdfCopy copy = new PdfCopy(doc, out);
      doc.open();
      int next = 1;
      for (ServiceInvoice si : items) {
        PdfReader reader = new PdfReader(documents.content(si));
        readers.add(reader);
        int count = reader.getNumberOfPages();
        for (int page = 1; page <= count; page++) {
          copy.addPage(copy.getImportedPage(reader, page));
        }
        pages.add(new int[] {next, next + count - 1});
        next += count;
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot merge the service invoices of the batch", ex);
    } finally {
      readers.forEach(PdfReader::close);
    }
    String base = name(batch);
    return new Files(
        out.toByteArray(), manifest(batch, items, pages), base + ".pdf", base + "_manifest.csv");
  }

  /**
   * The base file name of a batch: insurer, date and, for a supplementary batch, its number.
   *
   * @param batch batch
   * @return name without extension
   */
  static String name(SiBatch batch) {
    return "SI_BATCH_"
        + batch.getInsurerCode()
        + "_"
        + batch.getBatchDate()
        + (batch.getSequenceNo() > 1 ? "_" + batch.getSequenceNo() : "");
  }

  private static byte[] manifest(SiBatch batch, List<ServiceInvoice> items, List<int[]> pages) {
    StringBuilder csv =
        new StringBuilder(MANIFEST_CAPACITY)
            .append(
                "Batch,Insurer,Batch Date,Sequence,Service Invoice,Invoice,Issue Date,Currency,"
                    + "Commission,VAT,Withholding Tax,Net Amount,PDF Pages\n");
    for (int i = 0; i < items.size(); i++) {
      ServiceInvoice si = items.get(i);
      int[] range = pages.get(i);
      csv.append(
              String.join(
                  SEPARATOR,
                  name(batch),
                  quote(batch.getInsurerCode() + " " + batch.getInsurerName()),
                  String.valueOf(batch.getBatchDate()),
                  String.valueOf(batch.getSequenceNo()),
                  si.getSiNo(),
                  si.getInvoiceNo() == null ? "" : quote(si.getInvoiceNo()),
                  String.valueOf(si.getIssueDate()),
                  si.getCurrency(),
                  si.getCommission().toPlainString(),
                  si.getVatOnCommission().toPlainString(),
                  si.getWtaxAmount().toPlainString(),
                  si.getNetAmount().toPlainString(),
                  range[0] + "-" + range[1]))
          .append('\n');
    }
    csv.append(
            String.join(
                SEPARATOR,
                "Total",
                "",
                "",
                "",
                String.valueOf(batch.getItemCount()),
                "",
                "",
                batch.getCurrency() == null ? "" : batch.getCurrency(),
                batch.getCommissionTotal().toPlainString(),
                "",
                "",
                batch.getNetTotal().toPlainString(),
                ""))
        .append('\n');
    return csv.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static String quote(String text) {
    return "\"" + text.replace("\"", "\"\"") + "\"";
  }
}
