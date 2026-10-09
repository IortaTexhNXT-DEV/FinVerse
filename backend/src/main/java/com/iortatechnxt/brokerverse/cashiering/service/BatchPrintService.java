package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.PrintBatch;
import com.iortatechnxt.brokerverse.cashiering.domain.PrintBatchRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptPrinter.Printed;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Batch printing of ARs and ORs (CSHID.019): the selected receipts (a selection or the result of a
 * filter by location, insurer or date) are printed into one merged PDF kept as the batch copy; each
 * receipt's print count is logged, cancelled receipts fail with a message, and the failures can be
 * retried in a new batch. Also issues Certificates of Payment (Annex II report 19). The merged PDF
 * is kept in the file store (owner type {@value #OWNER_TYPE}, record class {@code
 * OFFICIAL_RECEIPT}; build step ST1).
 */
@Service
@Transactional
public class BatchPrintService {

  /** Owner entity type of the stored merged PDFs. */
  public static final String OWNER_TYPE = "PrintBatch";

  /** Record class of receipt prints. */
  public static final String RECORD_CLASS = "OFFICIAL_RECEIPT";

  /** Owner entity type of the documents kept with a receipt. */
  public static final String RECEIPT_OWNER = "Receipt";

  private static final String PDF = "application/pdf";

  private static final String ENTITY = OWNER_TYPE;
  private static final String COP_LOG =
      "insert into csh_certificate_of_payment (company_id, receipt_id, receipt_no, policy_no,"
          + " requesting_unit, issued_at, issued_by, series_no) values (?, ?, ?, ?, ?, ?, ?, ?)";
  private static final int MAX_BATCH = 500;

