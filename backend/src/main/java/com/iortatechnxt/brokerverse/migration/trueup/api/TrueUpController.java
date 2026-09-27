package com.iortatechnxt.brokerverse.migration.trueup.api;

import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.migration.trueup.api.dto.TrueUpResponse;
import com.iortatechnxt.brokerverse.migration.trueup.domain.MigTrueup;
import com.iortatechnxt.brokerverse.migration.trueup.service.TrueUpService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The opening-balance adjustments of the year-end cut-over (FY2027 true-ups, DATA_MIGRATION_DESIGN
 * 17.7): list, prepare, submit, approve or return, reconcile and sign.
 */
@RestController
@RequestMapping("/api/v1/migration/trueups")
@Transactional
public class TrueUpController {

  private static final String VIEW = "hasAuthority('MIG_VIEW')";
  private static final String PREPARE = "hasAuthority('MIG_TRUEUP_PREPARE')";
  private static final String APPROVE = "hasAuthority('MIG_TRUEUP_APPROVE')";

  private final TrueUpService trueups;
  private final MigBatchRepository batches;

  /**
   * Creates the controller.
   *
   * @param trueups true-ups
   * @param batches batches (numbers)
   */
  public TrueUpController(TrueUpService trueups, MigBatchRepository batches) {
    this.trueups = trueups;
    this.batches = batches;
  }

  /**
   * The true-ups of a company.
   *
   * @param companyId company
   * @return true-ups
   */
  @GetMapping
  @PreAuthorize(VIEW)
  @Transactional(readOnly = true)
  public List<TrueUpResponse> list(@RequestParam Long companyId) {
    return trueups.list(companyId).stream().map(this::response).toList();
  }

  /**
   * Prepares a true-up.
   *
   * @param companyId company
   * @param body number, as-of date and batches
   * @return the true-up
   */
  @PostMapping
  @PreAuthorize(PREPARE)
  public TrueUpResponse prepare(
      @RequestParam Long companyId, @Valid @RequestBody PrepareBody body) {
    return response(
        trueups.prepare(
            companyId,
            new TrueUpService.Prepare(
                body.trueupNo(), body.asOf(), body.batchNo(), body.tbBatchNo())));
  }

  /**
   * Submits a true-up for approval.
   *
   * @param reference MIG-TU-n
   * @param body remarks
   * @return the true-up
   */
  @PostMapping("/{reference}/submit")
  @PreAuthorize(PREPARE)
  public TrueUpResponse submit(@PathVariable String reference, @RequestBody NoteBody body) {
    return response(trueups.submit(reference, body.note()));
  }

  /**
   * Approves or returns a true-up.
   *
   * @param reference MIG-TU-n
   * @param body decision and remarks
   * @return the true-up
   */
  @PostMapping("/{reference}/decide")
  @PreAuthorize(APPROVE)
  public TrueUpResponse decide(@PathVariable String reference, @RequestBody DecisionBody body) {
    return response(trueups.decide(reference, body.approve(), body.note()));
  }

  /**
   * Reconciles a posted true-up.
   *
   * @param reference MIG-TU-n
   * @return the true-up
   */
  @PostMapping("/{reference}/reconcile")
  @PreAuthorize(PREPARE + " or " + APPROVE)
  public TrueUpResponse reconcile(@PathVariable String reference) {
    return response(trueups.reconcile(reference));
  }

  /**
   * Signs a reconciled true-up.
   *
   * @param reference MIG-TU-n
   * @return the true-up
   */
  @PostMapping("/{reference}/sign")
  @PreAuthorize(APPROVE)
  public TrueUpResponse sign(@PathVariable String reference) {
    return response(trueups.sign(reference));
  }

  private TrueUpResponse response(MigTrueup t) {
    return TrueUpResponse.from(t, batchNo(t.getBatchId()), batchNo(t.getTbBatchId()));
  }

  private String batchNo(Long id) {
    return id == null ? null : batches.findById(id).map(MigBatch::getBatchNo).orElse(null);
  }

  /**
   * A true-up to prepare.
   *
   * @param trueupNo 1, 2, 3 or F
   * @param asOf as-of date of the legacy trial balance
   * @param batchNo G03 batch
   * @param tbBatchNo G01 batch of the legacy trial balance, optional
   */
  public record PrepareBody(
      @NotBlank @Size(max = 2) String trueupNo,
      @NotNull LocalDate asOf,
      @NotBlank String batchNo,
      String tbBatchNo) {}

  /**
   * Remarks.
   *
   * @param note remarks
   */
  public record NoteBody(@Size(max = 1000) String note) {}

  /**
   * A decision.
   *
   * @param approve approve or return
   * @param note remarks (reason of a return)
   */
  public record DecisionBody(boolean approve, @Size(max = 1000) String note) {}
}
