package com.iortatechnxt.brokerverse.eb.bor.service;

import com.iortatechnxt.brokerverse.attachment.domain.AllowedFileType;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.eb.cycle.service.BorGate;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService.Registration;
import com.iortatechnxt.brokerverse.eb.domain.EbBor;
import com.iortatechnxt.brokerverse.eb.domain.EbBorRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbBorStatus;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbDocument;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentSource;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The client's signed Broker on Record (BRID-008; FR-EB-031): the AO uploads it (PDF or Word) as a
 * new version of the cycle, a validator completes the checklist and sets the validity, or rejects
 * it with a reason; the AO then uploads a corrected version. The latest validated version in force
 * is the programme's active BOR: going to market (franchise and insurer requests of a new business
 * or a remarketed renewal) is refused without it. The uploader is notified of the decision.
 * E-signature verification is a parked seam: the validator attests the signature (EBQ06).
 */
@Service
@Transactional
public class BorService implements BorGate {

  private static final String BOR_VERSION = "Broker on Record version ";

  private static final Set<AllowedFileType> BOR_TYPES =
      Set.of(AllowedFileType.PDF, AllowedFileType.DOC, AllowedFileType.DOCX);

  private final EbBorRepository bors;
  private final EbRecords records;
  private final EbDocumentService documents;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param bors BOR versions
   * @param records cycle look-up
   * @param documents EB document register
   * @param notifications in-app notices
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public BorService(
      EbBorRepository bors,
      EbRecords records,
      EbDocumentService documents,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.bors = bors;
    this.records = records;
    this.documents = documents;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Uploads the signed BOR of a cycle as a new version (status UPLOADED).
   *
   * @param companyId company
   * @param cycleId cycle
   * @param file the signed BOR (PDF or Word)
   * @return the version
   */
  public EbBor upload(Long companyId, Long cycleId, UploadedFile file) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    boolean wordOrPdf =
        AllowedFileType.fromFileName(file.name()).map(BOR_TYPES::contains).orElse(false);
    if (!wordOrPdf) {
      throw new BusinessRuleException(
          "ATTACHMENT_TYPE_NOT_ALLOWED", "Upload the BOR as PDF or Word");
    }
    List<EbBor> versions = bors.findByCycleIdOrderByVersionNoDesc(cycle.getId());
    if (!versions.isEmpty() && versions.get(0).getStatus() == EbBorStatus.UPLOADED) {
      throw new BusinessRuleException(
          "EB_BOR_PENDING",
          "Version " + versions.get(0).getVersionNo() + " of the BOR waits for validation");
    }
    int version = versions.isEmpty() ? 1 : versions.get(0).getVersionNo() + 1;
    EbDocument stored =
        documents
            .store(
                cycle,
                new Registration(
                    EbDocumentTypes.BOR,
                    EbDocumentService.placementProcess(cycle),
                    EbDocumentSource.CLIENT,
                    false,
                    BOR_VERSION + version),
                List.of(file))
            .get(0);
    EbBor bor = bors.save(new EbBor(cycle, version, stored.getAttachmentId()));
    audit.record(
        EbCodes.ENTITY_CYCLE,
        cycle.getCycleNo(),
        AuditAction.CREATE,
        BOR_VERSION + version + " uploaded");
    return bor;
  }

  /**
   * Validates a BOR version with the validator's checklist and validity; the earlier validated
   * version of the cycle is superseded.
   *
   * @param companyId company
   * @param borId BOR version
   * @param checklist checklist and validity
   * @return the version
   */
  public EbBor validate(Long companyId, Long borId, EbBor.Checklist checklist) {
    EbBor bor = require(companyId, borId);
    EbCycle cycle = records.openCycle(companyId, bor.getCycleId());
    String validator = currentUser.username();
    bor.validate(checklist, validator, clock.instant());
    bors.findByCycleIdOrderByVersionNoDesc(cycle.getId()).stream()
        .filter(b -> !b.getId().equals(bor.getId()))
        .forEach(EbBor::supersede);
    audit.record(
        EbCodes.ENTITY_CYCLE,
        cycle.getCycleNo(),
        AuditAction.AUTHORIZE,
        BOR_VERSION
            + bor.getVersionNo()
            + " validated, valid "
            + checklist.validFrom()
            + " to "
            + checklist.validTo());
    tellUploader(bor, cycle, "validated");
    return bor;
  }

  /**
   * Rejects a BOR version with a reason.
   *
   * @param companyId company
   * @param borId BOR version
   * @param reason why
   * @return the version
   */
  public EbBor reject(Long companyId, Long borId, String reason) {
    EbBor bor = require(companyId, borId);
    EbCycle cycle = records.openCycle(companyId, bor.getCycleId());
    bor.reject(reason, currentUser.username(), clock.instant());
    documents.reject(bor.getAttachmentId());
    audit.record(
        EbCodes.ENTITY_CYCLE,
        cycle.getCycleNo(),
        AuditAction.REJECT,
        BOR_VERSION + bor.getVersionNo() + " rejected: " + bor.getRejectReason());
    tellUploader(bor, cycle, "rejected");
    return bor;
  }

  @Override
  @Transactional(readOnly = true)
  public void requireValidated(EbCycle cycle, LocalDate date) {
    boolean active =
        bors.findByProgrammeIdOrderByIdDesc(cycle.getProgrammeId()).stream()
            .anyMatch(b -> b.activeOn(date));
    if (!active) {
      throw new BusinessRuleException(
          "EB_BOR_REQUIRED", "Cycle " + cycle.getCycleNo() + " has no validated Broker on Record");
    }
  }

  /**
   * The BOR versions of a programme, latest first.
   *
   * @param companyId company
   * @param programmeId programme
   * @return versions
   */
  @Transactional(readOnly = true)
  public List<EbBor> ofProgramme(Long companyId, Long programmeId) {
    EbProgramme programme = records.programme(companyId, programmeId);
    return bors.findByProgrammeIdOrderByIdDesc(programme.getId());
  }

  private EbBor require(Long companyId, Long borId) {
    return bors.findById(borId)
        .filter(b -> b.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException("EbBor", borId));
  }

  private void tellUploader(EbBor bor, EbCycle cycle, String outcome) {
    if (CurrentUser.sameUser(bor.getCreatedBy(), currentUser.username())) {
      return;
    }
    notifications.notifyUser(
        bor.getCreatedBy(),
        new Notice(
            cycle.getCycleNo() + ": Broker on Record " + outcome,
            "Version " + bor.getVersionNo() + " of the Broker on Record was " + outcome + ".",
            EbCodes.PROGRAMME_LINK + cycle.getProgrammeId() + "?tab=bor",
            EbCodes.ENTITY_CYCLE,
            cycle.getId().toString()),
        EbCodes.EVENT_BOR_DECIDED);
  }
}
