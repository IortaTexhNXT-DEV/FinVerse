package com.iortatechnxt.finverse.payables.api;

import com.iortatechnxt.finverse.payables.api.dto.DatedReasonRequest;
import com.iortatechnxt.finverse.payables.api.dto.IssuedPdcResponse;
import com.iortatechnxt.finverse.payables.api.dto.PdcEventResponse;
import com.iortatechnxt.finverse.payables.api.dto.ReplaceChequeRequest;
import com.iortatechnxt.finverse.payables.domain.IssuedPdcStatus;
import com.iortatechnxt.finverse.payables.service.IssuedPdcService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Register of post-dated cheques issued and its lifecycle actions. */
@RestController
@RequestMapping("/api/v1/payables/pdc-issued")
public class IssuedPdcController {

  private final IssuedPdcService service;

  /**
   * Creates the controller.
   *
   * @param service PDC service
   */
  public IssuedPdcController(IssuedPdcService service) {
    this.service = service;
  }

  /**
   * Lists the register.
   *
   * @param companyId company
   * @param statuses statuses (default all)
   * @param from cheque date from
   * @param to cheque date to
   * @return cheques
   */
  @GetMapping
  @PreAuthorize(PayablesAccess.VIEW)
  public List<IssuedPdcResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) Set<IssuedPdcStatus> statuses,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    Set<IssuedPdcStatus> effective =
        statuses == null || statuses.isEmpty() ? EnumSet.allOf(IssuedPdcStatus.class) : statuses;
    return service.search(companyId, effective, ApiDefaults.from(from), ApiDefaults.to(to)).stream()
        .map(IssuedPdcResponse::from)
        .toList();
  }

  /**
   * Gets a cheque.
   *
   * @param id id
   * @return cheque
   */
  @GetMapping("/{id}")
  @PreAuthorize(PayablesAccess.VIEW)
  public IssuedPdcResponse get(@PathVariable Long id) {
    return IssuedPdcResponse.from(service.get(id));
  }

  /**
   * Status history (confirmation audit trail).
   *
   * @param id id
   * @return events
   */
  @GetMapping("/{id}/history")
  @PreAuthorize(PayablesAccess.VIEW)
  public List<PdcEventResponse> history(@PathVariable Long id) {
    return service.history(id).stream().map(PdcEventResponse::from).toList();
  }

  /**
   * Flags cheques whose date has been reached as DUE.
   *
   * @param asOf date
   * @return number of cheques flagged
   */
  @PostMapping("/refresh-due")
  @PreAuthorize(PayablesAccess.MAINTAIN_OR_AUTHORIZE)
  public int refreshDue(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
    return service.refreshDue(asOf);
  }

  /**
   * Confirms presentation (posts Dr PDC clearing / Cr bank).
   *
   * @param id id
   * @param request presentation date
   * @return cheque
   */
  @PostMapping("/{id}/present")
  @PreAuthorize(PayablesAccess.AUTHORIZE)
  public IssuedPdcResponse present(
      @PathVariable Long id, @Valid @RequestBody DatedReasonRequest request) {
    return IssuedPdcResponse.from(service.present(id, request.date()));
  }

  /**
   * Records clearing by the bank.
   *
   * @param id id
   * @param request clearing date
   * @return cheque
   */
  @PostMapping("/{id}/clear")
  @PreAuthorize(PayablesAccess.MAINTAIN_OR_AUTHORIZE)
  public IssuedPdcResponse clear(
      @PathVariable Long id, @Valid @RequestBody DatedReasonRequest request) {
    return IssuedPdcResponse.from(service.clear(id, request.date()));
  }

  /**
   * Stops the cheque and reverses the payment.
   *
   * @param id id
   * @param request date and reason
   * @return cheque
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize(PayablesAccess.AUTHORIZE)
  public IssuedPdcResponse cancel(
      @PathVariable Long id, @Valid @RequestBody DatedReasonRequest request) {
    return IssuedPdcResponse.from(
        service.cancel(
            id, request.date(), ApiDefaults.reasonOrDefault(request.reason(), "PDC cancelled")));
  }

  /**
   * Replaces the cheque by a new leaf.
   *
   * @param id id
   * @param request new cheque date, replacement date, reason
   * @return new cheque
   */
  @PostMapping("/{id}/replace")
  @PreAuthorize(PayablesAccess.AUTHORIZE)
  public IssuedPdcResponse replace(
      @PathVariable Long id, @Valid @RequestBody ReplaceChequeRequest request) {
    return IssuedPdcResponse.from(
        service.replace(id, request.chequeDate(), request.date(), request.reason()));
  }
}
