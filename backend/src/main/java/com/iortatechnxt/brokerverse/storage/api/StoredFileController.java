package com.iortatechnxt.brokerverse.storage.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.storage.api.dto.FileLinkResponse;
import com.iortatechnxt.brokerverse.storage.api.dto.InboundUploadRequest;
import com.iortatechnxt.brokerverse.storage.api.dto.StoredFileResponse;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.service.FileAccessPolicy;
import com.iortatechnxt.brokerverse.storage.service.FileLinkService;
import com.iortatechnxt.brokerverse.storage.service.FileLinkService.IssuedLink;
import com.iortatechnxt.brokerverse.storage.service.InboundUploadService;
import com.iortatechnxt.brokerverse.storage.service.InboundUploadService.InboundRequest;
import com.iortatechnxt.brokerverse.storage.service.InboundUploadService.StartedUpload;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import com.iortatechnxt.brokerverse.storage.service.UploadRules;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
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
 * Stored files (document storage, DOCUMENT_STORAGE_DECISION option C): upload, metadata, the
 * presigned download link, delete, and the presigned upload of large inbound files. Every file
 * endpoint is open to signed-in users and then decided by the owning module's {@code
 * FileOwnerAccess} (the permission of the record the file belongs to); owner types without a
 * resolver are refused.
 */
@RestController
@RequestMapping("/api/v1/files")
public class StoredFileController {

  private static final String SIGNED_IN = "isAuthenticated()";
  private static final int MAX_PAGE = 200;

  private final StoredFileService files;
  private final FileLinkService links;
  private final InboundUploadService inbound;
  private final FileAccessPolicy access;
  private final UploadRules rules;

  /**
   * Creates the controller.
   *
   * @param files stored files
   * @param links download links
   * @param inbound inbound uploads
   * @param access owner permission check
   * @param rules upload rules
   */
  public StoredFileController(
      StoredFileService files,
      FileLinkService links,
      InboundUploadService inbound,
      FileAccessPolicy access,
      UploadRules rules) {
    this.files = files;
    this.links = links;
    this.inbound = inbound;
    this.access = access;
    this.rules = rules;
  }

  /**
   * Uploads a file (up to {@code brokerverse.storage.max-upload-size}) to a record.
   *
   * @param file the file
   * @param ownerType owner entity type
   * @param ownerId owner key
   * @param companyId company (optional)
   * @param recordClass record class
   * @param documentType document type (optional)
   * @param sha256 SHA-256 the client computed (optional; a mismatch is refused)
   * @return metadata
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(consumes = "multipart/form-data")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(SIGNED_IN)
  public StoredFileResponse upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam String ownerType,
      @RequestParam String ownerId,
      @RequestParam(required = false) Long companyId,
      @RequestParam String recordClass,
      @RequestParam(required = false) String documentType,
      @RequestParam(required = false) String sha256)
      throws IOException {
    FileOwner owner = new FileOwner(companyId, ownerType, ownerId);
    access.requireStore(owner, documentType);
    rules.requireSize(file.getSize(), rules.maxUploadBytes());
    StoredFile saved =
        files.store(
            new StoreRequest(
                owner,
                documentType,
                recordClass,
                file.getOriginalFilename(),
                file.getBytes(),
                sha256));
    return StoredFileResponse.from(saved);
  }

  /**
   * The live files of a record.
   *
   * @param ownerType owner entity type
   * @param ownerId owner key
   * @return files
   */
  @GetMapping
  @PreAuthorize(SIGNED_IN)
  public List<StoredFileResponse> list(
      @RequestParam String ownerType, @RequestParam String ownerId) {
    FileOwner owner = new FileOwner(null, ownerType, ownerId);
    access.requireRead(owner, null);
    return files.filesOf(owner).stream()
        .filter(f -> access.mayRead(f.owner(), f.getDocumentType()))
        .map(StoredFileResponse::from)
        .toList();
  }

  /**
   * Metadata of a file.
   *
   * @param id id
   * @return metadata
   */
  @GetMapping("/{id}")
  @PreAuthorize(SIGNED_IN)
  public StoredFileResponse get(@PathVariable Long id) {
    StoredFile file = files.get(id);
    access.requireRead(file.owner(), file.getDocumentType());
    return StoredFileResponse.from(file);
  }

  /**
   * A presigned download link (valid {@code FILE_LINK_TTL_SECONDS}); audited.
   *
   * @param id id
   * @param request HTTP request (client address for the audit entry)
   * @return link, never cached
   */
  @GetMapping("/{id}/link")
  @PreAuthorize(SIGNED_IN)
  public ResponseEntity<FileLinkResponse> link(@PathVariable Long id, HttpServletRequest request) {
    IssuedLink issued = links.issue(id, request.getRemoteAddr());
    StoredFile file = issued.file();
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            FileLinkResponse.from(
                file.getId(), issued.link(), file.getFileName(), file.getContentType()));
  }

  /**
   * Deletes a file (soft; refused under legal hold).
   *
   * @param id id
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(SIGNED_IN)
  public void delete(@PathVariable Long id) {
    StoredFile file = files.get(id);
    access.requireStore(file.owner(), file.getDocumentType());
    files.delete(id);
  }

  /**
   * Announces a large inbound file and returns its presigned upload link.
   *
   * @param body the file
   * @return file and link, never cached
   */
  @PostMapping("/inbound-uploads")
  @PreAuthorize(SIGNED_IN)
  public ResponseEntity<FileLinkResponse> startInbound(
      @Valid @RequestBody InboundUploadRequest body) {
    FileOwner owner = new FileOwner(body.companyId(), body.ownerType(), body.ownerId());
    access.requireStore(owner, body.documentType());
    StartedUpload started =
        inbound.start(
            new InboundRequest(
                owner,
                body.documentType(),
                body.recordClass(),
                body.fileName(),
                body.sizeBytes(),
                body.sha256()));
    StoredFile file = started.file();
    return ResponseEntity.status(HttpStatus.CREATED)
        .cacheControl(CacheControl.noStore())
        .body(
            FileLinkResponse.from(
                file.getId(), started.link(), file.getFileName(), file.getContentType()));
  }

  /**
   * Confirms an inbound upload; the file then waits for its malware scan.
   *
   * @param id id
   * @return metadata
   */
  @PostMapping("/{id}/upload-complete")
  @PreAuthorize(SIGNED_IN)
  public StoredFileResponse completeInbound(@PathVariable Long id) {
    StoredFile file = files.get(id);
    access.requireStore(file.owner(), file.getDocumentType());
    return StoredFileResponse.from(inbound.complete(id));
  }

  /**
   * Quarantined files, newest first (security review).
   *
   * @param page page number
   * @param size page size
   * @return files
   */
  @GetMapping("/quarantined")
  @PreAuthorize("hasAuthority('FILE_QUARANTINE_VIEW')")
  public PageResponse<StoredFileResponse> quarantined(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
    return PageResponse.of(
        files.quarantined(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE))),
        StoredFileResponse::from);
  }
}
