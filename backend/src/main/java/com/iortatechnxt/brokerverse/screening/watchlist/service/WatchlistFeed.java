package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistSource;
import java.util.List;

/**
 * Port through which the scheduled job {@code SCR_WATCHLIST_INGEST} receives the list files of a
 * source (SNSRP-201; design section 10). The default adapter {@link StagedFileWatchlistFeed} reads
 * the files staged on the source (uploaded on screen or through the attachments API). Transports
 * named by BDOI later (SFTP, the AML unit's e-mail advisory, an NLDS or sanctions API; SQ01) are
 * further adapters of this port.
 */
public interface WatchlistFeed {

  /**
   * The files of a source that no run has read yet, oldest first.
   *
   * @param source the source
   * @return files, empty when none was received
   */
  List<FeedFile> pending(WatchlistSource source);

  /**
   * One received list file.
   *
   * @param attachmentId the stored file (attachment id), marks it as read once a run links it
   * @param fileName file name
   * @param content bytes
   */
  record FeedFile(Long attachmentId, String fileName, byte[] content) {

    /** Defensive copy. */
    public FeedFile {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof FeedFile f
          && java.util.Objects.equals(attachmentId, f.attachmentId)
          && java.util.Objects.equals(fileName, f.fileName)
          && java.util.Arrays.equals(content, f.content);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(attachmentId, fileName, java.util.Arrays.hashCode(content));
    }

    @Override
    public String toString() {
      return "FeedFile[" + fileName + ", " + content.length + " bytes]";
    }
  }
}
