package com.iortatechnxt.finverse.reinsurance.api;

import com.iortatechnxt.finverse.reinsurance.api.dto.DateRequest;
import com.iortatechnxt.finverse.reinsurance.api.dto.FacAssignRequest;
import com.iortatechnxt.finverse.reinsurance.api.dto.FacPlacementResponse;
import com.iortatechnxt.finverse.reinsurance.domain.FacStatus;
import com.iortatechnxt.finverse.reinsurance.service.FacPlacementService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for facultative placements (slip, submission, approval, closing). */
@RestController
@RequestMapping("/api/v1/reinsurance/fac-placements")
public class FacPlacementController {

  private final FacPlacementService service;

  /**
   * Creates the controller.
   *
   * @param service placement service
   */
  public FacPlacementController(FacPlacementService service) {
    this.service = service;
  }

  /**
   * Lists placements.
   *
   * @param companyId company
   * @param status optional status filter
   * @return placements
   */
  @GetMapping
  @PreAuthorize("hasAuthority('REINSURANCE_VIEW')")
  public List<FacPlacementResponse> list(
      @RequestParam Long companyId, @RequestParam(required = false) FacStatus status) {
    return service.list(companyId, status).stream().map(FacPlacementResponse::from).toList();
  }

  /**
   * Gets a placement.
   *
   * @param id id
   * @return placement
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('REINSURANCE_VIEW')")
  public FacPlacementResponse get(@PathVariable Long id) {
    return FacPlacementResponse.from(service.get(id));
  }

  /**
   * Records the participants of a provisional slip.
   *
   * @param id id
   * @param request participants
   * @return placement
   */
  @PutMapping("/{id}/participants")
  @PreAuthorize("hasAuthority('REINSURANCE_MAINTAIN')")
  public FacPlacementResponse assign(
      @PathVariable Long id, @Valid @RequestBody FacAssignRequest request) {
    return FacPlacementResponse.from(service.assign(id, request));
  }

  /**
   * Submits a slip for approval.
   *
   * @param id id
   * @return placement
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize("hasAuthority('REINSURANCE_MAINTAIN')")
  public FacPlacementResponse submit(@PathVariable Long id) {
    return FacPlacementResponse.from(service.submit(id));
  }

  /**
   * Approves a slip (cedes the placed premium).
   *
   * @param id id
   * @param request placement date (optional)
   * @return placement
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAuthority('REINSURANCE_AUTHORIZE')")
  public FacPlacementResponse approve(
      @PathVariable Long id, @RequestBody(required = false) DateRequest request) {
    return FacPlacementResponse.from(service.approve(id, request == null ? null : request.date()));
  }

  /**
   * Returns a slip to the maker.
   *
   * @param id id
   * @return placement
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAuthority('REINSURANCE_AUTHORIZE')")
  public FacPlacementResponse reject(@PathVariable Long id) {
    return FacPlacementResponse.from(service.reject(id));
  }

  /**
   * Closes a placed slip.
   *
   * @param id id
   * @param request closing date (optional)
   * @return placement
   */
  @PostMapping("/{id}/close")
  @PreAuthorize("hasAuthority('REINSURANCE_MAINTAIN')")
  public FacPlacementResponse close(
      @PathVariable Long id, @RequestBody(required = false) DateRequest request) {
    return FacPlacementResponse.from(service.close(id, request == null ? null : request.date()));
  }
}
