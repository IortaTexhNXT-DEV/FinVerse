package com.iortatechnxt.brokerverse.eb.document.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbDocument;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Documents tab of a programme (FR-EB-002, FR-EB-003, FR-EB-030): the registered EB documents
 * the current user may see (department access classes of the document types, BRID-025), latest
 * first, with their cycle, type, process, version, source and status.
 */
@Service
@Transactional(readOnly = true)
public class EbDocumentQuery {

  private final EbDocumentRepository register;
  private final EbCycleRepository cycles;
  private final EbRecords records;
  private final DocumentService documents;
  private final LovService lovs;

  /**
   * Creates the query.
   *
   * @param register EB document register
   * @param cycles cycles
   * @param records programme look-up
   * @param documents attachment documents (access classes)
   * @param lovs labels
   */
  public EbDocumentQuery(
      EbDocumentRepository register,
      EbCycleRepository cycles,
      EbRecords records,
      DocumentService documents,
      LovService lovs) {
    this.register = register;
    this.cycles = cycles;
    this.records = records;
    this.documents = documents;
    this.lovs = lovs;
  }

  /**
   * The documents of a programme the current user may see.
   *
   * @param companyId company
   * @param programmeId programme
   * @return documents, latest first
   */
  public List<DocumentView> list(Long companyId, Long programmeId) {
    EbProgramme programme = records.programme(companyId, programmeId);
    Map<Long, Attachment> visible =
        documents
            .list(new AttachmentTarget(EbCodes.ENTITY_PROGRAMME, programme.getId().toString()))
            .stream()
            .collect(Collectors.toMap(Attachment::getId, Function.identity(), (a, b) -> a));
    Map<Long, String> cycleNos =
        cycles.findByProgrammeIdOrderByPolicyYearDescIdDesc(programme.getId()).stream()
            .collect(Collectors.toMap(EbCycle::getId, EbCycle::getCycleNo));
    return register.findByProgrammeIdOrderByIdAsc(programme.getId()).stream()
        .filter(d -> visible.containsKey(d.getAttachmentId()))
        .sorted(Comparator.comparing(EbDocument::getId).reversed())
        .map(d -> view(d, visible.get(d.getAttachmentId()), cycleNos.get(d.getCycleId())))
        .toList();
  }

  private DocumentView view(EbDocument d, Attachment a, String cycleNo) {
    return new DocumentView(
        d.getId(),
        d.getCycleId(),
        cycleNo,
        d.getDocumentType(),
        lovs.label("DOCUMENT_TYPE", d.getDocumentType()),
        d.getProcessType(),
        lovs.label(EbDocumentTypes.PROCESS_TYPE_LOV, d.getProcessType()),
        d.getVersionNo(),
        d.getStatus().name(),
        d.getSource().name(),
        d.getAttachmentId(),
        a.getFileName(),
        a.getSizeBytes(),
        d.getCreatedBy(),
        d.getCreatedAt());
  }

  /**
   * A registered document as listed.
   *
   * @param id register entry
   * @param cycleId cycle
   * @param cycleNo cycle number
   * @param documentType document type
   * @param documentTypeLabel its label
   * @param processType process tag
   * @param processLabel its label
   * @param versionNo version
   * @param status ACTIVE, SUPERSEDED or REJECTED
   * @param source AO, PROCESSING, CLIENT, INSURER or SYSTEM
   * @param attachmentId stored file (download through the attachment API)
   * @param fileName file name
   * @param sizeBytes size
   * @param uploadedBy user
   * @param uploadedAt time
   */
  public record DocumentView(
      Long id,
      Long cycleId,
      String cycleNo,
      String documentType,
      String documentTypeLabel,
      String processType,
      String processLabel,
      int versionNo,
      String status,
      String source,
      Long attachmentId,
      String fileName,
      long sizeBytes,
      String uploadedBy,
      Instant uploadedAt) {}
}
