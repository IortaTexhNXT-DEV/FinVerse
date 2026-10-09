package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.OtherChargeDtos.OtherChargeRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.OtherChargeDtos.OtherChargeResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.OtherChargeDtos.OtherChargesView;
import com.iortatechnxt.brokerverse.catalog.service.OtherChargeService;
import jakarta.validation.Valid;
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

/**
 * Other charges billed with the premium (template PM-04 Charges): the rows and whether they are
 * billed, and their maintenance under maker-checker (authorized on the catalog records).
 */
@RestController
@RequestMapping("/api/v1/catalog/rates/other-charges")
public class OtherChargeController {

  private final OtherChargeService charges;

  /**
   * Creates the controller.
   *
   * @param charges other charges
   */
  public OtherChargeController(OtherChargeService charges) {
    this.charges = charges;
  }

  /**
   * The charge rows and the billing switch.
   *
   * @return view
   */
  @GetMapping
  @PreAuthorize(CatalogAccess.READ)
  public OtherChargesView list() {
    return new OtherChargesView(
        charges.enabled(), charges.list().stream().map(OtherChargeResponse::from).toList());
  }

  /**
   * Adds a charge row, pending authorization.
   *
   * @param request row
   * @return row
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public OtherChargeResponse create(@Valid @RequestBody OtherChargeRequest request) {
    return OtherChargeResponse.from(
        charges.create(
            request.companyId(), request.chargeCode(), request.scope(), request.terms()));
  }

  /**
   * Changes a charge row, pending authorization.
   *
   * @param id row
   * @param request new terms (code, line and product are kept)
   * @return row
   */
  @PutMapping("/{id}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public OtherChargeResponse update(
      @PathVariable Long id, @Valid @RequestBody OtherChargeRequest request) {
    return OtherChargeResponse.from(charges.update(request.companyId(), id, request.terms()));
  }
}
