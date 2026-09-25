package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.remittance.domain.BatchDocument;
import com.iortatechnxt.brokerverse.remittance.domain.BatchDocument.StoredFile;
import com.iortatechnxt.brokerverse.remittance.domain.BatchDocumentRepository;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceAmounts;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.DocumentKind;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RemittanceType;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Remittance documents (RMTID.011, Annex III): the remittance schedule as PDF and Excel (layout per
 * type, With Incentives with the incentive columns; the Mall Assurance columns of the Normal layout
 * are parked with the layouts, OQ42) and the payment request to Disbursement. Documents are
 * rendered live before submission and stored on the batch at submission with the template version.
 */
@Component
public class BatchDocuments {

  /** PDF content type. */
  public static final String PDF = "application/pdf";

  /** Excel content type. */
  public static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private static final String SCHEDULE = "REMITTANCE_SCHEDULE";
  private static final String PAYMENT_REQUEST = "REMITTANCE_PAYMENT_REQUEST";
  private static final String NONE = "-";
  private static final List<String> COMMON_HEADERS =
      List.of(
          "Invoice Number",
          "Policy Number",
          "Endorsement Number",
          "Name of Assured",
          "Risk Code",
          "Date Last Paid",
          "Date Inception",
          "Date Booked",
          "Date Expiry",
          "Paid AR",
          "Realized Commission",
          "Realized VAT",
          "WTAX",
          "DTIP",
          "Net Due");
  private static final List<String> INCENTIVE_HEADERS =
      List.of("Basic Premium", "Incentive", "VAT on Incentive", "Net Due After Incentive");
  private static final List<String> OR_HEADERS =
      List.of("Official Receipt Number", "OR Date", "OR Amount");
  private static final int FIRST_AMOUNT_COLUMN = 9;

  private final DocTemplateService templates;
  private final DocumentComposer composer;
  private final BatchDocumentRepository documents;
  private final OrganizationService organization;
  private final PartyService parties;
  private final Clock clock;

  /**
   * Creates the documents.
   *
   * @param templates templates
   * @param composer PDF / XLSX composer
   * @param documents stored documents
   * @param organization company names
   * @param parties insurer names
   * @param clock clock
   */
  public BatchDocuments(
      DocTemplateService templates,
      DocumentComposer composer,
      BatchDocumentRepository documents,
      OrganizationService organization,
      PartyService parties,
      Clock clock) {
    this.templates = templates;
    this.composer = composer;
    this.documents = documents;
    this.organization = organization;
    this.parties = parties;
    this.clock = clock;
  }

  /**
   * A document of a batch: the stored one after submission, else rendered now.
   *
   * @param batch batch with lines
   * @param kind kind
   * @return file
   */
  public StoredFile document(RemittanceBatch batch, DocumentKind kind) {
    return documents
        .findByBatchIdAndKind(batch.getId(), kind)
        .map(
            d ->
                new StoredFile(
                    d.getFileName(), d.getContentType(), d.getContent(), d.getTemplateVersion()))
        .orElseGet(() -> render(batch, kind));
  }

  /**
   * Renders and stores the three documents on the batch (submission, RMTID.011).
   *
   * @param batch batch with lines
   */
  public void store(RemittanceBatch batch) {
    for (DocumentKind kind : DocumentKind.values()) {
      StoredFile file = render(batch, kind);
      documents
          .findByBatchIdAndKind(batch.getId(), kind)
          .ifPresentOrElse(
              d -> d.replace(file),
              () -> documents.save(new BatchDocument(batch.getId(), kind, file)));
    }
  }

  /**
   * Renders a document.
   *
   * @param batch batch with lines
   * @param kind kind
   * @return file
   */
  public StoredFile render(RemittanceBatch batch, DocumentKind kind) {
    return switch (kind) {
      case SCHEDULE_XLSX ->
          new StoredFile(batch.getBatchNo() + "-schedule.xlsx", XLSX, scheduleXlsx(batch), null);
      case SCHEDULE_PDF -> schedulePdf(batch);
      case PAYMENT_REQUEST_PDF -> paymentRequestPdf(batch);
    };
  }

