package com.iortatechnxt.brokerverse.storage.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.common.storage.PresignedLink;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.service.FileAccessPolicy;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.storage.service.FileLinkService;
import com.iortatechnxt.brokerverse.storage.service.FileLinkService.IssuedLink;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

/** The answers of the module download endpoints in both download modes. */
class FileDownloadsTest {

  private static final byte[] PDF = "%PDF-1.4".getBytes(StandardCharsets.US_ASCII);
  private static final FileOwner OWNER = new FileOwner(null, "PrintBatch", "7");

  private final FileLinkService links = mock(FileLinkService.class);
  private final StoredFileService files = mock(StoredFileService.class);
  private final FileAccessPolicy access = mock(FileAccessPolicy.class);
  private final MockHttpServletRequest request = new MockHttpServletRequest();

  private FileDownloads downloads(String mode) {
    return new FileDownloads(links, files, access, new DownloadProperties(mode));
  }

  private StoredFile stored() {
    StoredFile file = mock(StoredFile.class);
    when(file.getId()).thenReturn(5L);
    when(file.owner()).thenReturn(OWNER);
    when(file.getFileName()).thenReturn("batch.pdf");
    when(file.getContentType()).thenReturn("application/pdf");
    when(files.get(5L)).thenReturn(file);
    return file;
  }

  @Test
  void redirectModeSendsTheClientToThePresignedLink() {
    StoredFile file = stored();
    URI url = URI.create("https://bucket.example/object?signature=x");
    when(links.issue(5L, request.getRemoteAddr()))
        .thenReturn(new IssuedLink(file, new PresignedLink(url, "GET", Instant.now(), Map.of())));
    ResponseEntity<byte[]> answer = downloads("redirect").respond(FileDownload.stored(5L), request);
    assertThat(answer.getStatusCode()).isEqualTo(HttpStatus.FOUND);
    assertThat(answer.getHeaders().getLocation()).isEqualTo(url);
    assertThat(answer.getHeaders().getCacheControl()).isEqualTo("no-store");
    assertThat(answer.getBody()).isNull();
    verify(files, never()).read(any());
  }

  @Test
  void streamModeChecksTheOwnerAndReadsThroughTheStore() {
    stored();
    when(files.read(5L)).thenReturn(PDF);
    ResponseEntity<byte[]> answer = downloads(null).respond(FileDownload.stored(5L), request);
    assertThat(answer.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(answer.getBody()).isEqualTo(PDF);
    assertThat(answer.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .startsWith("attachment; filename=\"batch.pdf\"");
    verify(access).requireRead(OWNER, null);
    verify(links, never()).issue(any(), any());

    doThrow(new AccessDeniedException("no")).when(access).requireRead(OWNER, null);
    assertThatThrownBy(() -> downloads("stream").respond(FileDownload.stored(5L), request))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void contentKeptInTheDatabaseIsSentAsBefore() {
    ResponseEntity<byte[]> answer =
        downloads("redirect").respond(FileDownload.inline("old.bin", null, PDF), request);
    assertThat(answer.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(answer.getBody()).isEqualTo(PDF);
    assertThat(answer.getHeaders().getContentType()).hasToString("application/octet-stream");
    assertThat(FileDownload.inline("old.bin", "text/csv", PDF))
        .isEqualTo(FileDownload.inline("old.bin", "text/csv", PDF))
        .hasSameHashCodeAs(FileDownload.inline("old.bin", "text/csv", PDF))
        .hasToString("FileDownload[old.bin, 8 bytes]");
    assertThat(FileDownload.stored(3L)).hasToString("FileDownload[stored 3]");
    assertThatThrownBy(() -> new DownloadProperties("both"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
