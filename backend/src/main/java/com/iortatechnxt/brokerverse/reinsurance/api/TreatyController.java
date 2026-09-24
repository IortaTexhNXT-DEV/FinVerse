package com.iortatechnxt.brokerverse.reinsurance.api;

import com.iortatechnxt.brokerverse.reinsurance.api.dto.TreatyRequest;
import com.iortatechnxt.brokerverse.reinsurance.api.dto.TreatyResponse;
import com.iortatechnxt.brokerverse.reinsurance.service.TreatyService;
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

/** REST API for reinsurance treaties (maker-checker master data). */
@RestController
@RequestMapping("/api/v1/reinsurance/treaties")
public class TreatyController {

  private final TreatyService service;

  /**
   * Creates the controller.
   *
   * @param service treaty service
   */
  public TreatyController(TreatyService service) {
    this.service = service;
  }

  /**
   * Lists treaties.
   *
   * @param companyId company
   * @return treaties
   */
  @GetMapping
  @PreAuthorize("hasAuthority('REINSURANCE_VIEW')")
  public List<TreatyResponse> list(@RequestParam Long companyId) {
    return service.list(companyId).stream().map(TreatyResponse::from).toList();
  }

  /**
   * Gets a treaty.
   *
   * @param id id
   * @return treaty
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('REINSURANCE_VIEW')")
  public TreatyResponse get(@PathVariable Long id) {
    return TreatyResponse.from(service.get(id));
  }

  /**
   * Creates a treaty.
   *
   * @param request request
   * @return treaty
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('REINSURANCE_MAINTAIN')")
  public TreatyResponse create(@Valid @RequestBody TreatyRequest request) {
    return TreatyResponse.from(service.create(request));
  }

  /**
   * Updates a treaty.
   *
   * @param id id
   * @param request request
   * @return treaty
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('REINSURANCE_MAINTAIN')")
  public TreatyResponse update(@PathVariable Long id, @Valid @RequestBody TreatyRequest request) {
    return TreatyResponse.from(service.update(id, request));
  }

  /**
   * Authorizes a treaty.
   *
   * @param id id
   * @return treaty
   */
  @PostMapping("/{id}/authorize")
  @PreAuthorize("hasAuthority('REINSURANCE_AUTHORIZE')")
  public TreatyResponse authorize(@PathVariable Long id) {
    return TreatyResponse.from(service.authorize(id));
  }
}
