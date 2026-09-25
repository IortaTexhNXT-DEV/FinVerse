package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.journal.domain.JournalHeader;
import com.iortatechnxt.brokerverse.journal.domain.JournalLine;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Automatic reversal of accruals (FRBS 2.8.1): a posted manual, adjustment or accrual journal with
 * a reversal date is reversed on that date by a REVERSAL journal (sides swapped, linked by {@code
 * reversal_of_id}, source {@value #SOURCE} and key {@code JV:<batch>:AUTOREV}), posted at once
 * because the original was authorized with its reversal date. Each journal is reversed in its own
 * transaction, so one failure (e.g. a closed period) never stops the others.
 */
@Service
public class JournalAutoReversalService {

  /** Job name. */
  public static final String JOB_NAME = "JOURNAL_AUTO_REVERSAL";

  /** Source module of automatic reversals. */
  public static final String SOURCE = "AUTOREV";

  private final JournalBatchRepository batches;
  private final JournalFactory factory;
  private final JournalValidator validator;
  private final PostingService posting;
  private final UserDirectory userDirectory;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final TransactionTemplate tx;

  /**
   * Creates the service.
   *
   * @param batches batch repository
   * @param factory journal factory
   * @param validator validator
   * @param posting posting engine
   * @param userDirectory user facts (roles of the original maker)
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   * @param transactions transaction manager (one transaction per journal)
   */
  @SuppressWarnings("java:S107") // constructor injection
  public JournalAutoReversalService(
      JournalBatchRepository batches,
      JournalFactory factory,
      JournalValidator validator,
      PostingService posting,
      UserDirectory userDirectory,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock,
      PlatformTransactionManager transactions) {
    this.batches = batches;
    this.factory = factory;
    this.validator = validator;
    this.posting = posting;
    this.userDirectory = userDirectory;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
    this.tx = new TransactionTemplate(transactions);
    this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Reverses every journal whose reversal date is on or before a date.
   *
   * @param date business date
   * @return reversed batch numbers and failures
   */
  public Result reverseDue(LocalDate date) {
    List<String> reversed = new ArrayList<>();
    List<String> failed = new ArrayList<>();
    List<Long> due = tx.execute(s -> batches.reversalsDue(date));
    for (Long id : due == null ? List.<Long>of() : due) {
      try {
        String no = tx.execute(s -> reverse(id, date));
        if (no != null) {
          reversed.add(no);
        }
      } catch (BusinessRuleException ex) {
        failed.add(id + ": " + ex.getMessage());
      }
    }
    return new Result(reversed, failed);
  }

  private String reverse(Long id, LocalDate date) {
    JournalBatch original =
        batches.findById(id).orElseThrow(() -> new ResourceNotFoundException("Journal", id));
    if (!original.isReversalDue(date)) {
      return null;
    }
    JournalHeader header =
        new JournalHeader(
            original.getCompanyId(),
            original.getBranchId(),
            JournalType.REVERSAL,
            LocalDate.now(clock),
            original.getReverseOn(),
            original.getCurrency(),
            "Automatic reversal of " + original.getBatchNo(),
            original.getReference(),
            SOURCE,
            "JV:" + original.getBatchNo() + ":AUTOREV",
            original.getId());
    JournalBatch reversal =
        factory.createFromSpecs(
            header, original.getLines().stream().map(JournalLine::reversedSpec).toList());
    validator.validate(reversal, userDirectory.roleCodes(original.getSubmittedBy()));
    String user = currentUser.username();
    Instant now = clock.instant();
    reversal.submit(user, now);
    reversal.authorize(user, now, true);
    posting.post(reversal, user);
    original.markReversed(reversal.getId());
    audit.record(
        JournalEntryService.ENTITY,
        original.getBatchNo(),
        AuditAction.REVERSE,
        "Reversed automatically on " + original.getReverseOn() + " by " + reversal.getBatchNo());
    return reversal.getBatchNo();
  }

  /**
   * Outcome of a run.
   *
   * @param reversed reversal batch numbers
   * @param failed journals that could not be reversed, with the reason
   */
  public record Result(List<String> reversed, List<String> failed) {

    /** Defensive copies. */
    public Result {
      reversed = List.copyOf(reversed);
      failed = List.copyOf(failed);
    }
  }
}
