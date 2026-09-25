package com.iortatechnxt.brokerverse.screening.config.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigStatus;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigVersion;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigVersionRepository;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateType;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maker-checker life cycle of screening configuration versions (SNSRP-101-109; FR-SS-010, 019): new
 * draft (a copy of the latest approved version), save (validated rows), submit (difference against
 * the version in force), approve (ACTIVE from the effective date), reject (reason) and withdraw. An
 * ACTIVE version becomes SUPERSEDED once a newer version is in force.
 */
@Service
@Transactional
public class ConfigVersionService {

  /** Notification event of a version submitted for approval. */
  static final String EVENT_TO_APPROVE = "SCR_CONFIG_TO_APPROVE";

  private static final String APPROVE_PERMISSION = "SCR_CONFIG_APPROVE";
  private static final List<ConfigStatus> IN_FORCE =
      List.of(ConfigStatus.ACTIVE, ConfigStatus.SUPERSEDED);
  private static final List<ConfigStatus> OPEN = List.of(ConfigStatus.DRAFT, ConfigStatus.PENDING);
  private static final TypeReference<List<ConfigChange>> CHANGES = new TypeReference<>() {};

  private final ConfigVersionRepository versions;
  private final ConfigContentStore store;
  private final ConfigValidator validator;
  private final ConfigDecisionService decisions;
  private final AuditTrailService audit;
  private final NotificationService notifications;
  private final CurrentUser currentUser;
  private final ObjectMapper json;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param versions versions
   * @param store version rows
   * @param validator row checks
   * @param decisions checker decisions (supersession)
   * @param audit audit trail
   * @param notifications in-app notifications
   * @param currentUser current user
   * @param json JSON mapper (stored differences)
   * @param clock clock
   */
  public ConfigVersionService(
      ConfigVersionRepository versions,
      ConfigContentStore store,
      ConfigValidator validator,
      ConfigDecisionService decisions,
      AuditTrailService audit,
      NotificationService notifications,
      CurrentUser currentUser,
      ObjectMapper json,
      Clock clock) {
    this.versions = versions;
    this.store = store;
    this.validator = validator;
    this.decisions = decisions;
    this.audit = audit;
    this.notifications = notifications;
    this.currentUser = currentUser;
    this.json = json;
    this.clock = clock;
  }

  /**
   * The versions of a type (all template types for TEMPLATE), newest first. Versions whose
   * successor is in force are marked SUPERSEDED first.
   *
   * @param companyId company
   * @param type type
   * @return versions
   */
  public List<ConfigVersion> versions(Long companyId, ConfigType type) {
    decisions.supersedeDue(today());
    return versions.findByCompanyIdAndConfigTypeOrderByScopeAscVersionNoDesc(companyId, type);
  }

  /**
   * One version.
   *
   * @param id version id
   * @return the version
   */
  @Transactional(readOnly = true)
  public ConfigVersion get(Long id) {
    return versions
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Screening configuration version", id));
  }

  /**
   * The rows of a version.
   *
   * @param version version
   * @return content
   */
  @Transactional(readOnly = true)
  public ConfigContent content(ConfigVersion version) {
    return store.load(version.getId(), version.getConfigType());
  }

  /**
   * The version in force on a date.
   *
   * @param companyId company
   * @param type type
   * @param scope template type for TEMPLATE, otherwise {@code null}
   * @param asOf date
   * @return the version, empty when none
   */
  @Transactional(readOnly = true)
  public Optional<ConfigVersion> inForce(
      Long companyId, ConfigType type, String scope, LocalDate asOf) {
    return versions
        .inForce(companyId, type, ConfigVersions.scopeKey(type, scope), IN_FORCE, asOf)
        .stream()
        .findFirst();
  }

  /**
   * Opens a draft of a type: the existing draft when there is one (FR-SS-010 alternate flow), or a
   * new draft copied from the latest approved version.
   *
   * @param companyId company
   * @param type type
   * @param scope template type for TEMPLATE, otherwise {@code null}
   * @return the draft
   */
  public ConfigVersion newDraft(Long companyId, ConfigType type, String scope) {
    String key = ConfigVersions.scopeKey(type, scope);
    Optional<ConfigVersion> open =
        versions.findFirstByCompanyIdAndConfigTypeAndScopeAndStatusInOrderByVersionNoDesc(
            companyId, type, key, OPEN);
    if (open.isPresent()) {
      ConfigVersion existing = open.get();
      if (existing.getStatus() == ConfigStatus.PENDING) {
        throw new BusinessRuleException(
            "SCR_CONFIG_PENDING",
            ConfigVersions.VERSION
                + existing.getVersionNo()
                + " is waiting for approval; a new draft can be made once it is decided");
      }
      return existing;
    }
    Optional<ConfigVersion> base =
        versions.findFirstByCompanyIdAndConfigTypeAndScopeAndStatusInOrderByVersionNoDesc(
            companyId, type, key, List.of(ConfigStatus.ACTIVE));
    ConfigVersion draft =
        versions.save(
            new ConfigVersion(
                companyId,
                type,
                key,
                versions.maxVersionNo(companyId, type, key) + 1,
                today(),
                base.map(ConfigVersion::getId).orElse(null)));
    ConfigContent content = base.map(this::content).orElseGet(() -> emptyContent(type, key));
    store.replace(draft.getId(), type, content);
    audit.record(
        ConfigVersions.ENTITY,
        draft.getId(),
        AuditAction.CREATE,
        "Draft "
            + ConfigVersions.label(draft)
            + base.map(b -> " copied from version " + b.getVersionNo()).orElse(""));
    return draft;
  }

