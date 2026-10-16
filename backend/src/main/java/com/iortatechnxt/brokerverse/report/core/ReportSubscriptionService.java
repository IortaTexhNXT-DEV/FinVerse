package com.iortatechnxt.brokerverse.report.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.security.DataScope;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription.Schedule;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscriptionRepository;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscriptionRun;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscriptionRunRepository;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Report subscriptions of the signed-in user (G04): subscribe to a report with its parameters,
 * frequency, format and recipients, change or stop a subscription, and read its runs. Subscribing
 * needs the export permission of the report and the companies and branches named in the parameters
 * within the user's data scope; both are checked again at every run ({@link
 * ReportSubscriptionRunner}).
 */
@Service
@Transactional
public class ReportSubscriptionService {

  private static final TypeReference<Map<String, String>> PARAMS = new TypeReference<>() {};

  private final ReportSubscriptionRepository subscriptions;
  private final ReportSubscriptionRunRepository runs;
  private final ReportRegistry registry;
  private final CurrentUser currentUser;
  private final DataScope dataScope;
  private final AuditTrailService audit;
  private final ObjectMapper json;

  /**
   * Creates the service.
   *
   * @param subscriptions subscriptions
   * @param runs runs
   * @param registry reports
   * @param currentUser signed-in user
   * @param dataScope data scope of the signed-in user
   * @param audit audit trail
   * @param json JSON (parameters)
   */
  public ReportSubscriptionService(
      ReportSubscriptionRepository subscriptions,
      ReportSubscriptionRunRepository runs,
      ReportRegistry registry,
      CurrentUser currentUser,
      DataScope dataScope,
      AuditTrailService audit,
      ObjectMapper json) {
    this.subscriptions = subscriptions;
    this.runs = runs;
    this.registry = registry;
    this.currentUser = currentUser;
    this.dataScope = dataScope;
    this.audit = audit;
    this.json = json;
  }

  /**
   * What a user subscribes to.
   *
   * @param parameters report parameters by name
   * @param frequency frequency
   * @param dayOfWeek day of a weekly run
   * @param dayOfMonth day of a monthly run
   * @param format file format
   * @param recipients e-mail addresses
   */
  public record Request(
      Map<String, String> parameters,
      ReportSubscription.Frequency frequency,
      java.time.DayOfWeek dayOfWeek,
      Integer dayOfMonth,
      ExportFormat format,
      List<String> recipients) {}

  /**
   * The subscriptions of the signed-in user.
   *
   * @return subscriptions, newest first
   */
  @Transactional(readOnly = true)
  public List<ReportSubscription> mine() {
    return subscriptions.findByUsernameOrderByIdDesc(currentUser.username());
  }

  /**
   * Subscribes the signed-in user to a report.
   *
   * @param reportCode report
   * @param request schedule
   * @return subscription
   */
  public ReportSubscription subscribe(String reportCode, Request request) {
    ReportDefinition def = registry.get(reportCode);
    requireAllowed(def, request.parameters());
    ReportSubscription saved =
        subscriptions.save(
            new ReportSubscription(reportCode, currentUser.username(), schedule(request)));
    audit.record(
        "ReportSubscription",
        String.valueOf(saved.getId()),
        AuditAction.CREATE,
        "Subscribed to " + reportCode + " " + request.frequency());
    return saved;
  }

  /**
   * Changes a subscription of the signed-in user.
   *
   * @param id subscription
   * @param request schedule
   * @return subscription
   */
  public ReportSubscription update(Long id, Request request) {
    ReportSubscription subscription = own(id);
    requireAllowed(registry.get(subscription.getReportCode()), request.parameters());
    subscription.update(schedule(request));
    audit.record("ReportSubscription", String.valueOf(id), AuditAction.UPDATE, "Changed");
    return subscription;
  }

  /**
   * Stops a subscription of the signed-in user.
   *
   * @param id subscription
   * @return subscription
   */
  public ReportSubscription deactivate(Long id) {
    ReportSubscription subscription = own(id);
    subscription.deactivate();
    audit.record("ReportSubscription", String.valueOf(id), AuditAction.DEACTIVATE, "Stopped");
    return subscription;
  }

  /**
   * The runs of a subscription of the signed-in user.
   *
   * @param id subscription
   * @return runs, newest first
   */
  @Transactional(readOnly = true)
  public List<ReportSubscriptionRun> runs(Long id) {
    return runs.findBySubscriptionIdOrderByRunDateDesc(own(id).getId());
  }

  /**
   * The parameters of a subscription.
   *
   * @param subscription subscription
   * @return parameters by name
   */
  public Map<String, String> parameters(ReportSubscription subscription) {
    try {
      return json.readValue(subscription.getParameters(), PARAMS);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unreadable parameters of subscription", e);
    }
  }

  private ReportSubscription own(Long id) {
    ReportSubscription subscription =
        subscriptions
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Report subscription", id));
    if (!CurrentUser.sameUser(subscription.getUsername(), currentUser.username())) {
      throw new ResourceNotFoundException("Report subscription", id);
    }
    return subscription;
  }

  private void requireAllowed(ReportDefinition def, Map<String, String> parameters) {
    if (!ReportAccess.mayExport(def.metadata())) {
      throw new AccessDeniedException("Not permitted to export report " + def.metadata().code());
    }
    Map<String, String> values = parameters == null ? Map.of() : parameters;
    for (ParameterSpec spec : def.metadata().parameters()) {
      String value = values.get(spec.name());
      if (value == null || value.isBlank()) {
        continue;
      }
      if (spec.type() == ParameterType.COMPANY) {
        dataScope.requireCompany(Long.valueOf(value));
      } else if (spec.type() == ParameterType.BRANCH) {
        dataScope.requireBranch(null, Long.valueOf(value));
      }
    }
  }

  private Schedule schedule(Request request) {
    if (request.frequency() == null || request.format() == null) {
      throw new BusinessRuleException(
          "SCHEDULE_REQUIRED", "Choose the frequency and the file format");
    }
    try {
      String parameters =
          json.writeValueAsString(
              new TreeMap<>(request.parameters() == null ? Map.of() : request.parameters()));
      return new Schedule(
          parameters,
          request.frequency(),
          request.dayOfWeek(),
          request.dayOfMonth(),
          request.format().name(),
          request.recipients() == null
              ? List.of()
              : request.recipients().stream()
                  .map(String::strip)
                  .filter(r -> !r.isEmpty())
                  .toList());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Parameters cannot be stored", e);
    }
  }
}
