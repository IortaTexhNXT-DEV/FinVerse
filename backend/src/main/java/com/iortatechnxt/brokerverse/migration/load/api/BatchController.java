package com.iortatechnxt.brokerverse.migration.load.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtract;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtractRepository;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigIssueRepository;
import com.iortatechnxt.brokerverse.migration.intake.domain.RowStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRowRepository;
import com.iortatechnxt.brokerverse.migration.load.api.dto.BatchDtos.BatchResponse;
import com.iortatechnxt.brokerverse.migration.load.api.dto.BatchDtos.DecideRequest;
import com.iortatechnxt.brokerverse.migration.load.api.dto.BatchDtos.IssueResponse;
import com.iortatechnxt.brokerverse.migration.load.api.dto.BatchDtos.LogResponse;
import com.iortatechnxt.brokerverse.migration.load.api.dto.BatchDtos.PlanRequest;
import com.iortatechnxt.brokerverse.migration.load.api.dto.BatchDtos.ResolveRequest;
import com.iortatechnxt.brokerverse.migration.load.api.dto.BatchDtos.ResubmissionResponse;
import com.iortatechnxt.brokerverse.migration.load.api.dto.BatchDtos.ResubmitRequest;
import com.iortatechnxt.brokerverse.migration.load.api.dto.BatchDtos.RowResponse;
import com.iortatechnxt.brokerverse.migration.load.api.dto.BatchDtos.RowsRequest;
import com.iortatechnxt.brokerverse.migration.load.api.dto.BatchDtos.SignRequest;
import com.iortatechnxt.brokerverse.migration.load.api.dto.BatchDtos.SignoffResponse;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchLogRepository;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.migration.load.service.BatchPlanService;
import com.iortatechnxt.brokerverse.migration.load.service.LoadRunner;
import com.iortatechnxt.brokerverse.migration.load.service.RejectsFile;
import com.iortatechnxt.brokerverse.migration.load.service.ResubmissionService;
import com.iortatechnxt.brokerverse.migration.load.service.RollbackService;
import com.iortatechnxt.brokerverse.migration.load.service.ValidationService;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoffRepository;
import com.iortatechnxt.brokerverse.migration.signoff.service.SignoffService;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Batches (FR-DM-013 to FR-DM-015, FR-DM-003; screen Batches): plan, validate, resolve issues, sign
 * the validation, approve and run the load, rerun, roll back, download the rejects, sign the
 * reconciliation and accept the object, and the resubmissions of corrected rows.
 */
@RestController
@RequestMapping("/api/v1/migration")
@Transactional
public class BatchController {

  private static final String VIEW = "hasAuthority('MIG_VIEW')";
  private static final String RUN = "hasAuthority('MIG_LOAD_RUN')";
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final int MAX_PAGE = 500;

  private final BatchPlanService plans;
  private final ValidationService validation;
  private final LoadRunner runner;
  private final RollbackService rollbacks;
  private final ResubmissionService resubmissions;
  private final SignoffService signoffs;
  private final RejectsFile rejects;
  private final MigBatchRepository batches;
  private final MigExtractRepository extracts;
  private final StageRowRepository rows;
  private final MigIssueRepository issues;
  private final BatchLogRepository logs;
  private final MigSignoffRepository signoffRows;
  private final FileDownloads downloads;
  private final TransactionTemplate readTx;

