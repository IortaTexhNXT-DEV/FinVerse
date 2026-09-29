package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
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
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails;
import com.iortatechnxt.brokerverse.nonpackage.service.ComparativeTable.Row;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Documents of the non-package flow (BRNB.004/008/010/017): the quotation slip sent to the insurers
 * (template QUOTATION_SLIP), the comparative table (PDF and Excel) and the proposal slip built from
 * the chosen insurer's terms (template PROPOSAL_SLIP).
 */
@Component
public class ProposalDocuments {

  /** PDF media type. */
  public static final String PDF = "application/pdf";

  /** Excel media type. */
  public static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  /** Quotation slip template. */
  public static final String QS_TEMPLATE = "QUOTATION_SLIP";

  private static final String PS_TEMPLATE = "PROPOSAL_SLIP";
  private static final String REFERENCE = "reference";
  private static final String PREMIUM = "Premium";
  private static final String RISK = "Risk";
  private static final List<Integer> AMOUNT_COLUMNS = List.of(2, 3);
  private static final List<String> COMPARISON =
      List.of(
          "Insurer",
          "Status",
          PREMIUM,
          "Rate %",
          "Deductibles",
          "Conditions",
          "Valid until",
          "Remarks",
          "Recommended");
  // Amounts, rates, status words and the recommended flag keep to one line; free text wraps.
  private static final List<Float> COMPARISON_WIDTHS =
      List.of(1.5f, 1.3f, 1.5f, 1.1f, 1.5f, 1.9f, 1.2f, 1.6f, 2.1f);

  private final DocumentComposer composer;
  private final DocTemplateService templates;
  private final OrganizationService organization;
  private final RiskDetailsCodec codec;
  private final CatalogNames names;
  private final UserDirectory users;
  private final Clock clock;

