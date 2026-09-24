package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.BranchRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.BranchResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.CommissionRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.CommissionResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.InsurerDetailResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.InsurerRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.InsurerResponse;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Insurer panel: profiles, branches with LGT and commission rates (Appendix A, Q06). */
@RestController
@RequestMapping("/api/v1/catalog/insurers")
public class InsurerController {

  private final InsurerService insurers;
  private final RateTableService rates;

  /**
   * Creates the controller.
   *
   * @param insurers insurers
   * @param rates commission rates
   */
  public InsurerController(InsurerService insurers, RateTableService rates) {
    this.insurers = insurers;
    this.rates = rates;
  }

  /**
   * Insurers of a company.
   *
   * @param companyId company
   * @return insurers
   */
  @GetMapping
  @PreAuthorize(CatalogAccess.READ)
  public List<InsurerResponse> list(@RequestParam Long companyId) {
    return insurers.insurers(companyId).stream().map(InsurerResponse::from).toList();
  }

  /**
   * An insurer with its branches and commission rates.
   *
   * @param id profile
   * @return detail
   */
  @GetMapping("/{id}")
  @PreAuthorize(CatalogAccess.READ)
  public InsurerDetailResponse get(@PathVariable Long id) {
    InsurerProfile insurer = insurers.get(id);
    return new InsurerDetailResponse(
        InsurerResponse.from(insurer),
        insurers.branches(id).stream().map(BranchResponse::from).toList(),
        rates.commissions(insurer.getCompanyId(), insurer.getPartyCode()).stream()
            .map(CommissionResponse::from)
            .toList());
  }

  /**
   * Adds an insurer (and its party).
   *
   * @param request insurer
   * @return insurer
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public InsurerResponse create(@Valid @RequestBody InsurerRequest request) {
    return InsurerResponse.from(
        insurers.create(
            request.companyId(), request.partyCode(), request.contact(), request.details()));
  }

  /**
   * Changes an insurer profile.
   *
   * @param id profile
   * @param request profile
   * @return insurer
   */
  @PutMapping("/{id}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public InsurerResponse update(@PathVariable Long id, @Valid @RequestBody InsurerRequest request) {
    return InsurerResponse.from(insurers.update(id, request.details()));
  }

  /**
   * Adds a branch.
   *
   * @param id profile
   * @param request branch
   * @return branch
   */
  @PostMapping("/{id}/branches")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public BranchResponse createBranch(
      @PathVariable Long id, @Valid @RequestBody BranchRequest request) {
    return BranchResponse.from(insurers.createBranch(id, request.code(), request.details()));
  }

  /**
   * Changes a branch.
   *
   * @param branchId branch
   * @param request branch
   * @return branch
   */
  @PutMapping("/branches/{branchId}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public BranchResponse updateBranch(
      @PathVariable Long branchId, @Valid @RequestBody BranchRequest request) {
    return BranchResponse.from(insurers.updateBranch(branchId, request.details()));
  }

  /**
   * Adds a commission rate.
   *
   * @param id profile
   * @param request rate
   * @return rate
   */
  @PostMapping("/{id}/commissions")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public CommissionResponse createCommission(
      @PathVariable Long id, @Valid @RequestBody CommissionRequest request) {
    InsurerProfile insurer = insurers.get(id);
    String product =
        request.productCode() == null || request.productCode().isBlank()
            ? null
            : request.productCode();
    return CommissionResponse.from(
        rates.createCommission(
            insurer.getCompanyId(), insurer.getPartyCode(), product, request.validity()));
  }

  /**
   * Changes a commission rate.
   *
   * @param rateId rate
   * @param request rate
   * @return rate
   */
  @PutMapping("/commissions/{rateId}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public CommissionResponse updateCommission(
      @PathVariable Long rateId, @Valid @RequestBody CommissionRequest request) {
    return CommissionResponse.from(rates.updateCommission(rateId, request.validity()));
  }
}
