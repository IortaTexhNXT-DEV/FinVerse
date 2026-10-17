package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigStatus;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigVersion;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigVersionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ActiveConfig} over the versioned tables (design 4.1): the version in force on a date is
 * the approved (ACTIVE or SUPERSEDED) version with the latest effective date on or before it.
 * Drafts, pending and rejected versions are never read by screening.
 */
@Component
@Transactional(readOnly = true)
public class VersionedActiveConfig implements ActiveConfig {

  private static final List<ConfigStatus> READABLE =
      List.of(ConfigStatus.ACTIVE, ConfigStatus.SUPERSEDED);

  private final ConfigVersionRepository versions;
  private final ConfigContentStore store;

  /**
   * Creates the resolver.
   *
   * @param versions versions
   * @param store version rows
   */
  public VersionedActiveConfig(ConfigVersionRepository versions, ConfigContentStore store) {
    this.versions = versions;
    this.store = store;
  }

  @Override
  public Optional<ConfigVersionRef> activeVersion(
      Long companyId, ConfigType type, String scope, LocalDate asOf) {
    String key = type == ConfigType.TEMPLATE && scope != null ? scope : "";
    return versions.inForce(companyId, type, key, READABLE, asOf).stream()
        .findFirst()
        .map(VersionedActiveConfig::ref);
  }

  /**
   * The reference of a version.
   *
   * @param v version
   * @return reference
   */
  static ConfigVersionRef ref(ConfigVersion v) {
    return new ConfigVersionRef(
        v.getId(), v.getConfigType(), v.getScope(), v.getVersionNo(), v.getEffectiveFrom());
  }

  private ConfigVersion require(Long versionId, ConfigType type) {
    return versions
        .findById(versionId)
        .filter(v -> v.getConfigType() == type)
        .orElseThrow(
            () -> new ResourceNotFoundException(type + " configuration version", versionId));
  }

  private ConfigContent content(ConfigVersion v) {
    return store.load(v.getId(), v.getConfigType());
  }

  @Override
  public MatchCriteria matchCriteria(Long versionId) {
    ConfigVersion v = require(versionId, ConfigType.MATCH_CRITERIA);
    return new MatchCriteria(ref(v), content(v).matchRules());
  }

  @Override
  public RiskRules riskRules(Long versionId) {
    ConfigVersion v = require(versionId, ConfigType.RISK_RULES);
    ConfigContent c = content(v);
    return new RiskRules(ref(v), c.riskCategories(), c.riskRules());
  }

  @Override
  public ApprovalMatrix approvalMatrix(Long versionId) {
    ConfigVersion v = require(versionId, ConfigType.APPROVAL_MATRIX);
    return new ApprovalMatrix(ref(v), content(v).routes());
  }

  @Override
  public AssignmentMatrix assignmentMatrix(Long versionId) {
    ConfigVersion v = require(versionId, ConfigType.ASSIGNMENT_MATRIX);
    return new AssignmentMatrix(ref(v), content(v).assignmentRules());
  }

  @Override
  public SlaMatrix slaMatrix(Long versionId) {
    ConfigVersion v = require(versionId, ConfigType.SLA_MATRIX);
    return new SlaMatrix(ref(v), content(v).slaRules());
  }

  @Override
  public ValidationRules validationRules(Long versionId) {
    ConfigVersion v = require(versionId, ConfigType.VALIDATION_RULES);
    return new ValidationRules(ref(v), content(v).validationRules());
  }

  @Override
  public ReviewTemplate template(Long versionId) {
    ConfigVersion v = require(versionId, ConfigType.TEMPLATE);
    ConfigContent.Template t = content(v).template();
    if (t == null) {
      throw new ResourceNotFoundException("Template of configuration version", versionId);
    }
    return new ReviewTemplate(ref(v), t.templateType(), t.name(), t.fields());
  }

  @Override
  public StrLayout strLayout(Long versionId) {
    ConfigVersion v = require(versionId, ConfigType.STR_LAYOUT);
    ConfigContent.Layout l = content(v).layout();
    if (l == null) {
      throw new ResourceNotFoundException("Layout of configuration version", versionId);
    }
    return new StrLayout(ref(v), l.format(), l.delimiter(), l.encoding(), l.columns());
  }
}
