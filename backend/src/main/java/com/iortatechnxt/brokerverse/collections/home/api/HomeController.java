package com.iortatechnxt.brokerverse.collections.home.api;

import com.iortatechnxt.brokerverse.collections.home.api.dto.HomeResponse;
import com.iortatechnxt.brokerverse.collections.home.service.CollectionsHomeService;
import com.iortatechnxt.brokerverse.collections.worklist.api.ClxAccess;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The Collections home (COLLECTIONS_DESIGN 11, CQ21): work tiles and the aging chart. */
@RestController
@RequestMapping("/api/v1/collections")
public class HomeController {

  private final CollectionsHomeService home;

  /**
   * Creates the controller.
   *
   * @param home home
   */
  public HomeController(CollectionsHomeService home) {
    this.home = home;
  }

  /**
   * The home of the signed-in user.
   *
   * @param companyId company
   * @return tiles and aging chart
   */
  @GetMapping("/home")
  @PreAuthorize(ClxAccess.HOME)
  public HomeResponse home(@RequestParam Long companyId) {
    return HomeResponse.from(home.home(companyId));
  }
}
