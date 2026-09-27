package com.iortatechnxt.brokerverse.submitted.service.adapter;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPrintBatch;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPrintBatchRepository;
import com.iortatechnxt.brokerverse.submitted.service.port.MailHouseGateway;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link MailHouseGateway} while the mail house (COG) has no interface (SP SQ09): the
 * letters of a batch are merged into one PDF and listed in a control list (CSV: reference,
 * addressee, address), both kept in the file store as a print batch for the mail house to collect.
 * A mail-house interface replaces it as the {@code @Primary} bean of the port.
 */
@Component
@Transactional(propagation = Propagation.REQUIRED)
public class PrintBatchMailHouse implements MailHouseGateway {

  private static final String RECORD_CLASS = "WORKING_FILE";

  private final SbmPrintBatchRepository batches;
  private final StoredFileService files;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;

  /**
   * Creates the adapter.
   *
   * @param batches print batches
   * @param files file store
   * @param numbers document numbers
   * @param audit audit trail
   */
  public PrintBatchMailHouse(
      SbmPrintBatchRepository batches,
      StoredFileService files,
      DocumentNumberService numbers,
      AuditTrailService audit) {
    this.batches = batches;
    this.files = files;
    this.numbers = numbers;
    this.audit = audit;
  }

  @Override
  public PrintHandOver handOver(PrintRequest request) {
    SbmPrintBatch batch =
        batches.save(
            new SbmPrintBatch(
                request.companyId(),
                numbers.next("SBP-" + request.batchDate().getYear()),
                new SbmPrintBatch.Kind(request.source(), request.letterType(), request.batchDate()),
                request.letters().size()));
    List<byte[]> pdfs = new ArrayList<>();
    StringBuilder control = new StringBuilder("Reference,Addressee,Address\n");
    for (PrintedLetter l : request.letters()) {
      pdfs.add(files.read(l.storedFileId()));
      control
          .append(csv(l.reference()))
          .append(',')
          .append(csv(l.recipientName()))
          .append(',')
          .append(csv(l.address()))
          .append('\n');
    }
    Long merged = store(request, batch, batch.getBatchNo() + ".pdf", merge(pdfs));
    Long list =
        store(
            request,
            batch,
            batch.getBatchNo() + "-control.csv",
            control.toString().getBytes(StandardCharsets.UTF_8));
    batch.files(merged, list);
    audit.record(
        "SubmittedPrintBatch",
        batch.getBatchNo(),
        AuditAction.CREATE,
        request.letters().size() + " " + request.letterType() + " letters for the mail house");
    return new PrintHandOver(batch.getBatchNo(), merged, list, request.letters().size());
  }

  private Long store(PrintRequest r, SbmPrintBatch batch, String name, byte[] content) {
    return files
        .storeChecked(
            new StoreRequest(
                new FileOwner(r.companyId(), SubmittedFileStorage.PRINT_BATCH, batch.getId().toString()),
                null,
                RECORD_CLASS,
                name,
                content,
                null),
            name.endsWith(".pdf") ? "application/pdf" : "text/csv",
            FileOrigin.GENERATED)
        .getId();
  }

  private static String csv(String value) {
    String v = value == null ? "" : value.replace("\"", "\"\"");
    return "\"" + v + "\"";
  }

  /**
   * Merges PDFs into one.
   *
   * @param pdfs PDFs in order
   * @return merged PDF
   */
  @SuppressWarnings("PMD.CloseResource") // the copy writer is closed with its document
  static byte[] merge(List<byte[]> pdfs) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    List<PdfReader> readers = new ArrayList<>();
    try (Document doc = new Document()) {
      PdfCopy copy = new PdfCopy(doc, out);
      doc.open();
      for (byte[] pdf : pdfs) {
        PdfReader reader = new PdfReader(pdf);
        readers.add(reader);
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
          copy.addPage(copy.getImportedPage(reader, page));
        }
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    } finally {
      readers.forEach(PdfReader::close);
    }
    return out.toByteArray();
  }
}
