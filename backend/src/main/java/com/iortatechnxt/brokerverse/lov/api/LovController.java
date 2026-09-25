package com.iortatechnxt.brokerverse.lov.api;

import com.iortatechnxt.brokerverse.lov.api.dto.LovOption;
import com.iortatechnxt.brokerverse.lov.api.dto.LovTypeResponse;
import com.iortatechnxt.brokerverse.lov.api.dto.LovValueRequest;
import com.iortatechnxt.brokerverse.lov.api.dto.LovValueResponse;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import jakarta.validation.Valid;
import java.time.Clock;
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

/** Lists of values: pick lists for every signed-in user, maintenance for administrators. */
@RestController
@RequestMapping("/api/v1/lov")
public class LovController {

  private static final String MANAGE = "hasAuthority('LOV_MANAGE')";

  private final LovService service;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param service list service
   * @param clock clock
   */
  public LovController(LovService service, Clock clock) {
    this.service = service;
    this.clock = clock;
  }

  /**
   * Usable options of a list (pick lists).
   *
   * @param type list
   * @param date business date (default today)
   * @return options
   */
  @GetMapping("/{type}/options")
  @PreAuthorize("isAuthenticated()")
  public List<LovOption> options(
      @PathVariable String type,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {
    LocalDate on = date == null ? LocalDate.now(clock) : date;
    return service.options(type, on).stream().map(LovOption::from).toList();
  }

  /**
   * All list types.
   *
   * @return types
   */
  @GetMapping("/types")
  @PreAuthorize("hasAnyAuthority('LOV_MANAGE', 'MASTER_AUTHORIZE')")
  public List<LovTypeResponse> types() {
    return service.types().stream().map(LovTypeResponse::from).toList();
  }

  /**
   * Every value of a list (maintenance).
   *
   * @param type list
   * @return values
   */
  @GetMapping("/{type}/values")
  @PreAuthorize("hasAnyAuthority('LOV_MANAGE', 'MASTER_AUTHORIZE')")
  public List<LovValueResponse> values(@PathVariable String type) {
    return service.values(type).stream().map(LovValueResponse::from).toList();
  }

  /**
   * Adds a value, pending authorization.
   *
   * @param type list
   * @param request value
   * @return value
   */
  @PostMapping("/{type}/values")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MANAGE)
  public LovValueResponse create(
      @PathVariable String type, @Valid @RequestBody LovValueRequest request) {
    return LovValueResponse.from(service.create(type, request.code(), request.details()));
  }

  /**
   * Changes a value, pending authorization.
   *
   * @param id value id
   * @param request new attributes
   * @return value
   */
  @PutMapping("/values/{id}")
  @PreAuthorize(MANAGE)
  public LovValueResponse update(
      @PathVariable Long id, @Valid @RequestBody LovValueRequest request) {
    return LovValueResponse.from(service.update(id, request.details()));
  }

  /**
   * Authorizes a new or changed value.
   *
   * @param id value id
   * @return value
   */
  @PostMapping("/values/{id}/authorize")
  @PreAuthorize("hasAuthority('MASTER_AUTHORIZE')")
  public LovValueResponse authorize(@PathVariable Long id) {
    return LovValueResponse.from(service.authorize(id));
  }

  /**
   * Deactivates a value.
   *
   * @param id value id
   * @return value
   */
  @PostMapping("/values/{id}/deactivate")
  @PreAuthorize(MANAGE)
  public LovValueResponse deactivate(@PathVariable Long id) {
    return LovValueResponse.from(service.deactivate(id));
  }
}
