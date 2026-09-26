package com.iortatechnxt.brokerverse.collections.files.api.dto;

import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile.Frequency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/** Requests and responses of the Collections files (BRCLXN.024-029, 045). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class FileDtos {

  private FileDtos() {}

  /**
   * A published file; download it through the report archive ({@code
   * /api/v1/reports/runs/{reportRunId}/file}) from its availability time.
   *
   * @param id id
   * @param frequency DAILY, WEEKLY, MONTHLY or ON_REQUEST
   * @param reportCode report
   * @param periodKey period key
   * @param periodFrom first day
   * @param periodTo last day
   * @param scope unit and branch (weekly), requestor (export)
   * @param reportRunId archived run with the file
   * @param rowCount rows
   * @param availableFrom when it may be downloaded, null = at once
   * @param available whether it may be downloaded now
   * @param status PUBLISHED or FAILED
   * @param message error of a failed file
   * @param createdAt published at
   * @param createdBy published by
   */
  public record FileResponse(
      Long id,
      String frequency,
      String reportCode,
      String periodKey,
      LocalDate periodFrom,
      LocalDate periodTo,
      String scope,
      Long reportRunId,
      int rowCount,
      Instant availableFrom,
      boolean available,
      String status,
      String message,
      Instant createdAt,
      String createdBy) {

    /**
     * Maps a file.
     *
     * @param f file
     * @param now current time
     * @return response
     */
    public static FileResponse from(ScheduledFile f, Instant now) {
      return new FileResponse(
          f.getId(),
          f.getFrequency().name(),
          f.getReportCode(),
          f.getPeriodKey(),
          f.getPeriodFrom(),
          f.getPeriodTo(),
          f.getScope(),
          f.getReportRunId(),
          f.getRowCount(),
          f.getAvailableFrom(),
          f.getReportRunId() != null
              && (f.getAvailableFrom() == null || !f.getAvailableFrom().isAfter(now)),
          f.getStatus().name(),
          f.getMessage(),
          f.getCreatedAt(),
          f.getCreatedBy());
    }
  }

  /**
   * Runs a file job now for a date (Collections Setup).
   *
   * @param frequency DAILY, WEEKLY or MONTHLY
   * @param date business date
   */
  public record GenerateRequest(@NotNull Frequency frequency, @NotNull LocalDate date) {}

  /**
   * Exports the Outstanding PR List of a segment and unit.
   *
   * @param segment segment, blank for all
   * @param salesUnit unit, blank for all
   */
  public record ExportRequest(@Size(max = 40) String segment, @Size(max = 20) String salesUnit) {}
}