  private static ConfigContent emptyContent(ConfigType type, String scope) {
    if (type == ConfigType.TEMPLATE) {
      TemplateType templateType = TemplateType.valueOf(scope);
      return ConfigContent.ofTemplate(
          new ConfigContent.Template(templateType, templateType.name(), List.of()));
    }
    return ConfigContent.empty();
  }

  /**
   * Saves a draft: header and rows (validated, FR-SS-011 to 017).
   *
   * @param id draft id
   * @param effectiveFrom effective date (today or later)
   * @param changeNote change note
   * @param content rows of the draft's type
   * @return the draft
   */
  public ConfigVersion saveDraft(
      Long id, LocalDate effectiveFrom, String changeNote, ConfigContent content) {
    ConfigVersion draft = get(id);
    draft.requireDraft();
    requireEffectiveDate(effectiveFrom);
    ConfigContent rows = scoped(draft, content);
    validator.validate(draft.getConfigType(), rows, today());
    draft.editHeader(effectiveFrom, changeNote);
    store.replace(draft.getId(), draft.getConfigType(), rows);
    audit.record(
        ConfigVersions.ENTITY,
        draft.getId(),
        AuditAction.UPDATE,
        "Draft " + ConfigVersions.label(draft) + " saved");
    return draft;
  }

  /** A TEMPLATE draft keeps the template type of its scope. */
  private static ConfigContent scoped(ConfigVersion draft, ConfigContent content) {
    if (draft.getConfigType() != ConfigType.TEMPLATE || content.template() == null) {
      return content;
    }
    ConfigContent.Template t = content.template();
    return ConfigContent.ofTemplate(
        new ConfigContent.Template(TemplateType.valueOf(draft.getScope()), t.name(), t.fields()));
  }

  private void requireEffectiveDate(LocalDate effectiveFrom) {
    if (effectiveFrom == null || effectiveFrom.isBefore(today())) {
      throw new BusinessRuleException(
          "SCR_CONFIG_EFFECTIVE_PAST", "The effective date cannot be before today");
    }
  }

  /**
   * Submits a draft for approval (FR-SS-010): the difference against the version in force today is
   * stored with it, and the checkers are notified.
   *
   * @param id draft id
   * @return the PENDING version
   */
  public ConfigVersion submit(Long id) {
    ConfigVersion draft = get(id);
    draft.requireDraft();
    requireEffectiveDate(draft.getEffectiveFrom());
    ConfigContent content = content(draft);
    if (content.isEmptyFor(draft.getConfigType())) {
      throw new BusinessRuleException(
          "SCR_CONFIG_EMPTY", "Add at least one rule before submitting");
    }
    validator.validate(draft.getConfigType(), content, today());
    Optional<ConfigVersion> current =
        inForce(draft.getCompanyId(), draft.getConfigType(), draft.getScope(), today());
    List<ConfigChange> changes = changesAgainst(current, draft, content);
    if (changes.isEmpty()) {
      throw new BusinessRuleException(
          "SCR_CONFIG_UNCHANGED", "The draft does not change the active configuration");
    }
    draft.submit(
        currentUser.username(),
        clock.instant(),
        current.map(ConfigVersion::getId).orElse(null),
        write(changes));
    audit.record(
        ConfigVersions.ENTITY,
        draft.getId(),
        AuditAction.SUBMIT,
        ConfigVersions.VERSION
            + ConfigVersions.label(draft)
            + " submitted with "
            + changes.size()
            + " change(s)");
    notifications.notifyPermission(
        APPROVE_PERMISSION,
        ConfigVersions.notice(
            draft, "Screening configuration to approve", "is waiting for your approval"),
        EVENT_TO_APPROVE);
    return draft;
  }

  private List<ConfigChange> changesAgainst(
      Optional<ConfigVersion> current, ConfigVersion draft, ConfigContent content) {
    ConfigContent before = current.map(this::content).orElseGet(ConfigContent::empty);
    return ConfigDiff.between(draft.getConfigType(), before, content);
  }

  /**
   * The before / after changes of a version: the stored difference once submitted, otherwise the
   * live difference of the draft against the version in force today.
   *
   * @param id version id
   * @return changes
   */
  @Transactional(readOnly = true)
  public List<ConfigChange> changes(Long id) {
    ConfigVersion version = get(id);
    if (version.getDiff() != null) {
      return read(version.getDiff());
    }
    Optional<ConfigVersion> current =
        inForce(version.getCompanyId(), version.getConfigType(), version.getScope(), today())
            .filter(v -> !v.getId().equals(version.getId()));
    return changesAgainst(current, version, content(version));
  }

  /**
   * The maker discards a draft; it is kept as REJECTED "withdrawn by maker" (FR-SS-010).
   *
   * @param id draft id
   * @return the REJECTED version
   */
  public ConfigVersion withdraw(Long id) {
    ConfigVersion draft = get(id);
    String user = currentUser.username();
    if (!draft.isMaker(user)) {
      throw new BusinessRuleException(
          "SCR_CONFIG_NOT_MAKER", "Only the maker of the draft can discard it");
    }
    draft.withdraw(user, clock.instant());
    audit.record(
        ConfigVersions.ENTITY,
        draft.getId(),
        AuditAction.DEACTIVATE,
        "Draft " + ConfigVersions.label(draft) + " discarded");
    return draft;
  }

  private String write(List<ConfigChange> changes) {
    try {
      return json.writeValueAsString(changes);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Cannot store the configuration difference", ex);
    }
  }

  private List<ConfigChange> read(String diff) {
    try {
      return json.readValue(diff, CHANGES);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Cannot read the configuration difference", ex);
    }
  }

  private LocalDate today() {
    return LocalDate.now(clock);
  }
}
