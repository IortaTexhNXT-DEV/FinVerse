package com.iortatechnxt.brokerverse.migration.mapping.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;

/**
 * A survivorship rule of client matching (DATA_MIGRATION_DESIGN section 9): for a field of the
 * merged client, the first non-blank value by source priority, or the most recently updated value.
 */
@Entity
@Table(name = "mig_survivorship_rule")
public class SurvivorshipRule extends BaseEntity {

  @Column(nullable = false, length = 60, updatable = false)
  private String field;

  @Column(nullable = false, length = 20)
  private String rule;

  @Column(name = "source_priority", nullable = false, length = 100)
  private String sourcePriority;

  protected SurvivorshipRule() {}

  /**
   * Source systems in priority order.
   *
   * @return sources
   */
  public List<String> priority() {
    return Arrays.stream(sourcePriority.split(",")).map(String::trim).toList();
  }

  public boolean latest() {
    return "LATEST".equals(rule);
  }

  public String getField() {
    return field;
  }

  public String getRule() {
    return rule;
  }

  public String getSourcePriority() {
    return sourcePriority;
  }
}
