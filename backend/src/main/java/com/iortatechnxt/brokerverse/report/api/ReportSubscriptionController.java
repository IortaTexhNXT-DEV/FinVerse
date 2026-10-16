package com.iortatechnxt.brokerverse.report.api;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.report.api.dto.ReportSubscriptionDtos;
import com.iortatechnxt.brokerverse.report.api.dto.ReportSubscriptionDtos.RunResponse;
import com.iortatechnxt.brokerverse.report.api.dto.ReportSubscriptionDtos.SubscriptionRequest;
import com.iortatechnxt.brokerverse.report.api.dto.ReportSubscriptionDtos.SubscriptionResponse;
import com.iortatechnxt.brokerverse.report.core.ReportSubscriptionService;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Report subscriptions of the signed-in user (G04): subscribe to a report with its parameters,
 * frequency, format and recipients, change or stop it, and follow its runs. The report's export
 * permission and the data scope are checked on subscribing and again at every run.
 */
@RestController
@RequestMapping("/api/v1/report-subscriptions")
@PreAuthorize("hasAuthority('REPORT_VIEW')")
public class ReportSubscriptionController {

  private final ReportSubscriptionService subscriptions;

  /**
   * Creates the controller.
   *
   * @param subscriptions subscriptions
   */
  public ReportSubscriptionController(ReportSubscriptionService subscriptions) {
    this.subscriptions = subscriptions;
  }

  /**
   * The subscriptions of the signed-in user.
   *
   * @return subscriptions
   */
  @GetMapping
  @PreAuthorize("hasAuthority('REPORT_VIEW')")
  public List<SubscriptionResponse> mine() {
    return subscriptions.mine().stream().map(this::response).toList();
  }

  /**
   * Subscribes to a report.
   *
   * @param request report and schedule
   * @return subscription
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('REPORT_VIEW')")
  public SubscriptionResponse subscribe(@Valid @RequestBody SubscriptionRequest request) {
    if (!ReportSubscriptionDtos.hasReport(request)) {
      throw new BusinessRuleException("REPORT_REQUIRED", "Choose the report");
    }
    return response(subscriptions.subscribe(request.reportCode(), request.toRequest()));
  }

  /**
   * Changes a subscription.
   *
   * @param id subscription
   * @param request schedule
   * @return subscription
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('REPORT_VIEW')")
  public SubscriptionResponse update(
      @PathVariable Long id, @Valid @RequestBody SubscriptionRequest request) {
    return response(subscriptions.update(id, request.toRequest()));
  }

  /**
   * Stops a subscription.
   *
   * @param id subscription
   * @return subscription
   */
  @PostMapping("/{id}/deactivate")
  @PreAuthorize("hasAuthority('REPORT_VIEW')")
  public SubscriptionResponse deactivate(@PathVariable Long id) {
    return response(subscriptions.deactivate(id));
  }

  /**
   * The runs of a subscription.
   *
   * @param id subscription
   * @return runs, newest first
   */
  @GetMapping("/{id}/runs")
  @PreAuthorize("hasAuthority('REPORT_VIEW')")
  public List<RunResponse> runs(@PathVariable Long id) {
    return subscriptions.runs(id).stream().map(RunResponse::from).toList();
  }

  private SubscriptionResponse response(ReportSubscription s) {
    return SubscriptionResponse.from(s, subscriptions.parameters(s));
  }
}
