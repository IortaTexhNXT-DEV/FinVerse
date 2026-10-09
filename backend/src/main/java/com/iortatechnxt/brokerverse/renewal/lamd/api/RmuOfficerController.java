package com.iortatechnxt.brokerverse.renewal.lamd.api;

import com.iortatechnxt.brokerverse.renewal.lamd.service.RmuOfficerService;
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

/** RMU Account Maintenance (FRRN.041.01). */
@RestController
@RequestMapping("/api/v1/renewal/rmu-officers")
public class RmuOfficerController {

  private static final String MAINTAIN = "hasAuthority('RNW_RMU_MAINTAIN')";

  private final RmuOfficerService officers;

  /**
   * Creates the controller.
   *
   * @param officers RMU Account Officers
   */
  public RmuOfficerController(RmuOfficerService officers) {
    this.officers = officers;
  }

  /**
   * The list.
   *
   * @param companyId company
   * @return officers
   */
  @GetMapping
  @PreAuthorize("hasAnyAuthority('RNW_RMU_MAINTAIN','RNW_LAMD_UPLOAD')")
  public List<RmuOfficerService.Officer> list(@RequestParam Long companyId) {
    return officers.list(companyId);
  }

  /**
   * Adds an AO code.
   *
   * @param companyId company
   * @param input code, name, remarks
   * @return officer
   */
  @PostMapping
  @PreAuthorize(MAINTAIN)
  public RmuOfficerService.Officer add(
      @RequestParam Long companyId, @RequestBody RmuOfficerService.Input input) {
    return officers.add(companyId, input);
  }

  /**
   * Updates an AO code.
   *
   * @param companyId company
   * @param id officer
   * @param input name, remarks, active
   * @return officer
   */
  @PutMapping("/{id}")
  @PreAuthorize(MAINTAIN)
  public RmuOfficerService.Officer update(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @RequestBody RmuOfficerService.Input input) {
    return officers.update(companyId, id, input);
  }
}
