package com.iortatechnxt.brokerverse.remittance.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.AssignRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.CommentRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.RemittanceDtos.FeedRunResponse;
import com.iortatechnxt.brokerverse.remittance.api.dto.RequestDtos.DecisionRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.RequestDtos.ExtendRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.RequestDtos.HoldCreateRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.RequestDtos.HoldResponse;
import com.iortatechnxt.brokerverse.remittance.api.dto.RequestDtos.HoldTermsRequest;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.HoldStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RequestSource;
import com.iortatechnxt.brokerverse.remittance.service.HoldService;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceQueryService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * Marketing hold requests (MKTID.002-007, RMTID.021/031): list, create, submit, approve, reject,
 * extend, cancel, release, assign and the Collection hold file upload ({@code COLLECTION_HOLD}).
 */
@RestController
@RequestMapping("/api/v1/remittance/holds")
public class HoldController {

  private final HoldService holds;
  private final RemittanceQueryService queries;
  private final FlowInService flowIn;

  /**
   * Creates the controller.
   *
   * @param holds hold actions
   * @param queries reads
   * @param flowIn feed uploads
   */
  public HoldController(HoldService holds, RemittanceQueryService queries, FlowInService flowIn) {
    this.holds = holds;
    this.queries = queries;
    this.flowIn = flowIn;
  }

  /**
   * Hold requests, newest first.
   *
   * @param companyId company
   * @param stage stages
   * @param q request, invoice or assured
   * @param page page
   * @param size size
   * @return requests
   */
  @GetMapping
  @PreAuthorize(RemittanceAccess.HOLD_READ)
  public PageResponse<HoldResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<HoldStage> stage,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.holds(companyId, stage, q, RemittanceAccess.newestFirst(page, size)),
        HoldResponse::from);
  }

  /**
   * One request.
   *
   * @param id request
   * @return request
   */
  @GetMapping("/{id}")
  @PreAuthorize(RemittanceAccess.HOLD_READ)
  public HoldResponse get(@PathVariable Long id) {
    return HoldResponse.from(holds.get(id));
  }

  /**
   * Creates a hold request, optionally submitting it (MKTID.003).
   *
   * @param request invoice and terms
   * @return request
   */
  @PostMapping
  @PreAuthorize(RemittanceAccess.HOLD_REQUEST)
  @ResponseStatus(HttpStatus.CREATED)
  public HoldResponse create(@Valid @RequestBody HoldCreateRequest request) {
    return HoldResponse.from(
        holds.create(
            request.companyId(),
            request.invoiceNo(),
            request.terms(),
            request.submit(),
            RequestSource.SCREEN));
  }

  /**
   * Changes a draft.
   *
   * @param id request
   * @param request terms
   * @return request
   */
  @PutMapping("/{id}")
  @PreAuthorize(RemittanceAccess.HOLD_REQUEST)
  public HoldResponse update(@PathVariable Long id, @Valid @RequestBody HoldTermsRequest request) {
    return HoldResponse.from(holds.update(id, request.terms()));
  }

  /**
   * Submits a draft.
   *
   * @param id request
   * @return request
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(RemittanceAccess.HOLD_REQUEST)
  public HoldResponse submit(@PathVariable Long id) {
    return HoldResponse.from(holds.submit(id));
  }

  /**
   * Cancels a draft.
   *
   * @param id request
   * @return request
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize(RemittanceAccess.HOLD_REQUEST)
  public HoldResponse cancel(@PathVariable Long id) {
    return HoldResponse.from(holds.cancel(id));
  }

  /**
   * Approves or rejects a hold request (MKTID.006).
   *
   * @param id request
   * @param request decision
   * @return request
   */
  @PostMapping("/{id}/decision")
  @PreAuthorize(RemittanceAccess.HOLD_APPROVE)
  public HoldResponse decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
    return HoldResponse.from(
        request.approve()
            ? holds.approve(id, request.comment())
            : holds.reject(id, request.comment()));
  }

  /**
   * Asks for an extension (MKTID.005).
   *
   * @param id request
   * @param request new date
   * @return request
   */
  @PostMapping("/{id}/extend")
  @PreAuthorize(RemittanceAccess.HOLD_REQUEST)
  public HoldResponse extend(@PathVariable Long id, @Valid @RequestBody ExtendRequest request) {
    return HoldResponse.from(holds.extend(id, request.holdUntil(), request.comment()));
  }

  /**
   * Approves or rejects an extension.
   *
   * @param id request
   * @param request decision
   * @return request
   */
  @PostMapping("/{id}/extension-decision")
  @PreAuthorize(RemittanceAccess.HOLD_APPROVE)
  public HoldResponse decideExtension(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
    return HoldResponse.from(holds.decideExtension(id, request.approve(), request.comment()));
  }

  /**
   * Asks to cancel an active hold (MKTID.005).
   *
   * @param id request
   * @param request reason
   * @return request
   */
  @PostMapping("/{id}/request-cancel")
  @PreAuthorize(RemittanceAccess.HOLD_REQUEST)
  public HoldResponse requestCancel(
      @PathVariable Long id, @Valid @RequestBody CommentRequest request) {
    return HoldResponse.from(holds.requestCancel(id, request.comment()));
  }

  /**
   * Approves or rejects a cancellation (MKTID.006).
   *
   * @param id request
   * @param request decision
   * @return request
   */
  @PostMapping("/{id}/cancel-decision")
  @PreAuthorize(RemittanceAccess.HOLD_APPROVE)
  public HoldResponse decideCancel(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
    return HoldResponse.from(holds.decideCancel(id, request.approve(), request.comment()));
  }

  /**
   * Releases an active hold (MKTID.002).
   *
   * @param id request
   * @param request reason
   * @return request
   */
  @PostMapping("/{id}/release")
  @PreAuthorize(RemittanceAccess.HOLD_REQUEST)
  public HoldResponse release(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
    return HoldResponse.from(holds.release(id, request.comment()));
  }

  /**
   * Assigns an approved hold to a processor (MKTID.004).
   *
   * @param id request
   * @param request processor
   * @return request
   */
  @PostMapping("/{id}/assign")
  @PreAuthorize(RemittanceAccess.HOLD_APPROVE)
  public HoldResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
    return HoldResponse.from(holds.assign(id, request.username().strip()));
  }

  /**
   * Uploads a Collection hold file (COLLECTION_HOLD: invoiceNo, reasonCode, holdUntil, remarks).
   *
   * @param file file
   * @return the run
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(RemittanceAccess.HOLD_REQUEST)
  public FeedRunResponse upload(@RequestParam MultipartFile file) throws IOException {
    return FeedRunResponse.from(
        flowIn.upload(
            "COLLECTION_HOLD", new FlowInFile(file.getOriginalFilename(), file.getBytes())));
  }
}
