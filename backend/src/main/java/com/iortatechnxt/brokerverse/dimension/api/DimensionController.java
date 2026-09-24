package com.iortatechnxt.brokerverse.dimension.api;

import com.iortatechnxt.brokerverse.dimension.api.dto.DimensionValueRequest;
import com.iortatechnxt.brokerverse.dimension.api.dto.DimensionValueResponse;
import com.iortatechnxt.brokerverse.dimension.domain.DimensionType;
import com.iortatechnxt.brokerverse.dimension.service.DimensionService;
import jakarta.validation.Valid;
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

/** REST API for financial dimensions (cost centres, business lines, departments...). */
@RestController
@RequestMapping("/api/v1/dimensions")
public class DimensionController {

  private final DimensionService service;

  /**
   * Creates the controller.
   *
   * @param service dimension service
   */
  public DimensionController(DimensionService service) {
    this.service = service;
  }

  /**
   * Lists values of a dimension.
   *
   * @param companyId company
   * @param type type
   * @return values
   */
  @GetMapping
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<DimensionValueResponse> list(
      @RequestParam Long companyId, @RequestParam DimensionType type) {
    return service.list(companyId, type).stream().map(DimensionValueResponse::from).toList();
  }

  /**
   * Creates a value.
   *
   * @param request request
   * @return value
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public DimensionValueResponse create(@Valid @RequestBody DimensionValueRequest request) {
    return DimensionValueResponse.from(
        service.create(request.companyId(), request.type(), request.code(), request.name()));
  }

  /**
   * Activates a value.
   *
   * @param id id
   * @return value
   */
  @PostMapping("/{id}/activate")
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public DimensionValueResponse activate(@PathVariable Long id) {
    return DimensionValueResponse.from(service.setActive(id, true));
  }

  /**
   * Deactivates a value.
   *
   * @param id id
   * @return value
   */
  @PostMapping("/{id}/deactivate")
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public DimensionValueResponse deactivate(@PathVariable Long id) {
    return DimensionValueResponse.from(service.setActive(id, false));
  }
}
