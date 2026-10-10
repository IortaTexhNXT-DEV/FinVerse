package com.iortatechnxt.brokerverse.renewal.kyc.api;

import com.iortatechnxt.brokerverse.renewal.kyc.service.KycMonitoring;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** KYC monitoring of the renewal accounts (FRRN.039): dashboard, drill-down, account history. */
@RestController
@RequestMapping("/api/v1/renewal")
public class KycMonitoringController {

  private static final String VIEW = "hasAuthority('RNW_VIEW')";

  private final KycMonitoring monitoring;

  /**
   * Creates the controller.
   *
   * @param monitoring KYC monitoring
   */
  public KycMonitoringController(KycMonitoring monitoring) {
    this.monitoring = monitoring;
  }

  /**
   * The KYC Monitoring Dashboard counts.
   *
   * @param companyId company
   * @return counts
   */
  @GetMapping("/kyc/dashboard")
  @PreAuthorize(VIEW)
  public Map<String, Object> dashboard(@RequestParam Long companyId) {
    return monitoring.dashboard(companyId);
  }

  /**
   * The accounts of a KYC status.
   *
   * @param companyId company
   * @param status status
   * @return accounts
   */
  @GetMapping("/kyc/accounts")
  @PreAuthorize(VIEW)
  public List<Map<String, Object>> accounts(
      @RequestParam Long companyId, @RequestParam String status) {
    return monitoring.accounts(companyId, status);
  }

  /**
   * Refreshes the KYC status of the open accounts now.
   *
   * @param companyId company
   * @return accounts changed
   */
  @PostMapping("/kyc/refresh")
  @PreAuthorize("hasAnyAuthority('RNW_DISPOSE','RNW_PROCESS')")
  public Map<String, Integer> refresh(@RequestParam Long companyId) {
    return Map.of("changed", monitoring.refresh(companyId));
  }

  /**
   * The KYC status and history of an account.
   *
   * @param companyId company
   * @param ref renewal
   * @return monitoring
   */
  @GetMapping("/candidates/{ref}/kyc")
  @PreAuthorize(VIEW)
  public KycMonitoring.Monitoring of(@RequestParam Long companyId, @PathVariable String ref) {
    return monitoring.of(companyId, ref);
  }

  /**
   * Records a KYC review activity.
   *
   * @param companyId company
   * @param ref renewal
   * @param body status, activity and remarks
   */
  @PostMapping("/candidates/{ref}/kyc")
  @PreAuthorize("hasAnyAuthority('RNW_DISPOSE','RNW_PROCESS')")
  public void record(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody Activity body) {
    monitoring.record(companyId, ref, body.status(), body.activity(), body.remarks());
  }

  /**
   * A KYC review activity.
   *
   * @param status new status
   * @param activity activity
   * @param remarks remarks or follow-up note
   */
  public record Activity(String status, String activity, String remarks) {}
}
