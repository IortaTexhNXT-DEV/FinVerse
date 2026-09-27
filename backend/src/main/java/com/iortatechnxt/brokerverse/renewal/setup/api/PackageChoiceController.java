package com.iortatechnxt.brokerverse.renewal.setup.api;

import com.iortatechnxt.brokerverse.renewal.setup.api.dto.SetupDtos.PackageChoiceView;
import com.iortatechnxt.brokerverse.renewal.setup.service.PackageChoiceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Package choices of migrated renewals in the Exception bucket (DMQ36): the Renewal processing team
 * chooses the BIBS package version, another member approves it.
 */
@RestController
@RequestMapping("/api/v1/renewal")
public class PackageChoiceController {

  private static final String REMAP = "hasAuthority('RNW_PACKAGE_REMAP')";

  private final PackageChoiceService choices;

  /**
   * Creates the controller.
   *
   * @param choices package choices
   */
  public PackageChoiceController(PackageChoiceService choices) {
    this.choices = choices;
  }

  /**
   * Choices of a renewal.
   *
   * @param companyId company
   * @param ref renewal reference
   * @return choices, newest first
   */
  @GetMapping("/candidates/{ref}/package-choices")
  @PreAuthorize("hasAuthority('RNW_VIEW')")
  public List<PackageChoiceView> of(@RequestParam Long companyId, @PathVariable String ref) {
    return choices.of(companyId, ref).stream().map(PackageChoiceView::of).toList();
  }

  /**
   * Chooses the package version of a renewal (maker).
   *
   * @param companyId company
   * @param ref renewal reference
   * @param request choice
   * @return choice
   */
  @PostMapping("/candidates/{ref}/package-choices")
  @PreAuthorize(REMAP)
  public PackageChoiceView propose(
      @RequestParam Long companyId,
      @PathVariable String ref,
      @Valid @RequestBody ChoiceRequest request) {
    return PackageChoiceView.of(
        choices.propose(
            companyId, ref, request.productCode(), request.productVersionNo(), request.reason()));
  }

  /**
   * Choices waiting for approval.
   *
   * @return choices
   */
  @GetMapping("/package-choices/pending")
  @PreAuthorize(REMAP)
  public List<PendingView> pending() {
    return choices.pending().stream()
        .map(p -> new PendingView(PackageChoiceView.of(p.choice()), p.renewalRef(), p.clientName()))
        .toList();
  }

  /**
   * A choice waiting for approval.
   *
   * @param choice choice
   * @param renewalRef renewal reference
   * @param clientName client
   */
  public record PendingView(PackageChoiceView choice, String renewalRef, String clientName) {}

  /**
   * Decides a choice (checker).
   *
   * @param id choice
   * @param request decision
   * @return choice
   */
  @PostMapping("/package-choices/{id}/decision")
  @PreAuthorize(REMAP)
  public PackageChoiceView decide(@PathVariable Long id, @RequestBody DecisionRequest request) {
    return PackageChoiceView.of(choices.decide(id, request.approve(), request.remarks()));
  }

  /**
   * A choice.
   *
   * @param productCode package (risk code)
   * @param productVersionNo version
   * @param reason reason
   */
  public record ChoiceRequest(
      @NotBlank String productCode, @NotNull Integer productVersionNo, @NotBlank String reason) {}

  /**
   * A decision.
   *
   * @param approve approve or reject
   * @param remarks remarks
   */
  public record DecisionRequest(boolean approve, String remarks) {}
}
