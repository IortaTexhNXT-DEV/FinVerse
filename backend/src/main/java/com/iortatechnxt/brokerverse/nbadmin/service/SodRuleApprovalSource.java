package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule.PendingAction;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Approval inbox source: separation-of-duties rules whose creation or deactivation waits for the
 * holders of UAM_SOD_AUTHORIZE other than the maker.
 */
@Component
public class SodRuleApprovalSource implements PendingApprovalSource {

  private final SodRuleService rules;
  private final AccessRequestDescriber describer;

  /**
   * Creates the source.
   *
   * @param rules rules
   * @param describer profile names
   */
  public SodRuleApprovalSource(SodRuleService rules, AccessRequestDescriber describer) {
    this.rules = rules;
    this.describer = describer;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(SodRuleService.AUTHORIZE)) {
      return List.of();
    }
    return rules.pending().stream()
        .filter(r -> viewer.mayApproveItemOf(r.getMaker()))
        .map(this::item)
        .toList();
  }

  private PendingApproval item(SodRule r) {
    String what = r.getPendingAction() == PendingAction.DEACTIVATE ? "Deactivate: " : "New: ";
    return new PendingApproval(
        AccessRequestApprovalSource.MODULE,
        "Separation-of-duties rule",
        r.getRuleCode(),
        what
            + describer.profileName(r.getProfileA())
            + " and "
            + describer.profileName(r.getProfileB())
            + " not held by one user",
        null,
        null,
        r.getMaker(),
        r.getUpdatedAt() == null ? r.getCreatedAt() : r.getUpdatedAt(),
        null,
        SodRuleService.SCREEN);
  }
}
