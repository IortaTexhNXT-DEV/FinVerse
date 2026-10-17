package com.iortatechnxt.brokerverse.nbadmin.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.nbadmin.service.CertificationExtract;
import com.iortatechnxt.brokerverse.nbadmin.service.CertificationExtract.ExtractFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The certification extract of Reports &amp; Certification: the file of the accounts, their group
 * profiles and permissions, and the log of the extracts taken.
 */
@RestController
@RequestMapping("/api/v1/nbadmin/certification")
@PreAuthorize("hasAuthority('UAM_REPORT_VIEW')")
public class CertificationController {

  private static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  private static final MediaType CSV = MediaType.parseMediaType("text/csv; charset=UTF-8");

  private final CertificationExtract extract;

  /**
   * Creates the controller.
   *
   * @param extract the extract
   */
  public CertificationController(CertificationExtract extract) {
    this.extract = extract;
  }

  /**
   * Takes an extract; it is logged and Information Security is told.
   *
   * @param request format
   * @return the file
   */
  @PostMapping("/extract")
  public ResponseEntity<byte[]> take(@Valid @RequestBody ExtractRequest request) {
    ExtractFile file = extract.take(request.format());
    return ResponseEntity.ok()
        .contentType(file.xlsx() ? XLSX : CSV)
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(file.fileName()))
        .body(file.content());
  }

  /**
   * The extracts taken, newest first.
   *
   * @return log
   */
  @GetMapping("/extracts")
  public List<CertificationExtract.LogEntry> log() {
    return extract.log();
  }

  /**
   * The format of an extract.
   *
   * @param format CSV or XLSX
   */
  public record ExtractRequest(@NotNull @Pattern(regexp = "CSV|XLSX") String format) {}
}
