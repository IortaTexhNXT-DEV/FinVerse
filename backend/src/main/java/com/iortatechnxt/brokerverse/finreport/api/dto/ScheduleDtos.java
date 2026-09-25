package com.iortatechnxt.brokerverse.finreport.api.dto;

import com.iortatechnxt.brokerverse.finreport.domain.ScheduleColumn;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleDefinition;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Basis;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Comparative;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Grouping;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.LayoutStatus;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Measure;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.ScheduleFamily;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.SelectorKind;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Side;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleValues;
import com.iortatechnxt.brokerverse.finreport.domain.StatementComment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Request and response bodies of the account schedule endpoints (FRBS 3.2.0). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class ScheduleDtos {

  private ScheduleDtos() {}

  /**
   * A figure of a schedule.
   *
   * @param measure figure
   * @param label heading
   */
  public record ColumnBody(@NotNull Measure measure, @NotBlank @Size(max = 60) String label) {}

  /**
   * A schedule definition to add or change.
   *
   * @param code code (ignored on change)
   * @param name title
   * @param family Appendix A family
   * @param sourceRef appendix row or report list number
   * @param description description
   * @param selectorKind prefixes or report groups
   * @param accountSelector comma-separated entries
   * @param grouping rows
   * @param currency currency, blank for every currency
   * @param side side shown positive
   * @param basis balance or movement
   * @param ageingSlots ageing bounds, blank without ageing
   * @param comparative comparative period
   * @param commentary commentary column
   * @param boardDocument board-deck document
   * @param layoutStatus layout confirmed or to confirm
   * @param active offered
   * @param columns figures in order
   */
  public record ScheduleBody(
      @Size(max = 40) String code,
      @NotBlank @Size(max = 200) String name,
      @NotNull ScheduleFamily family,
      @Size(max = 80) String sourceRef,
      @Size(max = 500) String description,
      @NotNull SelectorKind selectorKind,
      @NotBlank @Size(max = 500) String accountSelector,
      @NotNull Grouping grouping,
      @Size(max = 3) String currency,
      @NotNull Side side,
      @NotNull Basis basis,
      @Size(max = 60) String ageingSlots,
      Comparative comparative,
      boolean commentary,
      boolean boardDocument,
      LayoutStatus layoutStatus,
      boolean active,
      @NotNull @Size(min = 1, max = 8) List<ColumnBody> columns) {

    /**
     * The content.
     *
     * @return values
     */
    public ScheduleValues values() {
      List<ScheduleColumn> cols = new java.util.ArrayList<>();
      for (int i = 0; i < columns.size(); i++) {
        ColumnBody c = columns.get(i);
        cols.add(new ScheduleColumn((i + 1) * 10, c.measure(), c.label().trim()));
      }
      return new ScheduleValues(
          name,
          family,
          sourceRef,
          description,
          selectorKind,
          accountSelector,
          grouping,
          currency,
          side,
          basis,
          ageingSlots,
          comparative,
          commentary,
          boardDocument,
          layoutStatus,
          active,
          cols);
    }
  }

  /**
   * A schedule definition.
   *
   * @param code code
   * @param values content
   * @param wordOutput whether the schedule is also exported to Word (board decks, client
   *     requirement 16)
   * @param updatedAt last change
   */
  public record ScheduleResponse(
      String code, ScheduleValues values, boolean wordOutput, Instant updatedAt) {

    /**
     * Maps a definition.
     *
     * @param d definition
     * @return response
     */
    public static ScheduleResponse from(ScheduleDefinition d) {
      return new ScheduleResponse(
          d.getCode(),
          d.values(),
          d.values().boardDocument(),
          d.getUpdatedAt() == null ? d.getCreatedAt() : d.getUpdatedAt());
    }
  }

  /**
   * A comment to keep; a blank text removes it.
   *
   * @param companyId company
   * @param period month {@code yyyy-MM}
   * @param rowKey row (account, party or cost centre code)
   * @param text comment
   */
  public record CommentBody(
      @NotNull Long companyId,
      @NotBlank String period,
      @NotBlank @Size(max = 120) String rowKey,
      @Size(max = 1000) String text) {}

  /**
   * A comment.
   *
   * @param rowKey row
   * @param period month
   * @param text comment
   * @param by last changed by
   * @param at last changed at
   */
  public record CommentResponse(String rowKey, String period, String text, String by, Instant at) {

    /**
     * Maps a comment.
     *
     * @param c comment
     * @return response
     */
    public static CommentResponse from(StatementComment c) {
      return new CommentResponse(
          c.getRowKey(),
          c.getPeriod(),
          c.getText(),
          c.getUpdatedBy() == null ? c.getCreatedBy() : c.getUpdatedBy(),
          c.getUpdatedAt() == null ? c.getCreatedAt() : c.getUpdatedAt());
    }
  }
}
