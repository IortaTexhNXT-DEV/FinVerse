package com.iortatechnxt.brokerverse.nbadmin.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessBatchDecisionResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessBatchResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessBatchSubmitRequest;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessRequestResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.DecisionRequest;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessBatchService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bulk access request batches (BRD 1.009; FR-UA-019). The file is uploaded through the bulk
 * framework (handler {@code UAM_ACCESS_REQUEST}); its valid rows become the draft lines of a batch
 * that is submitted, approved, rejected, returned or cancelled here.
 */
@RestController
@RequestMapping("/api/v1/nbadmin/access-batches")
public class AccessBatchController {

  private static final int PAGE_SIZE = 25;
  private static final String DECIDE = "hasAnyAuthority('ACCESS_APPROVE', 'UAM_SECOND_APPROVE')";

  private final AccessBatchService batches;

  /**
   * Creates the controller.
   *
   * @param batches bulk batches
   */
  public AccessBatchController(AccessBatchService batches) {
    this.batches = batches;
  }

  /**
   * Batches of the current user (all with ACCESS_APPROVE, USER_MANAGE or AUDIT_VIEW), newest first.
   *
   * @param page page
   * @return batches
   */
  @GetMapping
  @PreAuthorize(AccessRequestController.VIEW)
  public PageResponse<AccessBatchResponse> list(@RequestParam(defaultValue = "0") int page) {
    return PageResponse.of(
        batches.list(PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"))),
        AccessBatchResponse::from);
  }

  /**
   * One batch.
   *
   * @param id batch
   * @return batch
   */
  @GetMapping("/{id}")
  @PreAuthorize(AccessRequestController.VIEW)
  public AccessBatchResponse get(@PathVariable Long id) {
    return AccessBatchResponse.from(batches.get(id));
  }

  /**
   * The line requests of a batch.
   *
   * @param id batch
   * @return lines in row order
   */
  @GetMapping("/{id}/lines")
  @PreAuthorize(AccessRequestController.VIEW)
  public List<AccessRequestResponse> lines(@PathVariable Long id) {
    return batches.lines(id).stream().map(AccessRequestResponse::from).toList();
  }

  /**
   * Submits the draft lines to the chosen approver (requester).
   *
   * @param id batch
   * @param body approver and remarks
   * @return the batch
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(AccessRequestController.REQUEST)
  public AccessBatchResponse submit(
      @PathVariable Long id, @Valid @RequestBody AccessBatchSubmitRequest body) {
    return AccessBatchResponse.from(batches.submit(id, body.approver(), body.remarks()));
  }

  /**
   * Approves the pending lines, each in its own transaction.
   *
   * @param id batch
   * @param body optional comment
   * @return every line with its outcome and the temporary passwords of created users
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(DECIDE)
  public AccessBatchDecisionResponse approve(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
    return AccessBatchDecisionResponse.from(batches.approve(id, body.comment()));
  }

  /**
   * Rejects the pending lines.
   *
   * @param id batch
   * @param body reason
   * @return the batch
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(DECIDE)
  public AccessBatchResponse reject(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
    return AccessBatchResponse.from(batches.reject(id, body.comment()));
  }

  /**
   * Returns the pending lines to the requester.
   *
   * @param id batch
   * @param body remarks
   * @return the batch
   */
  @PostMapping("/{id}/return")
  @PreAuthorize(DECIDE)
  public AccessBatchResponse returnBatch(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
    return AccessBatchResponse.from(batches.returnBatch(id, body.comment()));
  }

  /**
   * Cancels the open lines (requester).
   *
   * @param id batch
   * @param body reason
   * @return the batch
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAnyAuthority('ACCESS_REQUEST', 'UAM_CANCEL')")
  public AccessBatchResponse cancel(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
    return AccessBatchResponse.from(batches.cancel(id, body.comment()));
  }
}