  private final PrintBatchRepository batches;
  private final StoredFileService storedFiles;
  private final CashReceiptRepository receipts;
  private final ReceiptDocument documents;
  private final ReceiptPrinter printer;
  private final DocumentNumberService numbers;
  private final JdbcTemplate jdbc;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param batches print batches
   * @param storedFiles file store (merged PDFs)
   * @param receipts receipts
   * @param documents receipt documents
   * @param printer one receipt in its form
   * @param numbers document numbers
   * @param jdbc JDBC
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public BatchPrintService(
      PrintBatchRepository batches,
      StoredFileService storedFiles,
      CashReceiptRepository receipts,
      ReceiptDocument documents,
      ReceiptPrinter printer,
      DocumentNumberService numbers,
      JdbcTemplate jdbc,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.batches = batches;
    this.storedFiles = storedFiles;
    this.receipts = receipts;
    this.documents = documents;
    this.printer = printer;
    this.numbers = numbers;
    this.jdbc = jdbc;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Prints receipts into one batch, the Client's Copy.
   *
   * @param companyId company
   * @param receiptIds receipts
   * @param criteria how they were selected
   * @return the batch
   */
  public PrintBatch print(Long companyId, Collection<Long> receiptIds, String criteria) {
    return print(companyId, receiptIds, criteria, ReceiptForms.CLIENT_COPY);
  }

  /**
   * Prints receipts into one batch (FRS.CSH.02.04.05 to 02.04.17): at most 500, each in the
   * approved form with its certificate number, the REPRINT mark when printed before and the copy
   * asked; one merged PDF to print and a ZIP of one PDF per receipt named {@code <Receipt
   * Type>_<Receipt Number>.pdf}; a receipt that cannot be printed is listed with the reason.
   *
   * @param companyId company
   * @param receiptIds receipts
   * @param criteria how they were selected
   * @param copy CLIENT, COMPANY or BOTH
   * @return the batch
   */
  public PrintBatch print(
      Long companyId, Collection<Long> receiptIds, String criteria, String copy) {
    if (receiptIds.isEmpty() || receiptIds.size() > MAX_BATCH) {
      throw new BusinessRuleException(
          "PRINT_SELECTION", "Select between 1 and " + MAX_BATCH + " receipts to print");
    }
    LocalDate today = BusinessClock.today(clock);
    PrintBatch batch = new PrintBatch(companyId, numbers.next("PRB-" + today.getYear()), criteria);
    batch.copy(copy);
    List<byte[]> pdfs = new ArrayList<>();
    Map<String, byte[]> zip = new LinkedHashMap<>();
    for (Receipt r : receipts.findByIdInOrderByIdAsc(receiptIds)) {
      if (r.getCompanyId().equals(companyId)) {
        batch.add(printOne(r, copy, today, pdfs, zip));
      }
    }
    batch.finish(batch.getBatchNo() + ".pdf");
    PrintBatch saved = batches.save(batch);
    if (!pdfs.isEmpty()) {
      saved.storedIn(store(saved, saved.getFileName(), ReceiptDocument.merge(pdfs)));
      saved.zippedIn(store(saved, saved.getBatchNo() + ".zip", zip(zip)));
    }
    audit.record(
        ENTITY,
        saved.getBatchNo(),
        AuditAction.EXPORT,
        saved.getPrintedCount()
            + " printed, "
            + saved.getFailedCount()
            + " failed, copy "
            + copy
            + ": "
            + criteria
            + reprints(saved));
    return saved;
  }

  private PrintBatch.Line printOne(
      Receipt r, String copy, LocalDate today, List<byte[]> pdfs, Map<String, byte[]> zip) {
    if (r.getStatus() == ReceiptStatus.CANCELLED) {
      return new PrintBatch.Line(
          r.getId(), r.getReceiptNo(), PrintBatch.FAILED, "Receipt is cancelled");
    }
    try {
      Printed printed = printer.print(r, copy, today);
      pdfs.addAll(printed.pdfs());
      zip.put(printed.fileName(), ReceiptDocument.merge(printed.pdfs()));
      return new PrintBatch.Line(r.getId(), r.getReceiptNo(), PrintBatch.PRINTED, null)
          .printedAs(printed.reprint(), printed.certificateNo());
    } catch (BusinessRuleException | IllegalStateException | UncheckedIOException ex) {
      return new PrintBatch.Line(
          r.getId(), r.getReceiptNo(), PrintBatch.FAILED, "Print error: " + ex.getMessage());
    }
  }

  private static String reprints(PrintBatch batch) {
    List<String> again =
        batch.getLines().stream()
            .filter(PrintBatch.Line::isReprint)
            .map(PrintBatch.Line::getReceiptNo)
            .toList();
    return again.isEmpty() ? "" : "; reprinted " + String.join(", ", again);
  }

  private static byte[] zip(Map<String, byte[]> files) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      for (Map.Entry<String, byte[]> file : files.entrySet()) {
        zip.putNextEntry(new ZipEntry(file.getKey()));
        zip.write(file.getValue());
        zip.closeEntry();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("The ZIP of the receipts could not be written", ex);
    }
    return out.toByteArray();
  }

  /**
   * Skips failed receipts of a batch (FRS.CSH.02.04.15): they stay unprinted and the print log
   * shows them as skipped.
   *
   * @param batchId batch
   * @param receiptIds receipts to skip, all failed ones when empty
   * @return the batch
   */
  public PrintBatch skip(Long batchId, Collection<Long> receiptIds) {
    PrintBatch batch = get(batchId);
    int skipped = batch.skip(receiptIds);
    if (skipped == 0) {
      throw new BusinessRuleException(
          "PRINT_NOTHING_FAILED", batch.getBatchNo() + " has no failure to skip");
    }
    audit.record(
        ENTITY,
        batch.getBatchNo(),
        AuditAction.UPDATE,
        skipped + " failed receipt(s) skipped: " + String.join(", ", skippedNumbers(batch)));
    return batch;
  }

  private static List<String> skippedNumbers(PrintBatch batch) {
    return batch.getLines().stream()
        .filter(l -> PrintBatch.SKIPPED.equals(l.getStatus()))
        .map(PrintBatch.Line::getReceiptNo)
        .toList();
  }

  /**
   * The ZIP of the receipts of a batch, one PDF per receipt (FRS.CSH.02.04.09.02).
   *
   * @param id batch
   * @return download
   */
  @Transactional(readOnly = true)
  public FileDownload zipFile(Long id) {
    PrintBatch batch = get(id);
    if (batch.getZipFileId() == null) {
      throw new BusinessRuleException(
          "PRINT_NO_ZIP", batch.getBatchNo() + " has no ZIP of its receipts");
    }
    return FileDownload.stored(batch.getZipFileId());
  }

