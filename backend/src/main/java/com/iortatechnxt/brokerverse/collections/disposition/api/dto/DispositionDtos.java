package com.iortatechnxt.brokerverse.collections.disposition.api.dto;

import com.iortatechnxt.brokerverse.collections.common.service.LovAttributes.DispositionRule;
import com.iortatechnxt.brokerverse.collections.disposition.domain.Effort;
import com.iortatechnxt.brokerverse.collections.disposition.domain.PrDisposition;
import com.iortatechnxt.brokerverse.collections.disposition.service.EffortService.EffortCommand;
import com.iortatechnxt.brokerverse.collections.disposition.service.PrDispositionService.DispositionCommand;
import com.iortatechnxt.brokerverse.collections.feed.domain.OutboxItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Requests and responses of the collector's work on accounts (BRCLXN.016-023, 051). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class DispositionDtos {

  private static final int REMARKS = 1000;

  private DispositionDtos() {}

  /**
   * A disposition on one or several accounts.
   *
   * @param invoiceNos accounts
   * @param code LOV CLX_PR_DISPOSITION code
   * @param remarks remarks
   * @param effectiveOn date it applies from, null for today
   * @param details hand-off details (pickupDate, pickupAddress, contactPerson, checkNo, checkBank,
   *     amount; path, certificateNo, periodFrom, periodTo)
   */
  public record DispositionRequest(
      @NotEmpty @Size(max = 500) List<String> invoiceNos,
      @NotBlank @Size(max = 40) String code,
      @Size(max = REMARKS) String remarks,
      LocalDate effectiveOn,
      Map<String, String> details) {

    /**
     * The command.
     *
     * @return command
     */
    public DispositionCommand toCommand() {
      return new DispositionCommand(invoiceNos, code.strip(), remarks, effectiveOn, details);
    }
  }

  /**
   * An effort on one or several accounts.
   *
   * @param invoiceNos accounts
   * @param code LOV CLX_EFFORT_CODE code
   * @param at when it was made, null for now
   * @param channel channel
   * @param contactPerson person contacted
   * @param remarks remarks
   */
  public record EffortRequest(
      @NotEmpty @Size(max = 500) List<String> invoiceNos,
      @NotBlank @Size(max = 40) String code,
      Instant at,
      @Size(max = 30) String channel,
      @Size(max = 120) String contactPerson,
      @Size(max = REMARKS) String remarks) {

    /**
     * The command.
     *
     * @return command
     */
    public EffortCommand toCommand() {
      return new EffortCommand(invoiceNos, code.strip(), at, channel, contactPerson, remarks);
    }
  }

  /**
   * Remarks and tagging category of an account.
   *
   * @param remarks remarks
   * @param category A, B or C
   */
  public record DetailsRequest(
      @Size(max = REMARKS) String remarks, @Size(max = 1) String category) {}

  /**
   * A disposition (BRCLXN.019-021: who encoded it and when).
   *
   * @param id id
   * @param code code
   * @param category category
   * @param taggingOwner owner
   * @param opsAction Operations hand-off
   * @param remarks remarks
   * @param effectiveOn effective date
   * @param details hand-off details (JSON)
   * @param source USER, BULK or INBOX
   * @param outboxId hand-off item
   * @param supersededBy later disposition
   * @param bulkRef bulk reference
   * @param createdAt encoded at
   * @param createdBy encoded by
   */
  public record DispositionResponse(
      Long id,
      String code,
      String category,
      String taggingOwner,
      String opsAction,
      String remarks,
      LocalDate effectiveOn,
      String details,
      String source,
      Long outboxId,
      Long supersededBy,
      String bulkRef,
      Instant createdAt,
      String createdBy) {

    /**
     * Maps a disposition.
     *
     * @param d disposition
     * @return response
     */
    public static DispositionResponse from(PrDisposition d) {
      return new DispositionResponse(
          d.getId(),
          d.getDispositionCode(),
          d.getCategory(),
          d.getTaggingOwner() == null ? null : d.getTaggingOwner().name(),
          d.getOpsAction().name(),
          d.getRemarks(),
          d.getEffectiveOn(),
          d.getPayload(),
          d.getSource().name(),
          d.getOutboxId(),
          d.getSupersededBy(),
          d.getBulkRef(),
          d.getCreatedAt(),
          d.getCreatedBy());
    }
  }

  /**
   * An effort.
   *
   * @param id id
   * @param code code
   * @param at when
   * @param channel channel
   * @param contactPerson contact
   * @param remarks remarks
   * @param bulkRef bulk reference
   * @param createdBy encoded by
   */
  public record EffortResponse(
      Long id,
      String code,
      Instant at,
      String channel,
      String contactPerson,
      String remarks,
      String bulkRef,
      String createdBy) {

    /**
     * Maps an effort.
     *
     * @param e effort
     * @return response
     */
    public static EffortResponse from(Effort e) {
      return new EffortResponse(
          e.getId(),
          e.getEffortCode(),
          e.getEffortAt(),
          e.getChannel(),
          e.getContactPerson(),
          e.getRemarks(),
          e.getBulkRef(),
          e.getCreatedBy());
    }
  }

  /**
   * A hand-off to Operations (outbox item).
   *
   * @param id id
   * @param feedCode feed
   * @param key idempotency key
   * @param status PENDING, TAKEN or CANCELLED
   * @param fields fields sent
   * @param createdAt queued at
   * @param takenAt taken at
   */
  public record HandoffResponse(
      Long id,
      String feedCode,
      String key,
      String status,
      Map<String, String> fields,
      Instant createdAt,
      Instant takenAt) {

    /**
     * Maps an outbox item.
     *
     * @param o item
     * @param fields its fields
     * @return response
     */
    public static HandoffResponse from(OutboxItem o, Map<String, String> fields) {
      return new HandoffResponse(
          o.getId(),
          o.getFeedCode(),
          o.getIdempotencyKey(),
          o.getStatus().name(),
          fields,
          o.getCreatedAt(),
          o.getTakenAt());
    }
  }

  /**
   * A PR collector disposition and its rule (BRCLXN.016).
   *
   * @param code code
   * @param label label
   * @param category tagging category, null when not set
   * @param taggingOwner owner after the disposition, null when not set
   * @param opsAction Operations hand-off (NONE, CWT2307_REVERSAL, DP_REVERSAL, CHECK_PICKUP,
   *     CANCEL_REQUEST)
   * @param allowedRoles roles allowed to set it; empty = every user with CLX_WORK
   */
  public record DispositionRuleResponse(
      String code,
      String label,
      String category,
      String taggingOwner,
      String opsAction,
      List<String> allowedRoles) {

    /** Defensive copy. */
    public DispositionRuleResponse {
      allowedRoles = List.copyOf(allowedRoles);
    }

    /**
     * Maps a rule.
     *
     * @param label value label
     * @param r rule
     * @return response
     */
    public static DispositionRuleResponse from(String label, DispositionRule r) {
      return new DispositionRuleResponse(
          r.code(),
          label,
          r.category(),
          r.taggingOwner() == null ? null : r.taggingOwner().name(),
          r.opsAction().name(),
          r.allowedRoles().stream().sorted().toList());
    }
  }
}
