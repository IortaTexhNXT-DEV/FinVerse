package com.iortatechnxt.brokerverse.opsledger.api;

import com.iortatechnxt.brokerverse.opsledger.api.dto.OperationsHomeResponse;
import com.iortatechnxt.brokerverse.opsledger.service.OperationsHomeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The Operations home (BRQID.003): team sections with work counts and external links. */
@RestController
@RequestMapping("/api/v1/ops")
public class OperationsHomeController {

  private final OperationsHomeService home;

  /**
   * Creates the controller.
   *
   * @param home home service
   */
  public OperationsHomeController(OperationsHomeService home) {
    this.home = home;
  }

  /**
   * The current user's Operations home.
   *
   * @param companyId company
   * @return sections and links
   */
  @GetMapping("/home")
  @PreAuthorize(OpsAccess.VIEW)
  public OperationsHomeResponse home(@RequestParam Long companyId) {
    return new OperationsHomeResponse(home.sections(companyId), home.links());
  }
}
