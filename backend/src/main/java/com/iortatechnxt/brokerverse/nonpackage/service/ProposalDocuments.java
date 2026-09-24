package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
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
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

  private final DocumentComposer composer;
  private final DocTemplateService templates;
  private final OrganizationService organization;
  private final RiskDetailsCodec codec;
  private final Clock clock;

  /**
   * Creates the builder.
   *
   * @param composer PDF and Excel composer
   * @param templates document templates
   * @param organization companies (letterhead)
   * @param codec risk details JSON
   * @param clock clock
   */
  public ProposalDocuments(
      DocumentComposer composer,
      DocTemplateService templates,
      OrganizationService organization,
      RiskDetailsCodec codec,
      Clock clock) {
    this.composer = composer;
    this.templates = templates;
    this.organization = organization;
    this.codec = codec;
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
            LocalDate.now(clock),
            Map.of(REFERENCE, p.getQsNo(), "replyBy", String.valueOf(p.getQsReplyBy())));
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
                List.of("Prepared by", "Approved by"),
                text.versionTag()));
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
                        AMOUNT_COLUMNS)),
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
      row.add(r.status().name());
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
            LocalDate.now(clock),
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
                new Field("Rate %", text(chosen.getRate())),
                new Field("Deductibles", text(chosen.getDeductibles())),
                new Field("Conditions", text(chosen.getConditions())),
                new Field("Valid until", text(chosen.getValidUntil())))));
    sections.addAll(riskSections(codec.of(p)));
    byte[] pdf =
        composer.pdf(
            new DocumentSpec(
                company(p),
                "Proposal Slip",
                p.getPsNo() + " / " + p.getArn(),
                sections,
                List.of("Prepared by", "Approved by"),
                text.versionTag() + " / version " + p.getPsVersion()));
    return new MessageFile(p.getPsNo() + "_v" + p.getPsVersion() + ".pdf", PDF, pdf);
  }

  private String company(ProposalRequest p) {
    return organization.getCompany(p.getCompanyId()).getName();
  }

  private static List<Field> riskFields(ProposalRequest p) {
    return List.of(
        new Field("PRF", p.getPrfNo()),
        new Field("ARN", p.getArn()),
        new Field("Client", p.getClientName()),
        new Field("Product", p.getProductCode() + " (" + p.getLineCode() + ")"),
        new Field("Period", text(p.getPeriodFrom()) + " to " + text(p.getPeriodTo())),
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
        r.status().name(),
        money(r.premium()),
        text(r.rate()),
        text(r.deductibles()),
        text(r.conditions()),
        text(r.validUntil()),
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
    return amount == null ? "" : String.format(Locale.ROOT, "%,.2f", amount);
  }

  private static String text(Object value) {
    return value == null ? "" : value.toString();
  }
}
