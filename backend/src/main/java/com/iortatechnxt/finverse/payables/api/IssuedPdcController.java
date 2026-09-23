package com.iortatechnxt.finverse.payables.api;

import com.iortatechnxt.finverse.payables.api.dto.DatedReasonRequest;
import com.iortatechnxt.finverse.payables.api.dto.PdcEventResponse;
import com.iortatechnxt.finverse.payables.api.dto.PdcResponse;
import com.iortatechnxt.finverse.payables.api.dto.ReplaceChequeRequest;
import com.iortatechnxt.finverse.payables.domain.PdcStatus;
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
  public List<PdcResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) Set<PdcStatus> statuses,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    Set<PdcStatus> effective =
        statuses == null || statuses.isEmpty() ? EnumSet.allOf(PdcStatus.class) : statuses;
    return service.search(companyId, effective, ApiDefaults.from(from), ApiDefaults.to(to)).stream()
        .map(PdcResponse::from)
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
  public PdcResponse get(@PathVariable Long id) {
    return PdcResponse.from(service.get(id));
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
  public PdcResponse present(
      @PathVariable Long id, @Valid @RequestBody DatedReasonRequest request) {
    return PdcResponse.from(service.present(id, request.date()));
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
  public PdcResponse clear(@PathVariable Long id, @Valid @RequestBody DatedReasonRequest request) {
    return PdcResponse.from(service.clear(id, request.date()));
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
  public PdcResponse cancel(@PathVariable Long id, @Valid @RequestBody DatedReasonRequest request) {
    return PdcResponse.from(
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
  public PdcResponse replace(
      @PathVariable Long id, @Valid @RequestBody ReplaceChequeRequest request) {
    return PdcResponse.from(
        service.replace(id, request.chequeDate(), request.date(), request.reason()));
  }
}
