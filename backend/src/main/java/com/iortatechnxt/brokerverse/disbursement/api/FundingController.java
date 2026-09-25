package com.iortatechnxt.brokerverse.disbursement.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.api.dto.OperationDtos.FundingApproval;
import com.iortatechnxt.brokerverse.disbursement.api.dto.OperationDtos.FundingRequestBody;
import com.iortatechnxt.brokerverse.disbursement.api.dto.OperationDtos.FundingResponse;
import com.iortatechnxt.brokerverse.disbursement.api.dto.VoucherDtos.CommentRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.FundingStage;
import com.iortatechnxt.brokerverse.disbursement.service.FundingService;
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
 * Account funding (DIS 2.17.0-2.17.4): requests by stage, one request, create and update by the
 * maker, submit, verify (a team leader other than the maker) and the two approvals, the second of
 * which posts the transfer. Return, decline and cancel run through the workflow panel.
 */
@RestController
@RequestMapping("/api/v1/disbursement/funding")
public class FundingController {

  private final FundingService funding;

  /**
   * Creates the controller.
   *
   * @param funding funding requests
   */
  public FundingController(FundingService funding) {
    this.funding = funding;
  }

  /**
   * Funding requests, newest first.
   *
   * @param companyId company
   * @param stage stages
   * @param page page
   * @param size size
   * @return requests
   */
  @GetMapping
  @PreAuthorize(DisbursementAccess.FUNDING_READ)
  public PageResponse<FundingResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<FundingStage> stage,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        funding.list(companyId, stage, DisbursementAccess.plain(page, size)),
        FundingResponse::from);
  }

  /**
   * One request.
   *
   * @param id request
   * @return request
   */
  @GetMapping("/{id}")
  @PreAuthorize(DisbursementAccess.FUNDING_READ)
  public FundingResponse get(@PathVariable Long id) {
    return FundingResponse.from(funding.get(id));
  }

  /**
   * Creates a request (DIS 2.17.0).
   *
   * @param body terms
   * @return request
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(DisbursementAccess.FUNDING_REQUEST)
  public FundingResponse create(@Valid @RequestBody FundingRequestBody body) {
    if (body.companyId() == null) {
      throw new BusinessRuleException("FUNDING_COMPANY", "Select the company");
    }
    return FundingResponse.from(funding.create(body.companyId(), body.terms()));
  }

  /**
   * Updates a request still with the maker.
   *
   * @param id request
   * @param body terms
   * @return request
   */
  @PutMapping("/{id}")
  @PreAuthorize(DisbursementAccess.FUNDING_REQUEST)
  public FundingResponse update(
      @PathVariable Long id, @Valid @RequestBody FundingRequestBody body) {
    return FundingResponse.from(funding.update(id, body.terms()));
  }

  /**
   * Submits for verification (DIS 2.17.2).
   *
   * @param id request
   * @return request
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(DisbursementAccess.FUNDING_REQUEST)
  public FundingResponse submit(@PathVariable Long id) {
    return FundingResponse.from(funding.submit(id));
  }

  /**
   * Verifies (DIS 2.17.3).
   *
   * @param id request
   * @param body comment
   * @return request
   */
  @PostMapping("/{id}/verify")
  @PreAuthorize(DisbursementAccess.FUNDING_VERIFY)
  public FundingResponse verify(@PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    return FundingResponse.from(funding.verify(id, body.comment()));
  }

  /**
   * First or second approval (DIS 2.17.4).
   *
   * @param id request
   * @param body remarks and BOB reference
   * @return request
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(DisbursementAccess.FUNDING_APPROVE)
  public FundingResponse approve(@PathVariable Long id, @Valid @RequestBody FundingApproval body) {
    return FundingResponse.from(funding.approve(id, body.comment(), body.bobReference()));
  }
}
