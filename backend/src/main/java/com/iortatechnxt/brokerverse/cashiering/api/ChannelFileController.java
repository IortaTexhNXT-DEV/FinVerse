package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelFile;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelProfile;
import com.iortatechnxt.brokerverse.cashiering.seed.PaymentChannelSimulator;
import com.iortatechnxt.brokerverse.cashiering.seed.PaymentChannelSimulator.SimulatedFile;
import com.iortatechnxt.brokerverse.cashiering.service.ChannelFileService;
import com.iortatechnxt.brokerverse.cashiering.service.ChannelRunReport;
import com.iortatechnxt.brokerverse.cashiering.service.ChannelRunReport.Report;
import com.iortatechnxt.brokerverse.cashiering.service.CommissionScheduleCheck;
import com.iortatechnxt.brokerverse.cashiering.service.CommissionScheduleCheck.Line;
import com.iortatechnxt.brokerverse.cashiering.service.MftIntakeService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Payment files in BDOI's layouts (FRS.CSH.05.01, 02.02.03 to 02.02.09): upload of several files of
 * several types in one event, the files received by upload or via MFT with their status, the
 * validation report, the failed and successful lists, the run report, the raw file, processing
 * again, the profiles of the file types, the MFT intake on demand and, in the SIT and UAT
 * environments, the channel simulator.
 */
@RestController
@RequestMapping("/api/v1/cashiering/payment-files")
public class ChannelFileController {

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final String SETUP =
      "hasAnyAuthority('CASH_SERIES_MANAGE', 'SYSTEM_PARAMETER_MANAGE')";

  private final ChannelFileService files;
  private final ChannelRunReport reports;
  private final CommissionScheduleCheck commissions;
  private final MftIntakeService mft;
  private final BulkService bulk;
  private final FileDownloads downloads;
  private final CurrentUser currentUser;
  private final ObjectProvider<PaymentChannelSimulator> simulator;

  /**
   * Creates the controller.
   *
   * @param files payment files
   * @param reports run reports
   * @param commissions matched and unmatched list of a Commission Schedule
   * @param mft MFT intake
   * @param bulk validation reports
   * @param downloads raw files
   * @param currentUser permissions of the user
   * @param simulator channel simulator (SIT and UAT only)
   */
  public ChannelFileController(
      ChannelFileService files,
      ChannelRunReport reports,
      CommissionScheduleCheck commissions,
      MftIntakeService mft,
      BulkService bulk,
      FileDownloads downloads,
      CurrentUser currentUser,
      ObjectProvider<PaymentChannelSimulator> simulator) {
    this.files = files;
    this.reports = reports;
    this.commissions = commissions;
    this.mft = mft;
    this.bulk = bulk;
    this.downloads = downloads;
    this.currentUser = currentUser;
    this.simulator = simulator;
  }

