package com.iortatechnxt.brokerverse.screening.watchlist.api.dto;

import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionRun;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionTrigger;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.RunStatus;
import java.time.Instant;
import java.util.Map;

/**
 * An ingestion run log line (SNSRP-201).
 *
 * @param id id
 * @param runNo run number
 * @param sourceCode source
 * @param trigger SCHEDULED, MANUAL_UPLOAD or API
 * @param fileName file read
 * @param fileAttachmentId stored file
 * @param received records read
 * @param added added
 * @param updated updated
 * @param delisted delisted
 * @param unchanged unchanged
 * @param failed failed
 * @param pendingApproval whether the changes wait for a checker
 * @param status status
 * @param error reason for failure
 * @param startedAt started
 * @param endedAt ended
 * @param createdBy who started it
 */
public record RunDto(
    Long id,
    String runNo,
    String sourceCode,
    IngestionTrigger trigger,
    String fileName,
    Long fileAttachmentId,
    int received,
    int added,
    int updated,
    int delisted,
    int unchanged,
    int failed,
    boolean pendingApproval,
    RunStatus status,
    String error,
    Instant startedAt,
    Instant endedAt,
    String createdBy) {

  /**
   * Maps a run.
   *
   * @param r run
   * @param sourceCodes source codes by id
   * @return DTO
   */
  public static RunDto from(IngestionRun r, Map<Long, String> sourceCodes) {
    return new RunDto(
        r.getId(),
        r.getRunNo(),
        sourceCodes.get(r.getSourceId()),
        r.getTrigger(),
        r.getFileName(),
        r.getFileAttachmentId(),
        r.getReceived(),
        r.getAdded(),
        r.getUpdated(),
        r.getDelisted(),
        r.getUnchanged(),
        r.getFailed(),
        r.isPendingApproval(),
        r.getStatus(),
        r.getError(),
        r.getStartedAt(),
        r.getEndedAt(),
        r.getCreatedBy());
  }
}
