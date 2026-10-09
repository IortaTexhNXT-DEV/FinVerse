package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.catalog.service.CatalogNames;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Section;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.productmaint.domain.NegotiationRound;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.CoverageTerm;
import com.iortatechnxt.brokerverse.productmaint.domain.Signoff;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Documents of the package request process (BRPM.005/012/014/015/016), built from the versioned
 * templates: the request form, the quotation slip of a round, the comparative outputs (PDF and
 * Excel, master or client selection), the package slip for signature and the ManCom sign-off
 * record.
 */
@Component
public class PackageDocuments {

  /** PDF media type. */
  public static final String PDF = "application/pdf";

  /** Excel media type. */
  public static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  /** Request form template. */
  public static final String FORM_TEMPLATE = "PKG_REQUEST_FORM";

  /** Quotation slip template. */
  public static final String QS_TEMPLATE = "PKG_QUOTATION_SLIP";

  /** Comparative template. */
  public static final String COMPARATIVE_TEMPLATE = "PKG_COMPARATIVE";

  /** Package slip template. */
  public static final String SLIP_TEMPLATE = "PKG_SLIP";

  private static final String REFERENCE = "reference";
  private static final String PACKAGE = "Package";
  private static final String PDF_EXTENSION = ".pdf";
  private static final String TITLE = "title";
  private static final String PREPARED_BY = "Prepared by";
  private static final List<Integer> AMOUNT_COLUMNS = List.of(2, 3);

  private final DocumentComposer composer;
  private final DocTemplateService templates;
  private final OrganizationService organization;
  private final TermsCodec codec;
  private final CatalogNames names;
  private final UserDirectory users;
  private final Clock clock;

  /**
   * Creates the builder.
   *
   * @param composer PDF and Excel composer
   * @param templates document templates
   * @param organization companies (letterhead)
   * @param codec terms JSON
   * @param names line, cover type, product and insurer names
   * @param users user names
   * @param clock clock
   */
  public PackageDocuments(
      DocumentComposer composer,
      DocTemplateService templates,
      OrganizationService organization,
      TermsCodec codec,
      CatalogNames names,
      UserDirectory users,
      Clock clock) {
    this.composer = composer;
    this.templates = templates;
    this.organization = organization;
    this.codec = codec;
    this.names = names;
    this.users = users;
    this.clock = clock;
  }

  /**
   * The Package Request Form (BRPM.005/008).
   *
   * @param p request
   * @return PDF file
   */
  public MessageFile requestForm(PackageRequest p) {
    MergedText text =
        templates.merge(
            FORM_TEMPLATE,
            BusinessClock.today(clock),
            Map.of(
                REFERENCE,
                p.getRequestNo(),
                "requestType",
                p.getRequestType().label(),
                TITLE,
                p.getTitle()));
    List<Section> sections = new ArrayList<>();
    sections.add(new Text(null, text.text()));
    sections.add(new Fields("Request", requestFields(p)));
    sections.addAll(termsSections(p, codec.terms(p.getRequestedTerms()), "Requested"));
    return pdf(p, "Package Request Form", p.getRequestNo(), sections, text.versionLabel());
  }

  /**
   * The quotation slip of a round (BRPM.012).
   *
   * @param p request
   * @param r round with its QS number
   * @return PDF file
   */
  public MessageFile quotationSlip(PackageRequest p, NegotiationRound r) {
    MergedText text =
        templates.merge(
            QS_TEMPLATE,
            BusinessClock.today(clock),
            Map.of(
                REFERENCE,
                r.getQsNo(),
                "roundNo",
                String.valueOf(r.getRoundNo()),
                "replyBy",
                DisplayFormat.date(r.getReplyDue())));
    List<Section> sections = new ArrayList<>();
    sections.add(new Text(null, text.text()));
    sections.add(new Fields(PACKAGE, requestFields(p)));
    if (r.getQsNotes() != null && !r.getQsNotes().isBlank()) {
      sections.add(new Text("Round " + r.getRoundNo() + " notes", r.getQsNotes()));
    }
    sections.addAll(termsSections(p, codec.terms(p.getRequestedTerms()), "Requested"));
    MessageFile f = pdf(p, "Package Quotation Slip", r.getQsNo(), sections, text.versionLabel());
    return new MessageFile(r.getQsNo() + PDF_EXTENSION, PDF, f.content());
  }

