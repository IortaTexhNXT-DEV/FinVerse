package com.iortatechnxt.brokerverse.migration.home.api;

import com.iortatechnxt.brokerverse.migration.home.service.MigrationHomeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Migration Home (screen Migration Home): the tiles of the migration console. */
@RestController
@RequestMapping("/api/v1/migration/home")
public class MigrationHomeController {

  private final MigrationHomeService home;

  /**
   * Creates the controller.
   *
   * @param home tiles
   */
  public MigrationHomeController(MigrationHomeService home) {
    this.home = home;
  }

  /**
   * The tiles of a company.
   *
   * @param companyId company
   * @return tiles
   */
  @GetMapping
  @PreAuthorize("hasAuthority('MIG_VIEW')")
  public MigrationHomeService.Tiles tiles(@RequestParam Long companyId) {
    return home.tiles(companyId);
  }
}
