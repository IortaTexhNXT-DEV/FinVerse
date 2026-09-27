package com.iortatechnxt.brokerverse.migration.recon.api;

import com.iortatechnxt.brokerverse.migration.load.service.BatchPlanService;
import com.iortatechnxt.brokerverse.migration.recon.api.dto.ReconDtos.ExplainRequest;
import com.iortatechnxt.brokerverse.migration.recon.api.dto.ReconDtos.LineResponse;
import com.iortatechnxt.brokerverse.migration.recon.api.dto.ReconDtos.RunResponse;
import com.iortatechnxt.brokerverse.migration.recon.domain.MigReconRun;
import com.iortatechnxt.brokerverse.migration.recon.service.ReconciliationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reconciliation (FR-DM-019 to FR-DM-021; screen Reconciliation): runs the reconciliation of a
 * loaded batch, shows its lines by level and records the explanation and approval of breaks.
 */
@RestController
@RequestMapping("/api/v1/migration")
public class ReconController {

  private final ReconciliationService recon;
  private final BatchPlanService plans;

  /**
   * Creates the controller.
   *
   * @param recon reconciliation
   * @param plans batches
   */
  public ReconController(ReconciliationService recon, BatchPlanService plans) {
    this.recon = recon;
    this.plans = plans;
  }

  /**
   * The latest reconciliation of a batch.
   *
   * @param batchNo batch
   * @return the run, or 204 when the batch was not reconciled yet
   */
  @GetMapping("/batches/{batchNo}/reconciliation")
  @PreAuthorize("hasAuthority('MIG_VIEW')")
  public ResponseEntity<RunResponse> latest(@PathVariable String batchNo) {
    return recon
        .latest(plans.get(batchNo).getId())
        .map(r -> ResponseEntity.ok(RunResponse.from(r, recon.lines(r.getId()))))
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  /**
   * Reconciles a loaded batch again.
   *
   * @param batchNo batch
   * @return the run
   */
  @PostMapping("/batches/{batchNo}/reconcile")
  @PreAuthorize("hasAuthority('MIG_LOAD_RUN')")
  public RunResponse reconcile(@PathVariable String batchNo) {
    MigReconRun r = recon.reconcile(batchNo);
    return RunResponse.from(r, recon.lines(r.getId()));
  }

  /**
   * Explains a break.
   *
   * @param lineId line
   * @param request reason and text
   * @return the line
   */
  @PostMapping("/recon-lines/{lineId}/explain")
  @PreAuthorize("hasAnyAuthority('MIG_DQ_RESOLVE', 'MIG_LOAD_RUN')")
  public LineResponse explain(
      @PathVariable Long lineId, @Valid @RequestBody ExplainRequest request) {
    return LineResponse.from(recon.explain(lineId, request.reason(), request.text()));
  }

  /**
   * Approves the explanation of a break.
   *
   * @param lineId line
   * @return the line
   */
  @PostMapping("/recon-lines/{lineId}/approve")
  @PreAuthorize("hasAuthority('MIG_RECON_SIGNOFF')")
  public LineResponse approve(@PathVariable Long lineId) {
    return LineResponse.from(recon.approve(lineId));
  }
}
