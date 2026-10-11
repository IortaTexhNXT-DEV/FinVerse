package com.iortatechnxt.brokerverse.renewal.allocation.api;

import com.iortatechnxt.brokerverse.renewal.allocation.service.InsurerAllocationService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Multiple insurer allocation of a renewal account (FRRN.014.03). */
@RestController
@RequestMapping("/api/v1/renewal/candidates/{ref}/insurers")
public class InsurerAllocationController {

  private final InsurerAllocationService allocations;

  /**
   * Creates the controller.
   *
   * @param allocations insurer allocation
   */
  public InsurerAllocationController(InsurerAllocationService allocations) {
    this.allocations = allocations;
  }

  /**
   * The insurers of a renewal account.
   *
   * @param companyId company
   * @param ref renewal
   * @return shares
   */
  @GetMapping
  @PreAuthorize("hasAuthority('RNW_VIEW')")
  public List<InsurerAllocationService.Share> list(
      @RequestParam Long companyId, @PathVariable String ref) {
    return allocations.of(companyId, ref);
  }

  /**
   * Allocates a renewal account to insurers.
   *
   * @param companyId company
   * @param ref renewal
   * @param shares insurers with a percentage or an amount
   * @return the allocation
   */
  @PutMapping
  @PreAuthorize("hasAnyAuthority('RNW_DISPOSE','RNW_PROCESS')")
  public List<InsurerAllocationService.Share> allocate(
      @RequestParam Long companyId,
      @PathVariable String ref,
      @RequestBody List<InsurerAllocationService.Input> shares) {
    return allocations.allocate(companyId, ref, shares);
  }
}
