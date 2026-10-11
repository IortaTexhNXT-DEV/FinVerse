package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkHandlerRegistry;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelFile;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelFile.Identity;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelFileRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelProfile;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelProfileRepository;
import com.iortatechnxt.brokerverse.cashiering.service.ChannelFileReader.Content;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Intake of the payment files in BDOI's layouts, by upload or via MFT (FRS.CSH.05.01.02 to
 * 05.01.06, 02.02.03 to 02.02.06): the checks of the file name convention, the size limit, the
 * duplicate file and the header or TOTAL controls; the raw file kept read-only with its upload
 * reference, source file, uploader and processing time; the rows mapped to the fields of the system
 * and processed one by one by the payment file run (matching, AR and application), so that a failed
 * row never stops the others and is corrected and processed again. A file received via MFT and
 * refused is alerted to the Cashiering Team Leader.
 */
@Service
public class ChannelFileService {

  /** Owner type of the raw copies. */
  public static final String OWNER = "PaymentFile";

  /** The Direct Credit run of BDOI's reading (identification of payments received). */
  public static final String DC_IDENTIFY = "PAY_DC_IDENTIFY";

  private static final String REFUSED_ALERT = "CASH_MFT_FILE_REFUSED";
  private static final long MB = 1024L * 1024L;

  private final ChannelProfileRepository profiles;
  private final ChannelFileRepository files;
  private final BulkService bulk;
  private final BulkHandlerRegistry handlers;
  private final StoredFileService storage;
  private final DocumentNumberService numbers;
  private final CommissionScheduleCheck commissionCheck;
  private final SystemParameterService parameters;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final TransactionTemplate tx;

  /**
   * Creates the service.
   *
   * @param profiles profiles of the file types
   * @param files files received
   * @param bulk payment file runs
   * @param handlers columns of the runs
   * @param storage raw copies
   * @param numbers upload references
   * @param commissionCheck whole-file check of a Commission Schedule
   * @param parameters settings (Direct Credit, Commission Schedule)
   * @param alerts alert of a refused MFT file
   * @param audit audit trail
   * @param currentUser uploader
   * @param clock clock
   * @param txManager transactions
   */
  public ChannelFileService(
      ChannelProfileRepository profiles,
      ChannelFileRepository files,
      BulkService bulk,
      BulkHandlerRegistry handlers,
      StoredFileService storage,
      DocumentNumberService numbers,
      CommissionScheduleCheck commissionCheck,
      SystemParameterService parameters,
      AlertService alerts,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock,
      PlatformTransactionManager txManager) {
    this.profiles = profiles;
    this.files = files;
    this.bulk = bulk;
    this.handlers = handlers;
    this.storage = storage;
    this.numbers = numbers;
    this.commissionCheck = commissionCheck;
    this.parameters = parameters;
    this.alerts = alerts;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
    this.tx = new TransactionTemplate(txManager);
  }

  /**
   * Receives one file, checks it and processes its rows.
   *
   * @param companyId company
   * @param fileType payment file type
   * @param fileName file name
   * @param content bytes
   * @param source UPLOAD or MFT
   * @return the file with its status
   */
  public ChannelFile receive(
      Long companyId, String fileType, String fileName, byte[] content, String source) {
    ChannelProfile profile =
        profiles
            .findById(fileType)
            .orElseThrow(() -> new ResourceNotFoundException("Payment file type", fileType));
    String sha = Sha256.hex(content);
    ChannelFile file =
        required(
            tx.execute(
                s ->
                    files.save(
                        new ChannelFile(
                            companyId,
                            numbers.next("UPL-" + BusinessClock.today(clock).getYear()),
                            new Identity(fileType, fileName, content.length, sha, source),
                            currentUser.username(),
                            clock.instant()))));
    String refusal = fileCheck(profile, file, content.length);
    Content read = null;
    if (refusal == null) {
      try {
        read = ChannelFileReader.read(profile, fileName, content, dateFields(handlerOf(profile)));
        refusal = read.controlFailure();
      } catch (BusinessRuleException ex) {
        refusal = ex.getMessage();
      }
    }
    Content parsed = read;
    store(file, content);
    if (refusal != null) {
      return refuse(file, refusal);
    }
    tx.executeWithoutResult(
        s ->
            files
                .findById(file.getId())
                .orElseThrow()
                .controls(
                    parsed.headerCount(),
                    parsed.controlTotal(),
                    parsed.detailTotal(),
                    parsed.rows().size()));
    return process(file, profile, parsed);
  }

