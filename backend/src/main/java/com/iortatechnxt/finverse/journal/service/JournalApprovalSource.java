package com.iortatechnxt.finverse.journal.service;

import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.approval.service.PendingApproval;
import com.iortatechnxt.finverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.finverse.journal.domain.JournalStatus;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.security.service.UserDirectory;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Approval inbox source: journals pending authorization, excluding the viewer's own submissions and
 * journals above the viewer's authorization limit (the checker could not approve them).
 */
@Component
public class JournalApprovalSource implements PendingApprovalSource {

  private static final String PERMISSION = "JOURNAL_AUTHORIZE";
  private static final Specification<JournalBatch> PENDING =
      (root, query, cb) -> cb.equal(root.get("status"), JournalStatus.PENDING_APPROVAL);

  private final JournalBatchRepository batches;
  private final UserDirectory users;
  private final OrganizationService organization;

  /**
   * Creates the source.
   *
   * @param batches batch repository
   * @param users user facts (authorization limits)
   * @param organization organization (base currency)
   */
  public JournalApprovalSource(
      JournalBatchRepository batches, UserDirectory users, OrganizationService organization) {
    this.batches = batches;
    this.users = users;
    this.organization = organization;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(PERMISSION)) {
      return List.of();
    }
    Optional<BigDecimal> limit =
        viewer.systemView() ? Optional.empty() : users.authorizationLimit(viewer.username());
    Map<Long, String> baseCurrencies = new HashMap<>();
    return batches.findAll(PENDING, Sort.by("submittedAt")).stream()
        .filter(b -> viewer.mayApproveItemOf(b.getSubmittedBy()))
        .filter(b -> limit.map(l -> b.getTotalDebit().compareTo(l) <= 0).orElse(true))
        .map(
            b ->
                new PendingApproval(
                    "GL",
                    "Journal (" + b.getJournalType() + ")",
                    b.getBatchNo(),
                    b.getNarration(),
                    b.getTotalDebit(),
                    baseCurrencies.computeIfAbsent(
                        b.getCompanyId(), id -> organization.getCompany(id).getBaseCurrency()),
                    b.getSubmittedBy(),
                    b.getSubmittedAt(),
                    b.getCompanyId(),
                    "/gl/journals/" + b.getId()))
        .toList();
  }
}