  /**
   * The schedule as a spreadsheet (extract file, MKTID.001 attachment).
   *
   * @param batch batch with lines
   * @return XLSX bytes
   */
  public byte[] scheduleXlsx(RemittanceBatch batch) {
    boolean incentives = hasIncentives(batch);
    List<List<Object>> rows = new ArrayList<>();
    for (BatchLine l : batch.included()) {
      List<Object> row = new ArrayList<>(commonCells(l));
      RemittanceAmounts a = l.getAmounts();
      row.addAll(
          List.of(a.paidAr(), a.commission(), a.commissionVat(), a.wtax(), a.dtip(), a.netDue()));
      if (incentives) {
        row.addAll(List.of(l.getBasicPremium(), a.incentive(), a.incentiveVat(), a.payable()));
      }
      row.addAll(List.of(nz(l.getInsurerOrNo()), date(l.getInsurerOrDate()), orAmount(l)));
      rows.add(row);
    }
    return composer.xlsx(new SheetSpec(batch.getBatchNo(), headers(incentives), rows));
  }

  private static List<String> headers(boolean incentives) {
    List<String> headers = new ArrayList<>(COMMON_HEADERS);
    if (incentives) {
      headers.addAll(INCENTIVE_HEADERS);
    }
    headers.addAll(OR_HEADERS);
    return headers;
  }

  private static List<String> commonCells(BatchLine l) {
    return List.of(
        l.getInvoiceNo(),
        nz(l.getPolicyNo()),
        nz(l.getEndorsementNo()),
        l.getAssuredName(),
        nz(l.getRiskCode()),
        date(l.getLastPaidOn()),
        date(l.getInceptionDate()),
        date(l.getBookingDate()),
        date(l.getExpiryDate()));
  }

  private StoredFile schedulePdf(RemittanceBatch batch) {
    boolean incentives = hasIncentives(batch);
    MergedText text =
        templates.merge(
            SCHEDULE,
            LocalDate.now(clock),
            Map.of(
                "reference", batch.getBatchNo(),
                "insurerName", insurerName(batch),
                "remittanceType", typeLabel(batch.getRemittanceType()),
                "incentiveNote", incentives ? ", less the early remittance incentive" : ""));
    List<List<String>> rows = new ArrayList<>();
    for (BatchLine l : batch.included()) {
      RemittanceAmounts a = l.getAmounts();
      List<String> row = new ArrayList<>(commonCells(l));
      row.addAll(
          List.of(
              amount(a.paidAr()),
              amount(a.commission()),
              amount(a.commissionVat()),
              amount(a.wtax()),
              amount(a.dtip()),
              amount(a.netDue())));
      rows.add(row);
    }
    DocumentSpec spec =
        new DocumentSpec(
            companyName(batch),
            "REMITTANCE SCHEDULE",
            batch.getBatchNo(),
            List.of(
                header(batch),
                new Table(
                    "Accounts remitted (" + batch.getCurrency() + ")",
                    COMMON_HEADERS,
                    rows,
                    amountColumns()),
                totals(batch),
                new Text(text.title(), text.text())),
            List.of("Prepared by", "Checked by", "Approved by"),
            text.versionTag());
    return new StoredFile(
        batch.getBatchNo() + "-schedule.pdf", PDF, composer.pdf(spec), text.versionTag());
  }

  private StoredFile paymentRequestPdf(RemittanceBatch batch) {
    MergedText text =
        templates.merge(
            PAYMENT_REQUEST,
            LocalDate.now(clock),
            Map.of(
                "reference", batch.getBatchNo(),
                "insurerName", insurerName(batch),
                "currency", batch.getCurrency(),
                "amount", amount(batch.amountDue()),
                "approvedBy",
                    batch.getApprovedBy() == null
                        ? "the Remittance Team Leader"
                        : batch.getApprovedBy()));
    DocumentSpec spec =
        new DocumentSpec(
            companyName(batch),
            "PAYMENT REQUEST",
            batch.getBatchNo(),
            List.of(header(batch), totals(batch), new Text(text.title(), text.text())),
            List.of("Requested by", "Approved by", "Received by Disbursement"),
            text.versionTag());
    return new StoredFile(
        batch.getBatchNo() + "-payment-request.pdf", PDF, composer.pdf(spec), text.versionTag());
  }

