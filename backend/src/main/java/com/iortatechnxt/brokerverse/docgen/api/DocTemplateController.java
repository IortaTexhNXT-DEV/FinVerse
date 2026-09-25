package com.iortatechnxt.brokerverse.docgen.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.docgen.api.dto.DocTemplateRequest;
import com.iortatechnxt.brokerverse.docgen.api.dto.DocTemplateResponse;
import com.iortatechnxt.brokerverse.docgen.api.dto.TemplateDraftResponse;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentFormat;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Document templates: versions and new versions (Broking Setup); a version is downloaded as Word
 * and an edited Word file is read back as the draft of a new version (client requirement 16).
 */
@RestController
@RequestMapping("/api/v1/doc-templates")
public class DocTemplateController {

  private final DocTemplateService templates;

  /**
   * Creates the controller.
   *
   * @param templates template service
   */
  public DocTemplateController(DocTemplateService templates) {
    this.templates = templates;
  }

  /**
   * Every version of every template.
   *
   * @return versions
   */
  @GetMapping
  @PreAuthorize("hasAnyAuthority('MASTER_VIEW', 'LOV_MANAGE')")
  public List<DocTemplateResponse> all() {
    return templates.all().stream().map(DocTemplateResponse::from).toList();
  }

  /**
   * A version as a Word file, for editing.
   *
   * @param code template
   * @param versionNo version
   * @return DOCX file
   */
  @GetMapping("/{code}/versions/{versionNo}/docx")
  @PreAuthorize("hasAnyAuthority('MASTER_VIEW', 'LOV_MANAGE')")
  public ResponseEntity<byte[]> word(@PathVariable String code, @PathVariable int versionNo) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(DocumentFormat.DOCX.contentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDispositions.attachment(code + "-v" + versionNo + ".docx"))
        .body(templates.word(code, versionNo));
  }

  /**
   * Reads an edited Word file as the draft of a new version (nothing is saved).
   *
   * @param code template
   * @param file DOCX file
   * @return title, text and placeholder differences
   */
  @PostMapping("/{code}/docx")
  @PreAuthorize("hasAnyAuthority('MASTER_MAINTAIN', 'LOV_MANAGE')")
  public TemplateDraftResponse readWord(
      @PathVariable String code, @RequestParam MultipartFile file) {
    try {
      return TemplateDraftResponse.from(templates.readWord(code, file.getBytes()));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Adds a version.
   *
   * @param code template
   * @param request text and effective date
   * @return new version
   */
  @PostMapping("/{code}/versions")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('MASTER_MAINTAIN', 'LOV_MANAGE')")
  public DocTemplateResponse newVersion(
      @PathVariable String code, @Valid @RequestBody DocTemplateRequest request) {
    return DocTemplateResponse.from(
        templates.newVersion(code, request.title(), request.body(), request.effectiveFrom()));
  }
}
