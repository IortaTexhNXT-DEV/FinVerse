package com.iortatechnxt.brokerverse.renewal.extraction.api;

import com.iortatechnxt.brokerverse.renewal.extraction.service.ManualCreationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Create Renewal Account (BDOI Renewal FRS FRRN.005.01): the renewal account of one expiring
 * account, with the duplicate warnings of FRRN.006.01.
 */
@RestController
@RequestMapping("/api/v1/renewal/manual-accounts")
@PreAuthorize("hasAnyAuthority('RNW_EXTRACT','RNW_DISPOSE','RNW_PROCESS')")
public class ManualCreationController {

  private final ManualCreationService manual;

  /**
   * Creates the controller.
   *
   * @param manual manual creation
   */
  public ManualCreationController(ManualCreationService manual) {
    this.manual = manual;
  }

  /**
   * Creates the renewal account of an expiring account, or returns the duplicates found.
   *
   * @param companyId company
   * @param body expiring invoice and the confirmation after a potential duplicate warning
   * @return outcome
   */
  @PostMapping
  public ManualCreationService.Result create(
      @RequestParam Long companyId, @Valid @RequestBody Body body) {
    return manual.create(companyId, body.invoiceNo().strip(), body.confirmed());
  }

  /**
   * A manual creation.
   *
   * @param invoiceNo invoice number of the expiring account
   * @param confirmed whether the user confirmed after a potential duplicate warning
   */
  public record Body(@NotBlank String invoiceNo, boolean confirmed) {}
}
