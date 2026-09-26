package com.iortatechnxt.brokerverse.screening.watchlist.api.dto;

import com.iortatechnxt.brokerverse.screening.watchlist.domain.ChangeStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.ChangeType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryValues;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistChange;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntry;
import java.time.Instant;
import java.util.function.Function;

/**
 * A watchlist change with its before and after values (SNSRP-203, 204).
 *
 * @param id id
 * @param entryId entry
 * @param externalRef entry reference
 * @param primaryName entry name
 * @param runId ingestion run that produced it, {@code null} for a manual change
 * @param changeType ADD, UPDATE or DEACTIVATE
 * @param before values before ({@code null} for an addition)
 * @param after values after
 * @param makerRemarks maker remarks
 * @param status PENDING, APPROVED or REJECTED
 * @param createdBy maker
 * @param createdAt made
 * @param decidedBy checker
 * @param decidedAt decided
 * @param decisionRemarks checker remarks
 */
public record ChangeDto(
    Long id,
    Long entryId,
    String externalRef,
    String primaryName,
    Long runId,
    ChangeType changeType,
    EntryValues before,
    EntryValues after,
    String makerRemarks,
    ChangeStatus status,
    String createdBy,
    Instant createdAt,
    String decidedBy,
    Instant decidedAt,
    String decisionRemarks) {

  /**
   * Maps a change.
   *
   * @param c change
   * @param e its entry
   * @param values JSON reader of the values
   * @return DTO
   */
  public static ChangeDto from(
      WatchlistChange c, WatchlistEntry e, Function<String, EntryValues> values) {
    return new ChangeDto(
        c.getId(),
        c.getEntryId(),
        e.getExternalRef(),
        e.getPrimaryName(),
        c.getRunId(),
        c.getChangeType(),
        values.apply(c.getBeforeValues()),
        values.apply(c.getAfterValues()),
        c.getMakerRemarks(),
        c.getStatus(),
        c.getCreatedBy(),
        c.getCreatedAt(),
        c.getDecidedBy(),
        c.getDecidedAt(),
        c.getDecisionRemarks());
  }
}