  /**
   * A comparative output as PDF (BRPM.014, PMADD03).
   *
   * @param p request
   * @param table compiled table
   * @param selection fields and insurers shown
   * @param variant "Audit master" or the client view title
   * @return PDF file
   */
  public MessageFile comparativePdf(
      PackageRequest p,
      ComparativeTable table,
      ComparativeTable.Selection selection,
      String variant) {
    MergedText text =
        templates.merge(
            COMPARATIVE_TEMPLATE,
            BusinessClock.today(clock),
            Map.of(
                REFERENCE,
                p.getRequestNo(),
                "roundNo",
                String.valueOf(table.roundNo()),
                "variant",
                "View: " + variant + "."));
    ComparativeTable shown = table.select(selection);
    List<String> headers = shown.headers();
    List<Section> sections =
        List.of(
            new Text(null, text.text()),
            new Fields(PACKAGE, requestFields(p)),
            new Table("Insurer terms", headers, shown.cells(), List.of()));
    MessageFile f = pdf(p, "Comparative Table", p.getRequestNo(), sections, text.versionLabel());
    return new MessageFile(fileName(p, variant, PDF_EXTENSION), PDF, f.content());
  }

  /**
   * A comparative output as Excel (BRPM.014).
   *
   * @param p request
   * @param table compiled table
   * @param selection fields and insurers shown
   * @param variant variant name
   * @return Excel file
   */
  public MessageFile comparativeXlsx(
      PackageRequest p,
      ComparativeTable table,
      ComparativeTable.Selection selection,
      String variant) {
    ComparativeTable shown = table.select(selection);
    List<List<Object>> rows = new ArrayList<>();
    shown.cells().forEach(r -> rows.add(new ArrayList<>(r)));
    byte[] xlsx = composer.xlsx(new SheetSpec("Comparative table", shown.headers(), rows));
    return new MessageFile(fileName(p, variant, ".xlsx"), XLSX, xlsx);
  }

  /**
   * The package slip with the proposed terms, printed for signature (BRPM.015).
   *
   * @param p request
   * @return PDF file
   */
  public MessageFile packageSlip(PackageRequest p) {
    MergedText text =
        templates.merge(
            SLIP_TEMPLATE,
            BusinessClock.today(clock),
            Map.of(REFERENCE, p.getRequestNo(), TITLE, p.getTitle()));
    List<Section> sections = new ArrayList<>();
    sections.add(new Text(null, text.text()));
    sections.add(new Fields(PACKAGE, requestFields(p)));
    PackageTerms terms =
        codec.terms(p.getProposedTerms() == null ? p.getRequestedTerms() : p.getProposedTerms());
    sections.addAll(termsSections(p, terms, "Proposed"));
    MessageFile f = pdf(p, "Package Slip", p.getRequestNo(), sections, text.versionLabel());
    return new MessageFile(p.getRequestNo() + "_package_slip.pdf", PDF, f.content());
  }

  /**
   * The in-system ManCom sign-off record (BRPM.015; PQ07: used when no signed sheet is uploaded).
   *
   * @param p request
   * @param s sign-off
   * @return PDF file
   */
  public MessageFile signoffRecord(PackageRequest p, Signoff s) {
    List<Section> sections =
        List.of(
            new Fields(PACKAGE, requestFields(p)),
            new Fields(
                "ManCom sign-off",
                List.of(
                    new Field("Reference", s.getReference()),
                    new Field(
                        "Decision", "SIGNED".equals(s.getDecision()) ? "Signed off" : "Returned"),
                    new Field("Signed by", users.displayName(s.getSignedBy())),
                    new Field("Signed at", DisplayFormat.dateTime(s.getSignedAt())),
                    new Field("Comment", text(s.getComment())))));
    MessageFile f =
        pdf(
            p,
            "ManCom Sign-off",
            s.getReference(),
            sections,
            "Signed in BIBS by " + users.displayName(s.getSignedBy()));
    return new MessageFile(s.getReference() + PDF_EXTENSION, PDF, f.content());
  }

  /**
   * A PDF with the company letterhead.
   *
   * @param p request (company)
   * @param title title
   * @param reference reference
   * @param sections sections
   * @param footer footer
   * @return file
   */
  MessageFile pdf(
      PackageRequest p, String title, String reference, List<Section> sections, String footer) {
    byte[] pdf =
        composer.pdf(
            new DocumentSpec(
                organization.getCompany(p.getCompanyId()).getName(),
                title,
                reference,
                sections,
                List.of(PREPARED_BY, "Approved by"),
                footer));
    return new MessageFile(reference + PDF_EXTENSION, PDF, pdf);
  }

