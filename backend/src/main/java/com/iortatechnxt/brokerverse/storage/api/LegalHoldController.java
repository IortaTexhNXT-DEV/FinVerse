package com.iortatechnxt.brokerverse.storage.api;

import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.storage.api.dto.LegalHoldCommand;
import com.iortatechnxt.brokerverse.storage.api.dto.LegalHoldRequestResponse;
import com.iortatechnxt.brokerverse.storage.service.LegalHoldService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legal hold of stored files as a controlled action (DOCUMENT_STORAGE_DECISION, decision 3):
 * request with {@code FILE_LEGAL_HOLD_REQUEST}, approve or reject with {@code
 * FILE_LEGAL_HOLD_APPROVE} (the DOA roles; the requester cannot decide).
 */
@RestController
@RequestMapping("/api/v1/files")
public class LegalHoldController {

  private static final String REQUEST = "hasAuthority('FILE_LEGAL_HOLD_REQUEST')";
  private static final String APPROVE = "hasAuthority('FILE_LEGAL_HOLD_APPROVE')";
  private static final String VIEW =
      "hasAnyAuthority('FILE_LEGAL_HOLD_REQUEST', 'FILE_LEGAL_HOLD_APPROVE', 'AUDIT_VIEW')";

  private final LegalHoldService holds;

  /**
   * Creates the controller.
   *
   * @param holds legal hold service
   */
  public LegalHoldController(LegalHoldService holds) {
    this.holds = holds;
  }

  /**
   * Requests to place or release the legal hold of a file.
   *
   * @param id stored file id
   * @param body action and reason
   * @return pending request
   */
  @PostMapping("/{id}/legal-hold-requests")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(REQUEST)
  public LegalHoldRequestResponse request(
      @PathVariable Long id, @Valid @RequestBody LegalHoldCommand body) {
    return LegalHoldRequestResponse.from(holds.request(id, body.action(), body.reason()));
  }

  /**
   * The legal hold requests of a file, newest first.
   *
   * @param id stored file id
   * @return requests
   */
  @GetMapping("/{id}/legal-hold-requests")
  @PreAuthorize(VIEW)
  public List<LegalHoldRequestResponse> history(@PathVariable Long id) {
    return holds.historyOf(id).stream().map(LegalHoldRequestResponse::from).toList();
  }

  /**
   * Pending legal hold requests, oldest first.
   *
   * @return requests
   */
  @GetMapping("/legal-hold-requests")
  @PreAuthorize(VIEW)
  public List<LegalHoldRequestResponse> pending() {
    return holds.pending().stream().map(LegalHoldRequestResponse::from).toList();
  }

  /**
   * Approves a request and applies it.
   *
   * @param requestId request
   * @param body decision note
   * @return decided request
   */
  @PostMapping("/legal-hold-requests/{requestId}/approve")
  @PreAuthorize(APPROVE)
  public LegalHoldRequestResponse approve(
      @PathVariable Long requestId, @Valid @RequestBody ReasonRequest body) {
    return LegalHoldRequestResponse.from(holds.decide(requestId, true, body.reason()));
  }

  /**
   * Rejects a request.
   *
   * @param requestId request
   * @param body decision note
   * @return decided request
   */
  @PostMapping("/legal-hold-requests/{requestId}/reject")
  @PreAuthorize(APPROVE)
  public LegalHoldRequestResponse reject(
      @PathVariable Long requestId, @Valid @RequestBody ReasonRequest body) {
    return LegalHoldRequestResponse.from(holds.decide(requestId, false, body.reason()));
  }
}