  /**
   * The documents kept with a receipt (the company's copies printed).
   *
   * @param receipt receipt
   * @return files
   */
  @Transactional(readOnly = true)
  public List<StoredFile> documents(Receipt receipt) {
    return storedFiles.filesOf(
        new FileOwner(receipt.getCompanyId(), RECEIPT_OWNER, String.valueOf(receipt.getId())));
  }

  private Long store(PrintBatch batch, String name, byte[] content) {
    return storedFiles
        .storeChecked(
            new StoreRequest(
                new FileOwner(batch.getCompanyId(), OWNER_TYPE, String.valueOf(batch.getId())),
                null,
                RECORD_CLASS,
                name,
                content,
                null),
            name.endsWith(".zip") ? "application/zip" : PDF,
            FileOrigin.GENERATED)
        .getId();
  }

  /**
   * The merged PDF of a batch for the download endpoint: a presigned link to the stored file, or
   * the bytes of a batch printed before ST1 (empty when nothing was printed).
   *
   * @param id batch
   * @return download
   */
  @Transactional(readOnly = true)
  public FileDownload file(Long id) {
    PrintBatch batch = get(id);
    return batch.getStoredFileId() == null
        ? FileDownload.inline(batch.getBatchNo() + ".pdf", PDF, batch.document())
        : FileDownload.stored(batch.getStoredFileId());
  }

  /**
   * Prints the failed receipts of a batch again.
   *
   * @param batchId batch
   * @return the new batch
   */
  public PrintBatch retry(Long batchId) {
    PrintBatch failed = get(batchId);
    List<Long> ids =
        failed.getLines().stream()
            .filter(l -> PrintBatch.FAILED.equals(l.getStatus()))
            .map(PrintBatch.Line::getReceiptId)
            .toList();
    if (ids.isEmpty()) {
      throw new BusinessRuleException(
          "PRINT_NOTHING_FAILED", failed.getBatchNo() + " has no failures");
    }
    return print(
        failed.getCompanyId(), ids, "Retry of " + failed.getBatchNo(), failed.getCopyLabel());
  }

  /**
   * Issues a Certificate of Payment and logs it.
   *
   * @param receiptId receipt
   * @param policyNo policy number
   * @param requestingUnit requesting marketing unit
   * @return PDF
   */
  public byte[] certificateOfPayment(Long receiptId, String policyNo, String requestingUnit) {
    Receipt r =
        receipts
            .findById(receiptId)
            .orElseThrow(() -> new ResourceNotFoundException(CashReceiptService.ENTITY, receiptId));
    if (r.getStatus() == ReceiptStatus.CANCELLED) {
      throw new BusinessRuleException(
          "COP_CANCELLED_RECEIPT", "Receipt " + r.getReceiptNo() + " is cancelled");
    }
    String seriesNo = numbers.next("COP-" + BusinessClock.today(clock).getYear());
    byte[] pdf = documents.certificateOfPayment(r, policyNo, requestingUnit, seriesNo);
    jdbc.update(
        COP_LOG,
        r.getCompanyId(),
        r.getId(),
        r.getReceiptNo(),
        policyNo,
        requestingUnit,
        Timestamp.from(clock.instant()),
        currentUser.username(),
        seriesNo);
    audit.record(
        CashReceiptService.ENTITY,
        r.getReceiptNo(),
        AuditAction.EXPORT,
        "Certificate of Payment " + seriesNo + " for " + requestingUnit);
    return pdf;
  }

  /**
   * Batches of a company.
   *
   * @param companyId company
   * @param pageable page
   * @return batches, newest first
   */
  @Transactional(readOnly = true)
  public Page<PrintBatch> list(Long companyId, Pageable pageable) {
    return batches.findByCompanyIdOrderByIdDesc(companyId, pageable);
  }

  /**
   * One batch.
   *
   * @param id id
   * @return batch
   */
  @Transactional(readOnly = true)
  public PrintBatch get(Long id) {
    PrintBatch b =
        batches.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    Hibernate.initialize(b.getLines());
    return b;
  }
}
