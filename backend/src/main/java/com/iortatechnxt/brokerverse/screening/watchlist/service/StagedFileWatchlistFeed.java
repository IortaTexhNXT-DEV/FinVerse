package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionRun;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionRunRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistSource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link WatchlistFeed}: the files staged on the source (attachments of entity type {@value
 * #SOURCE_ENTITY}) that no ingestion run has read yet. A file is read once: the run that reads it
 * links its attachment id.
 */
@Component
@Transactional
public class StagedFileWatchlistFeed implements WatchlistFeed {

  /** Attachment entity type of list files staged on a source. */
  public static final String SOURCE_ENTITY = "ScreeningListSource";

  private final AttachmentService attachments;
  private final IngestionRunRepository runs;

  /**
   * Creates the adapter.
   *
   * @param attachments attachments
   * @param runs ingestion runs
   */
  public StagedFileWatchlistFeed(AttachmentService attachments, IngestionRunRepository runs) {
    this.attachments = attachments;
    this.runs = runs;
  }

  @Override
  public List<FeedFile> pending(WatchlistSource source) {
    List<Attachment> staged =
        attachments.list(new AttachmentTarget(SOURCE_ENTITY, String.valueOf(source.getId())));
    if (staged.isEmpty()) {
      return List.of();
    }
    Set<Long> read =
        runs.findByFileAttachmentIdIn(staged.stream().map(Attachment::getId).toList()).stream()
            .map(IngestionRun::getFileAttachmentId)
            .collect(Collectors.toSet());
    return staged.stream()
        .filter(a -> !read.contains(a.getId()))
        .map(
            a ->
                new FeedFile(a.getId(), a.getFileName(), attachments.download(a.getId()).content()))
        .toList();
  }
}
