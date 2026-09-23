package com.iortatechnxt.finverse.investment.api;

import com.iortatechnxt.finverse.investment.api.dto.PortfolioRequest;
import com.iortatechnxt.finverse.investment.api.dto.PortfolioResponse;
import com.iortatechnxt.finverse.investment.service.PortfolioService;
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

/** REST API for investment portfolios. */
@RestController
@RequestMapping("/api/v1/investments/portfolios")
public class PortfolioController {

  private final PortfolioService service;

  /**
   * Creates the controller.
   *
   * @param service portfolio service
   */
  public PortfolioController(PortfolioService service) {
    this.service = service;
  }

  /**
   * Lists portfolios.
   *
   * @param companyId company
   * @return portfolios
   */
  @GetMapping
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<PortfolioResponse> list(@RequestParam Long companyId) {
    return service.list(companyId).stream().map(PortfolioResponse::from).toList();
  }

  /**
   * Creates a portfolio.
   *
   * @param request request
   * @return portfolio
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public PortfolioResponse create(@Valid @RequestBody PortfolioRequest request) {
    return PortfolioResponse.from(service.create(request));
  }

  /**
   * Updates a portfolio.
   *
   * @param id id
   * @param request request
   * @return portfolio
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public PortfolioResponse update(
      @PathVariable Long id, @Valid @RequestBody PortfolioRequest request) {
    return PortfolioResponse.from(service.update(id, request));
  }

  /**
   * Authorizes a portfolio.
   *
   * @param id id
   * @return portfolio
   */
  @PostMapping("/{id}/authorize")
  @PreAuthorize("hasAuthority('MASTER_AUTHORIZE')")
  public PortfolioResponse authorize(@PathVariable Long id) {
    return PortfolioResponse.from(service.authorize(id));
  }
}
