package com.iortatechnxt.brokerverse.reinsurance.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacPlacement;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacPlacementRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacStatus;
import com.iortatechnxt.brokerverse.reinsurance.domain.Soa;
import com.iortatechnxt.brokerverse.reinsurance.domain.SoaRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.SoaStatus;
import com.iortatechnxt.brokerverse.reinsurance.domain.Treaty;
import com.iortatechnxt.brokerverse.reinsurance.domain.TreatyRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Approval inbox source of the reinsurance module: treaties pending authorization, facultative
 * slips submitted for approval and statements of account pending approval. Visible to holders of
 * {@code REINSURANCE_AUTHORIZE}; a maker never sees his own items.
 */
@Component
@Transactional(readOnly = true)
public class ReinsuranceApprovalSource implements PendingApprovalSource {

  /** Module code shown in the inbox. */
  static final String MODULE = "REINSURANCE";

  private final TreatyRepository treaties;
  private final FacPlacementRepository placements;
  private final SoaRepository statements;

  /**
   * Creates the source.
   *
   * @param treaties treaties
   * @param placements facultative placements
   * @param statements statements of account
   */
  public ReinsuranceApprovalSource(
      TreatyRepository treaties, FacPlacementRepository placements, SoaRepository statements) {
    this.treaties = treaties;
    this.placements = placements;
    this.statements = statements;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(Permission.REINSURANCE_AUTHORIZE.name())) {
      return List.of();
    }
    List<PendingApproval> out = new ArrayList<>();
    treaties.findByRecordStatus(RecordStatus.PENDING_AUTHORIZATION).stream()
        .filter(t -> viewer.mayApproveItemOf(maker(t)))
        .map(ReinsuranceApprovalSource::treatyItem)
        .forEach(out::add);
    placements.findByStatusIn(EnumSet.of(FacStatus.PENDING_APPROVAL)).stream()
        .filter(f -> viewer.mayApproveItemOf(f.getSubmittedBy()))
        .map(ReinsuranceApprovalSource::placementItem)
        .forEach(out::add);
    statements.findByStatus(SoaStatus.PENDING_APPROVAL).stream()
        .filter(s -> viewer.mayApproveItemOf(s.maker()))
        .map(ReinsuranceApprovalSource::statementItem)
        .forEach(out::add);
    return out;
  }

  private static String maker(Treaty t) {
    return t.getUpdatedBy() != null ? t.getUpdatedBy() : t.getCreatedBy();
  }

  private static PendingApproval treatyItem(Treaty t) {
    return new PendingApproval(
        MODULE,
        "Treaty",
        t.getCode(),
        t.getName(),
        null,
        null,
        maker(t),
        t.getUpdatedAt() != null ? t.getUpdatedAt() : t.getCreatedAt(),
        t.getCompanyId(),
        "/reinsurance/treaties");
  }

  private static PendingApproval placementItem(FacPlacement f) {
    return new PendingApproval(
        MODULE,
        "Facultative placement",
        f.getPlacementNo(),
        f.getCession().getPolicyNo() + " - " + f.getRiskDescription(),
        f.getFacPremium(),
        f.getCession().getCurrency(),
        f.getSubmittedBy(),
        f.getSubmittedAt(),
        f.getCompanyId(),
        "/reinsurance/fac");
  }

  private static PendingApproval statementItem(Soa s) {
    return new PendingApproval(
        MODULE,
        "Statement of account",
        s.getSoaNo(),
        s.getTreaty().getCode() + " " + s.getParty().getName() + " Q" + s.getQuarter(),
        s.getBalance(),
        s.getCurrency(),
        s.maker(),
        s.getUpdatedAt() != null ? s.getUpdatedAt() : s.getCreatedAt(),
        s.getCompanyId(),
        "/reinsurance/soa");
  }
}
