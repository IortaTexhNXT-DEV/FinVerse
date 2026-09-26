package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestBatch;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestBatchRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService.Decision;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bulk access requests (BRD 1.009; FR-UA-019): the rows of an uploaded file become draft line
 * requests of one batch (bulk handler {@code UAM_ACCESS_REQUEST}); the requester submits the batch
 * to the chosen approver with remarks, cancels it or removes lines; the approver approves (each
 * line applied in its own transaction: failures are listed, the other lines stand), rejects or
 * returns it as a whole (UQ10). Every rule of a single request applies to each line.
 */
@Service
public class AccessBatchService {

  private final AccessRequestBatchRepository batches;
  private final AccessRequestRepository requests;
  private final AccessRequestService requestService;
  private final AccessRequestPermissions permissions;
  private final AccessRequestValidator validator;
  private final AccessDecisionService decisions;
  private final AccessRequestReturnService returns;
  private final AccessRequestVisibility visibility;
  private final CurrentUser currentUser;
  private final TransactionTemplate tx;
  private final TransactionTemplate lineTx;
  private final TransactionTemplate checkTx;

  /**
   * Creates the service.
   *
   * @param batches batches
   * @param requests line requests
   * @param requestService access requests
   * @param permissions request functions of the current user
   * @param validator request checks
   * @param decisions decisions
   * @param returns return and cancellation
   * @param visibility who sees all requests
   * @param currentUser current user
   * @param transactions transaction manager
   */
  public AccessBatchService(
      AccessRequestBatchRepository batches,
      AccessRequestRepository requests,
      AccessRequestService requestService,
      AccessRequestPermissions permissions,
      AccessRequestValidator validator,
      AccessDecisionService decisions,
      AccessRequestReturnService returns,
      AccessRequestVisibility visibility,
      CurrentUser currentUser,
      PlatformTransactionManager transactions) {
    this.batches = batches;
    this.requests = requests;
    this.requestService = requestService;
    this.permissions = permissions;
    this.validator = validator;
    this.decisions = decisions;
    this.returns = returns;
    this.visibility = visibility;
    this.currentUser = currentUser;
    this.tx = new TransactionTemplate(transactions);
    this.lineTx = new TransactionTemplate(transactions);
    this.lineTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.checkTx = new TransactionTemplate(transactions);
    this.checkTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.checkTx.setReadOnly(true);
  }

  /**
   * Checks one row as a single request (FR-UA-019 R1), before the batch is created. The check runs
   * in its own read-only transaction, so a refused row does not roll back the upload.
   *
   * @param content the row's request
   * @return normalised content
   */
  public AccessRequestContent validateLine(AccessRequestContent content) {
    return checkTx.execute(s -> check(content));
  }

  private AccessRequestContent check(AccessRequestContent content) {
    permissions.requireRequestPermission(content);
    return validator.validate(content);
  }

  /**
   * Adds one row as a draft line request of the batch of an upload (created with the first row).
   *
   * @param batchNo batch number (the upload number)
   * @param fileName uploaded file
   * @param content the row's request
   * @return the line request
   */
  public AccessRequest addLine(String batchNo, String fileName, AccessRequestContent content) {
    return tx.execute(
        s -> {
          AccessRequestContent clean = check(content);
          AccessRequestBatch batch =
              batches
                  .findByBatchNo(batchNo)
                  .orElseGet(() -> batches.save(new AccessRequestBatch(batchNo, fileName)));
          AccessRequest line = requestService.create(clean, true, null, batch.getId());
          batch.addLine();
          return line;
        });
  }

  /**
   * One batch the current user may see.
   *
   * @param id batch
   * @return batch
   */
  public AccessRequestBatch get(Long id) {
    return tx.execute(s -> requireVisible(find(id)));
  }