  private Fields header(RemittanceBatch batch) {
    return new Fields(
        "Batch",
        List.of(
            new Field("Batch number", batch.getBatchNo()),
            new Field("Insurer", insurerName(batch) + " (" + batch.getInsurerCode() + ")"),
            new Field("Remittance type", typeLabel(batch.getRemittanceType())),
            new Field("Currency", batch.getCurrency()),
            new Field("Accounts", String.valueOf(batch.getLineCount())),
            new Field("Processor", nz(batch.getProcessor())),
            new Field("Stage", batch.getStage().name())));
  }

  private static Table totals(RemittanceBatch batch) {
    RemittanceAmounts t = batch.getTotals();
    List<List<String>> rows = new ArrayList<>();
    rows.add(List.of("Paid AR", amount(t.paidAr())));
    rows.add(List.of("Less: realized commission", amount(t.commission().negate())));
    rows.add(List.of("Less: VAT on commission", amount(t.commissionVat().negate())));
    rows.add(List.of("Add: withholding tax on commission", amount(t.wtax())));
    rows.add(List.of("Net due", amount(t.netDue())));
    if (t.incentiveTotal().signum() != 0) {
      rows.add(
          List.of(
              "Less: early remittance incentive with VAT", amount(t.incentiveTotal().negate())));
    }
    if (t.cpc2Total().signum() != 0) {
      rows.add(List.of("Less: CPC2 incentive with VAT", amount(t.cpc2Total().negate())));
    }
    BigDecimal deducted = batch.getSettlement().getDeductionAmount();
    if (deducted.signum() != 0) {
      rows.add(List.of("Less: insurer-confirmed deductions", amount(deducted.negate())));
    }
    rows.add(List.of("Amount payable", amount(batch.amountDue())));
    return new Table(
        "Totals (" + batch.getCurrency() + ")", List.of("Item", "Amount"), rows, List.of(1));
  }

  private static List<Integer> amountColumns() {
    List<Integer> columns = new ArrayList<>();
    for (int i = FIRST_AMOUNT_COLUMN; i < COMMON_HEADERS.size(); i++) {
      columns.add(i);
    }
    return columns;
  }

  private static boolean hasIncentives(RemittanceBatch batch) {
    return batch.getRemittanceType() == RemittanceType.WITH_INCENTIVES;
  }

  /**
   * Display name of the batch's insurer.
   *
   * @param batch batch
   * @return name, else the code
   */
  public String insurerName(RemittanceBatch batch) {
    try {
      return parties.getByCode(batch.getCompanyId(), batch.getInsurerCode()).getName();
    } catch (ResourceNotFoundException ex) {
      return batch.getInsurerCode();
    }
  }

  private String companyName(RemittanceBatch batch) {
    return organization.getCompany(batch.getCompanyId()).getName();
  }

  /**
   * Label of a remittance type.
   *
   * @param type type
   * @return label
   */
  public static String typeLabel(RemittanceType type) {
    return switch (type) {
      case WITH_INCENTIVES -> "With Incentives";
      case NORMAL_PHP -> "Normal - Peso";
      case NORMAL_USD -> "Normal - Dollar";
      case SPECIAL -> "Special Remittance";
    };
  }

  private static Object orAmount(BatchLine l) {
    return l.getInsurerOrAmount() == null ? "" : l.getInsurerOrAmount();
  }

  private static String nz(String value) {
    return value == null ? NONE : value;
  }

  private static String date(LocalDate value) {
    return value == null ? NONE : value.toString();
  }

  private static String amount(BigDecimal value) {
    return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
  }
}