  private String fileCheck(ChannelProfile profile, ChannelFile file, long size) {
    if (!profile.accepts(file.getFileName())) {
      return "The file name "
          + file.getFileName()
          + " does not follow the convention of "
          + profile.getName()
          + " ("
          + profile.getNameExample()
          + ")";
    }
    if (size > profile.getMaxMb() * MB) {
      return "The file is larger than " + profile.getMaxMb() + " MB";
    }
    return files
        .findFirstByCompanyIdAndFileTypeAndSha256AndStatusNot(
            file.getCompanyId(), file.getFileType(), file.getSha256(), ChannelFile.REFUSED)
        .filter(earlier -> !earlier.getId().equals(file.getId()))
        .map(earlier -> "This file was already uploaded as " + earlier.getUploadRef())
        .orElse(null);
  }

  private ChannelFile process(ChannelFile file, ChannelProfile profile, Content content) {
    String handler = handlerOf(profile);
    BulkJob job;
    try {
      job =
          bulk.upload(
              new BulkUpload(
                  file.getCompanyId(),
                  handler,
                  file.getFileName() + ".csv",
                  csv(handlers.require(handler).columns(), content),
                  Map.of("channelFile", file.getUploadRef(), "fileName", file.getFileName())));
    } catch (BusinessRuleException ex) {
      return refuse(file, ex.getMessage());
    }
    String wholeFile = wholeFileRefusal(profile, job);
    if (wholeFile != null) {
      bulk.cancel(job.getId());
      return refuse(file, wholeFile);
    }
    BulkJob done = bulk.commit(job.getId());
    int failed = done.getInvalidRows() + done.getFailedRows();
    return required(
        tx.execute(
            s -> {
              ChannelFile f = files.findById(file.getId()).orElseThrow();
              f.processed(done.getId(), done.getJobNo(), failed, clock.instant());
              audit.record(
                  OWNER,
                  f.getUploadRef(),
                  AuditAction.RUN,
                  f.getFileName() + ": " + content.rows().size() + " rows, " + failed + " failed");
              return f;
            }));
  }

  /**
   * C10: with WHOLE_FILE a Commission Schedule with a failed or unmatched line is not processed.
   */
  private String wholeFileRefusal(ChannelProfile profile, BulkJob job) {
    boolean wholeFile =
        "COMMISSION_SCHEDULE".equals(profile.getFileType())
            && "WHOLE_FILE"
                .equals(parameters.text("CASH_COMMISSION_SCHEDULE_MODE", "LINE_LEVEL").strip());
    if (!wholeFile) {
      return null;
    }
    if (job.getInvalidRows() > 0) {
      return "The file has " + job.getInvalidRows() + " failed row(s) and is not processed";
    }
    long unmatched = commissionCheck.unmatched(job.getCompanyId(), job.getId());
    return unmatched > 0
        ? "The file has " + unmatched + " unmatched line(s) and is not processed"
        : null;
  }

  private String handlerOf(ChannelProfile profile) {
    boolean identify =
        "DIRECT_CREDIT".equals(profile.getFileType())
            && !"NEW_PAYMENT"
                .equals(parameters.text("CASH_DIRECT_CREDIT_MODE", "IDENTIFY").strip());
    return identify ? DC_IDENTIFY : profile.getHandlerCode();
  }

  private Set<String> dateFields(String handler) {
    return handlers
        .find(handler)
        .map(
            h ->
                h.columns().stream()
                    .filter(c -> c.type() == BulkColumn.Type.DATE)
                    .map(BulkColumn::header)
                    .collect(Collectors.toSet()))
        .orElse(Set.of());
  }

  private ChannelFile refuse(ChannelFile file, String reason) {
    ChannelFile refused =
        required(
            tx.execute(
                s -> {
                  ChannelFile f = files.findById(file.getId()).orElseThrow();
                  f.refuse(reason);
                  audit.record(
                      OWNER, f.getUploadRef(), AuditAction.REJECT, f.getFileName() + ": " + reason);
                  return f;
                }));
    if ("MFT".equals(refused.getSource())) {
      tx.executeWithoutResult(
          s ->
              alerts.raise(
                  REFUSED_ALERT,
                  new AlertFacts(
                      refused.getCompanyId(),
                      null,
                      OWNER,
                      refused.getUploadRef(),
                      "Payment file "
                          + refused.getFileName()
                          + " received via MFT was refused: "
                          + reason,
                      null,
                      REFUSED_ALERT + ":" + refused.getUploadRef())));
    }
    return refused;
  }

