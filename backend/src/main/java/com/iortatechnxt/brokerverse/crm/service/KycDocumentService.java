package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.domain.KycDocument;
import com.iortatechnxt.brokerverse.crm.domain.KycDocumentRepository;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import com.iortatechnxt.brokerverse.crm.service.KycChecklist.Document;
import com.iortatechnxt.brokerverse.crm.service.KycChecklist.Item;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KYC documents of a client (BRNB.030/090): typed uploads stored as attachments of the client and
 * the checklist of mandatory documents per client type. The checklist is configurable in the lists
 * of values KYC_DOCS_INDIVIDUAL and KYC_DOCS_CORPORATE because the BDO KYC standard is parked
 * (Q16).
 */
@Service
@Transactional
public class KycDocumentService {

  private static final String DOCUMENT_TYPE = "DOCUMENT_TYPE";

  private final ClientService clients;
  private final KycDocumentRepository documents;
  private final AttachmentService attachments;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param clients clients
   * @param documents KYC documents
   * @param attachments attachment store
   * @param lovs lists of values
   * @param audit audit trail
   * @param clock clock
   */
  public KycDocumentService(
      ClientService clients,
      KycDocumentRepository documents,
      AttachmentService attachments,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.clients = clients;
    this.documents = documents;
    this.attachments = attachments;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Uploads a KYC document of a type.
   *
   * @param clientId client
   * @param documentType document type (list of values DOCUMENT_TYPE)
   * @param fileName file name
   * @param content file bytes
   * @return the checklist after the upload
   */
  public KycChecklist upload(Long clientId, String documentType, String fileName, byte[] content) {
    Client client = clients.get(clientId);
    if (client.getStatus() == ClientStatus.INACTIVE) {
      throw new BusinessRuleException("CLIENT_INACTIVE", "The client is inactive");
    }
    LovValue type = lovs.requireValid(DOCUMENT_TYPE, documentType, LocalDate.now(clock));
    Attachment stored =
        attachments.upload(target(client), fileName, content, "KYC: " + type.getLabel());
    documents.save(new KycDocument(clientId, documentType, stored.getId()));
    if (client.getKycStatus() == KycStatus.NOT_STARTED) {
      client.setKycStatus(KycStatus.PENDING);
    }
    audit.record(
        ClientService.ENTITY,
        client.getProspectCode(),
        AuditAction.UPDATE,
        "KYC document " + type.getLabel() + " uploaded (" + stored.getFileName() + ")");
    return checklist(client);
  }

  /**
   * The KYC checklist of a client.
   *
   * @param clientId client
   * @return checklist
   */
  @Transactional(readOnly = true)
  public KycChecklist checklist(Long clientId) {
    return checklist(clients.get(clientId));
  }

  /**
   * The KYC checklist of a client.
   *
   * @param client client
   * @return checklist
   */
  @Transactional(readOnly = true)
  public KycChecklist checklist(Client client) {
    LocalDate today = LocalDate.now(clock);
    Map<Long, Attachment> live =
        attachments.list(target(client)).stream()
            .collect(Collectors.toMap(Attachment::getId, Function.identity()));
    Map<String, List<Document>> uploaded = new LinkedHashMap<>();
    for (KycDocument d : documents.findByClientIdOrderByIdAsc(client.getId())) {
      Attachment a = live.get(d.getAttachmentId());
      if (a != null) {
        uploaded
            .computeIfAbsent(d.getDocumentType(), k -> new ArrayList<>())
            .add(new Document(a.getId(), a.getFileName(), a.getCreatedBy(), a.getCreatedAt()));
      }
    }
    List<Item> items = new ArrayList<>();
    for (LovValue required : lovs.activeValues(checklistType(client.getClientType()), today)) {
      items.add(
          new Item(
              required.getCode(),
              required.getLabel(),
              true,
              uploaded.getOrDefault(required.getCode(), List.of())));
      uploaded.remove(required.getCode());
    }
    uploaded.forEach(
        (type, docs) -> items.add(new Item(type, lovs.label(DOCUMENT_TYPE, type), false, docs)));
    boolean complete = items.stream().noneMatch(i -> i.required() && i.documents().isEmpty());
    return new KycChecklist(items, complete);
  }

  /**
   * Blocks an onboarding step while mandatory KYC documents are missing.
   *
   * @param client client
   * @param step step, e.g. "confirm the client"
   */
  @Transactional(readOnly = true)
  public void requireComplete(Client client, String step) {
    List<String> missing = checklist(client).missing();
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "KYC_DOCUMENTS_MISSING",
          "Upload the mandatory KYC documents before you "
              + step
              + ": "
              + String.join(", ", missing));
    }
  }

  /**
   * List of values holding the mandatory KYC documents of a client type.
   *
   * @param type client type
   * @return list of values type code
   */
  public static String checklistType(ClientType type) {
    return type == ClientType.CORPORATE ? "KYC_DOCS_CORPORATE" : "KYC_DOCS_INDIVIDUAL";
  }

  private static AttachmentTarget target(Client client) {
    return new AttachmentTarget(ClientService.ENTITY, String.valueOf(client.getId()));
  }
}
