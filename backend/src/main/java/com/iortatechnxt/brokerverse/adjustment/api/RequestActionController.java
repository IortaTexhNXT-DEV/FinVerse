package com.iortatechnxt.brokerverse.adjustment.api;

import com.iortatechnxt.brokerverse.adjustment.api.dto.CommentInput;
import com.iortatechnxt.brokerverse.adjustment.api.dto.QuotationLinkInput;
import com.iortatechnxt.brokerverse.adjustment.api.dto.RequestResponse;
import com.iortatechnxt.brokerverse.adjustment.domain.BatchOutcome;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.PostingBatch;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentPostingService;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentQueryService;
import com.iortatechnxt.brokerverse.adjustment.service.EndorsementRequestService;
import com.iortatechnxt.brokerverse.adjustment.service.PostingBatchService;
import com.iortatechnxt.brokerverse.adjustment.service.RequestWorkflowService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Business actions of an endorsement request (OPERATIONS_DESIGN section 7): submit, resubmit,
 * validate, approve (four eyes), post (a batch of one), re-apply payments and link the quotation of
 * a TSI increase. Returns and cancellations are generic workflow actions.
 */
@RestController
@RequestMapping("/api/v1/adjustment/requests/{id}")
public class RequestActionController {

  private final RequestWorkflowService workflow;
  private final EndorsementRequestService requests;
  private final AdjustmentPostingService posting;
  private final PostingBatchService batches;
  private final AdjustmentQueryService queries;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param workflow steps before posting
   * @param requests quotation link
   * @param posting re-application
   * @param batches posting
   * @param queries reads
   * @param clock clock (aging)
   */
  public RequestActionController(
      RequestWorkflowService workflow,
      EndorsementRequestService requests,
      AdjustmentPostingService posting,
      PostingBatchService batches,
      AdjustmentQueryService queries,
      Clock clock) {
    this.workflow = workflow;
    this.requests = requests;
    this.posting = posting;
    this.batches = batches;
    this.queries = queries;
    this.clock = clock;
  }

  /**
   * Submits a draft for validation.
   *
   * @param id request
   * @param input comment
   * @return request
   */
  @PostMapping("/submit")
  @PreAuthorize(AdjustmentAccess.REQUEST)
  public RequestResponse submit(@PathVariable Long id, @Valid @RequestBody CommentInput input) {
    workflow.submit(id, input.comment());
    return view(id);
  }

  /**
   * Resubmits a returned request.
   *
   * @param id request
   * @param input comment
   * @return request
   */
  @PostMapping("/resubmit")
  @PreAuthorize(AdjustmentAccess.REQUEST)
  public RequestResponse resubmit(@PathVariable Long id, @Valid @RequestBody CommentInput input) {
    workflow.resubmit(id, input.comment());
    return view(id);
  }

  /**
   * Validates a request.
   *
   * @param id request
   * @param input comment
   * @return request
   */
  @PostMapping("/validate")
  @PreAuthorize(AdjustmentAccess.PROCESS)
  public RequestResponse validate(@PathVariable Long id, @Valid @RequestBody CommentInput input) {
    workflow.validate(id, input.comment());
    return view(id);
  }

  /**
   * Approves a request.
   *
   * @param id request
   * @param input comment
   * @return request
   */
  @PostMapping("/approve")
  @PreAuthorize(AdjustmentAccess.APPROVE)
  public RequestResponse approve(@PathVariable Long id, @Valid @RequestBody CommentInput input) {
    workflow.approve(id, input.comment());
    return view(id);
  }

  /**
   * Posts one request as a batch of one.
   *
   * @param id request
   * @param input remarks
   * @return request
   */
  @PostMapping("/post")
  @PreAuthorize(AdjustmentAccess.POST)
  public RequestResponse post(@PathVariable Long id, @Valid @RequestBody CommentInput input) {
    EndorsementRequest request = queries.get(id);
    PostingBatch batch = batches.post(request.getCompanyId(), List.of(id), input.comment());
    PostingBatch.Line line = batch.getLines().get(0);
    if (line.outcome() == BatchOutcome.FAILED) {
      throw new BusinessRuleException("ADJ_POSTING_FAILED", line.message());
    }
    return view(id);
  }

  /**
   * Re-applies the payments of a posted request.
   *
   * @param id request
   * @return request
   */
  @PostMapping("/reapply")
  @PreAuthorize(AdjustmentAccess.POST)
  public RequestResponse reapply(@PathVariable Long id) {
    posting.reapply(id);
    return view(id);
  }

  /**
   * Links the quotation of a TSI increase (ADJID.008).
   *
   * @param id request
   * @param input quotation number
   * @return request
   */
  @PostMapping("/quotation")
  @PreAuthorize(AdjustmentAccess.REQUEST)
  public RequestResponse linkQuotation(
      @PathVariable Long id, @Valid @RequestBody QuotationLinkInput input) {
    requests.linkQuotation(id, input.quotationRef());
    return view(id);
  }

  private RequestResponse view(Long id) {
    return RequestResponse.from(queries.get(id), clock.instant());
  }
}
