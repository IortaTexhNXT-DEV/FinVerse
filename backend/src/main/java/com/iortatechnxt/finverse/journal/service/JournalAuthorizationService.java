package com.iortatechnxt.finverse.journal.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.finverse.journal.domain.JournalHeader;
import com.iortatechnxt.finverse.journal.domain.JournalLine;
import com.iortatechnxt.finverse.journal.domain.JournalStatus;
import com.iortatechnxt.finverse.journal.domain.JournalType;
import com.iortatechnxt.finverse.security.service.UserDirectory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checker side of journals: authorization with on-line posting, rejection and reversal.
 *
 * <p>Controls: maker-checker segregation, per-user authorization limits and re-validation at
 * authorization time (period may have closed, account may have been frozen since submission).
 */
@Service
@Transactional
public class JournalAuthorizationService {

  private static final String REVERSAL_SOURCE = "REVERSAL";
  private static final Set<JournalStatus> DEAD_STATUSES =
      EnumSet.of(JournalStatus.CANCELLED, JournalStatus.REJECTED);

  private final JournalEntryService entries;
  private final JournalBatchRepository batches;
  private final JournalFactory factory;
  private final JournalValidator validator;
  private final PostingService posting;
  private final UserDirectory userDirectory;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param entries journal entry service
   * @param batches batch repository
   * @param factory journal factory
   * @param validator validator
   * @param posting posting engine
   * @param userDirectory user facts
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public JournalAuthorizationService(
      JournalEntryService entries,
      JournalBatchRepository batches,
      JournalFactory factory,
      JournalValidator validator,
      PostingService posting,
      UserDirectory userDirectory,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.entries = entries;
    this.batches = batches;
    this.factory = factory;
    this.validator = validator;
    this.posting = posting;
    this.userDirectory = userDirectory;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Authorizes and posts a journal (checker).
   *
   * @param id id
   * @return posted batch
   */
  public JournalBatch approve(Long id) {
    JournalBatch batch = entries.get(id);
    String checker = currentUser.username();
    requireWithinLimit(checker, batch.getTotalDebit());
    batch.authorize(checker, clock.instant(), false);
    validator.validate(batch, userDirectory.roleCodes(batch.getSubmittedBy()));
    audit.record(
        JournalEntryService.ENTITY,
        batch.getBatchNo(),
        AuditAction.AUTHORIZE,
        "Authorized journal");
    posting.post(batch, checker);
    completeReversal(batch);
    return batch;
  }

  /**
   * Rejects a journal back to its maker.
   *
   * @param id id
   * @param reason reason
   * @return rejected batch
   */
  public JournalBatch reject(Long id, String reason) {
    JournalBatch batch = entries.get(id);
    batch.reject(currentUser.username(), reason);
    audit.record(
        JournalEntryService.ENTITY, batch.getBatchNo(), AuditAction.REJECT, "Rejected: " + reason);
    return batch;
  }

  /**
   * Raises a reversal journal (sides swapped) pending authorization. The original is marked
   * REVERSED when the reversal is posted. Only one live reversal per journal is allowed.
   *
   * @param id posted batch
   * @param reversalDate value date of the reversal
   * @param reason reason
   * @return reversal batch
   */
  public JournalBatch reverse(Long id, LocalDate reversalDate, String reason) {
    JournalBatch original = entries.get(id);
    original.requireReversible();
    boolean pending =
        batches
            .findFirstByCompanyIdAndSourceModuleAndSourceReferenceAndStatusNotIn(
                original.getCompanyId(), REVERSAL_SOURCE, original.getBatchNo(), DEAD_STATUSES)
            .isPresent();
    if (pending) {
      throw new BusinessRuleException(
          "REVERSAL_PENDING", "A reversal of " + original.getBatchNo() + " already exists");
    }
    JournalHeader header =
        new JournalHeader(
            original.getCompanyId(),
            original.getBranchId(),
            JournalType.REVERSAL,
            LocalDate.now(clock),
            reversalDate,
            original.getCurrency(),
            "Reversal of " + original.getBatchNo() + ": " + reason,
            original.getReference(),
            REVERSAL_SOURCE,
            original.getBatchNo(),
            original.getId());
    JournalBatch reversal =
        factory.createFromSpecs(
            header, original.getLines().stream().map(JournalLine::reversedSpec).toList());
    String user = currentUser.username();
    validator.validate(reversal, userDirectory.roleCodes(user));
    reversal.submit(user, clock.instant());
    audit.record(
        JournalEntryService.ENTITY,
        original.getBatchNo(),
        AuditAction.REVERSE,
        "Reversal " + reversal.getBatchNo() + " raised: " + reason);
    return reversal;
  }

  private void completeReversal(JournalBatch posted) {
    if (posted.getJournalType() == JournalType.REVERSAL && posted.getReversalOfId() != null) {
      JournalBatch original = entries.get(posted.getReversalOfId());
      original.markReversed(posted.getId());
      audit.record(
          JournalEntryService.ENTITY,
          original.getBatchNo(),
          AuditAction.REVERSE,
          "Reversed by " + posted.getBatchNo());
    }
  }

  private void requireWithinLimit(String checker, BigDecimal amount) {
    userDirectory
        .authorizationLimit(checker)
        .filter(limit -> amount.compareTo(limit) > 0)
        .ifPresent(
            limit -> {
              throw new BusinessRuleException(
                  "AUTHORIZATION_LIMIT_EXCEEDED",
                  "Journal total " + amount + " exceeds your authorization limit " + limit);
            });
  }
}
