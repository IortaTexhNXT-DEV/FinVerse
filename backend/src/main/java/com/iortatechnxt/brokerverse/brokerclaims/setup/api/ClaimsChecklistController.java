package com.iortatechnxt.brokerverse.brokerclaims.setup.api;

import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimExtrasService;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.DocumentRequirement;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.DocumentChecklist;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Claims Setup of the OOTB points of the FRS BRD-07 Claims v1.0: the standard document checklist
 * per claim type (G-CLM-01, FRCLM.006.01) and the accredited repair shops (G-CLM-02, FRCLM.007.02:
 * the active parties of type Garage, maintained on Parties).
 */
@RestController
@RequestMapping("/api/v1/broker-claims")
public class ClaimsChecklistController {

  private final DocumentChecklist checklist;
  private final ClaimExtrasService extras;

  /**
   * Creates the controller.
   *
   * @param checklist document checklist
   * @param extras repair shops
   */
  public ClaimsChecklistController(DocumentChecklist checklist, ClaimExtrasService extras) {
    this.checklist = checklist;
    this.extras = extras;
  }

  /**
   * The document checklist.
   *
   * @return requirements by claim type
   */
  @GetMapping("/setup/document-requirements")
  @PreAuthorize("hasAnyAuthority('BCL_SETUP', 'BCL_VIEW')")
  public List<RequirementResponse> requirements() {
    return checklist.requirements().stream().map(RequirementResponse::from).toList();
  }

  /**
   * Adds or changes a requirement (Claims Unit Head).
   *
   * @param request requirement
   * @return requirement
   */
  @PostMapping("/setup/document-requirements")
  @PreAuthorize("hasAuthority('BCL_SETUP')")
  public RequirementResponse save(@Valid @RequestBody RequirementRequest request) {
    return RequirementResponse.from(
        checklist.save(
            request.claimType(), request.documentType(), request.required(), request.active()));
  }

  /**
   * The accredited repair shops offered as Preferred Shop.
   *
   * @param companyId company
   * @return shops
   */
  @GetMapping("/repair-shops")
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public List<ShopResponse> repairShops(@RequestParam Long companyId) {
    return extras.repairShops(companyId).stream()
        .map(p -> new ShopResponse(p.getCode(), p.getName()))
        .toList();
  }

  /**
   * A requirement to save.
   *
   * @param claimType claim type
   * @param documentType document type
   * @param required whether required
   * @param active whether it applies
   */
  public record RequirementRequest(
      @NotBlank String claimType,
      @NotBlank String documentType,
      boolean required,
      boolean active) {}

  /**
   * A requirement.
   *
   * @param id id
   * @param claimType claim type
   * @param documentType document type
   * @param required whether required
   * @param active whether it applies
   */
  public record RequirementResponse(
      Long id, String claimType, String documentType, boolean required, boolean active) {

    static RequirementResponse from(DocumentRequirement r) {
      return new RequirementResponse(
          r.getId(), r.getClaimType(), r.getDocumentType(), r.isRequired(), r.isActive());
    }
  }

  /**
   * A repair shop.
   *
   * @param code party code
   * @param name name
   */
  public record ShopResponse(String code, String name) {}
}
