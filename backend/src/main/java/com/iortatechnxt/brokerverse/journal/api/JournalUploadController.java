package com.iortatechnxt.brokerverse.journal.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.journal.service.JournalUploadService;
import com.iortatechnxt.brokerverse.journal.service.JournalUploadTemplate;
import com.iortatechnxt.brokerverse.journal.service.JournalUploadTemplate.Example;
import com.iortatechnxt.brokerverse.journal.service.UploadResult;
import com.iortatechnxt.brokerverse.organization.service.OrganizationDirectory;
import com.iortatechnxt.brokerverse.organization.service.OrganizationDirectory.ClientProfile;
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
  private final OrganizationDirectory organization;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param service upload service
   * @param organization company master (branch and currency of the examples)
   * @param clock clock
   */
  public JournalUploadController(
      JournalUploadService service, OrganizationDirectory organization, Clock clock) {
    this.service = service;
    this.organization = organization;
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
   * Downloads the upload template with example vouchers in the head office and base currency of the
   * company.
   *
   * @param format csv or xlsx
   * @param companyId company of the examples; blank branch and currency without it
   * @return template file
   */
  @GetMapping("/template")
  @PreAuthorize("hasAuthority('JOURNAL_CREATE')")
  public ResponseEntity<byte[]> template(
      @RequestParam(defaultValue = "csv") String format,
      @RequestParam(required = false) Long companyId) {
    boolean excel = "xlsx".equals(format);
    LocalDate today = BusinessClock.today(clock);
    Example example =
        companyId == null
            ? new Example(today, null, null)
            : example(today, organization.profile(companyId));
    byte[] body = excel ? JournalUploadTemplate.xlsx(example) : JournalUploadTemplate.csv(example);
    String name = "journal-upload-template." + (excel ? "xlsx" : "csv");
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(excel ? XLSX : "text/csv"))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(name))
        .body(body);
  }

  private static Example example(LocalDate today, ClientProfile company) {
    return new Example(today, company.headOfficeCode(), company.baseCurrency());
  }

  /** Upload processing mode. */
  public enum UploadMode {
    /** Validate only; nothing is created. */
    VALIDATE,
    /** Create valid vouchers as draft journals. */
    IMPORT
  }
}
