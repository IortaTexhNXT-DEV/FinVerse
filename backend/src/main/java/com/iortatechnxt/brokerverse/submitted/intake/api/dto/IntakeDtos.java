package com.iortatechnxt.brokerverse.submitted.intake.api.dto;

import com.iortatechnxt.brokerverse.submitted.domain.SbmIntakeRun;
import java.time.Instant;

/** Records of the intake API (FR-SP-001 to 006). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class IntakeDtos {

  private IntakeDtos() {}

  /**
   * An intake run.
   *
   * @param id id
   * @param runNo number
   * @param sourceCode source
   * @param bulkJobNo upload
   * @param fileName file
   * @param received rows received
   * @param created records created
   * @param updated records updated
   * @param duplicate duplicates
   * @param failed rows refused
   * @param status status
   * @param startedAt start
   * @param finishedAt end
   * @param processingRunNo processing run started after the intake
   */
  public record IntakeRunView(
      Long id,
      String runNo,
      String sourceCode,
      String bulkJobNo,
      String fileName,
      int received,
      int created,
      int updated,
      int duplicate,
      int failed,
      String status,
      Instant startedAt,
      Instant finishedAt,
      String processingRunNo) {

    /**
     * Maps a run.
     *
     * @param r run
     * @return view
     */
    public static IntakeRunView from(SbmIntakeRun r) {
      return new IntakeRunView(
          r.getId(),
          r.getRunNo(),
          r.getSourceCode(),
          r.getBulkJobNo(),
          r.getFileName(),
          r.getReceived(),
          r.getCreated(),
          r.getUpdated(),
          r.getDuplicate(),
          r.getFailed(),
          r.getStatus(),
          r.getStartedAt(),
          r.getFinishedAt(),
          r.getProcessingRunNo());
    }
  }

  /**
   * The work counts of the module home.
   *
   * @param iaafForApproval IAAFs waiting for approval
   * @param iaafApproved IAAFs approved, not yet sent
   * @param torForApproval TORs waiting for approval
   * @param renewalsPending hand-offs not taken by Renewal
   * @param lettersFailed letters refused
   * @param feesBilled handling fees billed, not tagged
   * @param feesTagged handling fees tagged, not applied
   */
  public record HomeCounts(
      long iaafForApproval,
      long iaafApproved,
      long torForApproval,
      long renewalsPending,
      long lettersFailed,
      long feesBilled,
      long feesTagged) {}
}
