package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountLifecycleService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.placement.domain.PlacementSlip;
import com.iortatechnxt.brokerverse.placement.domain.PlacementSlip.SlipInsurer;
import com.iortatechnxt.brokerverse.placement.domain.PlacementSlip.SlipNumber;
import com.iortatechnxt.brokerverse.placement.domain.PlacementSlipRepository;
import com.iortatechnxt.brokerverse.placement.domain.SlipAccount;
import com.iortatechnxt.brokerverse.placement.domain.SlipFile;
import com.iortatechnxt.brokerverse.placement.domain.SlipFileRepository;
import com.iortatechnxt.brokerverse.placement.domain.SlipStatus;
import com.iortatechnxt.brokerverse.placement.service.InsurerDirectory.PlacementAddress;
import com.iortatechnxt.brokerverse.placement.service.SlipDocuments.SlipHeader;
import com.iortatechnxt.brokerverse.placement.service.SlipPrerequisites.Unmet;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Placement slips (BRNB.069) and sending them to the insurer (BRNB.071). Slips are generated only
 * when every prerequisite of every account is met, one slip PL-yyyy per insurer branch, as PDF and
 * XLSX from the PLACEMENT_SLIP template. A slip regenerated after a return gets the next version;
 * the old one is kept. The first send records the placement of each account through {@code
 * AccountLifecycleService.recordPlacement}; later sends are resends. The send log is the messaging
 * outbox of entity {@value #ENTITY}.
 */
@Service
@Transactional
public class PlacementSlipService {

  /** Entity type of slips (audit, e-mail send log). */
  public static final String ENTITY = "PlacementSlip";

  /** Owner entity type of the stored slip files (the slip). */
  public static final String OWNER_TYPE = ENTITY;

  /** Record class of slip files. */
  public static final String RECORD_CLASS = "POLICY_DOCUMENT";

  /** PDF file format. */
  public static final String PDF = "PDF";

  /** XLSX file format. */
  public static final String XLSX = "XLSX";

  private static final String TEMPLATE = "PLACEMENT_SLIP";
  private static final String PDF_TYPE = "application/pdf";
  private static final String XLSX_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final PlacementSlipRepository slips;
  private final SlipFileRepository files;
  private final StoredFileService storedFiles;
  private final PlacementAccounts accounts;
  private final SlipPrerequisites prerequisites;
  private final InsurerDirectory insurers;
  private final SlipDocuments documents;
  private final DocTemplateService templates;
  private final DocumentNumberService numbers;
  private final MessageService messages;
  private final AccountLifecycleService lifecycle;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param slips slips
   * @param files slip files
   * @param storedFiles file store (slip files)
   * @param accounts account look-ups
   * @param prerequisites slip prerequisites
   * @param insurers insurer addressing
   * @param documents slip rendering
   * @param templates document templates
   * @param numbers document numbers
   * @param messages outbound e-mail
   * @param lifecycle account lifecycle
   * @param audit audit trail
   * @param clock clock
   */
  public PlacementSlipService(
      PlacementSlipRepository slips,
      SlipFileRepository files,
      StoredFileService storedFiles,
      PlacementAccounts accounts,
      SlipPrerequisites prerequisites,
      InsurerDirectory insurers,
      SlipDocuments documents,
      DocTemplateService templates,
      DocumentNumberService numbers,
      MessageService messages,
      AccountLifecycleService lifecycle,
      AuditTrailService audit,
      Clock clock) {
    this.slips = slips;
    this.files = files;
    this.storedFiles = storedFiles;
    this.accounts = accounts;
    this.prerequisites = prerequisites;
    this.insurers = insurers;
    this.documents = documents;
    this.templates = templates;
    this.numbers = numbers;
    this.messages = messages;
    this.lifecycle = lifecycle;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The readiness of accounts for a slip.
   *
   * @param companyId company
   * @param arns accounts
   * @return each account with its unmet prerequisites
   */
  @Transactional(readOnly = true)
  public List<Readiness> readiness(Long companyId, List<String> arns) {
    return arns.stream()
        .map(arn -> accounts.require(companyId, arn))
        .map(a -> new Readiness(a, prerequisites.check(a)))
        .toList();
  }

  /**
   * Generates the slips of the accounts, one per insurer branch.
   *
   * @param companyId company
   * @param arns accounts ready for placement
   * @return the new slips
   */
  public List<PlacementSlip> generate(Long companyId, List<String> arns) {
    if (arns == null || arns.isEmpty()) {
      throw new BusinessRuleException("SLIP_NO_ACCOUNTS", "Select the accounts to place");
    }
    List<Account> ready = requireReady(readiness(companyId, arns.stream().distinct().toList()));
    Map<SlipInsurer, List<Account>> groups =
        ready.stream()
            .collect(
                Collectors.groupingBy(
                    a -> new SlipInsurer(a.getInsurerCode(), a.getInsurerBranch()),
                    LinkedHashMap::new,
                    Collectors.toList()));
    String prefix = "PL-" + BusinessClock.today(clock).getYear();
    return groups.entrySet().stream()
        .map(g -> createReplacing(companyId, prefix, g.getKey(), g.getValue()))
        .toList();
  }

  /**
   * Creates the slip of one insurer branch. When the accounts were already on a live slip (placed
   * again after an insurer return), that slip is replaced: a slip to the same insurer branch with
   * no other account gets its next version (PL-2026-000001 v2), and every such earlier slip is
   * marked SUPERSEDED, as Regenerate does.
   */
  private PlacementSlip createReplacing(
      Long companyId, String prefix, SlipInsurer insurer, List<Account> slipAccounts) {
    List<PlacementSlip> replaced = replacedSlips(slipAccounts);
    SlipNumber number =
        replaced.stream()
            .filter(s -> sameInsurer(s, insurer))
            .findFirst()
            .map(s -> new SlipNumber(s.getSlipNo(), latestVersion(s.getSlipNo()) + 1))
            .orElseGet(() -> new SlipNumber(numbers.next(prefix), 1));
    replaced.forEach(PlacementSlip::supersede);
    PlacementSlip created = create(companyId, number, insurer, slipAccounts);
    replaced.forEach(s -> recordSuperseded(s, created));
    return created;
  }

  /** Live slips whose accounts are all on the new slip (they are replaced by it). */
  private List<PlacementSlip> replacedSlips(List<Account> slipAccounts) {
    Set<String> arns = slipAccounts.stream().map(Account::getArn).collect(Collectors.toSet());
    return arns.stream()
        .flatMap(arn -> slips.findByArn(arn).stream())
        .filter(s -> s.getStatus() != SlipStatus.SUPERSEDED)
        .filter(s -> s.getAccounts().stream().allMatch(a -> arns.contains(a.arn())))
        .distinct()
        .sorted(Comparator.comparing(PlacementSlip::getId))
        .toList();
  }

  private static boolean sameInsurer(PlacementSlip slip, SlipInsurer insurer) {
    return Objects.equals(slip.getInsurerCode(), insurer.insurerCode())
        && Objects.equals(slip.getBranchCode(), insurer.branchCode());
  }

  private int latestVersion(String slipNo) {
    return slips.findBySlipNo(slipNo).stream()
        .mapToInt(PlacementSlip::getVersionNo)
        .max()
        .orElse(1);
  }

  private void recordSuperseded(PlacementSlip previous, PlacementSlip replacement) {
    audit.record(
        ENTITY,
        previous.displayNo(),
        AuditAction.UPDATE,
        "Superseded by placement slip " + replacement.displayNo());
  }

  /**
   * Regenerates a slip after an insurer return (BRNB.069): the next version with the same accounts
   * and number; the previous version is kept as superseded.
   *
   * @param slipId slip (latest version)
   * @return the new version
   */
  public PlacementSlip regenerate(Long slipId) {
    PlacementSlip previous = get(slipId);
    if (previous.getStatus() == SlipStatus.SUPERSEDED) {
      throw new BusinessRuleException(
          "SLIP_SUPERSEDED", "Slip " + previous.displayNo() + " was already replaced");
    }
    List<Account> ready =
        requireReady(
            readiness(
                previous.getCompanyId(),
                previous.getAccounts().stream().map(SlipAccount::arn).toList()));
    previous.supersede();
    PlacementSlip created =
        create(
            previous.getCompanyId(),
            new SlipNumber(previous.getSlipNo(), previous.getVersionNo() + 1),
            new SlipInsurer(previous.getInsurerCode(), previous.getBranchCode()),
            ready);
    recordSuperseded(previous, created);
    return created;
  }

  private static List<Account> requireReady(List<Readiness> readiness) {
    List<String> problems =
        readiness.stream()
            .filter(r -> !r.unmet().isEmpty())
            .map(
                r ->
                    r.account().getArn()
                        + ": "
                        + r.unmet().stream().map(Unmet::message).collect(Collectors.joining("; ")))
            .toList();
    if (!problems.isEmpty()) {
      throw new BusinessRuleException(
          "SLIP_PREREQUISITES_UNMET",
          "Placement prerequisites not met - " + String.join(" | ", problems));
    }
    return readiness.stream().map(Readiness::account).toList();
  }

  private PlacementSlip create(
      Long companyId, SlipNumber number, SlipInsurer insurer, List<Account> slipAccounts) {
    PlacementAddress address =
        insurers.address(companyId, insurer.insurerCode(), insurer.branchCode());
    MergedText text =
        templates.merge(
            TEMPLATE,
            BusinessClock.today(clock),
            Map.of("reference", number.slipNo(), "insurerName", address.insurerName()));
    PlacementSlip slip =
        slips.save(
            new PlacementSlip(
                companyId,
                number,
                insurer,
                text.versionTag(),
                slipAccounts.stream().map(a -> new SlipAccount(a.getId(), a.getArn())).toList()));
    String name = slip.displayNo().replace(' ', '_');
    SlipHeader header =
        new SlipHeader(
            companyId, slip.displayNo(), insurer.insurerCode(), address, text, slip.getCreatedBy());
    store(slip, PDF, name + ".pdf", documents.slipPdf(header, slipAccounts));
    store(slip, XLSX, name + ".xlsx", documents.slipXlsx(slip.displayNo(), slipAccounts));
    audit.record(
        ENTITY,
        slip.displayNo(),
        AuditAction.CREATE,
        "Placement slip for "
            + insurer.insurerCode()
            + "/"
            + insurer.branchCode()
            + ": "
            + slip.getAccounts().stream().map(SlipAccount::arn).collect(Collectors.joining(", ")));
    return slip;
  }

  private void store(PlacementSlip slip, String format, String fileName, byte[] content) {
    String sha256 = sha256(content);
    Long stored =
        storedFiles
            .storeChecked(
                new StoreRequest(
                    new FileOwner(slip.getCompanyId(), OWNER_TYPE, String.valueOf(slip.getId())),
                    format,
                    RECORD_CLASS,
                    fileName,
                    content,
                    sha256),
                PDF.equals(format) ? PDF_TYPE : XLSX_TYPE,
                FileOrigin.GENERATED)
            .getId();
    files.save(new SlipFile(slip.getId(), format, fileName, sha256, stored));
  }

  /**
   * The e-mail proposed for a slip: the insurer branch mailbox, subject and body.
   *
   * @param slipId slip
   * @return draft
   */
  @Transactional(readOnly = true)
  public SlipEmail draft(Long slipId) {
    PlacementSlip slip = get(slipId);
    PlacementAddress address =
        insurers.address(slip.getCompanyId(), slip.getInsurerCode(), slip.getBranchCode());
    String arns =
        slip.getAccounts().stream().map(SlipAccount::arn).collect(Collectors.joining(", "));
    return new SlipEmail(
        address.recipients(),
        List.of(),
        "Placement " + slip.displayNo() + " - " + arns,
        "Dear "
            + address.insurerName()
            + ",\n\nPlease find attached our placement slip "
            + slip.displayNo()
            + " for "
            + slip.getAccounts().size()
            + " risk(s) (ARN "
            + arns
            + "). Kindly issue the policy and send the e-policy quoting"
            + " our Account Reference Number.\n\nBDO Insurance and Reinsurance Brokers, Inc.",
        true);
  }

  /**
   * Sends (or resends) a slip to the insurer by e-mail with its PDF and XLSX files; the first send
   * places the accounts (BRNB.071).
   *
   * @param slipId slip
   * @param email recipients, subject, body and protection
   * @return the slip
   */
  public PlacementSlip send(Long slipId, SlipEmail email) {
    PlacementSlip slip = get(slipId);
    insurers.address(slip.getCompanyId(), slip.getInsurerCode(), slip.getBranchCode());
    List<MessageFile> attachments =
        files.findBySlipIdOrderByFormatAsc(slipId).stream()
            .map(
                f ->
                    new MessageFile(
                        f.getFileName(),
                        PDF.equals(f.getFormat()) ? PDF_TYPE : XLSX_TYPE,
                        content(f)))
            .toList();
    messages.queueEmail(
        new OutboundEmail(
            slip.getCompanyId(),
            "PLACEMENT_SLIP",
            email.to(),
            email.cc(),
            email.subject(),
            email.body(),
            attachments,
            email.protect() ? new OutboundEmail.Protection(null, true, null) : null,
            new RecordLink(ENTITY, String.valueOf(slip.getId()), slip.displayNo())));
    boolean first = slip.recordSend(String.join(", ", email.to()), clock.instant());
    if (first) {
      for (SlipAccount a : slip.getAccounts()) {
        lifecycle.recordPlacement(
            a.arn(), slip.displayNo(), slip.getInsurerCode(), slip.getBranchCode());
      }
    }
    audit.record(
        ENTITY,
        slip.displayNo(),
        AuditAction.UPDATE,
        (first ? "Sent to " : "Resent to ") + String.join(", ", email.to()));
    return slip;
  }

  /**
   * Slips of a company, newest first.
   *
   * @param companyId company
   * @param status status, null for all
   * @param pageable page
   * @return slips
   */
  @Transactional(readOnly = true)
  public Page<PlacementSlip> search(Long companyId, SlipStatus status, Pageable pageable) {
    Page<PlacementSlip> page =
        status == null
            ? slips.findByCompanyIdOrderByIdDesc(companyId, pageable)
            : slips.findByCompanyIdAndStatusOrderByIdDesc(companyId, status, pageable);
    page.forEach(PlacementSlip::getAccounts);
    return page;
  }

  /**
   * One slip.
   *
   * @param id slip
   * @return slip with its accounts
   */
  @Transactional(readOnly = true)
  public PlacementSlip get(Long id) {
    PlacementSlip slip =
        slips.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    slip.getAccounts();
    return slip;
  }

  /**
   * One file of a slip for the download endpoint: a presigned link to the stored file, or the bytes
   * of a file rendered before ST1; the download is audited.
   *
   * @param slipId slip
   * @param format PDF or XLSX
   * @return download
   */
  public FileDownload file(Long slipId, String format) {
    SlipFile file =
        files
            .findBySlipIdAndFormat(slipId, format)
            .orElseThrow(
                () -> new ResourceNotFoundException("Placement slip file", slipId + "/" + format));
    audit.record(ENTITY, slipId, AuditAction.EXPORT, "Downloaded " + file.getFileName());
    return file.getStoredFileId() == null
        ? FileDownload.inline(
            file.getFileName(),
            PDF.equals(file.getFormat()) ? PDF_TYPE : XLSX_TYPE,
            file.getContent())
        : FileDownload.stored(file.getStoredFileId());
  }

  private byte[] content(SlipFile file) {
    return file.getStoredFileId() == null
        ? file.getContent()
        : storedFiles.read(file.getStoredFileId());
  }

  static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * An account with its unmet slip prerequisites.
   *
   * @param account account
   * @param unmet unmet prerequisites; empty when ready
   */
  public record Readiness(Account account, List<Unmet> unmet) {

    /** Defensive copy. */
    public Readiness {
      unmet = List.copyOf(unmet);
    }
  }

  /**
   * An e-mail to the insurer.
   *
   * @param to recipients
   * @param cc copy
   * @param subject subject
   * @param body body
   * @param protect password-protect the files (password in a separate e-mail)
   */
  public record SlipEmail(
      List<String> to, List<String> cc, String subject, String body, boolean protect) {

    /** Defensive copies. */
    public SlipEmail {
      to = to == null ? List.of() : List.copyOf(to);
      cc = cc == null ? List.of() : List.copyOf(cc);
    }
  }
}
