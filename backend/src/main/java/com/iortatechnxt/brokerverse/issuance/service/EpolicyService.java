package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountLifecycleService;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.issuance.domain.Epolicy;
import com.iortatechnxt.brokerverse.issuance.domain.Epolicy.ReceivedDocument;
import com.iortatechnxt.brokerverse.issuance.domain.EpolicyRepository;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyMatcher.Resolution;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E-policy receipt and review (BRNB.073/074/104/112). A received PDF is matched to its account (ARN
 * chosen by the user, policy number, ARN in the file name or in the document), stored as the
 * account's EPOLICY document and handed to the document triggers (extraction review, BRNB.105). The
 * review compares the extracted values with the account; confirming records the policy number(s)
 * through {@code AccountLifecycleService.recordPolicy}. Mailbox and SFTP reading are parked (Q12,
 * Q31): e-policies are uploaded.
 */
@Service
@Transactional
public class EpolicyService {

  /** Audit entity type. */
  public static final String ENTITY = "Epolicy";

  /** Document type of e-policies (list DOCUMENT_TYPE). */
  public static final String EPOLICY = "EPOLICY";

  private static final String REJECT_LOV = "EPOLICY_REJECT_REASON";
  private static final Set<AccountStatus> RECEIVABLE =
      EnumSet.of(AccountStatus.PLACED, AccountStatus.POLICY_ISSUED);

