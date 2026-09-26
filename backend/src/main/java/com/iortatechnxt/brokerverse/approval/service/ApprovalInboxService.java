package com.iortatechnxt.brokerverse.approval.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Universal approval inbox: collects the pending items of every {@link PendingApprovalSource} for
 * the signed-in user, oldest first.
 */
@Service
@Transactional(readOnly = true)
public class ApprovalInboxService {

  private static final Comparator<PendingApproval> OLDEST_FIRST =
      Comparator.comparing(
          PendingApproval::submittedAt, Comparator.nullsLast(Comparator.<Instant>naturalOrder()));

  private final List<PendingApprovalSource> sources;

  /**
   * Creates the service.
   *
   * @param sources all inbox sources
   */
  public ApprovalInboxService(List<PendingApprovalSource> sources) {
    this.sources = List.copyOf(sources);
  }

  /**
   * Items waiting for the current user's approval.
   *
   * @param companyId company filter (null = all companies)
   * @return items, oldest first
   */
  public List<PendingApproval> inbox(Long companyId) {
    return collect(currentViewer(), companyId);
  }

  /**
   * Number of items per module in the current user's inbox.
   *
   * @param companyId company filter (null = all companies)
   * @return counts
   */
  public ApprovalCounts counts(Long companyId) {
    List<PendingApproval> items = inbox(companyId);
    Map<String, Long> byModule =
        items.stream()
            .collect(
                Collectors.groupingBy(
                    PendingApproval::module, TreeMap::new, Collectors.counting()));
    return new ApprovalCounts(items.size(), byModule);
  }

  /**
   * Every pending item of every user (monitoring, ageing alerts).
   *
   * @return items, oldest first
   */
  public List<PendingApproval> pendingAll() {
    return collect(ApprovalViewer.system(), null);
  }

  private List<PendingApproval> collect(ApprovalViewer viewer, Long companyId) {
    return sources.stream()
        .flatMap(s -> s.pendingFor(viewer).stream())
        .filter(i -> companyId == null || i.companyId() == null || companyId.equals(i.companyId()))
        .sorted(OLDEST_FIRST)
        .toList();
  }

  private static ApprovalViewer currentViewer() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return ApprovalViewer.user(null, Set.of());
    }
    Set<String> authorities =
        auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    return ApprovalViewer.user(auth.getName(), authorities);
  }

  /**
   * Inbox counts.
   *
   * @param total total items
   * @param byModule items per module code
   */
  public record ApprovalCounts(long total, Map<String, Long> byModule) {

    /** Canonical constructor copying the map. */
    public ApprovalCounts {
      byModule = Map.copyOf(byModule);
    }
  }
}
