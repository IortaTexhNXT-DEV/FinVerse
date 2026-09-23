package com.iortatechnxt.finverse.attachment.api;

import com.iortatechnxt.finverse.attachment.api.dto.AttachmentResponse;
import com.iortatechnxt.finverse.attachment.domain.AllowedFileType;
import com.iortatechnxt.finverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.finverse.attachment.service.AttachmentService;
import com.iortatechnxt.finverse.attachment.service.AttachmentService.AttachmentFile;
import com.iortatechnxt.finverse.common.api.ContentDispositions;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Document attachments on any record (upload, list, download, remove). */
@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {

  private static final String VIEW = "hasAuthority('ATTACHMENT_VIEW')";

  private final AttachmentService service;

  /**
   * Creates the controller.
   *
   * @param service attachment service
   */
  public AttachmentController(AttachmentService service) {
    this.service = service;
  }

  /**
   * Lists the attachments of a record.
   *
   * @param entityType entity type
   * @param entityId entity id
   * @return attachments
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public List<AttachmentResponse> list(
      @RequestParam String entityType, @RequestParam String entityId) {
    return service.list(new AttachmentTarget(entityType, entityId)).stream()
        .map(AttachmentResponse::from)
        .toList();
  }

  /**
   * Upload limits for the client.
   *
   * @return policy
   */
  @GetMapping("/policy")
  @PreAuthorize(VIEW)
  public UploadPolicy policy() {
    return new UploadPolicy(service.maxSizeBytes(), AllowedFileType.allowedExtensions());
  }

  /**
   * Uploads a file.
   *
   * @param entityType entity type
   * @param entityId entity id
   * @param description optional description
   * @param file file
   * @return metadata
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('ATTACHMENT_MANAGE')")
  public AttachmentResponse upload(
      @RequestParam String entityType,
      @RequestParam String entityId,
      @RequestParam(required = false) String description,
      @RequestParam MultipartFile file)
      throws IOException {
    service.requireWithinLimit(file.getSize());
    return AttachmentResponse.from(
        service.upload(
            new AttachmentTarget(entityType, entityId),
            file.getOriginalFilename(),
            file.getBytes(),
            description));
  }

  /**
   * Downloads a file.
   *
   * @param id id
   * @return file
   */
  @GetMapping("/{id}/content")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> download(@PathVariable Long id) {
    AttachmentFile file = service.download(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.metadata().getContentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDispositions.attachment(file.metadata().getFileName()))
        .body(file.content());
  }

  /**
   * Removes an attachment (logical delete).
   *
   * @param id id
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('ATTACHMENT_MANAGE')")
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }

  /**
   * Upload limits.
   *
   * @param maxSizeBytes largest accepted file
   * @param allowedExtensions accepted extensions
   */
  public record UploadPolicy(long maxSizeBytes, String allowedExtensions) {}
}
