package com.iortatechnxt.brokerverse.screening.str.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.organization.service.OrganizationDirectory;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseCodes;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseDocumentService;
import com.iortatechnxt.brokerverse.screening.config.service.ReviewTemplate;
import com.iortatechnxt.brokerverse.screening.str.domain.StrTransaction.Line;
import com.iortatechnxt.brokerverse.screening.str.domain.SuspiciousTransactionReport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The STR as a business document in PDF and Word (SNSRP-705; FRS section 6.2 "STR (on screen and
 * PDF)"): STR number, subject party, template fields, transactions, reason codes, attachments list,
 * committee decision, prepared by and template version, composed with the BDO header and footer.
 */
@Service
@Transactional
public class StrDocumentService {

  /** The index of the amount column of the transactions table (right-aligned). */
  private static final int AMOUNT_COLUMN = 4;

  private final StrService strs;
  private final ScreeningCaseRepository cases;
  private final CaseDocumentService documents;
  private final OrganizationDirectory organizations;
  private final DocumentComposer composer;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param strs STRs
   * @param cases cases
   * @param documents case documents
   * @param organizations company names
   * @param composer document composer
   * @param audit audit trail
   */
  public StrDocumentService(
      StrService strs,
      ScreeningCaseRepository cases,
      CaseDocumentService documents,
      OrganizationDirectory organizations,
      DocumentComposer composer,
      AuditTrailService audit) {
    this.strs = strs;
    this.cases = cases;
    this.documents = documents;
    this.organizations = organizations;
    this.composer = composer;
    this.audit = audit;
  }

  /**
   * Renders an STR.
   *
   * @param strId the STR
   * @param format PDF or DOCX
   * @return the file name and bytes
   */
  public Rendered render(Long strId, DocumentFormat format) {
    SuspiciousTransactionReport str = strs.get(strId);
    ScreeningCase c = cases.findById(str.getCaseId()).orElseThrow();
    byte[] bytes = composer.render(spec(str, c), format);
    audit.record(
        CaseCodes.ENTITY,
        c.getCaseNo(),
        AuditAction.EXPORT,
        "STR " + str.getStrNo() + " " + format);
    return new Rendered(str.getStrNo() + "." + format.extension(), format.contentType(), bytes);
  }

  private DocumentSpec spec(SuspiciousTransactionReport str, ScreeningCase c) {
    List<DocumentSpec.Section> sections = new ArrayList<>();
    sections.add(
        new DocumentSpec.Fields(
            "Report",
            List.of(
                field("STR No.", str.getStrNo()),
                field("Case No.", c.getCaseNo()),
                field("Status", str.getStatus().name()),
                field("Template version", Objects.toString(str.getTemplateVersionId(), "-")),
                field("Reason codes", String.join(", ", str.reasons())))));
    sections.add(new DocumentSpec.Text("Subject party", str.getSubjectSnapshot()));
    Map<String, String> values = strs.values(str.getId());
    List<DocumentSpec.Field> templateFields = new ArrayList<>();
    strs.template(str).map(ReviewTemplate::fields).orElse(List.of()).stream()
        .filter(f -> values.containsKey(f.code()))
        .forEach(f -> templateFields.add(field(f.label(), values.get(f.code()))));
    if (!templateFields.isEmpty()) {
      sections.add(new DocumentSpec.Fields("Details", templateFields));
    }
    sections.add(
        new DocumentSpec.Table(
            "Transactions",
            List.of("Reference", "Date", "Type", "Currency", "Amount", "Description"),
            strs.transactions(str.getId()).stream().map(StrDocumentService::row).toList(),
            List.of(AMOUNT_COLUMN)));
    sections.add(
        new DocumentSpec.Table(
            "Attachments",
            List.of("Document", "Type", "Received"),
            documents.of(c.getId()).stream()
                .map(
                    d ->
                        List.of(
                            d.getNominatedName(),
                            d.getDocumentType(),
                            d.getDateReceived().toString()))
                .toList(),
            List.of()));
    sections.add(
        new DocumentSpec.Fields(
            "Decision",
            List.of(
                field("Committee decision", Objects.toString(c.getCommitteeDecision(), "-")),
                field("Decided at", Objects.toString(c.getCommitteeDecidedAt(), "-")),
                field("Prepared by", str.getCreatedBy()),
                field("Marked ready by", Objects.toString(str.getReadyBy(), "-")),
                field("AMLC reference", Objects.toString(str.getAmlcReference(), "-")))));
    return new DocumentSpec(
        organizations.company(str.getCompanyId()).name(),
        "Suspicious Transaction Report",
        str.getStrNo(),
        sections,
        List.of("Prepared by", "Compliance Officer"),
        "Confidential - AMLA (RA 9160): not to be disclosed to the subject");
  }

  private static DocumentSpec.Field field(String label, String value) {
    return new DocumentSpec.Field(label, value == null ? "-" : value);
  }

  private static List<String> row(Line l) {
    return List.of(
        l.reference(),
        l.date().toString(),
        l.type(),
        l.currency(),
        l.amount().toPlainString(),
        l.description() == null ? "" : l.description());
  }

  /**
   * A rendered STR.
   *
   * @param fileName the file name
   * @param contentType the media type
   * @param content the bytes
   */
  public record Rendered(String fileName, String contentType, byte[] content) {

    /** Defensive copy. */
    public Rendered {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Rendered r && fileName.equals(r.fileName);
    }

    @Override
    public int hashCode() {
      return fileName.hashCode();
    }

    @Override
    public String toString() {
      return fileName;
    }
  }
}
