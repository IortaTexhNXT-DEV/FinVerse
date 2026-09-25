package com.iortatechnxt.brokerverse.docgen.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.docgen.api.dto.WordAvailability;
import com.iortatechnxt.brokerverse.docgen.service.DocumentFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocumentRenditionService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentRenditionService.WordCopy;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Word copies of generated documents (client requirement 16): after a document is downloaded as
 * PDF, the client asks whether it can also be had in Word (by the SHA-256 of the PDF) and presents
 * the PDF to receive its Word copy with the same content.
 */
@RestController
@RequestMapping("/api/v1/doc-renditions")
@PreAuthorize("isAuthenticated()")
public class DocRenditionController {

  private final DocumentRenditionService renditions;

  /**
   * Creates the controller.
   *
   * @param renditions rendition service
   */
  public DocRenditionController(DocumentRenditionService renditions) {
    this.renditions = renditions;
  }

  /**
   * Whether a downloaded PDF can also be downloaded as Word.
   *
   * @param sha256 SHA-256 of the PDF (hex)
   * @return availability and document title
   */
  @GetMapping("/{sha256}")
  public WordAvailability availability(@PathVariable String sha256) {
    return WordAvailability.from(renditions.available(sha256).orElse(null));
  }

  /**
   * The Word copy of a downloaded PDF.
   *
   * @param file the PDF as downloaded
   * @return DOCX file
   */
  @PostMapping("/word")
  public ResponseEntity<byte[]> word(@RequestParam MultipartFile file) {
    if (file.isEmpty()) {
      throw new BusinessRuleException("DOCUMENT_PDF_REQUIRED", "Select the PDF of the document");
    }
    WordCopy copy;
    try {
      copy = renditions.word(file.getBytes());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(DocumentFormat.DOCX.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(copy.fileName()))
        .body(copy.content());
  }
}
