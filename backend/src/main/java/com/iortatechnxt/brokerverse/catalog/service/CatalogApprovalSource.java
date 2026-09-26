package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.Scope;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.catalog.domain.CatalogRecord;
import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Approval inbox source: catalog records (products, rules, insurers, rates, sales organisation,
 * coverages, clauses, incentive criteria and rate-scheme exceptions) pending authorization, for the
 * viewers holding one of the kind's authorising permissions, excluding the viewer's own changes.
 */
@Component
public class CatalogApprovalSource implements PendingApprovalSource {

  private final MasterRecordApprovals records;

  /**
   * Creates the source.
   *
   * @param records master record helper
   */
  public CatalogApprovalSource(MasterRecordApprovals records) {
    this.records = records;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    return Arrays.stream(CatalogKind.values())
        .flatMap(kind -> pendingOf(viewer, kind, kind.type()).stream())
        .toList();
  }

  private <E extends AuthorizableEntity> List<PendingApproval> pendingOf(
      ApprovalViewer viewer, CatalogKind kind, Class<E> type) {
    return kind.authorizers().stream()
        .filter(viewer::can)
        .findFirst()
        .map(permission -> pendingOf(viewer, kind, type, new Scope(kind.inboxModule(), permission)))
        .orElse(List.of());
  }

  private <E extends AuthorizableEntity> List<PendingApproval> pendingOf(
      ApprovalViewer viewer, CatalogKind kind, Class<E> type, Scope scope) {
    return records.pending(
        viewer,
        scope,
        type,
        e ->
            e instanceof CatalogRecord r
                ? new RecordFacts(
                    kind.label(),
                    r.catalogReference(),
                    r.catalogDescription(),
                    r.catalogCompanyId(),
                    kind.link())
                : new RecordFacts(
                    kind.label(), String.valueOf(e.getId()), null, null, kind.link()));
  }
}
