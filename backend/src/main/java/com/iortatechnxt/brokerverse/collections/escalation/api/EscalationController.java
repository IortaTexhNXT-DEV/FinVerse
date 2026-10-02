package com.iortatechnxt.brokerverse.collections.escalation.api;

import com.iortatechnxt.brokerverse.collections.escalation.api.dto.EscalationDtos.ActionRequest;
import com.iortatechnxt.brokerverse.collections.escalation.api.dto.EscalationDtos.EscalationResponse;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Stage;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
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
 * Escalations of collection accounts (BRCLXN.049/050): the inbox by stage, the escalations of an
 * account, one escalation with its invoices and its business actions (escalate further, resolve,
 * resubmit). Manual escalation is {@code POST /api/v1/collections/bulk/escalate}; acknowledge and
 * return to the handler run through the workflow API.
 */
@RestController
@RequestMapping("/api/v1/collections/escalations")
public class EscalationController {

  private static final int MAX_PAGE = 200;

  private final EscalationService escalations;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param escalations escalations
   * @param clock clock (SLA)
   */
  public EscalationController(EscalationService escalations, Clock clock) {
    this.escalations = escalations;
    this.clock = clock;
  }

  /**
   * Escalations, newest first.
   *
   * @param companyId company
   * @param stage stages
   * @param q number, account, client, assured or invoice
   * @param page page
   * @param size size
   * @return escalations
   */
  @GetMapping
  @PreAuthorize("hasAuthority('CLX_VIEW')")
  public PageResponse<EscalationResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<Stage> stage,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Instant now = clock.instant();
    return PageResponse.of(
        escalations.search(
            companyId,
            stage,
            q,
            PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE),
                Sort.by(Sort.Direction.DESC, "id"))),
        e -> EscalationResponse.from(e, now));
  }

  /**
   * An escalation with its invoices.
   *
   * @param id escalation
   * @return escalation
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('CLX_VIEW')")
  public EscalationResponse get(@PathVariable Long id) {
    return EscalationResponse.detail(escalations.get(id), clock.instant());
  }

  /**
   * Escalations of a collection account, newest first.
   *
   * @param invoiceNo invoice
   * @return escalations
   */
  @GetMapping("/by-invoice/{invoiceNo}")
  @PreAuthorize("hasAuthority('CLX_VIEW')")
  public List<EscalationResponse> byInvoice(@PathVariable String invoiceNo) {
    Instant now = clock.instant();
    return escalations.forInvoice(invoiceNo).stream()
        .map(e -> EscalationResponse.detail(e, now))
        .toList();
  }

  /**
   * A business action: {@code escalate_further} (reason), {@code resolve} (resolution) or {@code
   * resubmit}. The workflow checks the stage and the user's permission.
   *
   * @param id escalation
   * @param action action
   * @param request reason and comment
   * @return escalation
   */
  @PostMapping("/{id}/actions/{action}")
  @PreAuthorize("hasAnyAuthority('CLX_ESCALATION_HANDLE', 'CLX_ESCALATE', 'CLX_WORK')")
  public EscalationResponse act(
      @PathVariable Long id,
      @PathVariable String action,
      @Valid @RequestBody ActionRequest request) {
    escalations.act(id, action, new TransitionNote(request.reasonCode(), request.comment()));
    return EscalationResponse.detail(escalations.get(id), clock.instant());
  }
}
