package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.screening.common.service.ScreeningPermissions;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigStatus;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigVersion;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigVersionRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * My Approvals source of screening configuration versions waiting for a Compliance Checker
 * (SNSRP-109; the configuration part of the design's {@code ScreeningApprovalSource}). The maker
 * (creator or submitter) never sees the own version.
 */
@Component
public class ConfigApprovalSource implements PendingApprovalSource {

  private final ConfigVersionRepository versions;

  /**
   * Creates the source.
   *
   * @param versions versions
   */
  public ConfigApprovalSource(ConfigVersionRepository versions) {
    this.versions = versions;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(ScreeningPermissions.CONFIG_APPROVE)) {
      return List.of();
    }
    return versions.findByStatusOrderBySubmittedAtAsc(ConfigStatus.PENDING).stream()
        .filter(
            v ->
                viewer.mayApproveItemOf(v.getCreatedBy())
                    && viewer.mayApproveItemOf(v.getSubmittedBy()))
        .map(ConfigApprovalSource::item)
        .toList();
  }

  private static PendingApproval item(ConfigVersion v) {
    return new PendingApproval(
        ScreeningPermissions.MODULE,
        "Screening configuration",
        ConfigVersionService.label(v),
        "Effective "
            + v.getEffectiveFrom()
            + (v.getChangeNote() == null ? "" : ": " + v.getChangeNote()),
        null,
        null,
        v.getSubmittedBy(),
        v.getSubmittedAt(),
        v.getCompanyId(),
        ConfigVersionService.link(v));
  }
}
