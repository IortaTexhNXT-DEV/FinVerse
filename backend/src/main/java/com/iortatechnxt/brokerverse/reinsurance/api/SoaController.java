package com.iortatechnxt.brokerverse.reinsurance.api;

import com.iortatechnxt.brokerverse.reinsurance.api.dto.SettlementRequest;
import com.iortatechnxt.brokerverse.reinsurance.api.dto.SoaRequest;
import com.iortatechnxt.brokerverse.reinsurance.api.dto.SoaResponse;
import com.iortatechnxt.brokerverse.reinsurance.domain.Soa;
import com.iortatechnxt.brokerverse.reinsurance.service.SoaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for reinsurers' statements of account (generation, approval, settlement). */
@RestController
@RequestMapping("/api/v1/reinsurance/soas")
public class SoaController {

  private final SoaService service;

  /**
   * Creates the controller.
   *
   * @param service statement service
   */
  public SoaController(SoaService service) {
    this.service = service;
  }

  /**
   * Lists statements.
   *
   * @param companyId company
   * @return statements
   */
  @GetMapping
  @PreAuthorize("hasAuthority('REINSURANCE_VIEW')")
  public List<SoaResponse> list(@RequestParam Long companyId) {
    return service.list(companyId).stream().map(this::response).toList();
  }

  /**
   * Gets a statement with its printed layout.
   *
   * @param id id
   * @return statement
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('REINSURANCE_VIEW')")
  public SoaResponse get(@PathVariable Long id) {
    return response(service.get(id));
  }

  /**
   * Generates (or regenerates) the statements of a treaty for a quarter.
   *
   * @param request treaty, participant and quarter
   * @return statements
   */
  @PostMapping
  @PreAuthorize("hasAuthority('REINSURANCE_MAINTAIN')")
  public List<SoaResponse> generate(@Valid @RequestBody SoaRequest request) {
    return service.generate(request).stream().map(this::response).toList();
  }

  /**
   * Approves a statement.
   *
   * @param id id
   * @return statement
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAuthority('REINSURANCE_AUTHORIZE')")
  public SoaResponse approve(@PathVariable Long id) {
    return response(service.approve(id));
  }

  /**
   * Settles an approved statement.
   *
   * @param id id
   * @param request settlement date and bank account
   * @return statement
   */
  @PostMapping("/{id}/settle")
  @PreAuthorize("hasAuthority('REINSURANCE_AUTHORIZE')")
  public SoaResponse settle(@PathVariable Long id, @Valid @RequestBody SettlementRequest request) {
    return response(service.settle(id, request));
  }

  private SoaResponse response(Soa soa) {
    return SoaResponse.from(soa, service.layout(soa));
  }
}
