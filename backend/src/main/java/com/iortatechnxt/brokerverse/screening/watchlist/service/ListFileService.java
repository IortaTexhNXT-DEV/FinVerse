package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionError;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionErrorRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionRun;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionRunRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionTrigger;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistSource;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistFeed.FeedFile;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * List files and the run log (SNSRP-201, 202; FR-SS-020, 021): "Upload List File" (a run logged at
 * once whose changes wait for a checker), staging a file for the scheduled run of its source, and
 * the runs with their failed records.
 */
@Service
@Transactional
public class ListFileService {

  private final WatchlistService watchlists;
  private final WatchlistIngestionService ingestion;
  private final IngestionRunRepository runs;
  private final IngestionErrorRepository errors;
  private final AttachmentService attachments;

  /**
   * Creates the service.
   *
   * @param watchlists sources
   * @param ingestion ingestion
   * @param runs runs
   * @param errors failed records
   * @param attachments stored list files
   */
  public ListFileService(
      WatchlistService watchlists,
      WatchlistIngestionService ingestion,
      IngestionRunRepository runs,
      IngestionErrorRepository errors,
      AttachmentService attachments) {
    this.watchlists = watchlists;
    this.ingestion = ingestion;
    this.runs = runs;
    this.errors = errors;
    this.attachments = attachments;
  }

  /**
   * Runs, newest first.
   *
   * @param sourceCode source, blank for all
   * @param pageable page
   * @return runs
   */
  @Transactional(readOnly = true)
  public Page<IngestionRun> runs(String sourceCode, Pageable pageable) {
    Long sourceId =
        sourceCode == null || sourceCode.isBlank() ? null : watchlists.source(sourceCode).getId();
    return runs.search(sourceId, pageable);
  }

  /**
   * A run.
   *
   * @param id id
   * @return run
   */
  @Transactional(readOnly = true)
  public IngestionRun run(Long id) {
    return runs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ingestion run", id));
  }

  /**
   * The failed records of a run.
   *
   * @param runId run
   * @return records by line
   */
  @Transactional(readOnly = true)
  public List<IngestionError> errors(Long runId) {
    return errors.findByRunIdOrderByLineNoAsc(runId);
  }

  /**
   * Uploads a list file (FR-SS-020 "Upload List File"): the run is logged at once with trigger
   * MANUAL_UPLOAD; its additions, updates and delistings wait for a checker.
   *
   * @param sourceCode source
   * @param fileName file name
   * @param content bytes
   * @return the run
   */
  public IngestionRun upload(String sourceCode, String fileName, byte[] content) {
    WatchlistSource source = acceptedSource(sourceCode, fileName);
    Attachment stored = store(source, fileName, content, "List file uploaded");
    return ingestion.ingest(
        source,
        IngestionTrigger.MANUAL_UPLOAD,
        new FeedFile(stored.getId(), fileName, content),
        true);
  }

  /**
   * Stages a file for the next scheduled run of the source (the default file-drop transport).
   *
   * @param sourceCode source
   * @param fileName file name
   * @param content bytes
   * @return the stored file
   */
  public Attachment stage(String sourceCode, String fileName, byte[] content) {
    return store(acceptedSource(sourceCode, fileName), fileName, content, "List file staged");
  }

  private WatchlistSource acceptedSource(String sourceCode, String fileName) {
    WatchlistSource source = watchlists.source(sourceCode);
    if (!source.accepts(fileName)) {
      throw new BusinessRuleException(
          "SCR_LIST_FILE_TYPE", "The file type is not allowed for source " + source.getCode());
    }
    return source;
  }

  private Attachment store(
      WatchlistSource source, String fileName, byte[] content, String description) {
    return attachments.upload(
        new AttachmentTarget(StagedFileWatchlistFeed.SOURCE_ENTITY, String.valueOf(source.getId())),
        fileName,
        content,
        description + " for " + source.getCode());
  }
}
