package com.iortatechnxt.brokerverse.brokerclaims.diary.api.dto;

import com.iortatechnxt.brokerverse.brokerclaims.diary.domain.DiaryEntry;
import com.iortatechnxt.brokerverse.brokerclaims.diary.service.DiaryService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.function.UnaryOperator;

/** Requests and responses of the claims diary (FR-CL-052). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class DiaryDtos {

  private DiaryDtos() {}

  /**
   * Add Diary Entry.
   *
   * @param entryType type ({@code BCL_DIARY_TYPE})
   * @param entryDate date (default today)
   * @param dueDate due date
   * @param assignee assignee (default the author)
   * @param text text
   */
  public record DiaryRequest(
      String entryType, LocalDate entryDate, LocalDate dueDate, String assignee, String text) {

    /**
     * The service input.
     *
     * @return input
     */
    public DiaryService.DiaryInput toInput() {
      return new DiaryService.DiaryInput(entryType, entryDate, dueDate, assignee, text);
    }
  }

  /**
   * Mark Done.
   *
   * @param remark remark
   */
  public record DoneRequest(String remark) {}

  /**
   * A diary entry of a claim.
   *
   * @param id entry
   * @param claimId claim
   * @param entryType type
   * @param typeLabel type label
   * @param entryDate date
   * @param dueDate due date
   * @param assignee assignee
   * @param text text
   * @param doneAt completion time
   * @param doneBy completed by
   * @param doneRemark completion remark
   * @param createdBy author
   * @param createdAt time recorded
   */
  public record DiaryEntryResponse(
      Long id,
      Long claimId,
      String entryType,
      String typeLabel,
      LocalDate entryDate,
      LocalDate dueDate,
      String assignee,
      String text,
      Instant doneAt,
      String doneBy,
      String doneRemark,
      String createdBy,
      Instant createdAt) {

    /**
     * Maps an entry.
     *
     * @param e entry
     * @param labels type label lookup
     * @return response
     */
    public static DiaryEntryResponse from(DiaryEntry e, UnaryOperator<String> labels) {
      return new DiaryEntryResponse(
          e.getId(),
          e.getClaimId(),
          e.getEntryType(),
          labels.apply(e.getEntryType()),
          e.getEntryDate(),
          e.getDueDate(),
          e.getAssignee(),
          e.getText(),
          e.getDoneAt(),
          e.getDoneBy(),
          e.getDoneRemark(),
          e.getCreatedBy(),
          e.getCreatedAt());
    }
  }
}
