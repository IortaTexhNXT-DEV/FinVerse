package com.iortatechnxt.brokerverse.eb.soa.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService.Registration;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentSource;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbSoa;
import com.iortatechnxt.brokerverse.eb.domain.EbSoaRepository;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbActivityLog;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.organization.service.OrganizationDirectory;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The insurer SOAs of a programme (BRID-021; FR-EB-053), numbered {@code EBS-<yyyy>-nnnnnn} with
 * their {@code EB_SOA} work case: received (PDF or Excel, uploaded by the AO or Processing with the
 * source INSURER; a duplicate SOA number of the insurer or the same file is refused), validated by
 * Processing with the booked invoices it bills, then released to the client's HR contacts and to
 * Collection ({@link SoaRelease}), or rejected with a reason. The payment status is read from the
 * invoice ledger.
 */
@Service
@Transactional
public class EbSoaService {

  private static final String SOA_PREFIX = "SOA ";

  private static final Set<String> SOA_FILES = Set.of("pdf", "xls", "xlsx");

  private final EbSoaRepository soas;
  private final EbRecords records;
  private final EbParties parties;
  private final SoaInvoices invoices;
  private final EbDocumentService documents;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final EbActivityLog activity;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final OrganizationDirectory organization;

  /**
   * Creates the service.
   *
   * @param soas SOAs
   * @param records programme look-up
   * @param parties insurers
   * @param invoices booked invoices of the programme
   * @param documents EB document register
   * @param workflow workflow engine
   * @param numbers document numbers
   * @param activity TAT stamps
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public EbSoaService(
      EbSoaRepository soas,
      EbRecords records,
      EbParties parties,
      SoaInvoices invoices,
      EbDocumentService documents,
      WorkflowService workflow,
      DocumentNumberService numbers,
      EbActivityLog activity,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock,
      OrganizationDirectory organization) {
    this.soas = soas;
    this.records = records;
    this.parties = parties;
    this.invoices = invoices;
    this.documents = documents;
    this.workflow = workflow;
    this.numbers = numbers;
    this.activity = activity;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
    this.organization = organization;
  }

  /**
   * Registers a received SOA.
   *
   * @param companyId company
   * @param programmeId programme
   * @param intake insurer, SOA number, period, amount, currency, date received, remarks
   * @param invoiceNos booked invoices of the programme it bills, may be empty
   * @param file the SOA (PDF or Excel)
   * @return the SOA, RECEIVED
   */
  public EbSoa receive(
      Long companyId,
      Long programmeId,
      EbSoa.Intake intake,
      List<String> invoiceNos,
      UploadedFile file) {
    EbProgramme programme = records.programme(companyId, programmeId);
    InsurerProfile insurer = parties.insurer(companyId, intake.insurerCode());
    EbSoa.Intake clean = check(intake, insurer, companyId);
    if (file == null) {
      throw new BusinessRuleException("EB_SOA_FILE_REQUIRED", "Attach the SOA file");
    }
    requireSoaFile(file.name());
    String hash = sha256(file.content());
    refuseDuplicate(companyId, insurer, clean.insurerSoaNo(), hash);
    List<String> billed = invoices.requireOfProgramme(programme, invoiceNos);
    Long attachment =
        store(programme, SOA_PREFIX + clean.insurerSoaNo() + " of " + insurer.getName(), file);
    String number =
        numbers.next(
            EbCodes.series(EbCodes.PREFIX_SOA, BusinessClock.currentYear(clock).getValue()));
    EbSoa soa =
        soas.save(new EbSoa(programme, number, clean, new EbSoa.StoredFile(attachment, hash)));
    soa.linkInvoices(billed);
    documents.linkAll(
        List.of(attachment),
        List.of(new AttachmentTarget(EbCodes.ENTITY_SOA, soa.getId().toString())),
        EbDocumentTypes.RENEWAL_PLACEMENT);
    workflow.start(
        new StartCase(
            companyId,
            EbCodes.WORKFLOW_SOA,
            new CaseRecord(
                EbCodes.ENTITY_SOA,
                soa.getId().toString(),
                number,
                SOA_PREFIX + clean.insurerSoaNo() + " - " + programme.getName(),
                EbCodes.SOA_LINK + soa.getId(),
                programme.getTeamCode()),
            null));
    activity.received(
        companyId, programme.getId(), TatActivity.SOA_VALIDATION, number, currentUser.username());
    audit.record(
        EbCodes.ENTITY_SOA,
        number,
        AuditAction.CREATE,
        SOA_PREFIX
            + clean.insurerSoaNo()
            + " of "
            + insurer.getName()
            + " received, amount "
            + clean.currency()
            + " "
            + clean.amount().toPlainString());
    return soa;
  }

  private Long store(EbProgramme programme, String description, UploadedFile file) {
    return documents
        .storeOn(
            programme,
            new AttachmentTarget(EbCodes.ENTITY_PROGRAMME, programme.getId().toString()),
            new Registration(
                EbDocumentTypes.SOA,
                EbDocumentTypes.RENEWAL_PLACEMENT,
                EbDocumentSource.INSURER,
                false,
                description),
            List.of(file),
            List.of())
        .get(0)
        .getAttachmentId();
  }

  private EbSoa.Intake check(EbSoa.Intake intake, InsurerProfile insurer, Long companyId) {
    requireFields(intake);
    LocalDate today = BusinessClock.today(clock);
    LocalDate received = intake.receivedOn() == null ? today : intake.receivedOn();
    if (received.isAfter(today)) {
      throw new BusinessRuleException(
          "EB_SOA_DATE_FUTURE", "The date received cannot be after today");
    }
    return new EbSoa.Intake(
        insurer.getPartyCode(),
        intake.insurerSoaNo().strip(),
        intake.periodFrom(),
        intake.periodTo(),
        intake.amount(),
        intake.currency() == null || intake.currency().isBlank()
            ? organization.company(companyId).baseCurrency()
            : intake.currency().strip(),
        received,
        intake.remarks());
  }