  /**
   * Creates the builder.
   *
   * @param composer PDF and Excel composer
   * @param templates document templates
   * @param organization companies (letterhead)
   * @param codec risk details JSON
   * @param names product and line names
   * @param users user names (signatures)
   * @param clock clock
   */
  public ProposalDocuments(
      DocumentComposer composer,
      DocTemplateService templates,
      OrganizationService organization,
      RiskDetailsCodec codec,
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
   * The quotation slip (BRNB.008).
   *
   * @param p PRF with its QS number
   * @return PDF file
   */
  public MessageFile quotationSlip(ProposalRequest p) {
    MergedText text =
        templates.merge(
            QS_TEMPLATE,
            BusinessClock.today(clock),
            Map.of(REFERENCE, p.getQsNo(), "replyBy", DisplayFormat.date(p.getQsReplyBy())));
    List<Section> sections = new ArrayList<>();
    sections.add(new Text(null, text.text()));
    sections.add(new Fields(RISK, riskFields(p)));
    sections.addAll(riskSections(codec.of(p)));
    byte[] pdf =
        composer.pdf(
            new DocumentSpec(
                company(p),
                "Quotation Slip",
                p.getQsNo() + " / " + p.getArn(),
                sections,
                signatures(p.getQsSubmittedBy(), p.getQsApprovedBy()),
                text.versionLabel()));
    return new MessageFile(p.getQsNo() + ".pdf", PDF, pdf);
  }

  /**
   * The comparative table as PDF (BRNB.010).
   *
   * @param p PRF
   * @param responses insurer responses
   * @return PDF file
   */
  public MessageFile comparativePdf(ProposalRequest p, List<InsurerResponse> responses) {
    ComparativeTable table = ComparativeTable.of(responses);
    byte[] pdf =
        composer.pdf(
            new DocumentSpec(
                company(p),
                "Comparative Table",
                p.getPrfNo() + " / " + p.getArn(),
                List.of(
                    new Fields(RISK, riskFields(p)),
                    new Table(
                        "Insurer terms",
                        COMPARISON,
                        table.rows().stream().map(ProposalDocuments::cells).toList(),
                        AMOUNT_COLUMNS,
                        COMPARISON_WIDTHS)),
                List.of("Prepared by"),
                "Compiled from " + responses.size() + " insurer response(s)"));
    return new MessageFile(p.getPrfNo() + "_comparative.pdf", PDF, pdf);
  }

  /**
   * The comparative table as Excel (BRNB.010).
   *
   * @param p PRF
   * @param responses insurer responses
   * @return Excel file
   */
  public MessageFile comparativeXlsx(ProposalRequest p, List<InsurerResponse> responses) {
    List<List<Object>> rows = new ArrayList<>();
    for (Row r : ComparativeTable.of(responses).rows()) {
      List<Object> row = new ArrayList<>();
      row.add(r.insurerName());
      row.add(r.status().label());
      row.add(r.premium());
      row.add(r.rate());
      row.add(r.deductibles());
      row.add(r.conditions());
      row.add(r.validUntil());
      row.add(r.remarks());
      row.add(r.recommended() ? "Yes" : "");
      rows.add(row);
    }
    byte[] xlsx = composer.xlsx(new SheetSpec("Comparative table", COMPARISON, rows));
    return new MessageFile(p.getPrfNo() + "_comparative.xlsx", XLSX, xlsx);
  }

  /**
   * The proposal slip from the chosen insurer's terms (BRNB.017).
   *
   * @param p PRF with its PS number, version and chosen insurer
   * @param chosen terms of the chosen insurer
   * @return PDF file
   */
  public MessageFile proposalSlip(ProposalRequest p, InsurerResponse chosen) {
    MergedText text =
        templates.merge(
            PS_TEMPLATE,
            BusinessClock.today(clock),
            Map.of("clientName", p.getClientName(), REFERENCE, p.getPsNo()));
    List<Section> sections = new ArrayList<>();
    sections.add(new Text(null, text.text()));
    sections.add(new Fields(RISK, riskFields(p)));
    sections.add(
        new Fields(
            "Proposed terms",
            List.of(
                new Field("Insurer", chosen.getInsurerName()),
                new Field(PREMIUM, money(chosen.getPremium())),
                new Field("Rate %", DisplayFormat.rate(chosen.getRate())),
                new Field("Deductibles", text(chosen.getDeductibles())),
                new Field("Conditions", text(chosen.getConditions())),
                new Field("Valid until", DisplayFormat.date(chosen.getValidUntil())))));
    sections.addAll(riskSections(codec.of(p)));
    byte[] pdf =
        composer.pdf(
            new DocumentSpec(
                company(p),
                "Proposal Slip",
                p.getPsNo() + " / " + p.getArn(),
                sections,
                signatures(p.getPsSubmittedBy(), p.getPsApprovedBy()),
                text.versionLabel() + " | Proposal slip version " + p.getPsVersion()));
    return new MessageFile(p.getPsNo() + "_v" + p.getPsVersion() + ".pdf", PDF, pdf);
  }

  /**
   * Legal name of the company of a PRF (letterhead, signature of the insurer e-mails).
   *
   * @param p PRF
   * @return legal name from the company master
   */
  String company(ProposalRequest p) {
    return organization.getCompany(p.getCompanyId()).getName();
  }

  /** "Prepared by: <name>" and "Approved by: <name>" of the TSU preparer and approver. */
  private List<String> signatures(String preparer, String approver) {
    return List.of(
        DocumentSpec.signature("Prepared by", users.displayName(preparer)),
        DocumentSpec.signature("Approved by", users.displayName(approver)));
  }

  private List<Field> riskFields(ProposalRequest p) {
    return List.of(
        new Field("PRF", p.getPrfNo()),
        new Field("ARN", p.getArn()),
        new Field("Client", p.getClientName()),
        new Field(
            "Product",
            names.product(p.getProductCode()) + " (" + names.line(p.getLineCode()) + ")"),
        new Field("Period", DisplayFormat.period(p.getPeriodFrom(), p.getPeriodTo())),
        new Field("Total sum insured", p.getCurrency() + " " + money(p.getTotalSumInsured())));
  }

  private static List<Section> riskSections(RiskDetails details) {
    List<Section> sections = new ArrayList<>();
    details.sections().forEach(s -> sections.add(new Text(s.heading(), text(s.text()))));
    if (!details.items().isEmpty()) {
      sections.add(
          new Table(
              "Risk items",
              List.of("Group", RISK, "Sum insured"),
              details.items().stream()
                  .map(
                      i ->
                          List.of(
                              String.valueOf(i.riskGroup()),
                              label(i.data()),
                              money(i.data().sumInsured())))
                  .toList(),
              List.of(2)));
    }
    return sections;
  }

  private static List<String> cells(Row r) {
    return List.of(
        r.insurerName(),
        r.status().label(),
        money(r.premium()) + (r.lowest() ? " (lowest)" : ""),
        DisplayFormat.rate(r.rate()),
        text(r.deductibles()),
        text(r.conditions()),
        DisplayFormat.date(r.validUntil()),
        text(r.remarks()),
        r.recommended() ? "Recommended" : "");
  }

  /**
   * Label of a risk item.
   *
   * @param d item
   * @return plate, address, person or description
   */
  static String label(RiskItemData d) {
    return Stream.of(
            d.vehicle() == null ? null : d.vehicle().plateNo(),
            d.location() == null ? null : d.location().address(),
            d.person() == null ? null : d.person().name(),
            d.description())
        .filter(s -> s != null && !s.isBlank())
        .findFirst()
        .orElse("Risk item");
  }

  private static String money(BigDecimal amount) {
    return DisplayFormat.amount(amount);
  }

  private static String text(Object value) {
    return value == null ? "" : value.toString();
  }
}