  private void store(ChannelFile file, byte[] content) {
    Long stored =
        storage
            .storeChecked(
                new StoreRequest(
                    new FileOwner(file.getCompanyId(), OWNER, String.valueOf(file.getId())),
                    null,
                    "INBOUND_FILE",
                    file.getFileName(),
                    content,
                    null),
                "application/octet-stream",
                FileOrigin.UPLOADED)
            .getId();
    tx.executeWithoutResult(s -> files.findById(file.getId()).orElseThrow().storedIn(stored));
  }

  /**
   * Processes again the failed rows of a file once corrected (FRS.CSH.05.01.12, BRQID.006); the
   * rows already accepted are not processed again.
   *
   * @param fileId file
   * @return the file with its new counts
   */
  public ChannelFile reprocess(Long fileId) {
    ChannelFile file = get(fileId);
    if (file.getBulkJobId() == null) {
      throw new BusinessRuleException(
          "CHANNEL_FILE_NOT_PROCESSED", file.getFileName() + " has no run to process again");
    }
    BulkJob job = bulk.reprocess(file.getBulkJobId());
    int failed = job.getFailedRows();
    return required(
        tx.execute(
            s -> {
              ChannelFile f = files.findById(fileId).orElseThrow();
              f.processed(job.getId(), job.getJobNo(), failed, clock.instant());
              audit.record(
                  OWNER,
                  f.getUploadRef(),
                  AuditAction.RUN,
                  "Processed again: " + failed + " failed");
              return f;
            }));
  }

  /**
   * One file.
   *
   * @param fileId id
   * @return file
   */
  public ChannelFile get(Long fileId) {
    return files
        .findById(fileId)
        .orElseThrow(() -> new ResourceNotFoundException("Payment file", fileId));
  }

  /**
   * Files received, newest first.
   *
   * @param companyId company
   * @param types file types, all when empty
   * @param pageable page
   * @return files
   */
  public Page<ChannelFile> list(Long companyId, Collection<String> types, Pageable pageable) {
    Collection<String> wanted =
        types == null || types.isEmpty()
            ? profiles.findAll().stream().map(ChannelProfile::getFileType).toList()
            : types;
    return files.findByCompanyIdAndFileTypeInOrderByIdDesc(companyId, wanted, pageable);
  }

  /**
   * The raw copy of a file, as received.
   *
   * @param fileId file
   * @return download
   */
  public FileDownload raw(Long fileId) {
    ChannelFile file = get(fileId);
    if (file.getStoredFileId() == null) {
      throw new ResourceNotFoundException("Raw payment file", fileId);
    }
    return FileDownload.stored(file.getStoredFileId());
  }

  /**
   * The profiles of the payment file types.
   *
   * @return profiles
   */
  public List<ChannelProfile> profiles() {
    return profiles.findAll();
  }

  /**
   * Changes the profile of a file type (Cashiering Setup).
   *
   * @param fileType type
   * @param change new convention, field map, size limit and MFT folder
   * @return the profile
   */
  public ChannelProfile changeProfile(String fileType, ChannelProfile.Change change) {
    return required(
        tx.execute(
            s -> {
              ChannelProfile p =
                  profiles
                      .findById(fileType)
                      .orElseThrow(
                          () -> new ResourceNotFoundException("Payment file type", fileType));
              p.change(change, currentUser.username(), clock.instant());
              audit.record("ChannelProfile", fileType, AuditAction.UPDATE, change.toString());
              return p;
            }));
  }

  /**
   * The rows as a CSV file with the columns of the run (columns BDOI's file does not have are
   * empty).
   *
   * @param columns columns of the run
   * @param content rows mapped to the fields of the system
   * @return CSV bytes
   */
  static byte[] csv(List<BulkColumn> columns, Content content) {
    StringBuilder out = new StringBuilder();
    out.append(columns.stream().map(c -> quote(c.header())).collect(Collectors.joining(",")))
        .append('\n');
    for (Map<String, String> row : content.rows()) {
      out.append(
              columns.stream()
                  .map(c -> quote(clean(c, row.getOrDefault(c.header(), ""))))
                  .collect(Collectors.joining(",")))
          .append('\n');
    }
    return out.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static String clean(BulkColumn column, String value) {
    if (column.type() != BulkColumn.Type.NUMBER || value.isBlank()) {
      return value;
    }
    BigDecimal number = ChannelFileReader.number(value);
    return number.signum() == 0 && !value.matches("[0.,\\s]+") ? value : number.toPlainString();
  }

  private static String quote(String value) {
    return '"' + value.replace("\"", "\"\"") + '"';
  }

  private static <T> T required(T value) {
    if (value == null) {
      throw new IllegalStateException("The payment file transaction returned nothing");
    }
    return value;
  }
}
