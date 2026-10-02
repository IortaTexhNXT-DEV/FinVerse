package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigVersion;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateType;

/** Labels, links, scopes and notices of screening configuration versions. */
public final class ConfigVersions {

  /** Audit entity type. */
  public static final String ENTITY = "ScreeningConfigVersion";

  /** Label prefix of messages. */
  static final String VERSION = "Version ";

  private ConfigVersions() {}

  /**
   * The screen that opens a version.
   *
   * @param version version
   * @return frontend route
   */
  public static String link(ConfigVersion version) {
    String base =
        version.getConfigType() == ConfigType.TEMPLATE
            ? "/screening-setup/templates"
            : "/screening-setup/config";
    return base + "?version=" + version.getId();
  }

  /**
   * Display label of a version, e.g. "MATCH_CRITERIA v2" or "TEMPLATE KYC_REVIEW v3".
   *
   * @param version version
   * @return label
   */
  public static String label(ConfigVersion version) {
    return version.getConfigType()
        + (version.getScope() == null ? "" : " " + version.getScope())
        + " v"
        + version.getVersionNo();
  }

  static String scopeKey(ConfigVersion version) {
    return version.getScope() == null ? "" : version.getScope();
  }

  static String scopeKey(ConfigType type, String scope) {
    if (type != ConfigType.TEMPLATE) {
      return "";
    }
    if (scope == null || scope.isBlank()) {
      throw new BusinessRuleException(
          "SCR_TEMPLATE_TYPE_REQUIRED", "Select the template type (KYC review, EDD ...)");
    }
    try {
      return TemplateType.valueOf(scope.trim()).name();
    } catch (IllegalArgumentException ex) {
      throw new BusinessRuleException(
          "SCR_TEMPLATE_TYPE_REQUIRED", "Unknown template type " + scope, ex);
    }
  }

  static Notice notice(ConfigVersion version, String title, String what) {
    return new Notice(
        title,
        VERSION + label(version) + " " + what,
        link(version),
        ENTITY,
        String.valueOf(version.getId()));
  }
}
