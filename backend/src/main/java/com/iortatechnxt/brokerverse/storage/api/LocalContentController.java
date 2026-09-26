package com.iortatechnxt.brokerverse.storage.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.storage.FileStore;
import com.iortatechnxt.brokerverse.common.storage.FileStoreException;
import com.iortatechnxt.brokerverse.common.storage.LocalFileStore;
import com.iortatechnxt.brokerverse.common.storage.LocalFileStore.LinkedContent;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the presigned links of the local file store (developer machines, automated tests and seed
 * stacks). The signed token carries the object, the method and the expiry; the permission was
 * checked when the link was issued. With the S3 store the links point to S3 and this endpoint
 * answers 404.
 */
@RestController
@RequestMapping(LocalFileStore.CONTENT_PATH)
public class LocalContentController {

  private final FileStore store;

  /**
   * Creates the controller.
   *
   * @param store object store
   */
  public LocalContentController(FileStore store) {
    this.store = store;
  }

  /**
   * Downloads the object of a GET link as an attachment, never cached.
   *
   * @param token link token
   * @return content
   */
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<byte[]> download(@RequestParam String token) {
    LinkedContent content;
    try {
      content = local().readLinked(token);
    } catch (FileStoreException | IllegalArgumentException e) {
      throw new AccessDeniedException("The link is invalid or has expired", e);
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(content.fileName()))
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType(content.contentType()))
        .body(content.content());
  }

  /**
   * Receives the bytes of a PUT link.
   *
   * @param token link token
   * @param content bytes
   * @return 200 when stored
   */
  @PutMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> upload(@RequestParam String token, @RequestBody byte[] content) {
    try {
      local().writeLinked(token, content);
    } catch (FileStoreException | IllegalArgumentException e) {
      throw new AccessDeniedException("The link is invalid, has expired or the content differs", e);
    }
    return ResponseEntity.ok().build();
  }

  private LocalFileStore local() {
    if (store instanceof LocalFileStore local) {
      return local;
    }
    throw new ResourceNotFoundException("Local file link", "local-content");
  }
}
