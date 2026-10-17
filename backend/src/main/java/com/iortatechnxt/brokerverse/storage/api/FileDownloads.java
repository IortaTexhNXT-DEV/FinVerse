package com.iortatechnxt.brokerverse.storage.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.service.FileAccessPolicy;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.storage.service.FileLinkService;
import com.iortatechnxt.brokerverse.storage.service.FileLinkService.IssuedLink;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Answers the download endpoints of the modules (build step ST1). With {@code
 * brokerverse.storage.downloads.mode=redirect} a stored file is answered with a redirect ({@code
 * 302 Found}) to its presigned link, issued by {@link FileLinkService} after the owner's permission
 * check and the scan state, and audited; the bytes never pass through the application. With {@code
 * stream} (the default until the browser may fetch from the buckets) the same checks apply and the
 * file is read through {@link StoredFileService#read} (SHA-256 re-checked, audited). Content that
 * is still in a {@code bytea} column, or built on the fly, is sent as before. Every answer is
 * attachment-disposed and never cached.
 */
@Component
@EnableConfigurationProperties(DownloadProperties.class)
public class FileDownloads {

  private final FileLinkService links;
  private final StoredFileService files;
  private final FileAccessPolicy access;
  private final DownloadProperties properties;

  /**
   * Creates the responder.
   *
   * @param links presigned links
   * @param files stored files (stream mode)
   * @param access owner permission check (stream mode)
   * @param properties download mode
   */
  public FileDownloads(
      FileLinkService links,
      StoredFileService files,
      FileAccessPolicy access,
      DownloadProperties properties) {
    this.links = links;
    this.files = files;
    this.access = access;
    this.properties = properties;
  }

  /**
   * The answer of a download endpoint.
   *
   * @param download stored file or inline content
   * @param request HTTP request (client address for the audit entry)
   * @return redirect to the link, or the content
   */
  public ResponseEntity<byte[]> respond(FileDownload download, HttpServletRequest request) {
    if (!download.isStored()) {
      return content(download.fileName(), download.contentType(), download.content());
    }
    if (properties.redirect()) {
      IssuedLink issued = links.issue(download.storedFileId(), request.getRemoteAddr());
      return ResponseEntity.status(HttpStatus.FOUND)
          .location(issued.link().url())
          .cacheControl(CacheControl.noStore())
          .build();
    }
    StoredFile file = files.get(download.storedFileId());
    access.requireRead(file.owner(), file.getDocumentType());
    return content(file.getFileName(), file.getContentType(), files.read(file.getId()));
  }

  private static ResponseEntity<byte[]> content(String fileName, String type, byte[] bytes) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(type))
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(fileName))
        .body(bytes);
  }
}
