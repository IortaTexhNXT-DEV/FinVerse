package com.iortatechnxt.brokerverse.submitted.review.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Section;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.organization.service.OrganizationDirectory;
import com.iortatechnxt.brokerverse.submitted.domain.SbmSignature;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renders the PDF of an IAAF or a TOR from its template (drafts until BDOI supplies the layouts, SP
 * SQ07-SQ08) with the stamped signature of every approval level, and keeps it as an attachment of
 * the masterlist record with its document type.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class SignedPdfs {

  private static final int HASH_SHOWN = 16;

  private final DocTemplateService templates;
  private final DocumentComposer composer;
  private final AttachmentService attachments;
  private final OrganizationDirectory organizations;
  private final Clock clock;

  /**
   * Creates the renderer.
   *
   * @param templates document templates
   * @param composer document composer
   * @param attachments attachments
   * @param organizations company names
   * @param clock clock
   */
  public SignedPdfs(
      DocTemplateService templates,
      DocumentComposer composer,
      AttachmentService attachments,
      OrganizationDirectory organizations,
      Clock clock) {
    this.templates = templates;
    this.composer = composer;
    this.attachments = attachments;
    this.organizations = organizations;
    this.clock = clock;
  }

  /**
   * Renders and attaches a signed document.
   *
   * @param request template, values, signatures and where to attach it
   * @return the attachment and the template version used
   */
  public Rendered render(Request request) {
    MergedText text =
        templates.merge(request.templateCode(), BusinessClock.today(clock), request.values());
    List<Section> sections = new ArrayList<>();
    sections.add(new Text(null, text.text()));
    sections.add(
        new Table(
            "Approvals",
            List.of("Level", "Approver", "Position", "Signed", "Signature"),
            request.signatures().stream()
                .map(
                    s ->
                        List.of(
                            String.valueOf(s.getLevel()),
                            s.getSignerName(),
                            s.getPosition(),
                            DisplayFormat.dateTime(s.getSignedAt()),
                            s.getHash().substring(0, HASH_SHOWN)))
                .toList(),
            List.of()));
    byte[] pdf =
        composer.pdf(
            new DocumentSpec(
                organizations.company(request.companyId()).name(),
                text.title(),
                request.reference(),
                sections,
                request.signatures().stream()
                    .map(s -> DocumentSpec.signature(s.getPosition(), s.getSignerName()))
                    .toList(),
                text.versionTag()));
    Attachment a =
        attachments.upload(
            new AttachmentTarget(SubmittedCodes.ENTITY, request.policyId().toString()),
            request.reference() + ".pdf",
            pdf,
            text.title(),
            request.documentType());
    return new Rendered(a.getId(), text.versionNo(), pdf);
  }

  /**
   * The bytes of a signed PDF kept as an attachment (e-mail to the bank counterpart).
   *
   * @param attachmentId attachment
   * @return PDF bytes
   */
  public byte[] read(Long attachmentId) {
    return attachments.download(attachmentId).content();
  }

  /**
   * What to render.
   *
   * @param companyId company
   * @param policyId masterlist record the PDF is attached to
   * @param templateCode template (SBM_IAAF, SBM_TOR)
   * @param documentType attachment type (SBM_IAAF, SBM_TOR)
   * @param reference document number
   * @param values template values
   * @param signatures signatures of the levels
   */
  public record Request(
      Long companyId,
      Long policyId,
      String templateCode,
      String documentType,
      String reference,
      Map<String, Object> values,
      List<SbmSignature> signatures) {

    /** Defensive copies. */
    public Request {
      values = Map.copyOf(values);
      signatures = List.copyOf(signatures);
    }
  }

  /**
   * The rendered document.
   *
   * @param attachmentId attachment
   * @param versionNo template version
   * @param pdf PDF bytes (for the e-mail)
   */
  public record Rendered(Long attachmentId, int versionNo, byte[] pdf) {

    /** Defensive copy. */
    public Rendered {
      pdf = pdf == null ? new byte[0] : pdf.clone();
    }

    /**
     * The PDF.
     *
     * @return a copy
     */
    @Override
    public byte[] pdf() {
      return pdf.clone();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Rendered r
          && java.util.Objects.equals(attachmentId, r.attachmentId)
          && versionNo == r.versionNo
          && java.util.Arrays.equals(pdf, r.pdf);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(attachmentId, versionNo, java.util.Arrays.hashCode(pdf));
    }

    @Override
    public String toString() {
      return "Rendered[" + attachmentId + "]";
    }
  }
}
