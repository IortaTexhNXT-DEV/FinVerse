package com.iortatechnxt.brokerverse.screening.config.api.dto;

import com.iortatechnxt.brokerverse.screening.config.service.ConfigChange;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigContent;
import java.util.List;

/**
 * A configuration version with its rows and its changes against the version in force (FR-SS-010
 * "Changes").
 *
 * @param version header
 * @param content rows of the version's type
 * @param changes before / after lines
 */
public record ConfigVersionDetail(
    ConfigVersionDto version, ConfigContent content, List<ConfigChange> changes) {

  /** Defensive copy. */
  public ConfigVersionDetail {
    changes = List.copyOf(changes);
  }
}