  /**
   * The line requests of a batch.
   *
   * @param id batch
   * @return lines in row order
   */
  public List<AccessRequest> lines(Long id) {
    return tx.execute(
        s -> {
          requireVisible(find(id));
          return requests.findByBatchIdOrderByIdAsc(id);
        });
  }

  /**
   * Batches of the current user (all batches with ACCESS_APPROVE, USER_MANAGE or AUDIT_VIEW).
   *
   * @param pageable page
   * @return batches
   */
  public Page<AccessRequestBatch> list(Pageable pageable) {
    return tx.execute(
        s ->
            visibility.seesAll()
                ? batches.findAll(pageable)
                : batches.findByCreatedByIgnoreCase(currentUser.username(), pageable));
  }

  /**
   * Submits the draft lines to the chosen approver with the batch remarks (BRD 1.009.1.5-6).
   *
   * @param id batch
   * @param approver chosen approver
   * @param remarks batch remarks (mandatory)
   * @return the batch
   */
  public AccessRequestBatch submit(Long id, String approver, String remarks) {
    if (remarks == null || remarks.isBlank()) {
      throw new BusinessRuleException("ACCESS_JUSTIFICATION", "Enter the justification");
    }
    return tx.execute(
        s -> {
          AccessRequestBatch batch = requireCreator(find(id));
          List<AccessRequest> drafts = linesIn(id, AccessRequestStatus.EDITABLE);
          if (drafts.isEmpty()) {
            throw new BusinessRuleException(
                "ACCESS_BATCH_NOTHING_TO_SUBMIT", "Batch " + batch.getBatchNo() + " has no draft");
          }
          List<String> chosen = approver == null ? List.of() : List.of(approver);
          drafts.forEach(l -> requestService.submit(l.getId(), chosen, remarks.trim()));
          batch.setRemarks(remarks.trim());
          return mirror(batch);
        });
  }

  /**
   * Approves the pending lines, each in its own transaction (FR-UA-019): failures are listed and
   * the other lines stand.
   *
   * @param id batch
   * @param comment optional comment
   * @return the outcome of every line decided
   */
  public BatchDecision approve(Long id, String comment) {
    List<Long> pending =
        tx.execute(
            s ->
                linesIn(requireVisible(find(id)).getId(), AccessRequestStatus.AWAITING_DECISION)
                    .stream()
                    .map(AccessRequest::getId)
                    .toList());
    List<LineOutcome> outcomes = new ArrayList<>();
    for (Long lineId : pending == null ? List.<Long>of() : pending) {
      outcomes.add(approveLine(lineId, comment));
    }
    return new BatchDecision(tx.execute(s -> mirror(find(id))), outcomes);
  }

  private LineOutcome approveLine(Long lineId, String comment) {
    try {
      Decision d = lineTx.execute(s -> decide(lineId, comment));
      AccessRequest r = d == null ? null : d.request();
      return r == null
          ? new LineOutcome(null, null, null, null, "Not decided")
          : new LineOutcome(
              r.getRequestNo(), r.getUsername(), r.getStatus(), d.temporaryPassword(), null);
    } catch (RuntimeException e) {
      AccessRequest failed = lineTx.execute(s -> markFailed(lineId, e.getMessage()));
      return failed == null
          ? new LineOutcome(null, null, null, null, e.getMessage())
          : new LineOutcome(
              failed.getRequestNo(),
              failed.getUsername(),
              failed.getStatus(),
              null,
              e.getMessage());
    }
  }

  private Decision decide(Long lineId, String comment) {
    AccessRequest line = requests.findById(lineId).orElseThrow();
    return line.getStatus() == AccessRequestStatus.PENDING_SECOND
        ? decisions.secondApprove(lineId, comment)
        : decisions.approve(lineId, comment);
  }

  private AccessRequest markFailed(Long lineId, String message) {
    AccessRequest line = requests.findById(lineId).orElseThrow();
    line.applyFailed(message);
    return line;
  }

