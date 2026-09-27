package com.iortatechnxt.brokerverse.support;

import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import org.springframework.stereotype.Component;

/** Reads what a module download answers (a stored file or inline content) in service tests. */
@Component
public class StoredDownloads {

  private final StoredFileService files;

  StoredDownloads(StoredFileService files) {
    this.files = files;
  }

  /** The bytes of a download. */
  public byte[] bytes(FileDownload download) {
    return download.isStored() ? files.read(download.storedFileId()) : download.content();
  }

  /** The file name of a download. */
  public String fileName(FileDownload download) {
    return download.isStored()
        ? files.get(download.storedFileId()).getFileName()
        : download.fileName();
  }
}
