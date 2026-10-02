package com.iortatechnxt.brokerverse.opsledger.api.dto;

import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInFeed;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRecord;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request and response records of the flow-in API (BRQID.004/005). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class FlowInDtos {

  private FlowInDtos() {}

  /**
   * A feed.
   *
   * @param code code
   * @param name name
   * @param partnerSystem partner system
   * @param direction direction
   * @param transport transport
   * @param ownerModule owning module
   * @param cron schedule, "-" for manual
   * @param active active
   * @param description description
   * @param uploadable whether a module processes uploads of the feed
   */
  public record FeedResponse(
      String code,
      String name,
      String partnerSystem,
      FlowInEnums.Direction direction,
      FlowInEnums.Transport transport,
      String ownerModule,
      String cron,
      boolean active,
      String description,
      boolean uploadable) {

    /**
     * Maps a feed.
     *
     * @param f feed
     * @param uploadable whether a handler is installed
     * @return response
     */
    public static FeedResponse from(FlowInFeed f, boolean uploadable) {
      return new FeedResponse(
          f.getCode(),
          f.getName(),
          f.getPartnerSystem(),
          f.getDirection(),
          f.getTransport(),
          f.getOwnerModule(),
          f.getCron(),
          f.isActive(),
          f.getDescription(),
          uploadable);
    }
  }

  /**
   * A run.
   *
   * @param id id
   * @param feedCode feed
   * @param runNo run number
   * @param trigger trigger
   * @param status status
   * @param startedAt start
   * @param endedAt end
   * @param readCount records read
   * @param okCount accepted
   * @param duplicateCount duplicates skipped
   * @param failedCount failed
   * @param fileName uploaded file
   * @param message summary
   * @param errorDetail error of a broken run
   */
  public record RunResponse(
      Long id,
      String feedCode,
      String runNo,
      FlowInEnums.Trigger trigger,
      FlowInEnums.RunStatus status,
      Instant startedAt,
      Instant endedAt,
      int readCount,
      int okCount,
      int duplicateCount,
      int failedCount,
      String fileName,
      String message,
      String errorDetail) {

    /**
     * Maps a run.
     *
     * @param r run
     * @return response
     */
    public static RunResponse from(FlowInRun r) {
      return new RunResponse(
          r.getId(),
          r.getFeedCode(),
          r.getRunNo(),
          r.getTrigger(),
          r.getStatus(),
          r.getStartedAt(),
          r.getEndedAt(),
          r.getReadCount(),
          r.getOkCount(),
          r.getDuplicateCount(),
          r.getFailedCount(),
          r.getFileName(),
          r.getMessage(),
          r.getErrorDetail());
    }
  }

  /**
   * A record.
   *
   * @param id id
   * @param idempotencyKey key
   * @param status status
   * @param reference record created
   * @param message message
   * @param receivedAt last attempt
   */
  public record RecordResponse(
      Long id,
      String idempotencyKey,
      FlowInEnums.RecordStatus status,
      String reference,
      String message,
      Instant receivedAt) {

    /**
     * Maps a record.
     *
     * @param r record
     * @return response
     */
    public static RecordResponse from(FlowInRecord r) {
      return new RecordResponse(
          r.getId(),
          r.getIdempotencyKey(),
          r.getStatus(),
          r.getReference(),
          r.getMessage(),
          r.getReceivedAt());
    }
  }

  /**
   * Schedule and activation of a feed.
   *
   * @param cron Spring cron (6 fields, UTC) or "-" for manual only
   * @param active whether runs are accepted
   */
  public record FeedConfigRequest(@Size(max = 60) String cron, @NotNull Boolean active) {}

  /**
   * A replay of booked invoices into the ledger: one invoice, one account, or a whole company.
   *
   * @param companyId company (when neither invoice nor account is given)
   * @param arn account, optional
   * @param invoiceNo invoice, optional
   */
  public record ReplayRequest(
      Long companyId, @Size(max = 30) String arn, @Size(max = 40) String invoiceNo) {}
}
