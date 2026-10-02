package com.iortatechnxt.brokerverse.remittance.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.CommentRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.BatchDtos.ReturnRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.RemittanceDtos.FeedRunResponse;
import com.iortatechnxt.brokerverse.remittance.api.dto.RequestDtos.SpecialCreateRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.RequestDtos.SpecialResponse;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RequestSource;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.SpecialStage;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceQueryService;
import com.iortatechnxt.brokerverse.remittance.service.SpecialRemittanceService;
import com.iortatechnxt.brokerverse.remittance.service.SpecialRemittanceService.NewRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Special remittance requests (MKTID.009, RMTID.030/033): list, request (validated at once),
 * approve into a special batch, reject, and the Collection file upload ({@code
 * COLLECTION_SPECIAL_REMIT}).
 */
@RestController
@RequestMapping("/api/v1/remittance/special")
public class SpecialRemittanceController {

  private final SpecialRemittanceService specials;
  private final RemittanceQueryService queries;
  private final FlowInService flowIn;

  /**
   * Creates the controller.
   *
   * @param specials special remittance actions
   * @param queries reads
   * @param flowIn feed uploads
   */
  public SpecialRemittanceController(
      SpecialRemittanceService specials, RemittanceQueryService queries, FlowInService flowIn) {
    this.specials = specials;
    this.queries = queries;
    this.flowIn = flowIn;
  }

  /**
   * Requests, newest first (RMTID.030).
   *
   * @param companyId company
   * @param stage stages
   * @param q request, invoice or assured
   * @param page page
   * @param size size
   * @return requests
   */
  @GetMapping
  @PreAuthorize(RemittanceAccess.SPECIAL_READ)
  public PageResponse<SpecialResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<SpecialStage> stage,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.specials(companyId, stage, q, RemittanceAccess.newestFirst(page, size)),
        SpecialResponse::from);
  }

  /**
   * One request.
   *
   * @param id request
   * @return request
   */
  @GetMapping("/{id}")
  @PreAuthorize(RemittanceAccess.SPECIAL_READ)
  public SpecialResponse get(@PathVariable Long id) {
    return SpecialResponse.from(specials.get(id));
  }

  /**
   * Requests a special remittance (MKTID.009).
   *
   * @param request invoice and condition
   * @return the validated request
   */
  @PostMapping
  @PreAuthorize(RemittanceAccess.SPECIAL_REQUEST)
  @ResponseStatus(HttpStatus.CREATED)
  public SpecialResponse create(@Valid @RequestBody SpecialCreateRequest request) {
    return SpecialResponse.from(
        specials.request(
            request.companyId(),
            new NewRequest(request.invoiceNo(), request.conditionCode(), request.remarks()),
            RequestSource.SCREEN));
  }

  /**
   * Approves into a special batch.
   *
   * @param id request
   * @param request comment
   * @return request
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(RemittanceAccess.SPECIAL_APPROVE)
  public SpecialResponse approve(
      @PathVariable Long id, @Valid @RequestBody CommentRequest request) {
    return SpecialResponse.from(specials.approve(id, request.comment()));
  }

  /**
   * Rejects with a reason.
   *
   * @param id request
   * @param request reason and comment
   * @return request
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(RemittanceAccess.SPECIAL_APPROVE)
  public SpecialResponse reject(@PathVariable Long id, @Valid @RequestBody ReturnRequest request) {
    return SpecialResponse.from(specials.reject(id, request.reasonCode(), request.comment()));
  }

  /**
   * Uploads a Collection special remittance file (invoiceNo, conditionCode, remarks).
   *
   * @param file file
   * @return the run
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(RemittanceAccess.SPECIAL_REQUEST)
  public FeedRunResponse upload(@RequestParam MultipartFile file) throws IOException {
    return FeedRunResponse.from(
        flowIn.upload(
            "COLLECTION_SPECIAL_REMIT",
            new FlowInFile(file.getOriginalFilename(), file.getBytes())));
  }
}
