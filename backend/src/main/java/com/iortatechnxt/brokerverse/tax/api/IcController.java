package com.iortatechnxt.brokerverse.tax.api;

import com.iortatechnxt.brokerverse.tax.api.dto.IcLineRequest;
import com.iortatechnxt.brokerverse.tax.api.dto.IcLineResponse;
import com.iortatechnxt.brokerverse.tax.api.dto.IcScheduleResponse;
import com.iortatechnxt.brokerverse.tax.domain.IcSchedule;
import com.iortatechnxt.brokerverse.tax.service.IcMappingService;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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

/** Insurance Commission schedules and their ledger mapping. */
@RestController
@RequestMapping("/api/v1/tax/ic")
public class IcController {

  private final IcMappingService mappings;
  private final IcScheduleService schedules;

  /**
   * Creates the controller.
   *
   * @param mappings mapping maintenance
   * @param schedules schedule computation
   */
  public IcController(IcMappingService mappings, IcScheduleService schedules) {
    this.mappings = mappings;
    this.schedules = schedules;
  }

  /**
   * Lists the mapping.
   *
   * @param companyId company
   * @return lines
   */
  @GetMapping("/mappings")
  @PreAuthorize(TaxAccess.VIEW)
  public List<IcLineResponse> mappings(@RequestParam Long companyId) {
    return mappings.list(companyId).stream().map(IcLineResponse::from).toList();
  }

  /**
   * Creates a mapping line.
   *
   * @param request values
   * @return line
   */
  @PostMapping("/mappings")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(TaxAccess.MANAGE)
  public IcLineResponse create(@Valid @RequestBody IcLineRequest request) {
    return IcLineResponse.from(mappings.create(request.toCommand()));
  }

  /**
   * Updates a mapping line.
   *
   * @param id id
   * @param request values
   * @return line
   */
  @PutMapping("/mappings/{id}")
  @PreAuthorize(TaxAccess.MANAGE)
  public IcLineResponse update(@PathVariable Long id, @Valid @RequestBody IcLineRequest request) {
    return IcLineResponse.from(mappings.update(id, request.toCommand()));
  }

  /**
   * Authorizes a mapping line.
   *
   * @param id id
   * @return line
   */
  @PostMapping("/mappings/{id}/authorize")
  @PreAuthorize(TaxAccess.AUTHORIZE)
  public IcLineResponse authorize(@PathVariable Long id) {
    return IcLineResponse.from(mappings.authorize(id));
  }

  /**
   * Computes a schedule.
   *
   * @param schedule schedule
   * @param companyId company
   * @param from period start (movement lines)
   * @param to period end / balance date
   * @return schedule
   */
  @GetMapping("/schedules/{schedule}")
  @PreAuthorize(TaxAccess.INSURER_VIEW)
  public IcScheduleResponse schedule(
      @PathVariable IcSchedule schedule,
      @RequestParam Long companyId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return IcScheduleResponse.from(schedules.compute(companyId, schedule, from, to));
  }
}
