package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.report.domain.ReportBatch;
import com.iortatechnxt.brokerverse.report.domain.ReportBatch.BatchSpec;
import com.iortatechnxt.brokerverse.report.domain.ReportBatchRepository;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.report.render.PrintOptions;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Report batches (FRBS 2.4.5 / 2.4.7): several reports run with the same parameters and are
 * downloaded as one ZIP, or printed as one merged PDF. Every report is run with the user's own
 * permissions; a report that fails (not permitted, missing parameter) is recorded on its item and
 * the others go on. The batch file stays downloadable by its creator.
 *
 * <p>The batch file is kept in the file store (owner type {@value #OWNER_TYPE}, record class {@code
 * REPORT_OUTPUT}; build step ST1).
 *
 * <p>Not transactional itself: each report runs in its own transaction, so a refused report cannot
 * roll the batch back.
 */
@Service
public class ReportBatchService {

  /** Owner entity type of the stored files of batches. */
  public static final String OWNER_TYPE = "ReportBatch";

  private static final String ENTITY = OWNER_TYPE;
  private static final int MAX_PARAMETERS = 2000;
  private static final int MAX_REPORTS = 30;

  private final ReportService reports;
  private final ReportBatchRepository batches;
  private final StoredFileService storedFiles;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final TransactionTemplate tx;

  /**
   * Creates the service.
   *
   * @param reports report runs and rendering
   * @param batches batch repository
   * @param storedFiles file store
   * @param numbers batch numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   * @param transactions transaction manager (number and record of the batch)
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ReportBatchService(
      ReportService reports,
      ReportBatchRepository batches,
      StoredFileService storedFiles,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock,
      PlatformTransactionManager transactions) {
    this.reports = reports;
    this.batches = batches;
    this.storedFiles = storedFiles;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
    this.tx = new TransactionTemplate(transactions);
  }

  /**
   * Runs a batch.
   *
   * @param request reports, shared parameters, format and print options
   * @return the completed batch
   */
  public ReportBatch run(BatchRequest request) {
    if (request.codes().isEmpty() || request.codes().size() > MAX_REPORTS) {
      throw new BusinessRuleException(
          "REPORT_BATCH_SIZE", "Select between 1 and " + MAX_REPORTS + " reports");
    }
    boolean merged = request.mergedPdf();
    ExportFormat format = merged ? ExportFormat.PDF : request.format();
    Long companyId = companyOf(request.parameters());
    ReportBatch batch = newBatch(request, format, companyId);
    Map<String, byte[]> files = runReports(request, batch, format, companyId);
    byte[] content = complete(batch, files, merged);
    String described = merged ? "merged PDF" : format + " ZIP";
    return tx.execute(
        s -> {
          ReportBatch b = batches.save(batch);
          if (content != null) {
            b.storedIn(store(b, content));
          }
          audit.record(
              ENTITY,
              b.getBatchNo(),
              AuditAction.EXPORT,
              "Report batch " + request.codes() + " as " + described + ": " + b.getStatus());
          return b;
        });
  }

  private Long store(ReportBatch batch, byte[] content) {
    return storedFiles
        .storeChecked(
            new StoreRequest(
                new FileOwner(batch.getCompanyId(), OWNER_TYPE, String.valueOf(batch.getId())),
                null,
                ReportArchiveService.REPORT_OUTPUT,
                batch.getFileName(),
                content,
                null),
            batch.getContentType(),
            FileOrigin.GENERATED)
        .getId();
  }

  private byte[] complete(ReportBatch batch, Map<String, byte[]> files, boolean merged) {
    byte[] content = null;
    if (!files.isEmpty()) {
      content = merged ? merge(files) : zip(files);
    }
    String contentType = merged ? ExportFormat.PDF.contentType() : "application/zip";
    batch.complete(
        batch.getBatchNo() + (merged ? ".pdf" : ".zip"), contentType, content, clock.instant());
    return content;
  }