  private static String fileName(PackageRequest p, String variant, String extension) {
    String slug = variant.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    return p.getRequestNo() + "_comparative_" + slug + extension;
  }

  /**
   * Header fields of a request.
   *
   * @param p request
   * @return fields
   */
  List<Field> requestFields(PackageRequest p) {
    String coverType = names.coverType(p.getLineCode(), p.getCoverTypeCode());
    return List.of(
        new Field("Request", p.getRequestNo() + " (" + p.getRequestType().label() + ")"),
        new Field("Package / programme", p.getTitle()),
        new Field("Scope", p.getScope().label()),
        new Field("Client", text(p.getClientName())),
        new Field(
            "Line / cover type",
            names.line(p.getLineCode()) + (coverType.isEmpty() ? "" : " / " + coverType)),
        new Field("Product", names.product(p.getTargetProductCode())));
  }

  /**
   * Sections of a set of terms: text sections, coverages, rate scheme and dates, insurers.
   *
   * @param p request (company and line of the names)
   * @param t terms
   * @param label "Requested" or "Proposed"
   * @return sections
   */
  List<Section> termsSections(PackageRequest p, PackageTerms t, String label) {
    Long companyId = p.getCompanyId();
    List<Section> sections = new ArrayList<>();
    t.sections().forEach(s -> sections.add(new Text(s.heading(), s.text())));
    if (!t.coverages().isEmpty()) {
      sections.add(
          new Table(
              label + " coverages",
              List.of("Coverage", "Included", "Limit", "Sub-limit", "Deductible"),
              t.coverages().stream().map(c -> coverageRow(p.getLineCode(), c)).toList(),
              AMOUNT_COLUMNS));
    }
    PackageTerms.Scheme s = t.scheme();
    PackageTerms.Dates d = t.dates();
    sections.add(
        new Fields(
            label + " rate scheme and dates",
            List.of(
                new Field("Rate %", text(s.defaultRate())),
                new Field("Minimum premium", money(s.minimumPremium())),
                new Field("Commission %", text(s.commissionRate())),
                new Field("TSI limit", money(s.maxSumInsured())),
                new Field("Computation basis", text(s.ratingBasisNote())),
                new Field("Effective from", text(d.effectiveFrom())),
                new Field(
                    "Package term", DisplayFormat.period(d.packageStartDate(), d.packageEndDate())),
                new Field("Anniversary", text(d.anniversaryDate())))));
    if (!t.insurers().isEmpty()) {
      sections.add(
          new Table(
              label + " insurers",
              List.of("Insurer", "Role", "Share %", "Rate %", "Minimum premium"),
              t.insurers().stream()
                  .map(
                      i ->
                          List.of(
                              names.insurer(companyId, i.insurerCode()),
                              roleLabel(i.role()),
                              text(i.sharePercent()),
                              text(i.rate()),
                              money(i.minimumPremium())))
                  .toList(),
              List.of()));
    }
    return sections;
  }

  private List<String> coverageRow(String lineCode, CoverageTerm c) {
    return List.of(
        names.coverage(lineCode, c.coverageCode()),
        c.included() ? "Yes" : "No",
        money(c.limitAmount()),
        money(c.subLimit()),
        ComparativeTable.deductible(c));
  }

  /**
   * An amount with thousands separators.
   *
   * @param amount amount
   * @return text, empty for null
   */
  static String money(BigDecimal amount) {
    return DisplayFormat.amount(amount);
  }

  /**
   * The role of an insurer in a package as the FRS lists it (FR-PM-014 R1): Lead, Participant or
   * Panel.
   *
   * @param role role code, null for Panel
   * @return label
   */
  static String roleLabel(Object role) {
    if (role == null) {
      return "Panel";
    }
    return switch (role.toString()) {
      case "LEAD" -> "Lead";
      case "PARTICIPANT" -> "Participant";
      default -> "Panel";
    };
  }

  /**
   * A value as text.
   *
   * @param value value
   * @return text, empty for null
   */
  static String text(Object value) {
    return DisplayFormat.value(value);
  }
}
