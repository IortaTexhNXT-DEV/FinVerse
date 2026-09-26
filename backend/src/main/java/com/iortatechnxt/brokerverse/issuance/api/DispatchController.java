package com.iortatechnxt.brokerverse.issuance.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.issuance.api.dto.DispatchBatchRequest;
import com.iortatechnxt.brokerverse.issuance.api.dto.DispatchLogResponse;
import com.iortatechnxt.brokerverse.issuance.api.dto.DispatchRequest;
import com.iortatechnxt.brokerverse.issuance.api.dto.EpolicyResponse;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyDispatchService;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyDispatchService.DispatchEmail;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceBatchService;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceBatchService.Outcome;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * E-policy dispatch to the client (BRNB.077/035): proposed e-mail, single send, batch send, and the
 * dispatch report from the messaging log.
 */
@RestController
@RequestMapping("/api/v1/issuance/dispatch")
public class DispatchController {

  private static final String SEND = "hasAuthority('EPOLICY_SEND')";
  private static final String REPORT =
      "hasAnyAuthority('EPOLICY_SEND', 'EPOLICY_MANAGE', 'MESSAGE_VIEW')";

  private final EpolicyDispatchService dispatch;
  private final IssuanceBatchService batch;

  /**
   * Creates the controller.
   *
   * @param dispatch e-policy dispatch
   * @param batch batch dispatch
   */
  public DispatchController(EpolicyDispatchService dispatch, IssuanceBatchService batch) {
    this.dispatch = dispatch;
    this.batch = batch;
  }

  /**
   * The proposed e-mail of an e-policy.
   *
   * @param epolicyId confirmed e-policy
   * @return draft
   */
  @GetMapping("/{epolicyId}/draft")
  @PreAuthorize(SEND)
  public DispatchEmail draft(@PathVariable Long epolicyId) {
    return dispatch.draft(epolicyId);
  }

  /**
   * Sends one e-policy.
   *
   * @param epolicyId confirmed e-policy
   * @param request e-mail
   * @return e-policy
   */
  @PostMapping("/{epolicyId}")
  @PreAuthorize(SEND)
  public EpolicyResponse send(
      @PathVariable Long epolicyId, @Valid @RequestBody DispatchRequest request) {
    return EpolicyResponse.from(dispatch.dispatch(epolicyId, request.toEmail()));
  }

  /**
   * Sends several e-policies with their proposed e-mail.
   *
   * @param request e-policies and password hint
   * @return outcome per e-policy
   */
  @PostMapping("/batch")
  @PreAuthorize(SEND)
  public List<Outcome> sendMany(@Valid @RequestBody DispatchBatchRequest request) {
    return batch.dispatch(request.epolicyIds(), request.passwordHint());
  }

  /**
   * The dispatch report: e-policy e-mails with their outcome.
   *
   * @param text recipient, subject or ARN fragment
   * @param page page
   * @param size size
   * @return messages, newest first
   */
  @GetMapping("/log")
  @PreAuthorize(REPORT)
  public PageResponse<DispatchLogResponse> log(
      @RequestParam(required = false) String text,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return PageResponse.of(
        dispatch.log(text, IssuanceController.pageable(page, size)), DispatchLogResponse::from);
  }
}
