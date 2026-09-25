package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.approval.service.BulkApprovalAction;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Posting of several journals at once (FRBS 2.5.6): from the journal list and from the approval
 * inbox. Every journal is approved in its own transaction by {@link
 * JournalAuthorizationService#approve} with all its controls (maker-checker, limit, re-validation,
 * negative balance), so a refused journal never blocks the others.
 */
@Component
public class JournalBulkApprovals implements BulkApprovalAction {

  private static final String PERMISSION = "JOURNAL_AUTHORIZE";

  private final JournalAuthorizationService authorization;
  private final JournalBatchRepository batches;
  private final CurrentUser currentUser;

  /**
   * Creates the component.
   *
   * @param authorization single approval
   * @param batches batch repository
   * @param currentUser current user
   */
  public JournalBulkApprovals(
      JournalAuthorizationService authorization,
      JournalBatchRepository batches,
      CurrentUser currentUser) {
    this.authorization = authorization;
    this.batches = batches;
    this.currentUser = currentUser;
  }

  /**
   * Approves journals by id.
   *
   * @param ids journals
   * @return one outcome per journal
   */
  public List<Outcome> approveAll(List<Long> ids) {
    return ids.stream().map(this::approveOne).toList();
  }

  @Override
  public boolean supports(String module, String type) {
    return "GL".equals(module) && type != null && type.startsWith("Journal");
  }

  @Override
  public String approve(Long companyId, String reference) {
    requirePermission();
    JournalBatch batch =
        batches
            .findByCompanyIdAndBatchNo(companyId, reference)
            .orElseThrow(() -> new ResourceNotFoundException("Journal", reference));
    return "Posted " + authorization.approve(batch.getId()).getBatchNo();
  }

  private Outcome approveOne(Long id) {
    try {
      requirePermission();
      JournalBatch posted = authorization.approve(id);
      return new Outcome(id, posted.getBatchNo(), true, "Posted");
    } catch (BusinessRuleException | ResourceNotFoundException ex) {
      return new Outcome(id, null, false, ex.getMessage());
    }
  }

  private void requirePermission() {
    if (!currentUser.hasAuthority(PERMISSION)) {
      throw new AccessDeniedException("Not permitted to authorize journals");
    }
  }

  /**
   * Outcome of one journal.
   *
   * @param id journal
   * @param batchNo batch number when posted
   * @param posted whether it was posted
   * @param message outcome or refusal reason
   */
  public record Outcome(Long id, String batchNo, boolean posted, String message) {}
}