  /**
   * Uploads one or several files, each with its payment file type (FRS.CSH.05.01.03).
   *
   * @param companyId company
   * @param types payment file type of each file, in the order of the files
   * @param uploads files
   * @return the files with their status
   * @throws IOException when a file cannot be read
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(CashAccess.UPLOAD)
  public List<FileResponse> upload(
      @RequestParam Long companyId,
      @RequestParam List<String> types,
      @RequestParam("files") List<MultipartFile> uploads)
      throws IOException {
    if (types.size() != uploads.size()) {
      throw new BusinessRuleException(
          "CHANNEL_FILE_TYPES", "Select the payment file type of every file");
    }
    List<FileResponse> done = new ArrayList<>();
    for (int i = 0; i < uploads.size(); i++) {
      MultipartFile upload = uploads.get(i);
      done.add(
          FileResponse.from(
              files.receive(
                  companyId,
                  types.get(i),
                  upload.getOriginalFilename(),
                  upload.getBytes(),
                  "UPLOAD")));
    }
    return done;
  }

  /**
   * Files received, newest first (monitoring of the uploads and of the files received via MFT).
   *
   * @param companyId company
   * @param types file types, all when none
   * @param page page
   * @param size size
   * @return files
   */
  @GetMapping
  @PreAuthorize(CashAccess.VIEW)
  public PageResponse<FileResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) Set<String> types,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        files.list(companyId, types, CashAccess.page(page, size)), FileResponse::from);
  }

  /**
   * The run report of a file (FRS.CSH.05.01.12).
   *
   * @param id file
   * @return summary and payment records
   */
  @GetMapping("/{id}/report")
  @PreAuthorize(CashAccess.VIEW)
  public Report report(@PathVariable Long id) {
    return reports.report(id);
  }

  /**
   * The run report in Excel.
   *
   * @param id file
   * @return xlsx
   */
  @GetMapping("/{id}/report.xlsx")
  @PreAuthorize(CashAccess.VIEW)
  public ResponseEntity<byte[]> reportExcel(@PathVariable Long id) {
    ChannelFile file = files.get(id);
    return xlsx("Payment Batch Run Report_" + file.getUploadRef() + ".xlsx", reports.excel(id));
  }

  /**
   * The validation report: rows uploaded, rejected and their errors, with the counts
   * (FRS.CSH.02.02.04.01 to 02.02.04.04).
   *
   * @param id file
   * @return xlsx
   */
  @GetMapping("/{id}/validation.xlsx")
  @PreAuthorize(CashAccess.VIEW)
  public ResponseEntity<byte[]> validation(@PathVariable Long id) {
    ChannelFile file = requireRun(id);
    return xlsx(
        "Validation Report_" + file.getUploadRef() + ".xlsx", bulk.report(file.getBulkJobId()));
  }

  /**
   * The failed rows in the layout of the run with the reason, to correct and upload again.
   *
   * @param id file
   * @return xlsx
   */
  @GetMapping("/{id}/failed.xlsx")
  @PreAuthorize(CashAccess.VIEW)
  public ResponseEntity<byte[]> failed(@PathVariable Long id) {
    ChannelFile file = requireRun(id);
    return xlsx(
        "Failed Rows_" + file.getUploadRef() + ".xlsx", bulk.errorFile(file.getBulkJobId()));
  }

  /**
   * The raw file as received, kept read-only (FRS.CSH.05.01.06).
   *
   * @param id file
   * @param request request
   * @return file
   */
  @GetMapping("/{id}/raw")
  @PreAuthorize(CashAccess.VIEW)
  public ResponseEntity<byte[]> raw(@PathVariable Long id, HttpServletRequest request) {
    return downloads.respond(files.raw(id), request);
  }

  /**
   * The matched and unmatched list of a Commission Schedule (FRS.CSH.02.02.09.02 to 02.02.09.04).
   *
   * @param id file
   * @return lines
   */
  @GetMapping("/{id}/matching")
  @PreAuthorize(CashAccess.VIEW)
  public List<Line> matching(@PathVariable Long id) {
    ChannelFile file = requireRun(id);
    return commissions.lines(file.getCompanyId(), file.getBulkJobId());
  }

  /**
   * Processes again the corrected failed rows of a file (FRS.CSH.05.01.12).
   *
   * @param id file
   * @return the file
   */
  @PostMapping("/{id}/reprocess")
  @PreAuthorize(CashAccess.UPLOAD)
  public FileResponse reprocess(@PathVariable Long id) {
    return FileResponse.from(files.reprocess(id));
  }

  /**
   * Takes the files waiting in the MFT folders now (FRS.CSH.05.01.04).
   *
   * @return the files received
   */
  @PostMapping("/mft-intake")
  @PreAuthorize(CashAccess.UPLOAD)
  public List<FileResponse> mftIntake() {
    return mft.poll().stream().map(FileResponse::from).toList();
  }

  /**
   * The profiles of the payment file types (Cashiering Setup).
   *
   * @return profiles
   */
  @GetMapping("/profiles")
  @PreAuthorize(CashAccess.VIEW + " or " + SETUP)
  public List<ProfileResponse> profiles() {
    return files.profiles().stream().map(ProfileResponse::from).toList();
  }

  /**
   * Changes the profile of a payment file type.
   *
   * @param fileType type
   * @param change convention, field map, size limit and MFT folder
   * @return the profile
   */
  @PutMapping("/profiles/{fileType}")
  @PreAuthorize(SETUP)
  public ProfileResponse changeProfile(
      @PathVariable String fileType, @RequestBody ChannelProfile.Change change) {
    return ProfileResponse.from(files.changeProfile(fileType, change));
  }

  /**
   * The profile of a payment file type.
   *
   * @param fileType type
   * @param name name
   * @param namePattern file name convention
   * @param nameExample example of a file name
   * @param fileFormat TEXT, EXCEL or ANY
   * @param delimiter separator of a text file
   * @param headerLine whether the first line names the columns
   * @param totalRowLabel label of the total row
   * @param dateFormat date format of the file
   * @param fieldMap fields of the system and their place in the file
   * @param maxMb size limit
   * @param mftFolder MFT folder
   * @param mftEnabled whether the MFT folder is read
   * @param sourceSystem sending system
   * @param updatedBy changed by
   * @param updatedAt changed at
   */
  public record ProfileResponse(
      String fileType,
      String name,
      String namePattern,
      String nameExample,
      String fileFormat,
      String delimiter,
      boolean headerLine,
      String totalRowLabel,
      String dateFormat,
      String fieldMap,
      int maxMb,
      String mftFolder,
      boolean mftEnabled,
      String sourceSystem,
      String updatedBy,
      java.time.Instant updatedAt) {

    static ProfileResponse from(ChannelProfile p) {
      return new ProfileResponse(
          p.getFileType(),
          p.getName(),
          p.getNamePattern(),
          p.getNameExample(),
          p.getFileFormat(),
          p.getDelimiter(),
          p.isHeaderLine(),
          p.getTotalRowLabel(),
          p.getDateFormat(),
          p.getFieldMap(),
          p.getMaxMb(),
          p.getMftFolder(),
          p.isMftEnabled(),
          p.getSourceSystem(),
          p.getUpdatedBy(),
          p.getUpdatedAt());
    }
  }

  /**
   * Writes a payment file in BDOI's layout with the channel simulator of the SIT and UAT
   * environments; with {@code drop} the file is placed in the MFT folder of its type.
   *
   * @param companyId company
   * @param fileType type
   * @param rows booked accounts to pay
   * @param billsFile Bills Payment file a Direct Credit file identifies
   * @param drop place it in the MFT folder
   * @return the file
   */
  @PostMapping("/simulator")
  @PreAuthorize(CashAccess.UPLOAD)
  public ResponseEntity<byte[]> simulate(
      @RequestParam Long companyId,
      @RequestParam String fileType,
      @RequestParam(defaultValue = "5") int rows,
      @RequestParam(required = false) String billsFile,
      @RequestParam(defaultValue = "false") boolean drop) {
    PaymentChannelSimulator sim = simulator.getIfAvailable();
    if (sim == null || !currentUser.hasAuthority("CASH_UPLOAD")) {
      throw new BusinessRuleException(
          "CHANNEL_SIMULATOR_OFF",
          "The channel simulator runs in the SIT and UAT environments only");
    }
    if (drop) {
      sim.drop(companyId, fileType, rows);
      return ResponseEntity.noContent().build();
    }
    SimulatedFile file = sim.generate(companyId, fileType, rows, billsFile);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(file.name()).build().toString())
        .body(file.content());
  }

  private ChannelFile requireRun(Long id) {
    ChannelFile file = files.get(id);
    if (file.getBulkJobId() == null) {
      throw new BusinessRuleException(
          "CHANNEL_FILE_NOT_PROCESSED",
          file.getFileName() + " was not processed: " + file.getMessage());
    }
    return file;
  }

  private static ResponseEntity<byte[]> xlsx(String name, byte[] content) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(XLSX))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(name).build().toString())
        .body(content);
  }

  /**
   * A payment file received.
   *
   * @param id id
   * @param uploadRef upload reference
   * @param fileType type
   * @param fileName name
   * @param fileSize bytes
   * @param source UPLOAD or MFT
   * @param status RECEIVED, REFUSED, PROCESSED or PARTIAL
   * @param message reason of a refusal
   * @param rowsRead detail records
   * @param rowsFailed failed rows
   * @param headerCount records the header announces
   * @param controlTotal control total of the header or TOTAL row
   * @param detailTotal total of the detail records
   * @param runNo run that processed the rows
   * @param uploadedBy uploader (user name; MFT for the files received via MFT)
   * @param receivedAt time received
   * @param processedAt time processed
   * @param hasRaw the raw file is kept
   */
  public record FileResponse(
      Long id,
      String uploadRef,
      String fileType,
      String fileName,
      long fileSize,
      String source,
      String status,
      String message,
      int rowsRead,
      int rowsFailed,
      Integer headerCount,
      BigDecimal controlTotal,
      BigDecimal detailTotal,
      String runNo,
      String uploadedBy,
      Instant receivedAt,
      Instant processedAt,
      boolean hasRaw) {

    static FileResponse from(ChannelFile f) {
      return new FileResponse(
          f.getId(),
          f.getUploadRef(),
          f.getFileType(),
          f.getFileName(),
          f.getFileSize(),
          f.getSource(),
          f.getStatus(),
          f.getMessage(),
          f.getRowsRead(),
          f.getRowsFailed(),
          f.getHeaderCount(),
          f.getHeaderTotal(),
          f.getDetailTotal(),
          f.getBulkJobNo(),
          f.getUploadedBy(),
          f.getReceivedAt(),
          f.getProcessedAt(),
          f.getStoredFileId() != null);
    }
  }
}
