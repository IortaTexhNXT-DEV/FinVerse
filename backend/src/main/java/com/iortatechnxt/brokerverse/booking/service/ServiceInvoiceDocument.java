package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.SiKind;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Renders a service invoice or credit as a BDOI-branded PDF from its document template (BRNB.100,
 * BRNB.004 template versioning): recipient and references, the commission, VAT on commission and
 * withholding tax lines, the merged template text and the signature block. The PDF is kept in the
 * file store (owner type {@value #OWNER_TYPE}, the service invoice number; record class {@code
 * STATEMENT_OF_ACCOUNT}; build step ST1).
 */
@Component
public class ServiceInvoiceDocument {

  /** Owner entity type of the stored PDFs (key: the service invoice number). */
  public static final String OWNER_TYPE = "ServiceInvoice";

  /** Record class of service invoices. */
  public static final String RECORD_CLASS = "STATEMENT_OF_ACCOUNT";

  private static final String PDF = "application/pdf";
  private static final int CREDIT_DAYS = 30;
  private static final List<Integer> AMOUNT_COLUMN = List.of(1);

  private final DocTemplateService templates;
  private final DocumentComposer composer;
  private final BookingSettings settings;
  private final StoredFileService storedFiles;

  /**
   * Creates the renderer.
   *
   * @param templates document templates
   * @param composer PDF composer
   * @param settings company facts
   * @param storedFiles file store
   */
  public ServiceInvoiceDocument(
      DocTemplateService templates,
      DocumentComposer composer,
      BookingSettings settings,
      StoredFileService storedFiles) {
    this.templates = templates;
    this.composer = composer;
    this.settings = settings;
    this.storedFiles = storedFiles;
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
                        new Field("Issue date", DisplayFormat.date(si.getIssueDate())),
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
            text.versionLabel() + (si.getRemarks() == null ? "" : " - " + si.getRemarks()));
    byte[] pdf = composer.pdf(spec);
    Long stored =
        storedFiles
            .storeChecked(
                new StoreRequest(
                    new FileOwner(si.getCompanyId(), OWNER_TYPE, si.getSiNo()),
                    si.getKind().name(),
                    RECORD_CLASS,
                    si.getSiNo() + ".pdf",
                    pdf,
                    null),
                PDF,
                FileOrigin.GENERATED)
            .getId();
    si.attachDocument(text.code(), text.versionNo(), stored);
  }

  /**
   * The PDF of a service invoice (e-mail attachment): read from the file store with its SHA-256
   * checked, or from the row for a service invoice issued before ST1.
   *
   * @param si service invoice
   * @return bytes
   */
  public byte[] content(ServiceInvoice si) {
    return si.getStoredFileId() == null ? si.getDocument() : storedFiles.read(si.getStoredFileId());
  }

  /**
   * The PDF of a service invoice for the download endpoint: a presigned link to the stored file, or
   * the bytes of a service invoice issued before ST1.
   *
   * @param si service invoice
   * @return download
   */
  public static FileDownload download(ServiceInvoice si) {
    return si.getStoredFileId() == null
        ? FileDownload.inline(si.getSiNo() + ".pdf", PDF, si.getDocument())
        : FileDownload.stored(si.getStoredFileId());
  }

  private static String nz(String value) {
    return value == null ? "-" : value;
  }

  private static String amount(BigDecimal value) {
    return DisplayFormat.amount(value);
  }
}