  private final EpolicyRepository epolicies;
  private final AccountQueryService accounts;
  private final DocumentService documents;
  private final AttachmentService attachments;
  private final DocumentTriggerService triggers;
  private final EpolicyMatcher matcher;
  private final AccountLifecycleService lifecycle;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param epolicies e-policies
   * @param accounts account reads
   * @param documents account documents
   * @param attachments stored files
   * @param triggers document triggers
   * @param matcher account matching
   * @param lifecycle account lifecycle
   * @param lovs lists of values
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public EpolicyService(
      EpolicyRepository epolicies,
      AccountQueryService accounts,
      DocumentService documents,
      AttachmentService attachments,
      DocumentTriggerService triggers,
      EpolicyMatcher matcher,
      AccountLifecycleService lifecycle,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.epolicies = epolicies;
    this.accounts = accounts;
    this.documents = documents;
    this.attachments = attachments;
    this.triggers = triggers;
    this.matcher = matcher;
    this.lifecycle = lifecycle;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Receives an e-policy.
   *
   * @param file company, file and optional ARN or policy number
   * @return the e-policy (in review when the trigger rule is active)
   */
  public Epolicy receive(ReceivedFile file) {
    Resolution match =
        matcher
            .match(file)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "EPOLICY_ACCOUNT_NOT_FOUND",
                        "No account found for " + file.fileName() + ": choose the account (ARN)"));
    Account account = match.account();
    if (!RECEIVABLE.contains(account.getStatus())) {
      throw new BusinessRuleException(
          "EPOLICY_ACCOUNT_STATUS",
          "Account "
              + account.getArn()
              + " is "
              + account.getStatus()
              + "; an e-policy is received once placed");
    }
    Attachment stored =
        documents
            .upload(
                new AttachmentTarget(AccountService.ENTITY, String.valueOf(account.getId())),
                List.of(new UploadedFile(file.fileName(), file.content())),
                new UploadOptions(
                    EPOLICY, true, account.getArn(), "E-policy received from the insurer"))
            .get(0);
    Epolicy epolicy =
        epolicies.save(
            new Epolicy(
                account.getCompanyId(),
                account.getId(),
                account.getArn(),
                new ReceivedDocument(stored.getId(), stored.getFileName(), match.method())));
    triggers.onEpolicy(EPOLICY, epolicy, account.getInsurerCode(), file.content());
    audit.record(
        ENTITY,
        account.getArn(),
        AuditAction.CREATE,
        "E-policy " + stored.getFileName() + " received (matched by " + match.method() + ")");
    return epolicy;
  }

  /**
   * One e-policy with its account (extraction review).
   *
   * @param id e-policy
   * @return e-policy and account
   */
  @Transactional(readOnly = true)
  public Review review(Long id) {
    Epolicy epolicy = get(id);
    return new Review(epolicy, accounts.requireByArn(epolicy.getArn()));
  }

  /**
   * Extracts the policy data again (e.g. after a new pattern was configured).
   *
   * @param id e-policy
   * @return the e-policy
   */
  public Epolicy reextract(Long id) {
    Epolicy epolicy = get(id);
    Account account = accounts.requireByArn(epolicy.getArn());
    triggers.extract(
        epolicy,
        account.getInsurerCode(),
        attachments.download(epolicy.getAttachmentId()).content());
    return epolicy;
  }

  /**
   * Confirms the policy number(s) after review: the account records them (policy issued).
   *
   * @param id e-policy
   * @param policyNumbers policy numbers, one per policy year (BRNB.112)
   * @param issueDate issue date
   * @return the e-policy
   */
  public Epolicy confirm(Long id, List<String> policyNumbers, LocalDate issueDate) {
    Epolicy epolicy = get(id);
    if (!epolicy.isOpen()) {
      throw new BusinessRuleException(
          "EPOLICY_REVIEWED",
          "The e-policy " + epolicy.getFileName() + " is already " + epolicy.getStatus());
    }
    LocalDate date = issueDate == null ? LocalDate.now(clock) : issueDate;
    Account account = lifecycle.recordPolicy(epolicy.getArn(), policyNumbers, date);
    epolicy.confirm(account.getPolicyNumbers(), date, currentUser.username(), clock.instant());
    audit.record(
        ENTITY,
        epolicy.getArn(),
        AuditAction.AUTHORIZE,
        "Policy "
            + String.join(", ", account.getPolicyNumbers())
            + " confirmed from "
            + epolicy.getFileName());
    return epolicy;
  }

  /**
   * Rejects a received document at review.
   *
   * @param id e-policy
   * @param reasonCode reason (list EPOLICY_REJECT_REASON)
   * @param remarks remarks
   * @return the e-policy
   */
  public Epolicy reject(Long id, String reasonCode, String remarks) {
    Epolicy epolicy = get(id);
    lovs.requireValid(REJECT_LOV, reasonCode, LocalDate.now(clock));
    epolicy.reject(reasonCode, currentUser.username(), clock.instant());
    audit.record(
        ENTITY,
        epolicy.getArn(),
        AuditAction.REJECT,
        "E-policy "
            + epolicy.getFileName()
            + " rejected ("
            + reasonCode
            + ")"
            + (blank(remarks) ? "" : ": " + remarks));
    return epolicy;
  }

  /**
   * One e-policy.
   *
   * @param id id
   * @return e-policy
   */
  @Transactional(readOnly = true)
  public Epolicy get(Long id) {
    return epolicies.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  /**
   * A received file.
   *
   * @param companyId company
   * @param fileName file name
   * @param content PDF bytes
   * @param arn account chosen by the user, may be null
   * @param policyNo policy number given by the user, may be null
   */
  public record ReceivedFile(
      Long companyId, String fileName, byte[] content, String arn, String policyNo) {

    /** Defensive copy. */
    public ReceivedFile {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ReceivedFile f
          && Objects.equals(companyId, f.companyId)
          && Objects.equals(fileName, f.fileName)
          && Arrays.equals(content, f.content)
          && Objects.equals(arn, f.arn)
          && Objects.equals(policyNo, f.policyNo);
    }

    @Override
    public int hashCode() {
      return Objects.hash(companyId, fileName, arn, policyNo, Arrays.hashCode(content));
    }

    @Override
    public String toString() {
      return "ReceivedFile[" + fileName + "]";
    }
  }

  /**
   * An e-policy with its account.
   *
   * @param epolicy e-policy
   * @param account account
   */
  public record Review(Epolicy epolicy, Account account) {}
}
