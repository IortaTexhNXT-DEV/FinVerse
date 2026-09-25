package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail.Protection;
import com.iortatechnxt.brokerverse.productmaint.domain.Advisory;
import com.iortatechnxt.brokerverse.productmaint.domain.Advisory.Content;
import com.iortatechnxt.brokerverse.productmaint.domain.Advisory.Status;
import com.iortatechnxt.brokerverse.productmaint.domain.Advisory.Type;
import com.iortatechnxt.brokerverse.productmaint.domain.AdvisoryRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Package advisories (BRPM.016): drafted automatically from the PKG_ADVISORY / PKG_RENEWAL_ADVISORY
 * template when a request's version is released (or its package retired), edited by TSU, and sent
 * to the recipient groups: an in-app notice to each group's users and, when addresses are given, a
 * protected e-mail with the advisory PDF. Sending is blocked while the supporting documents are
 * missing ({@code ADVISORY_DOCUMENTS_MISSING}): the signed package slip and the ManCom sign-off, or
 * the sign-off alone for a retirement. The advisory PDF lists the supporting documents, which
 * authorised users open in BIBS.
 */
@Service
@Transactional
public class AdvisoryService {

  /** Document type of a sent advisory. */
  public static final String DOCUMENT_TYPE = "PKG_ADVISORY";

  /** Purpose code of the advisory e-mails. */
  public static final String PURPOSE = "PKG_ADVISORY";

  /**
   * The permission whose holders receive the in-app notice of a group (PQ12: the "relevant units"
   * are to be confirmed; groups added to the list later are reached by e-mail only).
   */
  static final Map<String, String> GROUP_PERMISSIONS =
      Map.of(
          "MARKETING", "QUOTE_MAINTAIN",
          "TSU", "PKG_NEGOTIATE",
          "MBS", "PRODUCT_MAINTAIN",
          "PROCESSING", "ACCOUNT_PROCESS",
          "OPERATIONS", "OPS_VIEW");

  private static final String ADVISORY = "Package advisory";
  private static final String GROUP_LIST = "PKG_ADVISORY_GROUP";

  private final PackageRequests requests;
  private final AdvisoryRepository advisories;
  private final DocTemplateService templates;
  private final PackageDocuments documents;
  private final DocumentService attachments;
  private final MessageService messages;
  private final NotificationService notifications;
  private final SystemParameterService parameters;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final TermsCodec codec;

  /**
   * Creates the service.
   *
   * @param requests request reads
   * @param advisories advisories
   * @param templates document templates
   * @param documents PDF builder
   * @param attachments documents of the request
   * @param messages outbox
   * @param notifications in-app notifications
   * @param parameters business parameters
   * @param lovs lists of values
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   * @param codec terms JSON
   */
  public AdvisoryService(
      PackageRequests requests,
      AdvisoryRepository advisories,
      DocTemplateService templates,
      PackageDocuments documents,
      DocumentService attachments,
      MessageService messages,
      NotificationService notifications,
      SystemParameterService parameters,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock,
      TermsCodec codec) {
    this.requests = requests;
    this.advisories = advisories;
    this.templates = templates;
    this.documents = documents;
    this.attachments = attachments;
    this.messages = messages;
    this.notifications = notifications;
    this.parameters = parameters;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
    this.codec = codec;
  }

  /**
   * Drafts the advisory of a released or retired request (BRPM.016: automatic on create / update).
   *
   * @param p request
   * @param type advisory type
   * @param versionNo released version, null for a retirement
   * @return the draft
   */
  public Advisory draftFor(PackageRequest p, Type type, Integer versionNo) {
    Advisory a =
        new Advisory(p.getCompanyId(), p.getId(), p.getTargetProductCode(), versionNo, type);
    MergedText text = text(p, type, versionNo);
    List<String> groups = parameters.items("PKG_ADVISORY_GROUPS");
    a.compose(
        new Content(
            groups.isEmpty() ? List.of("MARKETING") : groups,
            List.of(),
            text.title() + ": " + p.getTargetProductCode() + " - " + p.getTitle(),
            text.text(),
            text.code() == null ? null : text.versionTag()));
    Advisory saved = advisories.save(a);
    audit.record(
        PackageRequests.ENTITY,
        p.getRequestNo(),
        AuditAction.CREATE,
        type + " advisory drafted for " + p.getTargetProductCode());
    return saved;
  }

