package com.iortatechnxt.brokerverse.frbs.api;

import com.iortatechnxt.brokerverse.frbs.service.ReportPackService;
import com.iortatechnxt.brokerverse.frbs.service.ReportPackService.PackEntry;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The BDOI report pack (FRBS 3.2.0, Appendix A). */
@RestController
@RequestMapping("/api/v1/frbs/report-pack")
public class ReportPackController {

  private final ReportPackService pack;

  /**
   * Creates the controller.
   *
   * @param pack report pack
   */
  public ReportPackController(ReportPackService pack) {
    this.pack = pack;
  }

  /**
   * The report groups and their reports.
   *
   * @return entries in order
   */
  @GetMapping
  @PreAuthorize("hasAuthority('FRBS_REPORT_VIEW')")
  public List<PackEntry> entries() {
    return pack.entries();
  }
}
