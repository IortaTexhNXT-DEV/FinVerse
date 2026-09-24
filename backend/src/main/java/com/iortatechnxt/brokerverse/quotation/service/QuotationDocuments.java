package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.account.domain.AccountPremium;
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
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationContent;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationItem;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The quotation documents sent to the client (BRNB.004/043): a branded PDF (letter from the
 * QUOTATION_LETTER template, items, premium breakdown, QUOTATION_TERMS) and an Excel schedule of
 * the items, both of the current version.
 */
@Component
public class QuotationDocuments {

  /** Media type of the PDF. */
  public static final String PDF = "application/pdf";

  /** Media type of the Excel file. */
  public static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private static final String TERMS = "QUOTATION_TERMS";
  private static final String SUM_INSURED = "Sum insured";
  private static final String PREMIUM = "Premium";
  private static final List<Integer> AMOUNT_COLUMNS = List.of(3, 4, 5);

  private final DocumentComposer composer;
  private final DocTemplateService templates;
  private final OrganizationService organization;
  private final QuotationVersions versions;
  private final Clock clock;

  /**
   * Creates the document builder.
   *
   * @param composer PDF and XLSX composer
   * @param templates document templates
   * @param organization companies (letterhead)
   * @param versions version store
   * @param clock clock
   */
  public QuotationDocuments(
      DocumentComposer composer,
      DocTemplateService templates,
      OrganizationService organization,
      QuotationVersions versions,
      Clock clock) {
    this.composer = composer;
    this.templates = templates;
    this.organization = organization;
    this.versions = versions;
    this.clock = clock;
  }

  /**
   * The PDF and Excel files of the current version.
   *
   * @param q quotation
   * @return files
   */
  public List<MessageFile> files(Quotation q) {
    return List.of(pdfFile(q), xlsxFile(q));
  }

  /**
   * The PDF of the current version.
   *
   * @param q quotation
   * @return file
   */
  public MessageFile pdfFile(Quotation q) {
    return new MessageFile(baseName(q) + ".pdf", PDF, pdf(q, versions.current(q)));
  }

  /**
   * The Excel schedule of the current version.
   *
   * @param q quotation
   * @return file
   */
  public MessageFile xlsxFile(Quotation q) {
    return new MessageFile(baseName(q) + ".xlsx", XLSX, xlsx(q, versions.current(q)));
  }

  private byte[] pdf(Quotation q, QuotationContent c) {
    LocalDate today = LocalDate.now(clock);
    MergedText letter =
        templates.merge(
            QuotationService.TEMPLATE,
            today,
            Map.of(
                "clientName", q.getClientName(),
                "reference", q.getQuotationNo(),
                "productName", q.getProductCode(),
                "validUntil", String.valueOf(c.validUntil())));
    MergedText terms = templates.merge(TERMS, today, Map.of());
    List<Section> sections = new ArrayList<>();
    sections.add(new Text(null, letter.text()));
    sections.add(new Fields("Quotation", header(q, c)));
    sections.add(
        new Table(
            "Risk items",
            List.of("#", "Group", "Risk", SUM_INSURED, "Rate %", PREMIUM),
            itemRows(c),
            AMOUNT_COLUMNS));
    sections.add(new Fields("Premium", premium(c.premium(), q.getCurrency())));
    if (c.remarks() != null) {
      sections.add(new Text("Remarks", c.remarks()));
    }
    sections.add(new Text(terms.title(), terms.text()));
    return composer.pdf(
        new DocumentSpec(
            organization.getCompany(q.getCompanyId()).getName(),
            "Insurance Quotation",
            q.getQuotationNo() + " / " + q.getArn(),
            sections,
            List.of("Prepared by", "Approved by"),
            q.getTemplateVersion()
                + " / "
                + terms.versionTag()
                + " / version "
                + q.getCurrentVersion()));
  }

  private static List<Field> header(Quotation q, QuotationContent c) {
    return List.of(
        new Field("Quotation no.", q.getQuotationNo()),
        new Field("ARN", q.getArn()),
        new Field("Version", String.valueOf(q.getCurrentVersion())),
        new Field("Client", q.getClientCode() + " - " + q.getClientName()),
        new Field("Product", q.getProductCode()),
        new Field("Insurer", c.insurerCode() == null ? "To be advised" : c.insurerCode()),
        new Field("Period", c.periodFrom() + " to " + c.periodTo()),
        new Field("Valid until", String.valueOf(c.validUntil())),
        new Field("Premium payment", c.directPayment() ? "Directly to the insurer" : "Via BDOI"));
  }

  private static List<List<String>> itemRows(QuotationContent c) {
    List<List<String>> rows = new ArrayList<>();
    int n = 1;
    for (QuotationItem i : c.items()) {
      rows.add(
          List.of(
              String.valueOf(n++),
              String.valueOf(i.riskGroup()),
              QuotationPricing.label(i.data()),
              money(QuotationPricing.sumInsured(i.data())),
              i.ratePercent() == null ? "" : i.ratePercent().stripTrailingZeros().toPlainString(),
              money(i.premium())));
    }
    return rows;
  }

  private static List<Field> premium(AccountPremium p, String currency) {
    return List.of(
        new Field("Net premium", money(p.netPremium())),
        new Field("Documentary stamp tax", money(p.dst())),
        new Field("Premium tax", money(p.premiumTax())),
        new Field("VAT", money(p.vat())),
        new Field("Fire service tax", money(p.fst())),
        new Field("Local government tax", money(p.lgt())),
        new Field("Gross premium (" + currency + ")", money(p.grossPremium())));
  }

  private byte[] xlsx(Quotation q, QuotationContent c) {
    List<List<Object>> rows = new ArrayList<>();
    for (QuotationItem i : c.items()) {
      List<Object> row = new ArrayList<>();
      row.add(q.getQuotationNo());
      row.add(q.getArn());
      row.add(i.riskGroup());
      row.add(QuotationPricing.label(i.data()));
      row.add(QuotationPricing.sumInsured(i.data()));
      row.add(i.ratePercent());
      row.add(i.premium());
      rows.add(row);
    }
    List<Object> total = new ArrayList<>();
    total.add("Gross premium");
    total.add(null);
    total.add(null);
    total.add(null);
    total.add(QuotationPricing.totalSumInsured(c));
    total.add(null);
    total.add(c.premium().grossPremium());
    rows.add(total);
    return composer.xlsx(
        new SheetSpec(
            "Quotation",
            List.of("Quotation No", "ARN", "Risk Group", "Risk", SUM_INSURED, "Rate %", PREMIUM),
            rows));
  }

  private static String baseName(Quotation q) {
    return q.getQuotationNo() + "_v" + q.getCurrentVersion();
  }

  /**
   * An amount with thousands separators and two decimals; empty when null.
   *
   * @param amount amount
   * @return text
   */
  static String money(BigDecimal amount) {
    return amount == null ? "" : String.format(Locale.ROOT, "%,.2f", amount);
  }
}
