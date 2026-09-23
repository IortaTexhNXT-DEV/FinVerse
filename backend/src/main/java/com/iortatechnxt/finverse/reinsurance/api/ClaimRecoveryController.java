package com.iortatechnxt.finverse.reinsurance.api;

import com.iortatechnxt.finverse.reinsurance.api.dto.AllocationRunRequest;
import com.iortatechnxt.finverse.reinsurance.api.dto.ClaimMovementResponse;
import com.iortatechnxt.finverse.reinsurance.service.ClaimRecoveryService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for the reinsurers' shares of claims (recoveries and reserve shares). */
@RestController
@RequestMapping("/api/v1/reinsurance/claims")
public class ClaimRecoveryController {

  private final ClaimRecoveryService service;

  /**
   * Creates the controller.
   *
   * @param service claim recovery service
   */
  public ClaimRecoveryController(ClaimRecoveryService service) {
    this.service = service;
  }

  /**
   * Claim movements of a period with the reinsurers' shares.
   *
   * @param companyId company
   * @param from first movement date
   * @param to last movement date
   * @return movements
   */
  @GetMapping("/movements")
  @PreAuthorize("hasAuthority('REINSURANCE_VIEW')")
  public List<ClaimMovementResponse> movements(
      @RequestParam Long companyId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return service.movements(companyId, from, to).stream()
        .map(ClaimMovementResponse::from)
        .toList();
  }

  /**
   * Processes the posted claim movements of a period not yet processed (idempotent).
   *
   * @param request period
   * @return number of movements processed
   */
  @PostMapping("/catch-up")
  @PreAuthorize("hasAuthority('REINSURANCE_MAINTAIN')")
  public Map<String, Integer> catchUp(@Valid @RequestBody AllocationRunRequest request) {
    return Map.of(
        "processed", service.catchUp(request.companyId(), request.fromDate(), request.toDate()));
  }
}
