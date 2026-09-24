package com.iortatechnxt.brokerverse.reserves.api;

import com.iortatechnxt.brokerverse.reserves.api.dto.ReserveParameterRequest;
import com.iortatechnxt.brokerverse.reserves.api.dto.ReserveParameterResponse;
import com.iortatechnxt.brokerverse.reserves.api.dto.TakafulSettingRequest;
import com.iortatechnxt.brokerverse.reserves.api.dto.TakafulSettingResponse;
import com.iortatechnxt.brokerverse.reserves.service.ReserveParameterService;
import com.iortatechnxt.brokerverse.reserves.service.TakafulSettingService;
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
 * Reserve parameters per line of business (effective dated) and takaful settings, both under
 * maker-checker control (MASTER_MAINTAIN / MASTER_AUTHORIZE).
 */
@RestController
@RequestMapping("/api/v1/reserves")
public class ReserveParameterController {

  private static final String VIEW = "hasAnyAuthority('MASTER_VIEW', 'RESERVE_PREPARE')";
  private static final String MAINTAIN = "hasAuthority('MASTER_MAINTAIN')";
  private static final String AUTHORIZE = "hasAuthority('MASTER_AUTHORIZE')";

  private final ReserveParameterService parameters;
  private final TakafulSettingService takaful;

  /**
   * Creates the controller.
   *
   * @param parameters reserve parameters
   * @param takaful takaful settings
   */
  public ReserveParameterController(
      ReserveParameterService parameters, TakafulSettingService takaful) {
    this.parameters = parameters;
    this.takaful = takaful;
  }

  /**
   * Lists parameter sets.
   *
   * @param companyId company
   * @return parameter sets by line, latest effective date first
   */
  @GetMapping("/parameters")
  @PreAuthorize(VIEW)
  public List<ReserveParameterResponse> list(@RequestParam Long companyId) {
    return parameters.list(companyId).stream().map(ReserveParameterResponse::from).toList();
  }

  /**
   * Creates a parameter set (pending authorization).
   *
   * @param request values
   * @return created record
   */
  @PostMapping("/parameters")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public ReserveParameterResponse create(@Valid @RequestBody ReserveParameterRequest request) {
    return ReserveParameterResponse.from(
        parameters.create(
            request.companyId(),
            request.businessLine(),
            request.effectiveFrom(),
            request.toTerms()));
  }

  /**
   * Changes a parameter set pending authorization.
   *
   * @param id id
   * @param request values
   * @return record
   */
  @PutMapping("/parameters/{id}")
  @PreAuthorize(MAINTAIN)
  public ReserveParameterResponse update(
      @PathVariable Long id, @Valid @RequestBody ReserveParameterRequest request) {
    return ReserveParameterResponse.from(parameters.update(id, request.toTerms()));
  }

  /**
   * Authorizes a parameter set.
   *
   * @param id id
   * @return record
   */
  @PostMapping("/parameters/{id}/authorize")
  @PreAuthorize(AUTHORIZE)
  public ReserveParameterResponse authorize(@PathVariable Long id) {
    return ReserveParameterResponse.from(parameters.authorize(id));
  }

  /**
   * Deactivates a parameter set.
   *
   * @param id id
   * @return record
   */
  @PostMapping("/parameters/{id}/deactivate")
  @PreAuthorize(MAINTAIN)
  public ReserveParameterResponse deactivate(@PathVariable Long id) {
    return ReserveParameterResponse.from(parameters.deactivate(id));
  }

  /**
   * Takaful settings of a company.
   *
   * @param companyId company
   * @return settings (disabled when never configured)
   */
  @GetMapping("/takaful-setting")
  @PreAuthorize(VIEW)
  public TakafulSettingResponse takafulSetting(@RequestParam Long companyId) {
    return takaful
        .find(companyId)
        .map(TakafulSettingResponse::from)
        .orElseGet(() -> TakafulSettingResponse.none(companyId));
  }

  /**
   * Saves the takaful settings (pending authorization).
   *
   * @param request values
   * @return settings
   */
  @PutMapping("/takaful-setting")
  @PreAuthorize(MAINTAIN)
  public TakafulSettingResponse saveTakaful(@Valid @RequestBody TakafulSettingRequest request) {
    return TakafulSettingResponse.from(takaful.save(request.companyId(), request.toTerms()));
  }

  /**
   * Authorizes the takaful settings.
   *
   * @param companyId company
   * @return settings
   */
  @PostMapping("/takaful-setting/authorize")
  @PreAuthorize(AUTHORIZE)
  public TakafulSettingResponse authorizeTakaful(@RequestParam Long companyId) {
    return TakafulSettingResponse.from(takaful.authorize(companyId));
  }
}