  /**
   * Creates the controller.
   *
   * @param plans batches
   * @param validation validation
   * @param runner load runner
   * @param rollbacks rollbacks
   * @param resubmissions resubmissions
   * @param signoffs gates
   * @param rejects rejection file
   * @param batches batch repository
   * @param extracts extracts
   * @param rows staged rows
   * @param issues issues
   * @param logs run log
   * @param signoffRows sign-offs
   * @param downloads file answers
   * @param txManager transactions
   */
  @SuppressWarnings("java:S107") // constructor injection
  public BatchController(
      BatchPlanService plans,
      ValidationService validation,
      LoadRunner runner,
      RollbackService rollbacks,
      ResubmissionService resubmissions,
      SignoffService signoffs,
      RejectsFile rejects,
      MigBatchRepository batches,
      MigExtractRepository extracts,
      StageRowRepository rows,
      MigIssueRepository issues,
      BatchLogRepository logs,
      MigSignoffRepository signoffRows,
      FileDownloads downloads,
      PlatformTransactionManager txManager) {
    this.plans = plans;
    this.validation = validation;
    this.runner = runner;
    this.rollbacks = rollbacks;
    this.resubmissions = resubmissions;
    this.signoffs = signoffs;
    this.rejects = rejects;
    this.batches = batches;
    this.extracts = extracts;
    this.rows = rows;
    this.issues = issues;
    this.logs = logs;
    this.signoffRows = signoffRows;
    this.downloads = downloads;
    this.readTx = new TransactionTemplate(txManager);
    this.readTx.setReadOnly(true);
  }

  /**
   * Batches of a company.
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return batches
   */
  @GetMapping("/batches")
  @PreAuthorize(VIEW)
  public PageResponse<BatchResponse> list(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return PageResponse.of(
        plans.search(companyId, PageRequest.of(page, Math.min(size, MAX_PAGE))), this::map);
  }

  /**
   * A batch.
   *
   * @param batchNo batch
   * @return batch
   */
  @GetMapping("/batches/{batchNo}")
  @PreAuthorize(VIEW)
  public BatchResponse get(@PathVariable String batchNo) {
    return map(plans.get(batchNo));
  }

  private BatchResponse map(MigBatch b) {
    String parent =
        b.getParentBatchId() == null
            ? null
            : batches.findById(b.getParentBatchId()).map(MigBatch::getBatchNo).orElse(null);
    List<String> nos =
        extracts.findAllById(b.getExtractIds()).stream()
            .map(MigExtract::getExtractNo)
            .sorted()
            .toList();
    return BatchResponse.from(b, parent, nos);
  }

  /**
   * Plans a batch.
   *
   * @param companyId company
   * @param request object and extracts
   * @return batch
   */
  @PostMapping("/batches")
  @PreAuthorize(RUN)
  public BatchResponse plan(@RequestParam Long companyId, @Valid @RequestBody PlanRequest request) {
    return map(plans.plan(companyId, request.objectCode(), request.extractNos()));
  }

  /**
   * The run log of a batch.
   *
   * @param batchNo batch
   * @return log lines
   */
  @GetMapping("/batches/{batchNo}/log")
  @PreAuthorize(VIEW)
  public List<LogResponse> log(@PathVariable String batchNo) {
    return logs.findByBatchIdOrderByIdAsc(plans.get(batchNo).getId()).stream()
        .map(LogResponse::from)
        .toList();
  }

  /**
   * Rows of a batch.
   *
   * @param batchNo batch
   * @param status status filter
   * @param page page
   * @param size size
   * @return rows
   */
  @GetMapping("/batches/{batchNo}/rows")
  @PreAuthorize(VIEW)
  public PageResponse<RowResponse> rows(
      @PathVariable String batchNo,
      @RequestParam(required = false) RowStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    Long id = plans.get(batchNo).getId();
    PageRequest p = PageRequest.of(page, Math.min(size, MAX_PAGE));
    return PageResponse.of(
        status == null
            ? rows.findByBatchIdOrderByIdAsc(id, p)
            : rows.findByBatchIdAndStatusOrderByIdAsc(id, status, p),
        RowResponse::from);
  }

  /**
   * Issues of a batch.
   *
   * @param batchNo batch
   * @param page page
   * @param size size
   * @return issues
   */
  @GetMapping("/batches/{batchNo}/issues")
  @PreAuthorize(VIEW)
  public PageResponse<IssueResponse> issues(
      @PathVariable String batchNo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return PageResponse.of(
        issues.findByBatchIdOrderByIdAsc(
            plans.get(batchNo).getId(), PageRequest.of(page, Math.min(size, MAX_PAGE))),
        IssueResponse::from);
  }

  /**
   * Validates a batch.
   *
   * @param batchNo batch
   * @return batch
   */
  @PostMapping("/batches/{batchNo}/validate")
  @PreAuthorize(RUN)
  public BatchResponse validate(@PathVariable String batchNo) {
    return map(validation.validate(batchNo));
  }

