package com.iortatechnxt.brokerverse.brokerclaims.insurer.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerUpdate;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerUpdateRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insurer updates of a claim (BRCLM.041; FR-CM-022): each communication from an insurer with its
 * date, source, reference, remarks and the claim attachments that carry the insurer's document,
 * optionally tied to an insurer line. Updates are insert-only and allowed on closed claims too
 * (design 5.1); a correction is a new update that refers to the wrong one.
 */
@Service
@Transactional
public class InsurerUpdateService {

  private final InsurerUpdateRepository updates;
  private final InsurerClaimService lines;
  private final AttachmentService attachments;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param updates updates
   * @param lines insurer lines
   * @param attachments claim attachments
   * @param lovs update sources
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public InsurerUpdateService(
      InsurerUpdateRepository updates,
      InsurerClaimService lines,
      AttachmentService attachments,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.updates = updates;
    this.lines = lines;
    this.attachments = attachments;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records an insurer update.
   *
   * @param claim claim (open or closed)
   * @param request what the insurer communicated
   * @return the update
   */
  public InsurerUpdate record(Claim claim, NewUpdate request) {
    LocalDate today = LocalDate.now(clock);
    if (request.source() == null || request.source().isBlank()) {
      throw new BusinessRuleException("BCL_SOURCE_REQUIRED", "Select the source of the update");
    }
    lovs.requireValid(ClaimCodes.LOV_UPDATE_SOURCE, request.source(), today);
    if (request.insurerClaimId() != null) {
      lines.require(claim, request.insurerClaimId());
    }
    if (request.correctsUpdateId() != null) {
      updates
          .findByIdAndClaimId(request.correctsUpdateId(), claim.getId())
          .orElseThrow(
              () -> new ResourceNotFoundException("InsurerUpdate", request.correctsUpdateId()));
    }
    request.attachmentIds().forEach(id -> requireClaimAttachment(claim, id));
    InsurerUpdate saved =
        updates.save(
            new InsurerUpdate(
                claim.getId(),
                new InsurerUpdate.Content(
                    request.insurerClaimId(),
                    request.updateDate(),
                    request.source(),
                    request.reference(),
                    request.remarks(),
                    request.attachmentIds(),
                    request.correctsUpdateId(),
                    request.uploadRef(),
                    today),
                currentUser.username(),
                clock.instant()));
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.UPDATE,
        "Insurer update of "
            + saved.getUpdateDate()
            + " ("
            + saved.getSource()
            + (saved.getReference() == null ? "" : ", " + saved.getReference())
            + ")"
            + (saved.getCorrectsUpdateId() == null
                ? ""
                : " correcting update " + saved.getCorrectsUpdateId()));
    return saved;
  }

  /**
   * The insurer updates of a claim.
   *
   * @param claimId claim
   * @return updates, newest first
   */
  @Transactional(readOnly = true)
  public List<InsurerUpdate> ofClaim(Long claimId) {
    return updates.findByClaimIdOrderByUpdateDateDescIdDesc(claimId);
  }

  private void requireClaimAttachment(Claim claim, Long attachmentId) {
    Attachment file = attachments.get(attachmentId);
    boolean own =
        ClaimCodes.ENTITY_TYPE.equals(file.getEntityType())
            && String.valueOf(claim.getId()).equals(file.getEntityId());
    if (!own) {
      throw new BusinessRuleException(
          "BCL_ATTACHMENT_NOT_ON_CLAIM",
          "Attach the insurer's document to claim " + claim.getClaimNo() + " first");
    }
  }

  /**
   * An insurer update to record.
   *
   * @param insurerClaimId insurer line, may be null
   * @param updateDate date of the update
   * @param source source ({@code BCL_UPDATE_SOURCE})
   * @param reference insurer's reference, may be null
   * @param remarks remarks
   * @param attachmentIds attachments of the claim, may be empty
   * @param correctsUpdateId update this one corrects, may be null
   * @param uploadRef bulk upload number, null on screen
   */
  public record NewUpdate(
      Long insurerClaimId,
      LocalDate updateDate,
      String source,
      String reference,
      String remarks,
      List<Long> attachmentIds,
      Long correctsUpdateId,
      String uploadRef) {

    /** Defensive copy. */
    public NewUpdate {
      attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
    }
  }
}
