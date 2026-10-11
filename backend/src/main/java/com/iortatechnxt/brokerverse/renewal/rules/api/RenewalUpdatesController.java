package com.iortatechnxt.brokerverse.renewal.rules.api;

import com.iortatechnxt.brokerverse.renewal.rules.service.RenewalUpdatesService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Refresh Endorsements (BDOI Renewal FRS FRRN.004.06) and the CBG Motor automatic renewal values of
 * a renewal account (FRRN.009.01) next to its expiring values and its renewal account.
 */
@RestController
@RequestMapping("/api/v1/renewal/candidates/{ref}")
public class RenewalUpdatesController {

  private final RenewalUpdatesService updates;

  /**
   * Creates the controller.
   *
   * @param updates endorsements and automatic values
   */
  public RenewalUpdatesController(RenewalUpdatesService updates) {
    this.updates = updates;
  }

  /**
   * Re-reads the endorsements of the mother policy, refreshes the renewal account and re-runs the
   * checks.
   *
   * @param companyId company
   * @param ref renewal reference
   * @return the endorsements linked
   */
  @PostMapping("/refresh-endorsements")
  @PreAuthorize("hasAnyAuthority('RNW_DISPOSE','RNW_PROCESS','RNW_REVIEW')")
  public List<RenewalUpdatesService.EndorsementRow> refresh(
      @RequestParam Long companyId, @PathVariable String ref) {
    return updates.refreshEndorsements(companyId, ref);
  }

  /**
   * The automatic values of a CBG Motor renewal.
   *
   * @param companyId company
   * @param ref renewal reference
   * @return one row per field: expiring, automatic renewal value and the renewal account's
   */
  @GetMapping("/auto-update")
  @PreAuthorize("hasAuthority('RNW_VIEW')")
  public List<RenewalUpdatesService.ValueRow> autoUpdate(
      @RequestParam Long companyId, @PathVariable String ref) {
    return updates.autoUpdate(companyId, ref);
  }
}
