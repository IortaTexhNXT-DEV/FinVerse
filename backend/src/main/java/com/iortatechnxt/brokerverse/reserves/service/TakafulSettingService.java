package com.iortatechnxt.brokerverse.reserves.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.reserves.domain.TakafulSetting;
import com.iortatechnxt.brokerverse.reserves.domain.TakafulSettingRepository;
import com.iortatechnxt.brokerverse.reserves.domain.TakafulTerms;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Takaful surplus settings of a company (maker-checker). The surplus run is optional: valuation
 * runs compute it only while the settings are enabled and authorized.
 */
@Service
@Transactional
public class TakafulSettingService {

  /** Audit entity name. */
  public static final String ENTITY = "TakafulSetting";

  private final TakafulSettingRepository repository;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param repository settings repository
   * @param audit audit trail
   * @param currentUser current user (checker)
   * @param clock clock
   */
  public TakafulSettingService(
      TakafulSettingRepository repository,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.repository = repository;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Settings of a company.
   *
   * @param companyId company
   * @return settings, empty when never configured
   */
  @Transactional(readOnly = true)
  public Optional<TakafulSetting> find(Long companyId) {
    return repository.findByCompanyId(companyId);
  }

  /**
   * Settings used by valuation runs: authorized and enabled.
   *
   * @param companyId company
   * @return settings in force
   */
  @Transactional(readOnly = true)
  public Optional<TakafulSetting> inForce(Long companyId) {
    return find(companyId).filter(s -> s.isActive() && s.isEnabled());
  }

  /**
   * Creates or changes the settings of a company (pending authorization).
   *
   * @param companyId company
   * @param terms values
   * @return settings
   */
  public TakafulSetting save(Long companyId, TakafulTerms terms) {
    Optional<TakafulSetting> existing = find(companyId);
    TakafulSetting setting;
    if (existing.isPresent()) {
      setting = existing.get();
      setting.update(terms);
    } else {
      setting = repository.save(new TakafulSetting(companyId, terms));
    }
    audit.record(ENTITY, companyId, AuditAction.UPDATE, "Takaful settings saved: " + terms);
    return setting;
  }

  /**
   * Authorizes the settings of a company (checker).
   *
   * @param companyId company
   * @return settings
   */
  public TakafulSetting authorize(Long companyId) {
    TakafulSetting setting =
        find(companyId).orElseThrow(() -> new ResourceNotFoundException(ENTITY, companyId));
    setting.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, companyId, AuditAction.AUTHORIZE, "Takaful settings authorized");
    return setting;
  }
}