  private ReportBatch newBatch(BatchRequest request, ExportFormat format, Long companyId) {
    PrintOptions print = request.print();
    return new ReportBatch(
        tx.execute(s -> numbers.next("RB-" + BusinessClock.today(clock).getYear())),
        companyId,
        new BatchSpec(
            format.name(),
            request.mergedPdf(),
            print.paper().name(),
            print.orientation().name(),
            print.fitToWidth()),
        parametersText(request.parameters()));
  }

  private Map<String, byte[]> runReports(
      BatchRequest request, ReportBatch batch, ExportFormat format, Long companyId) {
    Map<String, byte[]> files = new TreeMap<>();
    for (String code : request.codes()) {
      try {
        ReportResult result = reports.runForExport(code, request.parameters());
        files.put(
            String.format("%02d_%s.%s", files.size() + 1, code, format.extension()),
            reports.render(result, format, request.print(), companyId));
        batch.addItem(code, true, result.rows().size(), null);
      } catch (BusinessRuleException | ResourceNotFoundException | AccessDeniedException ex) {
        batch.addItem(code, false, 0, ex.getMessage());
      }
    }
    return files;
  }

  /**
   * The batches of the current user.
   *
   * @param pageable page
   * @return batches, newest first
   */
  public Page<ReportBatch> mine(Pageable pageable) {
    return batches.findByCreatedByOrderByIdDesc(currentUser.username(), pageable);
  }

  /**
   * One batch of the current user.
   *
   * @param id batch
   * @return batch with items
   */
  public ReportBatch get(Long id) {
    ReportBatch batch =
        batches.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    if (!CurrentUser.sameUser(batch.getCreatedBy(), currentUser.username())) {
      throw new AccessDeniedException("Report batch " + batch.getBatchNo() + " is not yours");
    }
    return batch;
  }

  /**
   * The batch file for the download endpoint (creator only): a presigned link to the stored file,
   * or the bytes of a batch completed before ST1.
   *
   * @param id batch
   * @return download
   */
  @Transactional(readOnly = true)
  public FileDownload file(Long id) {
    ReportBatch batch = get(id);
    if (batch.getStoredFileId() != null) {
      return FileDownload.stored(batch.getStoredFileId());
    }
    byte[] content = batch.getContent();
    if (content == null) {
      throw new BusinessRuleException(
          "REPORT_BATCH_EMPTY", "Batch " + batch.getBatchNo() + " produced no file");
    }
    return FileDownload.inline(batch.getFileName(), batch.getContentType(), content);
  }

  private static Long companyOf(Map<String, String> parameters) {
    String value = parameters.get("companyId");
    try {
      return value == null || value.isBlank() ? null : Long.valueOf(value.trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static String parametersText(Map<String, String> parameters) {
    String text = new TreeMap<>(parameters).toString();
    return text.length() > MAX_PARAMETERS ? text.substring(0, MAX_PARAMETERS) : text;
  }

  private static byte[] zip(Map<String, byte[]> files) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      for (Map.Entry<String, byte[]> f : files.entrySet()) {
        zip.putNextEntry(new ZipEntry(f.getKey()));
        zip.write(f.getValue());
        zip.closeEntry();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return out.toByteArray();
  }

  @SuppressWarnings("PMD.CloseResource") // the copy writer is closed with its document
  private static byte[] merge(Map<String, byte[]> files) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    List<PdfReader> readers = new ArrayList<>();
    try (Document doc = new Document()) {
      PdfCopy copy = new PdfCopy(doc, out);
      doc.open();
      for (byte[] pdf : files.values()) {
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

  /**
   * A batch to run.
   *
   * @param codes report codes, in order
   * @param parameters shared parameters (each report takes those it declares)
   * @param format file format of the ZIP entries
   * @param mergedPdf one merged PDF instead of a ZIP
   * @param print PDF print options
   */
  public record BatchRequest(
      List<String> codes,
      Map<String, String> parameters,
      ExportFormat format,
      boolean mergedPdf,
      PrintOptions print) {

    /** Defensive copies and defaults. */
    public BatchRequest {
      codes = List.copyOf(codes);
      parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
      format = format == null ? ExportFormat.XLSX : format;
      print = print == null ? PrintOptions.DEFAULT : print;
    }
  }
}
