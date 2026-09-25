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
import com.iortatechnxt.brokerverse.docgen.domain.DocTemplate;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.productmaint.domain.ComparativeOutput;
import com.iortatechnxt.brokerverse.productmaint.domain.ComparativeOutput.Kind;
import com.iortatechnxt.brokerverse.productmaint.domain.ComparativeOutput.OutputSpec;
import com.iortatechnxt.brokerverse.productmaint.domain.ComparativeOutputRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.NegotiationRound;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.service.ComparativeTable.Selection;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stored comparative outputs of a package request (BRPM.014, PMADD03): the audit MASTER is compiled
 * from the latest round (automatically when the terms are final, or on demand); a new master
 * supersedes the previous one, so only one is current. CLIENT outputs are derived from the current
 * master with a selection of fields and insurers and never change a value. Every output is stored
 * as a PDF attachment with its SHA-256 and template version, and its generation is audited.
 */
@Service
@Transactional
public class ComparativeService {

  /** Document type of comparative outputs. */
  public static final String DOCUMENT_TYPE = "PKG_COMPARATIVE";

  private static final String MASTER_TITLE = "Audit master";
  private static final String OUTPUT = "Comparative output";

  private final PackageRequests requests;
  private final NegotiationService negotiation;
  private final PackageResponseService responses;
  private final ComparativeOutputRepository outputs;
  private final PackageDocuments documents;
  private final DocTemplateService templates;
  private final DocumentService attachments;
  private final TermsCodec codec;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests request reads
   * @param negotiation negotiation rounds
   * @param responses insurer responses
   * @param outputs stored outputs
   * @param documents PDF and Excel builder
   * @param templates document templates
   * @param attachments documents of the request
   * @param codec JSON
   * @param audit audit trail
   * @param clock clock
   */
  public ComparativeService(
      PackageRequests requests,
      NegotiationService negotiation,
      PackageResponseService responses,
      ComparativeOutputRepository outputs,
      PackageDocuments documents,
      DocTemplateService templates,
      DocumentService attachments,
      TermsCodec codec,
      AuditTrailService audit,
      Clock clock) {
    this.requests = requests;
    this.negotiation = negotiation;
    this.responses = responses;
    this.outputs = outputs;
    this.documents = documents;
    this.templates = templates;
    this.attachments = attachments;
    this.codec = codec;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Outputs of a request, newest first.
   *
   * @param id request
   * @return outputs
   */
  @Transactional(readOnly = true)
  public List<ComparativeOutput> outputs(Long id) {
    return outputs.findByRequestIdOrderByIdDesc(requests.get(id).getId());
  }

  /**
   * Compiles a new audit master from the latest round; the previous master is superseded.
   *
   * @param id request
   * @return the master
   */
  public ComparativeOutput compileMaster(Long id) {
    PackageRequest p = requests.get(id);
    NegotiationRound round = negotiation.latest(id);
    if (round.getSentAt() == null) {
      throw new BusinessRuleException(
          "COMPARATIVE_NO_TERMS",
          "Send the quotation slip of round " + round.getRoundNo() + " first");
    }
    ComparativeTable table = responses.comparative(round);
    outputs
        .findByRequestIdAndKindAndCurrentTrue(id, Kind.MASTER)
        .ifPresent(
            previous -> {
              previous.supersede();
              outputs.flush();
            });
    ComparativeOutput master =
        outputs.save(
            new ComparativeOutput(
                id,
                round.getRoundNo(),
                Kind.MASTER,
                null,
                new OutputSpec(
                    MASTER_TITLE,
                    table.fields(),
                    table.rows().stream().map(ComparativeTable.Row::insurerCode).toList(),
                    codec.write(table),
                    templateVersion())));
    store(p, master, documents.comparativePdf(p, table, Selection.ALL, MASTER_TITLE));
    audit.record(
        PackageRequests.ENTITY,
        p.getRequestNo(),
        AuditAction.CREATE,
        "Comparative master compiled from round " + round.getRoundNo());
    return master;
  }

  /**
   * Generates a client view of the current master (PMADD03).
   *
   * @param id request
   * @param title title of the view
   * @param selection fields and insurers to show
   * @return the client output
   */
  public ComparativeOutput clientView(Long id, String title, Selection selection) {
    PackageRequest p = requests.get(id);
    ComparativeOutput master =
        outputs
            .findByRequestIdAndKindAndCurrentTrue(id, Kind.MASTER)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "COMPARATIVE_NO_MASTER", "Compile the comparative master first"));
    if (title == null || title.isBlank()) {
      throw new BusinessRuleException("COMPARATIVE_TITLE_REQUIRED", "Enter the title of the view");
    }
    ComparativeTable table = codec.read(master.getContent(), ComparativeTable.class);
    ComparativeTable shown = table.select(selection);
    if (shown.rows().isEmpty()) {
      throw new BusinessRuleException(
          "COMPARATIVE_NO_INSURER", "Select at least one insurer of the master");
    }
    ComparativeOutput view =
        outputs.save(
            new ComparativeOutput(
                id,
                master.getRoundNo(),
                Kind.CLIENT,
                master.getId(),
                new OutputSpec(
                    title.strip(),
                    shown.fields(),
                    shown.rows().stream().map(ComparativeTable.Row::insurerCode).toList(),
                    master.getContent(),
                    templateVersion())));
    store(p, view, documents.comparativePdf(p, table, selectionOf(view), view.getTitle()));
    audit.record(
        PackageRequests.ENTITY,
        p.getRequestNo(),
        AuditAction.CREATE,
        "Client comparative '" + view.getTitle() + "' from master " + master.getId());
    return view;
  }

  /**
   * The file of an output.
   *
   * @param id request
   * @param outputId output
   * @param excel Excel instead of PDF
   * @return file
   */
  @Transactional(readOnly = true)
  public MessageFile file(Long id, Long outputId, boolean excel) {
    PackageRequest p = requests.get(id);
    ComparativeOutput o =
        outputs
            .findById(outputId)
            .filter(x -> x.getRequestId().equals(id))
            .orElseThrow(() -> new ResourceNotFoundException(OUTPUT, outputId));
    ComparativeTable table = codec.read(o.getContent(), ComparativeTable.class);
    return excel
        ? documents.comparativeXlsx(p, table, selectionOf(o), o.getTitle())
        : documents.comparativePdf(p, table, selectionOf(o), o.getTitle());
  }

  private static Selection selectionOf(ComparativeOutput o) {
    return new Selection(o.getFieldList(), o.getInsurerList());
  }

  private void store(PackageRequest p, ComparativeOutput o, MessageFile f) {
    Attachment a =
        attachments
            .upload(
                new AttachmentTarget(PackageRequests.ENTITY, String.valueOf(p.getId())),
                List.of(new UploadedFile(f.fileName(), f.content())),
                new UploadOptions(DOCUMENT_TYPE, false, p.getRequestNo(), o.getTitle()))
            .get(0);
    o.store(a.getId(), a.getSha256());
  }

  private String templateVersion() {
    DocTemplate t = templates.current(PackageDocuments.COMPARATIVE_TEMPLATE, LocalDate.now(clock));
    return t.getCode() + " v" + t.getVersionNo();
  }
}
