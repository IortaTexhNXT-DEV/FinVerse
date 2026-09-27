package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copies the SLA parameters of the package request stages (PKG_SLA_*, V755; values to confirm,
 * PQ08) into the SLA hours of the PM_PACKAGE_REQUEST workflow stages, which the workflow engine
 * uses for due dates and SLA alerts (BRPM.021). Run daily by the package expiry monitor, so a
 * parameter changed on Administration takes effect the next day; only changed values are written.
 */
@Component
public class PackageSlaSync {

  /** Stage to SLA parameter. */
  static final Map<String, String> STAGE_PARAMETERS =
      Map.of(
          "FOR_MKT_APPROVAL", "PKG_SLA_MKT_APPROVAL",
          "FOR_TSU_REVIEW", "PKG_SLA_TSU_REVIEW",
          "FOR_TSU_APPROVAL", "PKG_SLA_TSU_APPROVAL",
          "NEGOTIATION", "PKG_SLA_NEGOTIATION",
          "FOR_MANCOM", "PKG_SLA_MANCOM",
          "WITH_MBS", "PKG_SLA_MBS_SETUP");

  private static final String UPDATE =
      "update wf_stage set sla_hours = :hours where workflow_code = :workflow"
          + " and stage_code = :stage and (sla_hours is null or sla_hours <> :hours)";

  private final SystemParameterService parameters;
  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the synchroniser.
   *
   * @param parameters business parameters
   * @param jdbc JDBC template
   */
  public PackageSlaSync(SystemParameterService parameters, NamedParameterJdbcTemplate jdbc) {
    this.parameters = parameters;
    this.jdbc = jdbc;
  }

  /**
   * Writes the configured SLA hours into the workflow stages.
   *
   * @return number of stages changed
   */
  @Transactional
  public int sync() {
    int changed = 0;
    for (Map.Entry<String, String> e : STAGE_PARAMETERS.entrySet()) {
      int hours = parameters.intValue(e.getValue(), 0);
      if (hours > 0) {
        changed +=
            jdbc.update(
                UPDATE,
                new MapSqlParameterSource()
                    .addValue("hours", hours)
                    .addValue("workflow", PackageRequests.WORKFLOW)
                    .addValue("stage", e.getKey()));
      }
    }
    return changed;
  }
}
