package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRecord;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLineRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Uploads of insurer OR schedules and their exception report (RMTID.012/013/016): the file runs as
 * feed {@code INSURER_REMIT_OR} (each record through {@link InsurerOrService} in its own
 * transaction), then the lines it updated and the records it refused are read back.
 */
@Service
public class InsurerOrUploads {

  private static final int MAX_RECORDS = 1000;
  private static final int RECENT_RUNS = 200;

  private final FlowInService flowIn;
  private final BatchLineRepository lines;

  /**
   * Creates the service.
   *
   * @param flowIn feed runs
   * @param lines batch lines
   */
  public InsurerOrUploads(FlowInService flowIn, BatchLineRepository lines) {
    this.flowIn = flowIn;
    this.lines = lines;
  }

  /**
   * Uploads a schedule (outside any transaction: each record commits on its own).
   *
   * @param file uploaded file
   * @return the run and its exception report
   */
  public UploadResult upload(FlowInFile file) {
    return result(flowIn.upload(InsurerOrService.FEED, file));
  }

  /**
   * Upload runs, newest first.
   *
   * @param pageable page
   * @return runs
   */
  public Page<FlowInRun> runs(Pageable pageable) {
    return flowIn.runs(InsurerOrService.FEED, pageable);
  }

  /**
   * The exception report of a recent upload run.
   *
   * @param runId run
   * @return run and exception report
   */
  public UploadResult result(Long runId) {
    return flowIn.runs(InsurerOrService.FEED, PageRequest.of(0, RECENT_RUNS)).stream()
        .filter(r -> r.getId().equals(runId))
        .findFirst()
        .map(this::result)
        .orElseThrow(() -> new ResourceNotFoundException("Insurer OR upload", runId));
  }

  private UploadResult result(FlowInRun run) {
    return new UploadResult(
        run,
        lines.findByOrRunNo(run.getRunNo()),
        flowIn.records(run.getId(), PageRequest.of(0, MAX_RECORDS)).getContent());
  }

  /**
   * An upload and its outcome.
   *
   * @param run feed run
   * @param updated lines updated, with their exception status
   * @param records every record of the run (failed ones carry the reason)
   */
  public record UploadResult(FlowInRun run, List<BatchLine> updated, List<FlowInRecord> records) {

    /** Defensive copies. */
    public UploadResult {
      updated = List.copyOf(updated);
      records = List.copyOf(records);
    }
  }
}
