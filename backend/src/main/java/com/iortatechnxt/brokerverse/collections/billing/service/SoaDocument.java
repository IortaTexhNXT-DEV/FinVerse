package com.iortatechnxt.brokerverse.collections.billing.service;

import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatement;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatementLine;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Renders a statement of account as a BDOI-branded PDF from the document template {@code CLX_SOA}
 * (BRCLXN.058; layout and wording to confirm, CQ18): the client and account, the billing cycle and
 * due date, one row per installment billed (the cycle's and any arrears) and the merged template
 * text. The template version used is recorded on the statement.
 */
@Component
public class SoaDocument {

  /** Template code. */
  public static final String TEMPLATE = "CLX_SOA";

  private static final List<Integer> AMOUNT_COLUMNS = List.of(4, 5, 6);

  private final DocTemplateService templates;
  private final DocumentComposer composer;
  private final OrganizationService organizations;

  /**
   * Creates the renderer.
   *
   * @param templates document templates
   * @param composer PDF composer
   * @param organizations company name
   */
  public SoaDocument(
      DocTemplateService templates, DocumentComposer composer, OrganizationService organizations) {
    this.templates = templates;
    this.composer = composer;
    this.organizations = organizations;
  }

  /**
   * Renders a statement and records the template version on it.
   *
   * @param soa statement with its lines
   * @return PDF bytes
   */
  public byte[] render(BillingStatement soa) {
    MergedText text =
        templates.merge(
            TEMPLATE,
            soa.getCycleFrom(),
            Map.of(
                "arn", soa.getArn(),
                "cycleFrom", soa.getCycleFrom().toString(),
                "cycleTo", soa.getCycleTo().toString(),
                "frequency", soa.getFrequency().toLowerCase(Locale.ROOT).replace('_', '-'),
                "currency", soa.getCurrency(),
                "balance", amount(soa.getBalance()),
                "dueDate", soa.getDueDate().toString()));
    soa.renderedWith(text.code(), text.versionNo());
    DocumentSpec spec =
        new DocumentSpec(
            organizations.getCompany(soa.getCompanyId()).getName(),
            "STATEMENT OF ACCOUNT",
            soa.getSoaNo(),
            List.of(
                new Fields(
                    "Billed to",
                    List.of(
                        new Field("Client", soa.getAssuredName()),
                        new Field("Client code", soa.getClientCode()),
                        new Field("Account (ARN)", soa.getArn()),
                        new Field("Billing cycle", soa.getCycleFrom() + " to " + soa.getCycleTo()),
                        new Field("Due date", soa.getDueDate().toString()),
                        new Field(
                            "Amount due", soa.getCurrency() + " " + amount(soa.getBalance())))),
                new Table(
                    "Installments billed (" + soa.getCurrency() + ")",
                    List.of(
                        "Invoice",
                        "Policy Year",
                        "Coverage",
                        "Due Date",
                        "Amount",
                        "Paid",
                        "Balance"),
                    rows(soa),
                    AMOUNT_COLUMNS),
                new Text(text.title(), text.text())),
            List.of("Prepared by"),
            text.versionTag() + " - billing is for monitoring only (BRCLXN.060)");
    return composer.pdf(spec);
  }

  private static List<List<String>> rows(BillingStatement soa) {
    List<List<String>> rows = new ArrayList<>();
    for (BillingStatementLine l : soa.getLines()) {
      rows.add(
          List.of(
              l.getInvoiceNo() == null ? "Scheduled" : l.getInvoiceNo(),
              l.getPolicyYear()
                  + (l.getKind() == BillingStatementLine.LineKind.ARREARS ? " (arrears)" : ""),
              l.getCoverageFrom() + " to " + l.getCoverageTo(),
              l.getDueDate().toString(),
              amount(l.getAmount()),
              amount(l.getPaid()),
              amount(l.getBalance())));
    }
    rows.add(
        List.of(
            "Total",
            "",
            "",
            "",
            amount(soa.getTotal()),
            amount(soa.getPaid()),
            amount(soa.getBalance())));
    return rows;
  }

  private static String amount(BigDecimal value) {
    return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
  }
}
