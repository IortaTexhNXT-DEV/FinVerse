package com.iortatechnxt.finverse.alert.service;

import com.iortatechnxt.finverse.alert.domain.Alert;
import com.iortatechnxt.finverse.alert.domain.AlertFacts;
import com.iortatechnxt.finverse.alert.domain.AlertRepository;
import com.iortatechnxt.finverse.alert.domain.AlertSeverity;
import com.iortatechnxt.finverse.alert.domain.AlertStatus;
import com.iortatechnxt.finverse.alert.domain.ExceptionCode;
import com.iortatechnxt.finverse.alert.domain.ExceptionCodeRepository;
import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alert engine entry point. Modules raise exceptions with {@link #raise}; users acknowledge and
 * resolve them; administrators tune the Exception Codes Master. Raising is idempotent per
 * condition: while an alert with the same dedup key is open or acknowledged, nothing new is raised.
 */
@Service
@Transactional
public class AlertService {

  private static final String ALERT = "Alert";

  private final AlertRepository alerts;
  private final ExceptionCodeRepository codes;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param alerts alert repository
   * @param codes exception code repository
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public AlertService(
      AlertRepository alerts,
      ExceptionCodeRepository codes,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.alerts = alerts;
    this.codes = codes;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Raises an alert unless the code is inactive or the condition already has a live alert.
   *
   * @param code exception code
   * @param facts what and where
   * @return the new alert, empty when nothing was raised
   */
  public Optional<Alert> raise(String code, AlertFacts facts) {
    Optional<ExceptionCode> exceptionCode = activeCode(code);
    if (exceptionCode.isEmpty()
        || alerts.existsByDedupKeyAndStatusNot(facts.dedupKey(), AlertStatus.RESOLVED)) {
      return Optional.empty();
    }
    return Optional.of(alerts.save(new Alert(exceptionCode.get(), facts, clock.instant())));
  }

  /**
   * Returns an exception code when it is active (rules read their thresholds from it).
   *
   * @param code exception code
   * @return active code, empty when unknown or inactive
   */
  @Transactional(readOnly = true)
  public Optional<ExceptionCode> activeCode(String code) {
    return codes.findByCode(code).filter(ExceptionCode::isActive);
  }

  /**
   * Searches alerts.
   *
   * @param criteria filters
   * @param pageable paging
   * @return alerts, newest first
   */
  @Transactional(readOnly = true)
  public Page<Alert> search(AlertSearch criteria, Pageable pageable) {
    return alerts.search(
        criteria.status(),
        criteria.severity(),
        criteria.code(),
        criteria.companyId(),
        criteria.from(),
        criteria.to(),
        pageable);
  }

  /**
   * Live (open or acknowledged) alerts per severity.
   *
   * @return counts for every severity
   */
  @Transactional(readOnly = true)
  public Map<AlertSeverity, Long> liveSummary() {
    Map<AlertSeverity, Long> result = new EnumMap<>(AlertSeverity.class);
    for (AlertSeverity s : AlertSeverity.values()) {
      result.put(s, 0L);
    }
    alerts
        .countLiveBySeverity(AlertStatus.RESOLVED)
        .forEach(c -> result.put(c.getSeverity(), c.getTotal()));
    return result;
  }

  /**
   * Number of live (open or acknowledged) alerts of a company.
   *
   * @param companyId company
   * @return count
   */
  @Transactional(readOnly = true)
  public long liveCount(Long companyId) {
    return alerts.countByCompanyIdAndStatusNot(companyId, AlertStatus.RESOLVED);
  }

  /**
   * Acknowledges an alert.
   *
   * @param id id
   * @param comment optional comment
   * @return alert
   */
  public Alert acknowledge(Long id, String comment) {
    Alert alert = get(id);
    alert.acknowledge(currentUser.username(), clock.instant(), comment);
    audit.record(ALERT, id, AuditAction.UPDATE, "Acknowledged " + alert.getExceptionCode());
    return alert;
  }

  /**
   * Resolves an alert.
   *
   * @param id id
   * @param comment resolution comment
   * @return alert
   */
  public Alert resolve(Long id, String comment) {
    Alert alert = get(id);
    alert.resolve(currentUser.username(), clock.instant(), comment);
    audit.record(
        ALERT, id, AuditAction.UPDATE, "Resolved " + alert.getExceptionCode() + ": " + comment);
    return alert;
  }

  /**
   * Gets an alert.
   *
   * @param id id
   * @return alert
   */
  @Transactional(readOnly = true)
  public Alert get(Long id) {
    return alerts.findById(id).orElseThrow(() -> new ResourceNotFoundException(ALERT, id));
  }

  /**
   * Exception Codes Master.
   *
   * @return codes
   */
  @Transactional(readOnly = true)
  public List<ExceptionCode> codes() {
    return codes.findAllByOrderByModuleAscCodeAsc();
  }

  /**
   * Tunes an exception code (administrator action, audited).
   *
   * @param code code
   * @param settings new settings
   * @return code
   */
  public ExceptionCode configure(String code, CodeSettings settings) {
    ExceptionCode entity =
        codes
            .findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Exception code", code));
    entity.configure(
        settings.severity(),
        settings.thresholdAmount(),
        settings.thresholdDays(),
        settings.active());
    audit.record(
        "ExceptionCode",
        code,
        AuditAction.UPDATE,
        "Set severity "
            + settings.severity()
            + ", threshold amount "
            + settings.thresholdAmount()
            + ", threshold days "
            + settings.thresholdDays()
            + ", active "
            + settings.active());
    return entity;
  }

  /**
   * Alert search filters (null = any).
   *
   * @param status status
   * @param severity severity
   * @param code exception code
   * @param companyId company
   * @param from inclusive lower bound
   * @param to exclusive upper bound
   */
  public record AlertSearch(
      AlertStatus status,
      AlertSeverity severity,
      String code,
      Long companyId,
      Instant from,
      Instant to) {}

  /**
   * Tunable settings of an exception code.
   *
   * @param severity severity
   * @param thresholdAmount threshold amount (null = not used)
   * @param thresholdDays threshold days (null = not used)
   * @param active monitored
   */
  public record CodeSettings(
      AlertSeverity severity, BigDecimal thresholdAmount, Integer thresholdDays, boolean active) {}
}
