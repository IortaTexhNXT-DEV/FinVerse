package com.iortatechnxt.brokerverse.attachment.api;

import com.iortatechnxt.brokerverse.attachment.api.dto.AttachmentResponse;
import com.iortatechnxt.brokerverse.attachment.api.dto.LinkRequest;
import com.iortatechnxt.brokerverse.attachment.domain.AllowedFileType;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService.AttachmentFile;
import com.iortatechnxt.brokerverse.attachment.service.DocumentNamingService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Document attachments on any record: upload (one or several files, document type, inherited or
 * nominated names), list (own and linked files), download (single or ZIP), link to other records
 * and remove (BRNB.026/055/056). Lists and downloads apply the document access classes and uploads
 * and links take a process tag (BRID-025).
 */
@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {

  private static final String VIEW = "hasAuthority('ATTACHMENT_VIEW')";
  private static final String MANAGE = "hasAuthority('ATTACHMENT_MANAGE')";
  private static final String NOMINATE = "NOMINATE";

  private final AttachmentService service;
  private final DocumentService documents;

  /**
   * Creates the controller.
   *
   * @param service attachment service
   * @param documents document features
   */
  public AttachmentController(AttachmentService service, DocumentService documents) {
    this.service = service;
    this.documents = documents;
  }

  /**
   * Lists the documents of a record (its own files and the files linked to it).
   *
   * @param entityType entity type
   * @param entityId entity id
   * @return attachments
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public List<AttachmentResponse> list(
      @RequestParam String entityType, @RequestParam String entityId) {
    AttachmentTarget target = new AttachmentTarget(entityType, entityId);
    return documents.list(target).stream().map(a -> AttachmentResponse.from(a, target)).toList();
  }

  /**
   * Upload limits and the file naming syntax for the client.
   *
   * @return policy
   */
  @GetMapping("/policy")
  @PreAuthorize(VIEW)
  public UploadPolicy policy() {
    return new UploadPolicy(
        service.maxSizeBytes(),
        AllowedFileType.allowedExtensions(),
        DocumentNamingService.SYNTAX,
        DocumentService.MAX_FILES);
  }

  /**
   * Uploads a file.
   *
   * @param entityType entity type
   * @param entityId entity id
   * @param form description, document type, naming, reference and process tag (all optional)
   * @param file file
   * @return metadata
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MANAGE)
  public AttachmentResponse upload(
      @RequestParam String entityType,
      @RequestParam String entityId,
      UploadForm form,
      @RequestParam MultipartFile file)
      throws IOException {
    return uploadAll(new AttachmentTarget(entityType, entityId), List.of(file), form.options())
        .get(0);
  }

  /**
   * Uploads several files at once (BRNB.026 multi-file upload).
   *
   * @param entityType entity type
   * @param entityId entity id
   * @param form description, document type, naming, reference and process tag (all optional)
   * @param files files
   * @return metadata in upload order
   * @throws IOException when an upload cannot be read
   */
  @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MANAGE)
  public List<AttachmentResponse> uploadBatch(
      @RequestParam String entityType,
      @RequestParam String entityId,
      UploadForm form,
      @RequestParam("files") List<MultipartFile> files)
      throws IOException {
    return uploadAll(new AttachmentTarget(entityType, entityId), files, form.options());
  }

  private List<AttachmentResponse> uploadAll(
      AttachmentTarget target, List<MultipartFile> files, UploadOptions options)
      throws IOException {
    List<UploadedFile> uploaded = new ArrayList<>();
    for (MultipartFile file : files) {
      service.requireWithinLimit(file.getSize());
      uploaded.add(new UploadedFile(file.getOriginalFilename(), file.getBytes()));
    }
    return documents.upload(target, uploaded, options).stream()
        .map(a -> AttachmentResponse.from(a, target))
        .toList();
  }

  /**
   * Links a file to further records (e.g. one IDF for several accounts).
   *
   * @param id file
   * @param request records
   * @return metadata
   */
  @PostMapping("/{id}/links")
  @PreAuthorize(MANAGE)
  public AttachmentResponse link(@PathVariable Long id, @Valid @RequestBody LinkRequest request) {
    return AttachmentResponse.from(documents.link(id, request.targets(), request.processTag()));
  }

  /**
   * Downloads a file the user may see (document access classes, BRID-025).
   *
   * @param id id
   * @return file
   */
  @GetMapping("/{id}/content")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> download(@PathVariable Long id) {
    AttachmentFile file = documents.download(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.metadata().getContentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDispositions.attachment(file.metadata().getFileName()))
        .body(file.content());
  }

  /**
   * Downloads several files as one ZIP (BRNB.056).
   *
   * @param ids attachment ids
   * @param name ZIP file name without extension (e.g. the ARN)
   * @return ZIP file
   */
  @GetMapping("/zip")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> zip(
      @RequestParam List<Long> ids, @RequestParam(defaultValue = "documents") String name) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/zip"))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(name + ".zip"))
        .body(documents.zip(ids));
  }

  /**
   * Removes a document: from the record it is linked to (unlink), or everywhere when the record
   * owns it or no record is given (logical delete).
   *
   * @param id id
   * @param entityType record the user removes it from, optional
   * @param entityId record id, optional
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(MANAGE)
  public void delete(
      @PathVariable Long id,
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) String entityId) {
    AttachmentTarget target =
        entityType == null || entityId == null ? null : new AttachmentTarget(entityType, entityId);
    documents.remove(id, target);
  }

  /**
   * Optional fields of an upload, bound from the request parameters.
   *
   * @param description description of the files
   * @param documentType document type (list DOCUMENT_TYPE)
   * @param naming INHERIT (default) or NOMINATE
   * @param reference business reference for nominated names
   * @param processTag process the documents belong to (BRID-025)
   */
  public record UploadForm(
      String description, String documentType, String naming, String reference, String processTag) {

    /**
     * The upload options.
     *
     * @return options
     */
    UploadOptions options() {
      return new UploadOptions(
          documentType, NOMINATE.equals(naming), reference, description, processTag);
    }
  }

  /**
   * Upload limits.
   *
   * @param maxSizeBytes largest accepted file
   * @param allowedExtensions accepted extensions
   * @param namingSyntax syntax of nominated file names
   * @param maxFiles largest number of files per upload or ZIP
   */
  public record UploadPolicy(
      long maxSizeBytes, String allowedExtensions, String namingSyntax, int maxFiles) {}
}
