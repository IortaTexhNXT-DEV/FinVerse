package com.iortatechnxt.brokerverse.tax.api;

import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.tax.api.dto.FileReturnRequest;
import com.iortatechnxt.brokerverse.tax.api.dto.PayReturnRequest;
import com.iortatechnxt.brokerverse.tax.api.dto.TaxReturnRequest;
import com.iortatechnxt.brokerverse.tax.api.dto.TaxReturnResponse;
import com.iortatechnxt.brokerverse.tax.domain.ReturnStatus;
import com.iortatechnxt.brokerverse.tax.domain.TaxReturn;
import com.iortatechnxt.brokerverse.tax.service.TaxReturnService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tax returns register and lifecycle: prepare, refresh, file, pay, cancel. */
@RestController
@RequestMapping("/api/v1/tax/returns")
public class TaxReturnController {

  private final TaxReturnService service;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param service returns
   * @param clock clock
   */
  public TaxReturnController(TaxReturnService service, Clock clock) {
    this.service = service;
    this.clock = clock;
  }

  /**
   * Returns register of a year.
   *
   * @param companyId company
   * @param year year of the filing periods
   * @param formCode form filter
   * @param status status filter
   * @return returns
   */
  @GetMapping
  @PreAuthorize(TaxAccess.VIEW)
  public List<TaxReturnResponse> list(
      @RequestParam Long companyId,
      @RequestParam int year,
      @RequestParam(required = false) String formCode,
      @RequestParam(required = false) ReturnStatus status) {
    LocalDate today = LocalDate.now(clock);
    return service.list(companyId, year, formCode, status).stream()
        .map(r -> TaxReturnResponse.summary(r, today))
        .toList();
  }

  /**
   * A return with its lines and remittance.
   *
   * @param id id
   * @return return
   */
  @GetMapping("/{id}")
  @PreAuthorize(TaxAccess.VIEW)
  public TaxReturnResponse get(@PathVariable Long id) {
    return detail(service.get(id));
  }

  /**
   * Prepares a draft return from the worksheet.
   *
   * @param request form and period
   * @return draft
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(TaxAccess.MANAGE)
  public TaxReturnResponse create(@Valid @RequestBody TaxReturnRequest request) {
    return detail(service.create(request.companyId(), request.formCode(), request.periodStart()));
  }

  /**
   * Recomputes a draft.
   *
   * @param id id
   * @return draft
   */
  @PostMapping("/{id}/refresh")
  @PreAuthorize(TaxAccess.MANAGE)
  public TaxReturnResponse refresh(@PathVariable Long id) {
    return detail(service.refresh(id));
  }

  /**
   * Records the filing.
   *
   * @param id id
   * @param request filing date and reference
   * @return filed return
   */
  @PostMapping("/{id}/file")
  @PreAuthorize(TaxAccess.MANAGE)
  public TaxReturnResponse file(
      @PathVariable Long id, @Valid @RequestBody FileReturnRequest request) {
    return detail(service.file(id, request.filedOn(), request.reference()));
  }

  /**
   * Pays a filed return (posts the remittance).
   *
   * @param id id
   * @param request payment facts
   * @return paid return
   */
  @PostMapping("/{id}/pay")
  @PreAuthorize(TaxAccess.MANAGE)
  public TaxReturnResponse pay(
      @PathVariable Long id, @Valid @RequestBody PayReturnRequest request) {
    return detail(service.pay(id, request.toFacts()));
  }

  /**
   * Cancels a draft.
   *
   * @param id id
   * @param request reason
   * @return cancelled return
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize(TaxAccess.MANAGE)
  public TaxReturnResponse cancel(
      @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return detail(service.cancel(id, request.reason()));
  }

  private TaxReturnResponse detail(TaxReturn r) {
    return TaxReturnResponse.detail(
        r, LocalDate.now(clock), service.remittance(r.getId()).orElse(null));
  }
}
