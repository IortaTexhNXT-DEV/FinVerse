package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.BatchOutcome;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequestRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.PostingBatch;
import com.iortatechnxt.brokerverse.adjustment.domain.PostingBatchRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Batch posting of endorsement requests (ADJID.005/006, validation batch {@code VB-<yyyy>}):
 * returns the requests that do not qualify with a reason, then posts the selected requests, each in
 * its own transaction so a failure does not stop the others; the batch records the outcome of each.
 * A request is posted once (its posting is idempotent on {@code ADJ:<request>}).
 */
@Service
public class PostingBatchService {

  private static final Logger LOG = LoggerFactory.getLogger(PostingBatchService.class);
  private static final int MAX_MESSAGE = 1000;

  private final PostingBatchRepository batches;
  private final EndorsementRequestRepository requests;
  private final AdjustmentPostingService posting;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param batches posting batches
   * @param requests requests
   * @param posting posting of one request
   * @param workflow workflow engine (returns)
   * @param numbers document numbers
   * @param audit audit trail
   * @param txManager transaction manager (one transaction per request)
   * @param clock clock
   */
  public PostingBatchService(
      PostingBatchRepository batches,
      EndorsementRequestRepository requests,
      AdjustmentPostingService posting,
      WorkflowService workflow,
      DocumentNumberService numbers,
      AuditTrailService audit,
      PlatformTransactionManager txManager,
      Clock clock) {
    this.batches = batches;
    this.requests = requests;
    this.posting = posting;
    this.workflow = workflow;
    this.numbers = numbers;
    this.audit = audit;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.clock = clock;
  }

  /**
   * Posts requests ready for posting as one batch.
   *
   * @param companyId company
   * @param ids requests (FOR_POSTING)
   * @param remarks remarks, may be null
   * @return the batch with the outcome of each request
   */
  public PostingBatch post(Long companyId, List<Long> ids, String remarks) {
    List<Long> distinct = List.copyOf(new LinkedHashSet<>(ids));
    if (distinct.isEmpty()) {
      throw new BusinessRuleException("ADJ_BATCH_EMPTY", "Select the requests to post");
    }
    String batchNo = tx.execute(s -> numbers.next("VB-" + LocalDate.now(clock).getYear()));
    PostingBatch batch = new PostingBatch(companyId, batchNo, remarks);
    for (Long id : distinct) {
      batch.add(postOne(companyId, id, batchNo));
    }
    PostingBatch saved = Objects.requireNonNull(tx.execute(s -> batches.save(batch)));
    tx.executeWithoutResult(
        s ->
            audit.record(
                "PostingBatch",
                batchNo,
                AuditAction.POST,
                saved.getPostedCount()
                    + " posted, "
                    + saved.getPendingCount()
                    + " awaiting re-application, "
                    + saved.getFailedCount()
                    + " failed"));
    return saved;
  }

  private PostingBatch.Line postOne(Long companyId, Long id, String batchNo) {
    EndorsementRequest found = requests.findById(id).orElse(null);
    if (found == null || !found.getCompanyId().equals(companyId)) {
      return new PostingBatch.Line(id, String.valueOf(id), "-", BatchOutcome.FAILED, "Not found");
    }
    try {
      EndorsementRequest posted = tx.execute(s -> posting.post(id, batchNo));
      BatchOutcome outcome =
          posted != null && posted.getStage() == RequestStage.AWAITING_REAPPLICATION
              ? BatchOutcome.AWAITING_REAPPLICATION
              : BatchOutcome.POSTED;
      return line(found, outcome, null);
    } catch (RuntimeException e) {
      LOG.warn("Endorsement request {} not posted: {}", found.getRequestNo(), e.getMessage());
      return line(found, BatchOutcome.FAILED, e.getMessage());
    }
  }

  private static PostingBatch.Line line(
      EndorsementRequest request, BatchOutcome outcome, String message) {
    String text =
        message == null || message.length() <= MAX_MESSAGE
            ? message
            : message.substring(0, MAX_MESSAGE);
    return new PostingBatch.Line(
        request.getId(), request.getRequestNo(), request.getSubject().invoiceNo(), outcome, text);
  }

  /**
   * Returns requests that do not qualify for posting (ADJID.005/007): out of the batch, back to the
   * requester with a mandatory reason.
   *
   * @param ids requests
   * @param reasonCode reason (list {@code ADJ_RETURN_REASON})
   * @param comment comment
   * @return number of requests returned
   */
  public int returnRequests(List<Long> ids, String reasonCode, String comment) {
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BusinessRuleException("ADJ_RETURN_REASON_REQUIRED", "Select the return reason");
    }
    Integer done =
        tx.execute(
            s -> {
              int count = 0;
              for (Long id : new LinkedHashSet<>(ids)) {
                requests
                    .findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(Adjustments.ENTITY, id));
                workflow.transition(
                    Adjustments.ENTITY,
                    String.valueOf(id),
                    "return",
                    new TransitionNote(reasonCode, comment));
                count++;
              }
              return count;
            });
    return done == null ? 0 : done;
  }

  /**
   * A batch.
   *
   * @param batchNo batch number
   * @return batch with its lines
   */
  public PostingBatch get(String batchNo) {
    return tx.execute(
        s -> {
          PostingBatch batch =
              batches
                  .findByBatchNo(batchNo)
                  .orElseThrow(() -> new ResourceNotFoundException("Posting batch", batchNo));
          batch.loadLines();
          return batch;
        });
  }
}
