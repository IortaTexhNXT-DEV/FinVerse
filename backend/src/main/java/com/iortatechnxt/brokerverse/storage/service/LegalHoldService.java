package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.storage.FileStore;
import com.iortatechnxt.brokerverse.storage.domain.HoldAction;
import com.iortatechnxt.brokerverse.storage.domain.HoldRequestStatus;
import com.iortatechnxt.brokerverse.storage.domain.LegalHoldRequest;
import com.iortatechnxt.brokerverse.storage.domain.LegalHoldRequestRepository;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Legal hold as a controlled action (DOCUMENT_STORAGE_DECISION, decision 3): a holder of {@code
 * FILE_LEGAL_HOLD_REQUEST} asks to place or release the hold of a file with a reason; another user
 * holding {@code FILE_LEGAL_HOLD_APPROVE} (the roles the BDOI Delegation of Authority names,
 * question DSQ03) approves or rejects it. On approval the Object Lock legal hold of the object is
 * set or cleared and the file row records requester, approver and reason. Every step is audited.
 * The retention job never removes a held file.
 */
@Service
@Transactional
public class LegalHoldService {

  /** Entity type of the audit entries of requests. */
  public static final String REQUEST_ENTITY = "LegalHoldRequest";

  private static final int MAX_TEXT = 500;

  private final LegalHoldRequestRepository requests;
  private final StoredFileService files;
  private final FileStore store;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param files stored files
   * @param store object store
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public LegalHoldService(
      LegalHoldRequestRepository requests,
      StoredFileService files,
      FileStore store,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.files = files;
    this.store = store;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Requests to place or release the legal hold of a file.
   *
   * @param fileId stored file id
   * @param action place or release
   * @param reason reason (mandatory)
   * @return the pending request
   */
  public LegalHoldRequest request(Long fileId, HoldAction action, String reason) {
    String why = requireText(reason);
    StoredFile file = files.get(fileId);
    if (action == HoldAction.PLACE && file.isLegalHold()) {
      throw new BusinessRuleException("FILE_ALREADY_HELD", "The file is already under legal hold");
    }
    if (action == HoldAction.RELEASE && !file.isLegalHold()) {
      throw new BusinessRuleException("FILE_NOT_HELD", "The file is not under legal hold");
    }
    if (requests.existsByStoredFileIdAndStatus(fileId, HoldRequestStatus.PENDING)) {
      throw new BusinessRuleException(
          "HOLD_REQUEST_PENDING", "A legal hold request of the file is already pending");
    }
    LegalHoldRequest saved =
        requests.save(
            new LegalHoldRequest(fileId, action, why, currentUser.username(), clock.instant()));
    audit.record(
        REQUEST_ENTITY,
        saved.getId(),
        AuditAction.SUBMIT,
        "Legal hold "
            + action
            + " requested for file "
            + fileId
            + " ("
            + file.getFileName()
            + "): "
            + why);
    return saved;
  }

  /**
   * Approves or rejects a request; an approval applies it to the object and the file.
   *
   * @param requestId request
   * @param approve true to approve
   * @param note decision note (mandatory)
   * @return the decided request
   */
  public LegalHoldRequest decide(Long requestId, boolean approve, String note) {
    String why = requireText(note);
    LegalHoldRequest request =
        requests
            .findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException(REQUEST_ENTITY, requestId));
    String approver = currentUser.username();
    request.decide(approve, approver, why, clock.instant());
    StoredFile file = files.get(request.getStoredFileId());
    if (approve) {
      apply(request, file, approver);
    }
    audit.record(
        REQUEST_ENTITY,
        requestId,
        approve ? AuditAction.AUTHORIZE : AuditAction.REJECT,
        "Legal hold "
            + request.getAction()
            + " of file "
            + file.getId()
            + (approve ? " approved" : " rejected")
            + " by "
            + approver
            + " (requested by "
            + request.getRequestedBy()
            + "): "
            + why);
    return request;
  }

  /**
   * Pending requests, oldest first.
   *
   * @return requests
   */
  @Transactional(readOnly = true)
  public List<LegalHoldRequest> pending() {
    return requests.findByStatusOrderByRequestedAtAsc(HoldRequestStatus.PENDING);
  }

  /**
   * The requests of a file, newest first.
   *
   * @param fileId stored file id
   * @return requests
   */
  @Transactional(readOnly = true)
  public List<LegalHoldRequest> historyOf(Long fileId) {
    return requests.findByStoredFileIdOrderByIdDesc(fileId);
  }

  private void apply(LegalHoldRequest request, StoredFile file, String approver) {
    boolean place = request.getAction() == HoldAction.PLACE;
    if (place == file.isLegalHold()) {
      throw new BusinessRuleException(
          "HOLD_STATE_CHANGED", "The legal hold of the file changed since the request");
    }
    store.legalHold(file.objectRef(), place);
    if (place) {
      file.placeHold(request.getReason(), request.getRequestedBy(), approver, clock.instant());
    } else {
      file.releaseHold();
    }
    audit.record(
        StoredFileService.ENTITY,
        file.getId(),
        AuditAction.UPDATE,
        (place ? "Legal hold placed" : "Legal hold released")
            + " (request "
            + request.getId()
            + ", approved by "
            + approver
            + "): "
            + request.getReason());
  }

  private static String requireText(String text) {
    if (text == null || text.isBlank()) {
      throw new BusinessRuleException("REASON_REQUIRED", "Enter the reason");
    }
    String clean = text.strip();
    return clean.length() > MAX_TEXT ? clean.substring(0, MAX_TEXT) : clean;
  }
}
