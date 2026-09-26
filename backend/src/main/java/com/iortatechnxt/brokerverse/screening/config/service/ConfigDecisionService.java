package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigStatus;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigVersion;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigVersionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checker decisions on screening configuration versions (SNSRP-109; FR-SS-019): approve (ACTIVE
 * from the effective date), reject with a reason, and the supersession of a version once its
 * successor is in force. The maker never decides.
 */
@Service
@Transactional
public class ConfigDecisionService {

  private final ConfigVersionRepository versions;
  private final AuditTrailService audit;
  private final NotificationService notifications;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param versions versions
   * @param audit audit trail
   * @param notifications in-app notifications
   * @param currentUser current user
   * @param clock clock
   */
  public ConfigDecisionService(
      ConfigVersionRepository versions,
      AuditTrailService audit,
      NotificationService notifications,
      CurrentUser currentUser,
      Clock clock) {
    this.versions = versions;
    this.audit = audit;
    this.notifications = notifications;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  private ConfigVersion get(Long id) {
    return versions
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Screening configuration version", id));
  }

  /**
   * Approves a pending version (FR-SS-019): ACTIVE from its effective date; the version it replaces
   * is SUPERSEDED on that date. The maker is notified.
   *
   * @param id version id
   * @return the ACTIVE version
   */
  public ConfigVersion approve(Long id) {
    ConfigVersion version = get(id);
    LocalDate today = today();
    String checker = currentUser.username();
    version.requireDecidableBy(checker);
    LocalDate effective =
        version.getEffectiveFrom().isBefore(today) ? today : version.getEffectiveFrom();
    versions
        .findByCompanyIdAndConfigTypeAndScopeAndStatusOrderByEffectiveFromDesc(
            version.getCompanyId(),
            version.getConfigType(),
            ConfigVersions.scopeKey(version),
            ConfigStatus.ACTIVE)
        .stream()
        .filter(v -> v.getEffectiveFrom().equals(effective))
        .forEach(ConfigVersion::supersede);
    versions.flush();
    version.approve(checker, clock.instant(), today);
    versions.flush();
    supersedeDue(today);
    audit.record(
        ConfigVersions.ENTITY,
        version.getId(),
        AuditAction.AUTHORIZE,
        ConfigVersions.VERSION
            + ConfigVersions.label(version)
            + " approved, effective "
            + version.getEffectiveFrom());
    notifyMaker(version, "approved, effective " + version.getEffectiveFrom());
    return version;
  }

  /**
   * Rejects a pending version with a reason (FR-SS-019); the version in force stays.
   *
   * @param id version id
   * @param reason mandatory reason
   * @return the REJECTED version
   */
  public ConfigVersion reject(Long id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException(
          "SCR_REJECT_REASON_REQUIRED", "Enter the reason for the rejection");
    }
    ConfigVersion version = get(id);
    version.reject(currentUser.username(), clock.instant(), reason.trim());
    audit.record(
        ConfigVersions.ENTITY,
        version.getId(),
        AuditAction.REJECT,
        ConfigVersions.VERSION + ConfigVersions.label(version) + ": " + reason);
    notifyMaker(version, "rejected: " + reason.trim());
    return version;
  }

  /**
   * Marks as SUPERSEDED every ACTIVE version that is no longer in force because a newer ACTIVE
   * version of the same type and scope has taken effect.
   *
   * @param today business date
   * @return number of versions superseded
   */
  public int supersedeDue(LocalDate today) {
    Map<String, List<ConfigVersion>> groups =
        versions.findByStatus(ConfigStatus.ACTIVE).stream()
            .collect(
                Collectors.groupingBy(
                    v ->
                        v.getCompanyId()
                            + "|"
                            + v.getConfigType()
                            + "|"
                            + ConfigVersions.scopeKey(v)));
    int count = 0;
    for (List<ConfigVersion> group : groups.values()) {
      Optional<LocalDate> inForce =
          group.stream()
              .map(ConfigVersion::getEffectiveFrom)
              .filter(d -> !d.isAfter(today))
              .max(Comparator.naturalOrder());
      if (inForce.isEmpty()) {
        continue;
      }
      for (ConfigVersion v : group) {
        if (v.getEffectiveFrom().isBefore(inForce.get())) {
          v.supersede();
          count++;
        }
      }
    }
    return count;
  }

  private void notifyMaker(ConfigVersion version, String outcome) {
    String maker =
        version.getSubmittedBy() != null ? version.getSubmittedBy() : version.getCreatedBy();
    notifications.notifyUser(
        maker, ConfigVersions.notice(version, "Screening configuration decided", outcome));
  }

  private LocalDate today() {
    return LocalDate.now(clock);
  }
}