  private MergedText text(PackageRequest p, Type type, Integer versionNo) {
    if (type == Type.RETIREMENT) {
      return new MergedText(
          null,
          0,
          "Package retirement advisory",
          "This is to advise that the package "
              + p.getTargetProductCode()
              + " - "
              + p.getTitle()
              + " is retired and no longer available for new business. The ManCom sign-off is"
              + " kept with request "
              + p.getRequestNo()
              + " in BIBS.");
    }
    PackageTerms terms = codec.terms(p.getProposedTerms());
    Map<String, Object> values = new HashMap<>();
    values.put("productCode", p.getTargetProductCode());
    values.put("productName", p.getTitle());
    values.put("versionNo", String.valueOf(versionNo));
    values.put("effectiveFrom", String.valueOf(terms.dates().effectiveFrom()));
    values.put("reference", p.getRequestNo());
    String template = type == Type.RENEWAL ? "PKG_RENEWAL_ADVISORY" : "PKG_ADVISORY";
    return templates.merge(template, LocalDate.now(clock), values);
  }

  /**
   * Advisories of a request, newest first.
   *
   * @param id request
   * @return advisories
   */
  @Transactional(readOnly = true)
  public List<Advisory> ofRequest(Long id) {
    return advisories.findByRequestIdOrderByIdDesc(requests.get(id).getId());
  }

  /**
   * Draft advisories of a company (home tile "advisories pending").
   *
   * @param companyId company
   * @return drafts, newest first
   */
  @Transactional(readOnly = true)
  public List<Advisory> pending(Long companyId) {
    return advisories.findByCompanyIdAndStatusOrderByIdDesc(companyId, Status.DRAFT);
  }

  /**
   * Changes a draft advisory.
   *
   * @param advisoryId advisory
   * @param content groups, addresses, subject and body
   * @return the advisory
   */
  public Advisory update(Long advisoryId, Content content) {
    Advisory a = draft(advisoryId);
    LocalDate today = LocalDate.now(clock);
    if (content.groups().isEmpty()) {
      throw new BusinessRuleException(
          "ADVISORY_NO_RECIPIENT", "Select at least one recipient group");
    }
    content.groups().forEach(g -> lovs.requireValid(GROUP_LIST, g, today));
    if (content.subject() == null
        || content.subject().isBlank()
        || content.body() == null
        || content.body().isBlank()) {
      throw new BusinessRuleException(
          "ADVISORY_CONTENT_REQUIRED", "Enter the subject and the text");
    }
    a.compose(
        new Content(
            content.groups(),
            content.emailTo(),
            content.subject().strip(),
            content.body().strip(),
            a.getTemplateVersion()));
    audit.record(ADVISORY, a.getId(), AuditAction.UPDATE, "Advisory draft updated");
    return a;
  }

  /**
   * The supporting documents an advisory needs and whether the request has them (BRPM.016).
   *
   * @param a advisory
   * @return document type to attached flag
   */
  @Transactional(readOnly = true)
  public List<DocumentCheck> checklist(Advisory a) {
    List<String> required =
        a.getAdvisoryType() == Type.RETIREMENT
            ? List.of(RequirementsService.MANCOM_SIGNOFF)
            : List.of(RequirementsService.SIGNED_SLIP, RequirementsService.MANCOM_SIGNOFF);
    List<Attachment> present = supporting(a);
    return required.stream()
        .map(
            t ->
                new DocumentCheck(t, present.stream().anyMatch(d -> t.equals(d.getDocumentType()))))
        .toList();
  }

