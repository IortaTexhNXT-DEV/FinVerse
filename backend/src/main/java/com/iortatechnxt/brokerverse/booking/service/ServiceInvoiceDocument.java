package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.SiKind;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Renders a service invoice or credit as a BDOI-branded PDF from its document template (BRNB.100,
 * BRNB.004 template versioning): recipient and references, the commission, VAT on commission and
 * withholding tax lines, the merged template text and the signature block.
 */
@Component
public class ServiceInvoiceDocument {

  private static final int CREDIT_DAYS = 30;
  private static final List<Integer> AMOUNT_COLUMN = List.of(1);

  private final DocTemplateService templates;
  private final DocumentComposer composer;
  private final BookingSettings settings;

  /**
   * Creates the renderer.
   *
   * @param templates document templates
   * @param composer PDF composer
   * @param settings company facts
   */
  public ServiceInvoiceDocument(
      DocTemplateService templates, DocumentComposer composer, BookingSettings settings) {
    this.templates = templates;
    this.composer = composer;
    this.settings = settings;
  }

  /**
   * Renders the document and stores it on the service invoice with the template version used.
   *
   * @param si service invoice (numbered)
   * @param templateCode template
   */
  public void render(ServiceInvoice si, String templateCode) {
    MergedText text =
        templates.merge(
            templateCode,
            si.getIssueDate(),
            Map.of("reference", si.getSiNo(), "creditDays", String.valueOf(CREDIT_DAYS)));
    boolean credit = si.getKind() == SiKind.CREDIT;
    DocumentSpec spec =
        new DocumentSpec(
            settings.company(si.getCompanyId()).getName(),
            credit ? "SERVICE INVOICE CREDIT" : "SERVICE INVOICE",
            si.getSiNo(),
            List.of(
                new Fields(
                    "Billed to",
                    List.of(
                        new Field("Recipient", si.getRecipientName()),
                        new Field("Code", si.getRecipientCode()),
                        new Field("Issue date", si.getIssueDate().toString()),
                        new Field("Booked invoice", nz(si.getInvoiceNo())),
                        new Field("Account (ARN)", nz(si.getArn())),
                        new Field("Credit of", nz(si.getCreditOf())))),
                new Table(
                    "Charges (" + si.getCurrency() + ")",
                    List.of("Description", "Amount"),
                    List.of(
                        List.of("Brokerage commission", amount(si.getCommission())),
                        List.of("VAT on commission", amount(si.getVatOnCommission())),
                        List.of("Less: withholding tax", amount(si.getWtaxAmount().negate())),
                        List.of("Net amount", amount(si.getNetAmount()))),
                    AMOUNT_COLUMN),
                new Text(text.title(), text.text())),
            List.of("Prepared by", "Approved by"),
            text.versionTag() + (si.getRemarks() == null ? "" : " - " + si.getRemarks()));
    si.attachDocument(text.code(), text.versionNo(), composer.pdf(spec));
  }

  private static String nz(String value) {
    return value == null ? "-" : value;
  }

  private static String amount(BigDecimal value) {
    return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
  }
}
