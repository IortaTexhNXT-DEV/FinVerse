package com.iortatechnxt.finverse.budget.service;

import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.approval.service.PendingApproval;
import com.iortatechnxt.finverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.finverse.budget.domain.BudgetRepository;
import com.iortatechnxt.finverse.budget.domain.BudgetStatus;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Approval inbox source of budgeting: budget versions SUBMITTED for approval ({@code
 * BUDGET_MANAGE}), never shown to their submitter. The amount is the annual total of the version.
 */
@Component
public class BudgetApprovalSource implements PendingApprovalSource {

  /** Module code of budget items. */
  public static final String MODULE = "BUDGET";

  private static final String PERMISSION = "BUDGET_MANAGE";

  private final BudgetRepository budgets;

  /**
   * Creates the source.
   *
   * @param budgets budget repository
   */
  public BudgetApprovalSource(BudgetRepository budgets) {
    this.budgets = budgets;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(PERMISSION)) {
      return List.of();
    }
    return budgets.findByStatusOrderById(BudgetStatus.SUBMITTED).stream()
        .filter(b -> viewer.mayApproveItemOf(b.getSubmittedBy()))
        .map(
            b ->
                new PendingApproval(
                    MODULE,
                    "Budget (" + b.getVersionType() + ")",
                    "FY" + b.getFiscalYear() + " v" + b.getVersionNo(),
                    b.getName(),
                    b.total(),
                    b.getCurrency(),
                    b.getSubmittedBy(),
                    b.getSubmittedAt(),
                    b.getCompanyId(),
                    "/planning/budgets/" + b.getId()))
        .toList();
  }
}
