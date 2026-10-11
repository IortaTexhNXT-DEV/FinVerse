package com.iortatechnxt.brokerverse.adjustment.api;

import com.iortatechnxt.brokerverse.adjustment.api.dto.BatchInput;
import com.iortatechnxt.brokerverse.adjustment.api.dto.BatchResponse;
import com.iortatechnxt.brokerverse.adjustment.api.dto.ReturnInput;
import com.iortatechnxt.brokerverse.adjustment.api.dto.ReturnResult;
import com.iortatechnxt.brokerverse.adjustment.api.dto.WriteOffResponse;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentQueryService;
import com.iortatechnxt.brokerverse.adjustment.service.PostingBatchService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import jakarta.validation.Valid;
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
 * Posting batches (ADJID.005/006): return requests that do not qualify, post the selected requests,
 * the batch history; and the minimal balance write-offs (ADJID.026).
 */
@RestController
@RequestMapping("/api/v1/adjustment")
public class PostingBatchController {

  private final PostingBatchService batches;
  private final AdjustmentQueryService queries;

  /**
   * Creates the controller.
   *
   * @param batches posting and returns
   * @param queries reads
   */
  public PostingBatchController(PostingBatchService batches, AdjustmentQueryService queries) {
    this.batches = batches;
    this.queries = queries;
  }

  /**
   * Posts the selected requests as one batch.
   *
   * @param input company, requests and remarks
   * @return the batch with the outcome of each request
   */
  @PostMapping("/batches")
  @PreAuthorize(AdjustmentAccess.POST)
  public BatchResponse post(@Valid @RequestBody BatchInput input) {
    return BatchResponse.from(batches.post(input.companyId(), input.ids(), input.remarks()));
  }

  /**
   * Returns requests to their requesters with a reason.
   *
   * @param input requests, reason and comment
   * @return number returned
   */
  @PostMapping("/batches/return")
  @PreAuthorize(AdjustmentAccess.RETURN)
  public ReturnResult returnRequests(@Valid @RequestBody ReturnInput input) {
    return new ReturnResult(
        batches.returnRequests(input.ids(), input.reasonCode(), input.comment()));
  }

  /**
   * Posting batches of a company.
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return batches, newest first
   */
  @GetMapping("/batches")
  @PreAuthorize(AdjustmentAccess.VIEW)
  public PageResponse<BatchResponse> list(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(queries.batches(companyId, pageOf(page, size)), BatchResponse::summary);
  }

  /**
   * One batch with its lines.
   *
   * @param batchNo batch number
   * @return batch
   */
  @GetMapping("/batches/{batchNo}")
  @PreAuthorize(AdjustmentAccess.VIEW)
  public BatchResponse get(@PathVariable String batchNo) {
    return BatchResponse.from(batches.get(batchNo));
  }

  /**
   * Minimal balance write-offs and credits (ADJID.026).
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return write-offs, newest first
   */
  @GetMapping("/write-offs")
  @PreAuthorize(AdjustmentAccess.VIEW)
  public PageResponse<WriteOffResponse> writeOffs(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.writeOffs(companyId, pageOf(page, size)), WriteOffResponse::from);
  }

  private static PageRequest pageOf(int page, int size) {
    return PageRequest.of(
        Math.max(page, 0),
        Math.min(Math.max(size, 1), AdjustmentAccess.MAX_PAGE),
        Sort.by(Sort.Direction.DESC, "id"));
  }
}