  /**
   * Rejects every pending line with the reason.
   *
   * @param id batch
   * @param reason reason
   * @return the batch
   */
  public AccessRequestBatch reject(Long id, String reason) {
    return decideAll(id, AccessRequestStatus.AWAITING_DECISION, l -> decisions.reject(l, reason));
  }

  /**
   * Returns every pending line to the requester with the remarks.
   *
   * @param id batch
   * @param remarks remarks
   * @return the batch
   */
  public AccessRequestBatch returnBatch(Long id, String remarks) {
    return decideAll(
        id, AccessRequestStatus.AWAITING_DECISION, l -> returns.returnRequest(l, remarks));
  }

  /**
   * Cancels every line still open with the reason (creator).
   *
   * @param id batch
   * @param reason reason
   * @return the batch
   */
  public AccessRequestBatch cancel(Long id, String reason) {
    return decideAll(id, AccessRequestStatus.CANCELLABLE, l -> returns.cancel(l, reason));
  }

  private AccessRequestBatch decideAll(
      Long id, Set<AccessRequestStatus> statuses, Consumer<Long> step) {
    return tx.execute(
        s -> {
          AccessRequestBatch batch = requireVisible(find(id));
          List<AccessRequest> lines = linesIn(id, statuses);
          if (lines.isEmpty()) {
            throw new BusinessRuleException(
                "ACCESS_BATCH_NO_LINE", "Batch " + batch.getBatchNo() + " has no line to decide");
          }
          lines.forEach(l -> step.accept(l.getId()));
          return mirror(batch);
        });
  }

  private List<AccessRequest> linesIn(Long batchId, Set<AccessRequestStatus> statuses) {
    return requests.findByBatchIdOrderByIdAsc(batchId).stream()
        .filter(l -> statuses.contains(l.getStatus()))
        .toList();
  }

  private AccessRequestBatch mirror(AccessRequestBatch batch) {
    batch.mirror(
        requests.findByBatchIdOrderByIdAsc(batch.getId()).stream()
            .map(AccessRequest::getStatus)
            .toList());
    return batch;
  }

  private AccessRequestBatch find(Long id) {
    return batches.findById(id).orElseThrow(() -> new ResourceNotFoundException("Batch", id));
  }

  private AccessRequestBatch requireCreator(AccessRequestBatch batch) {
    if (!CurrentUser.sameUser(currentUser.username(), batch.getCreatedBy())) {
      throw new BusinessRuleException(
          "ACCESS_NOT_REQUESTER", "Only the creator can submit batch " + batch.getBatchNo());
    }
    return batch;
  }

  private AccessRequestBatch requireVisible(AccessRequestBatch batch) {
    String me = currentUser.username();
    boolean visible =
        CurrentUser.sameUser(me, batch.getCreatedBy())
            || visibility.seesAll()
            || requests.findByBatchIdOrderByIdAsc(batch.getId()).stream()
                .anyMatch(l -> CurrentUser.sameUser(me, l.getAssignedApprover()));
    if (!visible) {
      throw new ResourceNotFoundException("Batch", batch.getId());
    }
    return batch;
  }

  /**
   * Outcome of a batch approval.
   *
   * @param batch the batch after the approval
   * @param lines outcome of each line decided
   */
  public record BatchDecision(AccessRequestBatch batch, List<LineOutcome> lines) {

    /** Defensive copy. */
    public BatchDecision {
      lines = List.copyOf(lines);
    }
  }

  /**
   * Outcome of one line.
   *
   * @param requestNo line request
   * @param username user
   * @param status status after the approval
   * @param temporaryPassword temporary password of a created user (shown once), else null
   * @param error why the line could not be approved or applied, else null
   */
  public record LineOutcome(
      String requestNo,
      String username,
      AccessRequestStatus status,
      String temporaryPassword,
      String error) {

    @Override
    public String toString() {
      return "LineOutcome[" + requestNo + ", " + status + ", ***]";
    }
  }
}
