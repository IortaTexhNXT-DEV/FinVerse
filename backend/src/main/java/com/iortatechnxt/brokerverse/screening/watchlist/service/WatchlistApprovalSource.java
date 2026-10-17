package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.screening.common.service.ScreeningPermissions;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.ChangeStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistChange;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistChangeRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntry;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntryRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * My Approvals source of watchlist changes waiting for a Compliance Checker (SNSRP-204; the list
 * part of the design's {@code ScreeningApprovalSource}). The maker never sees the own change.
 */
@Component
public class WatchlistApprovalSource implements PendingApprovalSource {

  private final WatchlistChangeRepository changes;
  private final WatchlistEntryRepository entries;

  /**
   * Creates the source.
   *
   * @param changes changes
   * @param entries entries
   */
  public WatchlistApprovalSource(
      WatchlistChangeRepository changes, WatchlistEntryRepository entries) {
    this.changes = changes;
    this.entries = entries;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(ScreeningPermissions.LIST_APPROVE)) {
      return List.of();
    }
    List<WatchlistChange> pending =
        changes.findByStatusOrderByIdAsc(ChangeStatus.PENDING).stream()
            .filter(c -> viewer.mayApproveItemOf(c.getCreatedBy()))
            .toList();
    if (pending.isEmpty()) {
      return List.of();
    }
    Map<Long, WatchlistEntry> byId =
        entries.findAllById(pending.stream().map(WatchlistChange::getEntryId).toList()).stream()
            .collect(Collectors.toMap(WatchlistEntry::getId, Function.identity()));
    return pending.stream()
        .filter(c -> byId.containsKey(c.getEntryId()))
        .map(c -> item(c, byId.get(c.getEntryId())))
        .toList();
  }

  private static PendingApproval item(WatchlistChange c, WatchlistEntry e) {
    return new PendingApproval(
        ScreeningPermissions.MODULE,
        "Watchlist change",
        e.getExternalRef(),
        c.getChangeType() + " " + e.getPrimaryName() + ": " + c.getMakerRemarks(),
        null,
        null,
        c.getCreatedBy(),
        c.getCreatedAt(),
        null,
        "/screening-setup/watchlist?change=" + c.getId());
  }
}
