package com.iortatechnxt.brokerverse.renewal.home.api;

import com.iortatechnxt.brokerverse.renewal.home.service.RenewalHomeService;
import com.iortatechnxt.brokerverse.renewal.home.service.RenewalHomeService.Home;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Renewal Home (FR-RN-100). */
@RestController
@RequestMapping("/api/v1/renewal/home")
public class RenewalHomeController {

  private final RenewalHomeService home;

  /**
   * Creates the controller.
   *
   * @param home home figures
   */
  public RenewalHomeController(RenewalHomeService home) {
    this.home = home;
  }

  /**
   * The home figures of the user's scope.
   *
   * @param companyId company
   * @return figures
   */
  @GetMapping
  @PreAuthorize("hasAuthority('RNW_VIEW')")
  public Home get(@RequestParam Long companyId) {
    return home.home(companyId);
  }
}