  /**
   * Sends an advisory (PKG_ADVISORY): blocked while a supporting document is missing.
   *
   * @param advisoryId advisory
   * @return the sent advisory
   */
  public Advisory send(Long advisoryId) {
    Advisory a = draft(advisoryId);
    List<String> missing =
        checklist(a).stream().filter(c -> !c.attached()).map(DocumentCheck::documentType).toList();
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "ADVISORY_DOCUMENTS_MISSING",
          "Attach the supporting documents before sending: " + String.join(", ", missing));
    }
    PackageRequest p = requests.get(a.getRequestId());
    List<Attachment> supporting = supporting(a);
    MessageFile pdf = advisoryPdf(p, a, supporting);
    Long stored = store(p, a, pdf);
    notifyGroups(a, p);
    List<Long> sent = new ArrayList<>();
    if (!a.getEmailToList().isEmpty()) {
      sent.add(email(p, a, pdf));
    }
    List<Long> docs = new ArrayList<>(supporting.stream().map(Attachment::getId).toList());
    docs.add(stored);
    a.markSent(docs, sent, currentUser.username(), clock.instant());
    audit.record(
        PackageRequests.ENTITY,
        p.getRequestNo(),
        AuditAction.UPDATE,
        a.getAdvisoryType() + " advisory sent to " + String.join(", ", a.getRecipientGroupList()));
    return a;
  }

  private Advisory draft(Long advisoryId) {
    Advisory a =
        advisories
            .findById(advisoryId)
            .orElseThrow(() -> new ResourceNotFoundException(ADVISORY, advisoryId));
    if (a.getStatus() != Status.DRAFT) {
      throw new BusinessRuleException("ADVISORY_ALREADY_SENT", "The advisory was already sent");
    }
    return a;
  }

  private List<Attachment> supporting(Advisory a) {
    if (a.getRequestId() == null) {
      return List.of();
    }
    return attachments
        .list(new AttachmentTarget(PackageRequests.ENTITY, String.valueOf(a.getRequestId())))
        .stream()
        .filter(
            d ->
                RequirementsService.SIGNED_SLIP.equals(d.getDocumentType())
                    || RequirementsService.MANCOM_SIGNOFF.equals(d.getDocumentType()))
        .toList();
  }

  private MessageFile advisoryPdf(PackageRequest p, Advisory a, List<Attachment> supporting) {
    MessageFile f =
        documents.pdf(
            p,
            "Package Advisory",
            p.getRequestNo() + "-ADV-" + a.getId(),
            List.of(
                new Fields(
                    null,
                    List.of(
                        new Field("Subject", a.getSubject()),
                        new Field("Recipients", String.join(", ", a.getRecipientGroupList())))),
                new Text(null, a.getBody()),
                new Table(
                    "Supporting documents (kept in BIBS)",
                    List.of("Document", "Type"),
                    supporting.stream()
                        .map(d -> List.of(d.getFileName(), d.getDocumentType()))
                        .toList(),
                    List.of())),
            a.getTemplateVersion() == null ? "Package advisory" : a.getTemplateVersion());
    return new MessageFile(
        p.getRequestNo() + "_advisory_" + a.getId() + ".pdf", f.mimeType(), f.content());
  }

  private Long store(PackageRequest p, Advisory a, MessageFile pdf) {
    return attachments
        .upload(
            new AttachmentTarget(PackageRequests.ENTITY, String.valueOf(p.getId())),
            List.of(new UploadedFile(pdf.fileName(), pdf.content())),
            new UploadOptions(DOCUMENT_TYPE, false, p.getRequestNo(), a.getSubject()))
        .get(0)
        .getId();
  }

  private void notifyGroups(Advisory a, PackageRequest p) {
    Notice notice =
        new Notice(
            a.getSubject(),
            a.getBody(),
            PackageRequests.link(p),
            PackageRequests.ENTITY,
            String.valueOf(p.getId()));
    a.getRecipientGroupList().stream()
        .map(GROUP_PERMISSIONS::get)
        .filter(Objects::nonNull)
        .distinct()
        .forEach(permission -> notifications.notifyPermission(permission, notice));
  }

  private Long email(PackageRequest p, Advisory a, MessageFile pdf) {
    return messages
        .queueEmail(
            new OutboundEmail(
                p.getCompanyId(),
                PURPOSE,
                a.getEmailToList(),
                List.of(),
                a.getSubject(),
                a.getBody()
                    + "\n\nThe advisory is attached (password protected; the password follows"
                    + " separately).\n\nBDO Insurance and Reinsurance Brokers, Inc.",
                List.of(pdf),
                new Protection(null, true, null),
                new RecordLink(
                    PackageRequests.ENTITY, String.valueOf(p.getId()), p.getRequestNo())))
        .messageId();
  }

  /**
   * A supporting document an advisory needs.
   *
   * @param documentType document type
   * @param attached whether the request has it
   */
  public record DocumentCheck(String documentType, boolean attached) {}
}
