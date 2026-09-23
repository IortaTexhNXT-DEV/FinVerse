package com.iortatechnxt.finverse.investment.service;

import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals.Scope;
import com.iortatechnxt.finverse.approval.service.PendingApproval;
import com.iortatechnxt.finverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.finverse.investment.domain.HoldingStatus;
import com.iortatechnxt.finverse.investment.domain.InvestmentHolding;
import com.iortatechnxt.finverse.investment.domain.InvestmentHoldingRepository;
import com.iortatechnxt.finverse.investment.domain.InvestmentPortfolio;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Approval inbox source of investments: holdings pending approval (the purchase posts on approval)
 * and portfolios pending authorization, both under {@code MASTER_AUTHORIZE} and never shown to the
 * user who last maintained them. The amount of a holding is its purchase price.
 */
@Component
public class InvestmentApprovalSource implements PendingApprovalSource {

  /** Module code of investment items. */
  public static final String MODULE = "INVESTMENTS";

  private final InvestmentHoldingRepository holdings;
  private final MasterRecordApprovals records;

  /**
   * Creates the source.
   *
   * @param holdings holding repository
   * @param records master record helper (portfolios)
   */
  public InvestmentApprovalSource(
      InvestmentHoldingRepository holdings, MasterRecordApprovals records) {
    this.holdings = holdings;
    this.records = records;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(MasterRecordApprovals.PERMISSION)) {
      return List.of();
    }
    List<PendingApproval> items = new ArrayList<>();
    holdings.findByStatusOrderById(HoldingStatus.PENDING_APPROVAL).stream()
        .filter(h -> viewer.mayApproveItemOf(maker(h)))
        .map(
            h ->
                new PendingApproval(
                    MODULE,
                    "Investment (" + h.getInstrumentType() + ")",
                    h.getHoldingNo(),
                    h.getDescription(),
                    h.terms().purchasePrice(),
                    h.getCurrency(),
                    maker(h),
                    h.getUpdatedAt() != null ? h.getUpdatedAt() : h.getCreatedAt(),
                    h.getCompanyId(),
                    "/investments/holdings"))
        .forEach(items::add);
    items.addAll(
        records.pending(
            viewer,
            new Scope(MODULE, MasterRecordApprovals.PERMISSION),
            InvestmentPortfolio.class,
            p ->
                new RecordFacts(
                    "Investment portfolio",
                    p.getCode(),
                    p.getName(),
                    p.getCompanyId(),
                    "/investments/portfolios")));
    return items;
  }

  /** The approving checker must differ from the last maintainer (see {@code authorize}). */
  private static String maker(InvestmentHolding h) {
    return h.getUpdatedBy() != null ? h.getUpdatedBy() : h.getCreatedBy();
  }
}
