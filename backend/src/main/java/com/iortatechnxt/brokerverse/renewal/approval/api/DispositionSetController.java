package com.iortatechnxt.brokerverse.renewal.approval.api;

import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The dispositions offered to Marketing (FRRN.014.02): with the setting CLIENT, For Renewal, Not
 * for Renewal, For Quotation and For Proposal, Lost Business being a reason of Not for Renewal;
 * with SYSTEM also Lost Business.
 */
@RestController
@RequestMapping("/api/v1/renewal/dispositions")
public class DispositionSetController {

  private final RenewalParameters parameters;

  /**
   * Creates the controller.
   *
   * @param parameters disposition set
   */
  public DispositionSetController(RenewalParameters parameters) {
    this.parameters = parameters;
  }

  /**
   * The dispositions offered.
   *
   * @return code and label of each disposition
   */
  @GetMapping
  @PreAuthorize("hasAuthority('RNW_VIEW')")
  public List<Option> offered() {
    boolean lostIsReason = parameters.lostBusinessIsReason();
    return Arrays.stream(RenewalDisposition.values())
        .filter(d -> !lostIsReason || d != RenewalDisposition.LOST_BUSINESS)
        .map(d -> new Option(d.name(), d.label()))
        .toList();
  }

  /**
   * A disposition.
   *
   * @param code code
   * @param label label
   */
  public record Option(String code, String label) {}
}
