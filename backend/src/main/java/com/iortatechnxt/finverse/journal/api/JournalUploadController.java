package com.iortatechnxt.finverse.journal.api;

import com.iortatechnxt.finverse.common.api.ContentDispositions;
import com.iortatechnxt.finverse.journal.service.JournalUploadService;
import com.iortatechnxt.finverse.journal.service.JournalUploadTemplate;
import com.iortatechnxt.finverse.journal.service.UploadResult;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Bulk journal upload (CSV / XLSX): template download, validation and draft creation. */
@RestController
@RequestMapping("/api/v1/journals/upload")
public class JournalUploadController {

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final JournalUploadService service;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param service upload service
   * @param clock clock
   */
  public JournalUploadController(JournalUploadService service, Clock clock) {
    this.service = service;
    this.clock = clock;
  }

  /**
   * Validates a file and, in IMPORT mode, creates the valid vouchers as drafts.
   *
   * @param companyId company
   * @param mode VALIDATE (dry run, default) or IMPORT
   * @param file CSV or XLSX file
   * @return validation report
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('JOURNAL_CREATE')")
  public UploadResult upload(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "VALIDATE") UploadMode mode,
      @RequestParam MultipartFile file)
      throws IOException {
    if (file.getSize() > JournalUploadService.MAX_BYTES) {
      throw new IllegalArgumentException("The file must be at most 5 MB");
    }
    return service.process(
        companyId, file.getOriginalFilename(), file.getBytes(), mode == UploadMode.IMPORT);
  }

  /**
   * Downloads the upload template with sample vouchers.
   *
   * @param format csv or xlsx
   * @return template file
   */
  @GetMapping("/template")
  @PreAuthorize("hasAuthority('JOURNAL_CREATE')")
  public ResponseEntity<byte[]> template(@RequestParam(defaultValue = "csv") String format) {
    boolean excel = "xlsx".equals(format);
    LocalDate today = LocalDate.now(clock);
    byte[] body = excel ? JournalUploadTemplate.xlsx(today) : JournalUploadTemplate.csv(today);
    String name = "journal-upload-template." + (excel ? "xlsx" : "csv");
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(excel ? XLSX : "text/csv"))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(name))
        .body(body);
  }

  /** Upload processing mode. */
  public enum UploadMode {
    /** Validate only; nothing is created. */
    VALIDATE,
    /** Create valid vouchers as draft journals. */
    IMPORT
  }
}