  /**
   * Waives the errors of rows (data owner).
   *
   * @param batchNo batch
   * @param request rows and reason
   * @return batch
   */
  @PostMapping("/batches/{batchNo}/waive")
  @PreAuthorize("hasAuthority('MIG_DQ_WAIVE')")
  public BatchResponse waive(
      @PathVariable String batchNo, @Valid @RequestBody RowsRequest request) {
    return map(validation.waive(batchNo, request.rowIds(), request.reason(), request.note()));
  }

  /**
   * Excludes rows with a manual-entry plan (data owner).
   *
   * @param batchNo batch
   * @param request rows, reason and plan
   * @return batch
   */
  @PostMapping("/batches/{batchNo}/exclude")
  @PreAuthorize("hasAuthority('MIG_DQ_WAIVE')")
  public BatchResponse exclude(
      @PathVariable String batchNo, @Valid @RequestBody RowsRequest request) {
    return map(validation.exclude(batchNo, request.rowIds(), request.reason(), request.note()));
  }

  /**
   * Records the resolution of an issue (Data Steward).
   *
   * @param issueId issue
   * @param request resolution
   * @return issue
   */
  @PostMapping("/issues/{issueId}/resolve")
  @PreAuthorize("hasAuthority('MIG_DQ_RESOLVE')")
  public IssueResponse resolve(@PathVariable Long issueId, @RequestBody ResolveRequest request) {
    return IssueResponse.from(validation.resolve(issueId, request.resolution(), request.note()));
  }

  /**
   * Signs the validation (G3).
   *
   * @param batchNo batch
   * @param request decision
   * @return sign-off
   */
  @PostMapping("/batches/{batchNo}/signoff/validation")
  @PreAuthorize("hasAuthority('MIG_DQ_RESOLVE')")
  public SignoffResponse signValidation(
      @PathVariable String batchNo, @RequestBody SignRequest request) {
    return SignoffResponse.from(
        signoffs.signValidation(batchNo, request.approve(), request.comment()));
  }

  /**
   * Approves the load (G4).
   *
   * @param batchNo batch
   * @param request comment
   * @return batch
   */
  @PostMapping("/batches/{batchNo}/approve-load")
  @PreAuthorize("hasAuthority('MIG_LOAD_APPROVE')")
  public BatchResponse approveLoad(@PathVariable String batchNo, @RequestBody SignRequest request) {
    return map(signoffs.approveLoad(batchNo, request.comment()));
  }

  /**
   * Loads an approved batch and reconciles it.
   *
   * @param batchNo batch
   * @return batch
   */
  @PostMapping("/batches/{batchNo}/load")
  @PreAuthorize(RUN)
  @Transactional(propagation = Propagation.NEVER)
  public BatchResponse load(@PathVariable String batchNo) {
    runner.load(batchNo);
    return readTx.execute(status -> map(plans.get(batchNo)));
  }

  /**
   * Creates a rerun batch of the rejected rows.
   *
   * @param batchNo batch
   * @return rerun batch
   */
  @PostMapping("/batches/{batchNo}/rerun")
  @PreAuthorize(RUN)
  public BatchResponse rerun(@PathVariable String batchNo) {
    return map(plans.rerun(batchNo, null));
  }

  /**
   * Requests a rollback.
   *
   * @param batchNo batch
   * @param request reason
   * @return batch
   */
  @PostMapping("/batches/{batchNo}/rollback")
  @PreAuthorize("hasAuthority('MIG_ROLLBACK_REQUEST')")
  public BatchResponse requestRollback(
      @PathVariable String batchNo, @Valid @RequestBody ReasonRequest request) {
    return map(rollbacks.request(batchNo, request.reason()));
  }

  /**
   * Approves a rollback.
   *
   * @param batchNo batch
   * @param request comment
   * @return batch
   */
  @PostMapping("/batches/{batchNo}/rollback/approve")
  @PreAuthorize("hasAuthority('MIG_ROLLBACK_APPROVE')")
  public BatchResponse approveRollback(
      @PathVariable String batchNo, @RequestBody SignRequest request) {
    return map(rollbacks.approve(batchNo, request.comment()));
  }

