package com.iortatechnxt.brokerverse.migration.intake.api.dto;

import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtract;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * An extract received from a legacy system (FR-DM-008).
 *
 * @param extractNo number
 * @param objectCode object
 * @param layoutCode layout
 * @param layoutVersion layout version
 * @param sourceSystem source system
 * @param asOf as-of date and time
 * @param sequenceNo sequence of the day
 * @param mode FULL or DELTA
 * @param fileName data file
 * @param controlFileName control file
 * @param dataFileId stored data file
 * @param controlFileId stored control file
 * @param sha256 file hash
 * @param declaredRows rows declared in the control file
 * @param parsedRows rows read
 * @param stagedRows rows staged
 * @param hashTotal hash total
 * @param masked masked on intake
 * @param status status
 * @param rejectCode rejection reason code
 * @param rejectMessage rejection message, one line per failed check
 * @param rejectChecks every failed check with its reason (empty unless rejected)
 * @param receivedBy receiver
 * @param receivedAt time
 * @param purgedAt purge time of the staged rows
 */
public record ExtractResponse(
    String extractNo,
    String objectCode,
    String layoutCode,
    int layoutVersion,
    String sourceSystem,
    LocalDateTime asOf,
    int sequenceNo,
    String mode,
    String fileName,
    String controlFileName,
    Long dataFileId,
    Long controlFileId,
    String sha256,
    Integer declaredRows,
    int parsedRows,
    int stagedRows,
    String hashTotal,
    boolean masked,
    String status,
    String rejectCode,
    String rejectMessage,
    List<RejectCheck> rejectChecks,
    String receivedBy,
    Instant receivedAt,
    Instant purgedAt) {

  private static final String SEPARATOR = ": ";

  /**
   * Maps an extract.
   *
   * @param e extract
   * @return response
   */
  public static ExtractResponse from(MigExtract e) {
    return new ExtractResponse(
        e.getExtractNo(),
        e.getObjectCode(),
        e.getLayoutCode(),
        e.getLayoutVersion(),
        e.getSourceSystem(),
        e.getAsOf(),
        e.getSequenceNo(),
        e.getMode(),
        e.getFileName(),
        e.getControlFileName(),
        e.getStoredFileId(),
        e.getControlFileId(),
        e.getSha256(),
        e.getDeclaredRows(),
        e.getParsedRows(),
        e.getStagedRows(),
        e.getHashTotal(),
        e.isMasked(),
        e.getStatus().name(),
        e.getRejectCode(),
        e.getRejectMessage(),
        rejectChecks(e.getRejectMessage()),
        e.getReceivedBy(),
        e.getReceivedAt(),
        e.getPurgedAt());
  }

  /**
   * A failed intake check of a rejected extract.
   *
   * @param check check name (Checksum, Columns, Row count, Amount total, Hash total...)
   * @param reason the difference found
   */
  public record RejectCheck(String check, String reason) {}

  private static List<RejectCheck> rejectChecks(String message) {
    if (message == null || message.isBlank()) {
      return List.of();
    }
    return message
        .lines()
        .filter(line -> !line.isBlank())
        .map(
            line -> {
              int at = line.indexOf(SEPARATOR);
              return at < 0
                  ? new RejectCheck("Intake check", line)
                  : new RejectCheck(line.substring(0, at), line.substring(at + SEPARATOR.length()));
            })
        .toList();
  }
}
