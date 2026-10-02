package com.iortatechnxt.brokerverse.remittance.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.CommentRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.DeductionDtos.ApplicationResponse;
import com.iortatechnxt.brokerverse.remittance.api.dto.DeductionDtos.DeductionRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.DeductionDtos.DeductionResponse;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction.DeductionStage;
import com.iortatechnxt.brokerverse.remittance.service.DeductionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Remittance deductions on insurer confirmation (ACSL 2.9.2): list, view, prepare, change, submit,
 * confirm, the consumptions by batches and the deductions waiting for the next batch of an insurer.
 * Cancel (draft) and return (for confirmation) are generic workflow actions.
 */
@RestController
@RequestMapping("/api/v1/remittance/deductions")
public class DeductionController {

  private final DeductionService deductions;

  /**
   * Creates the controller.
   *
   * @param deductions deduction actions and reads
   */
  public DeductionController(DeductionService deductions) {
    this.deductions = deductions;
  }

  /**
   * Deductions, newest first.
   *
   * @param companyId company
   * @param stage stages
   * @param insurer insurer
   * @param q number, source reference or invoice
   * @param page page
   * @param size size
   * @return deductions
   */
  @GetMapping
  @PreAuthorize(RemittanceAccess.DEDUCTION_READ)
  public PageResponse<DeductionResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<DeductionStage> stage,
      @RequestParam(required = false) String insurer,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        deductions.search(companyId, stage, insurer, q, RemittanceAccess.newestFirst(page, size)),
        DeductionResponse::from);
  }

  /**
   * One deduction.
   *
   * @param id deduction
   * @return deduction
   */
  @GetMapping("/{id}")
  @PreAuthorize(RemittanceAccess.DEDUCTION_READ)
  public DeductionResponse get(@PathVariable Long id) {
    return DeductionResponse.from(deductions.get(id));
  }

  /**
   * The batches that consumed a deduction.
   *
   * @param id deduction
   * @return consumptions in order
   */
  @GetMapping("/{id}/applications")
  @PreAuthorize(RemittanceAccess.DEDUCTION_READ)
  public List<ApplicationResponse> applications(@PathVariable Long id) {
    return deductions.applications(id).stream().map(ApplicationResponse::from).toList();
  }

  /**
   * The deductions a batch consumed.
   *
   * @param batchId batch
   * @return consumptions in order
   */
  @GetMapping("/by-batch/{batchId}")
  @PreAuthorize(RemittanceAccess.BATCH_READ)
  public List<ApplicationResponse> ofBatch(@PathVariable Long batchId) {
    return deductions.applicationsOfBatch(batchId).stream().map(ApplicationResponse::from).toList();
  }

  /**
   * Confirmed deductions waiting for the next batch of an insurer and currency.
   *
   * @param companyId company
   * @param insurer insurer
   * @param currency currency
   * @return deductions in consumption order
   */
  @GetMapping("/pending")
  @PreAuthorize(RemittanceAccess.BATCH_READ)
  public List<DeductionResponse> pending(
      @RequestParam Long companyId, @RequestParam String insurer, @RequestParam String currency) {
    return deductions.pending(companyId, insurer, currency).stream()
        .map(DeductionResponse::from)
        .toList();
  }

  /**
   * Prepares a deduction.
   *
   * @param request terms
   * @return deduction
   */
  @PostMapping
  @PreAuthorize(RemittanceAccess.DEDUCTION_PREPARE)
  @ResponseStatus(HttpStatus.CREATED)
  public DeductionResponse create(@Valid @RequestBody DeductionRequest request) {
    if (request.companyId() == null) {
      throw new BusinessRuleException("COMPANY_REQUIRED", "Select the company");
    }
    return DeductionResponse.from(deductions.create(request.companyId(), request.terms()));
  }

  /**
   * Changes a draft.
   *
   * @param id deduction
   * @param request terms
   * @return deduction
   */
  @PutMapping("/{id}")
  @PreAuthorize(RemittanceAccess.DEDUCTION_PREPARE)
  public DeductionResponse update(
      @PathVariable Long id, @Valid @RequestBody DeductionRequest request) {
    return DeductionResponse.from(deductions.update(id, request.terms()));
  }

  /**
   * Submits a draft with the insurer's confirmation.
   *
   * @param id deduction
   * @param request comment
   * @return deduction
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(RemittanceAccess.DEDUCTION_PREPARE)
  public DeductionResponse submit(
      @PathVariable Long id, @Valid @RequestBody(required = false) CommentRequest request) {
    return DeductionResponse.from(deductions.submit(id, comment(request)));
  }

  /**
   * Confirms a submitted deduction (four eyes).
   *
   * @param id deduction
   * @param request comment
   * @return deduction
   */
  @PostMapping("/{id}/confirm")
  @PreAuthorize(RemittanceAccess.DEDUCTION_CONFIRM)
  public DeductionResponse confirm(
      @PathVariable Long id, @Valid @RequestBody(required = false) CommentRequest request) {
    return DeductionResponse.from(deductions.confirm(id, comment(request)));
  }

  private static String comment(CommentRequest request) {
    return request == null ? null : request.comment();
  }
}