  /**
   * Rejects a rollback.
   *
   * @param batchNo batch
   * @param request reason
   * @return batch
   */
  @PostMapping("/batches/{batchNo}/rollback/reject")
  @PreAuthorize("hasAuthority('MIG_ROLLBACK_APPROVE')")
  public BatchResponse rejectRollback(
      @PathVariable String batchNo, @Valid @RequestBody ReasonRequest request) {
    return map(rollbacks.reject(batchNo, request.reason()));
  }

  /**
   * Signs the reconciliation (G5).
   *
   * @param batchNo batch
   * @param request decision
   * @return sign-off
   */
  @PostMapping("/batches/{batchNo}/signoff/reconciliation")
  @PreAuthorize("hasAuthority('MIG_RECON_SIGNOFF')")
  public SignoffResponse signReconciliation(
      @PathVariable String batchNo, @RequestBody SignRequest request) {
    return SignoffResponse.from(
        signoffs.signReconciliation(batchNo, request.approve(), request.comment()));
  }

  /**
   * Accepts the object (G6) as data owner or Data Migration Lead.
   *
   * @param batchNo batch
   * @param request role and decision
   * @return sign-off
   */
  @PostMapping("/batches/{batchNo}/signoff/acceptance")
  @PreAuthorize("hasAuthority('MIG_SIGNOFF')")
  public SignoffResponse signAcceptance(
      @PathVariable String batchNo, @RequestBody SignRequest request) {
    return SignoffResponse.from(
        signoffs.signAcceptance(batchNo, request.role(), request.approve(), request.comment()));
  }

  /**
   * The sign-offs of a batch.
   *
   * @param batchNo batch
   * @return sign-offs
   */
  @GetMapping("/batches/{batchNo}/signoffs")
  @PreAuthorize(VIEW)
  public List<SignoffResponse> signoffs(@PathVariable String batchNo) {
    return signoffRows.findByBatchIdOrderByIdAsc(plans.get(batchNo).getId()).stream()
        .map(SignoffResponse::from)
        .toList();
  }

  /**
   * The rejection file of a batch.
   *
   * @param batchNo batch
   * @param request HTTP request
   * @return XLSX
   */
  @GetMapping("/batches/{batchNo}/rejects")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> rejects(@PathVariable String batchNo, HttpServletRequest request) {
    MigBatch b = plans.get(batchNo);
    return downloads.respond(
        FileDownload.inline(batchNo + "_rejects.xlsx", XLSX, rejects.workbook(b)), request);
  }

  /**
   * Prepares a resubmission of corrected rows (maker).
   *
   * @param batchNo parent batch
   * @param request corrected extract
   * @return resubmission
   */
  @PostMapping("/batches/{batchNo}/resubmissions")
  @PreAuthorize("hasAnyAuthority('MIG_DQ_RESOLVE', 'MIG_INTAKE')")
  public ResubmissionResponse resubmit(
      @PathVariable String batchNo, @Valid @RequestBody ResubmitRequest request) {
    return ResubmissionResponse.from(resubmissions.prepare(batchNo, request.extractNo()));
  }

  /**
   * Resubmissions of a company.
   *
   * @param companyId company
   * @return resubmissions
   */
  @GetMapping("/resubmissions")
  @PreAuthorize(VIEW)
  public List<ResubmissionResponse> resubmissions(@RequestParam Long companyId) {
    return resubmissions.list(companyId).stream().map(ResubmissionResponse::from).toList();
  }

  /**
   * The checker approves or returns a resubmission.
   *
   * @param resubmissionNo resubmission
   * @param request decision
   * @return resubmission
   */
  @PostMapping("/resubmissions/{resubmissionNo}/decide")
  @PreAuthorize("hasAuthority('MIG_RESUBMIT_APPROVE')")
  public ResubmissionResponse decide(
      @PathVariable String resubmissionNo, @RequestBody DecideRequest request) {
    return ResubmissionResponse.from(
        resubmissions.decide(resubmissionNo, request.approve(), request.note()));
  }
}
