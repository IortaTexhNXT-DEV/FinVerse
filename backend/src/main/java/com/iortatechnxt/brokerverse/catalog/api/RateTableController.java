package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.MotorLimitRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.MotorLimitResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.RateRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.RateResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.ShortPeriodRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.ShortPeriodResponse;
import com.iortatechnxt.brokerverse.catalog.service.RateTableService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Effective-dated taxes, rating factors, short-period table and motor limits (Appendix A). */
@RestController
@RequestMapping("/api/v1/catalog/rates")
public class RateTableController {

  private final RateTableService rates;

  /**
   * Creates the controller.
   *
   * @param rates rate tables
   */
  public RateTableController(RateTableService rates) {
    this.rates = rates;
  }

  /**
   * Taxes and rating factors.
   *
   * @return rows
   */
  @GetMapping("/taxes")
  @PreAuthorize(CatalogAccess.READ)
  public List<RateResponse> taxes() {
    return rates.rates().stream().map(RateResponse::from).toList();
  }

  /**
   * Adds a tax or factor row.
   *
   * @param request row
   * @return row
   */
  @PostMapping("/taxes")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public RateResponse createTax(@Valid @RequestBody RateRequest request) {
    String line =
        request.lineCode() == null || request.lineCode().isBlank() ? null : request.lineCode();
    return RateResponse.from(rates.createRate(request.rateCode(), line, request.validity()));
  }

  /**
   * Changes a tax or factor row.
   *
   * @param id row
   * @param request rate and effectivity
   * @return row
   */
  @PutMapping("/taxes/{id}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public RateResponse updateTax(@PathVariable Long id, @Valid @RequestBody RateRequest request) {
    return RateResponse.from(rates.updateRate(id, request.validity()));
  }

  /**
   * The short-period table.
   *
   * @return rows
   */
  @GetMapping("/short-period")
  @PreAuthorize(CatalogAccess.READ)
  public List<ShortPeriodResponse> shortPeriod() {
    return rates.shortPeriods().stream().map(ShortPeriodResponse::from).toList();
  }

  /**
   * Adds a short-period row.
   *
   * @param request row
   * @return row
   */
  @PostMapping("/short-period")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public ShortPeriodResponse createShortPeriod(@Valid @RequestBody ShortPeriodRequest request) {
    return ShortPeriodResponse.from(
        rates.createShortPeriod(request.monthsCovered(), request.validity()));
  }

  /**
   * Changes a short-period row.
   *
   * @param id row
   * @param request percent and effectivity
   * @return row
   */
  @PutMapping("/short-period/{id}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public ShortPeriodResponse updateShortPeriod(
      @PathVariable Long id, @Valid @RequestBody ShortPeriodRequest request) {
    return ShortPeriodResponse.from(rates.updateShortPeriod(id, request.validity()));
  }

  /**
   * Motor BI / PD limits.
   *
   * @return rows
   */
  @GetMapping("/motor-limits")
  @PreAuthorize(CatalogAccess.READ)
  public List<MotorLimitResponse> motorLimits() {
    return rates.motorLimits().stream().map(MotorLimitResponse::from).toList();
  }

  /**
   * Adds a motor limit row.
   *
   * @param request row
   * @return row
   */
  @PostMapping("/motor-limits")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public MotorLimitResponse createMotorLimit(@Valid @RequestBody MotorLimitRequest request) {
    return MotorLimitResponse.from(
        rates.createMotorLimit(request.coverage(), request.limitAmount(), request.price()));
  }

  /**
   * Changes a motor limit row.
   *
   * @param id row
   * @param request premium and effectivity
   * @return row
   */
  @PutMapping("/motor-limits/{id}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public MotorLimitResponse updateMotorLimit(
      @PathVariable Long id, @Valid @RequestBody MotorLimitRequest request) {
    return MotorLimitResponse.from(rates.updateMotorLimit(id, request.price()));
  }
}