  private static void requireFields(EbSoa.Intake intake) {
    if (intake.insurerSoaNo() == null || intake.insurerSoaNo().isBlank()) {
      throw new BusinessRuleException("EB_SOA_NO_REQUIRED", "Enter the SOA number");
    }
    requirePeriodAndAmount(intake);
  }

  private static void requirePeriodAndAmount(EbSoa.Intake intake) {
    if (intake.periodFrom() == null
        || intake.periodTo() == null
        || intake.periodTo().isBefore(intake.periodFrom())) {
      throw new BusinessRuleException(
          "EB_SOA_PERIOD", "Enter the period of the SOA, the end on or after the start");
    }
    if (intake.amount() == null || intake.amount().signum() < 0) {
      throw new BusinessRuleException("EB_SOA_AMOUNT", "Enter an amount of zero or more");
    }
  }

  private static void requireSoaFile(String name) {
    int dot = name == null ? -1 : name.lastIndexOf('.');
    String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    if (!SOA_FILES.contains(ext)) {
      throw new BusinessRuleException(
          "ATTACHMENT_TYPE_NOT_ALLOWED", "Upload the SOA as PDF or Excel");
    }
  }

  private void refuseDuplicate(Long companyId, InsurerProfile insurer, String soaNo, String hash) {
    if (soas.existsByCompanyIdAndInsurerCodeAndInsurerSoaNoIgnoreCaseAndStatusNot(
        companyId, insurer.getPartyCode(), soaNo, EbSoa.Status.REJECTED)) {
      throw new BusinessRuleException(
          "EB_SOA_DUPLICATE",
          SOA_PREFIX + soaNo + " of " + insurer.getName() + " is already registered");
    }
    soas.findFirstByCompanyIdAndFileHashAndStatusNot(companyId, hash, EbSoa.Status.REJECTED)
        .ifPresent(
            s -> {
              throw new BusinessRuleException(
                  "EB_SOA_DUPLICATE_FILE",
                  "This SOA file is already registered as " + s.getSoaNo());
            });
  }

  /**
   * SHA-256 of a file.
   *
   * @param content bytes
   * @return hex digest
   */
  static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  /**
   * Validates a received SOA (Processing), with the invoices it bills.
   *
   * @param companyId company
   * @param soaId SOA
   * @param invoiceNos booked invoices of the programme, null to keep the linked ones
   * @return the SOA, VALIDATED
   */
  public EbSoa validate(Long companyId, Long soaId, List<String> invoiceNos) {
    EbSoa soa = require(companyId, soaId);
    requireStatus(soa, EbSoa.Status.RECEIVED);
    if (invoiceNos != null) {
      soa.linkInvoices(
          invoices.requireOfProgramme(
              records.programme(companyId, soa.getProgrammeId()), invoiceNos));
    }
    soa.validated(currentUser.username(), clock.instant());
    workflow.systemTransition(
        EbCodes.ENTITY_SOA, soa.getId().toString(), "validate", TransitionNote.NONE);
    activity.released(TatActivity.SOA_VALIDATION, soa.getSoaNo(), "Validated");
    activity.received(
        companyId,
        soa.getProgrammeId(),
        TatActivity.RELEASE,
        soa.getSoaNo(),
        currentUser.username());
    audit.record(EbCodes.ENTITY_SOA, soa.getSoaNo(), AuditAction.AUTHORIZE, "Validated");
    return soa;
  }

  /**
   * Rejects a received SOA with a reason.
   *
   * @param companyId company
   * @param soaId SOA
   * @param reasonCode reason (list EB_SOA_REJECT_REASON)
   * @param remarks remarks, may be null
   * @return the SOA, REJECTED
   */
  public EbSoa reject(Long companyId, Long soaId, String reasonCode, String remarks) {
    EbSoa soa = require(companyId, soaId);
    requireStatus(soa, EbSoa.Status.RECEIVED);
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BusinessRuleException("WORKFLOW_REASON_REQUIRED", "Select a reason for 'reject'");
    }
    soa.rejected(reasonCode.strip());
    workflow.systemTransition(
        EbCodes.ENTITY_SOA,
        soa.getId().toString(),
        "reject",
        new TransitionNote(reasonCode.strip(), remarks));
    activity.released(TatActivity.SOA_VALIDATION, soa.getSoaNo(), "Rejected");
    audit.record(EbCodes.ENTITY_SOA, soa.getSoaNo(), AuditAction.REJECT, "Rejected: " + reasonCode);
    return soa;
  }

  /**
   * An SOA of a company.
   *
   * @param companyId company
   * @param soaId SOA
   * @return SOA
   */
  @Transactional(readOnly = true)
  public EbSoa require(Long companyId, Long soaId) {
    return soas.findByIdAndCompanyId(soaId, companyId)
        .orElseThrow(() -> new ResourceNotFoundException(EbCodes.ENTITY_SOA, soaId));
  }

  /**
   * Refuses an SOA that is not in a status.
   *
   * @param soa SOA
   * @param status expected status
   */
  static void requireStatus(EbSoa soa, EbSoa.Status status) {
    if (soa.getStatus() != status) {
      throw new BusinessRuleException(
          "EB_SOA_STATUS",
          SOA_PREFIX + soa.getSoaNo() + " is " + soa.getStatus().name().toLowerCase(Locale.ROOT));
    }
  }
}
