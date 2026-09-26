package com.iortatechnxt.brokerverse.adjustment.service;

import static com.iortatechnxt.brokerverse.adjustment.service.DocText.amount;
import static com.iortatechnxt.brokerverse.adjustment.service.DocText.text;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.adjustment.domain.ComponentChange;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestClass;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentQueryService.GlLine;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceShare;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Documents of an endorsement request, composed with docgen:
 *
 * <ul>
 *   <li>the endorsement slip (ADJID.015, MKTID.008): Annex V fields, numbered once {@code
 *       ES-<yyyy>}, not for internal adjustments;
 *   <li>the validation slip (ADJID.018): summary, validation status, before / after, insurer
 *       breakdown and GL entries, only once the request passed validation.
 * </ul>
 */
@Service
@Transactional
public class AdjustmentDocuments {

  private static final List<Integer> AMOUNTS_FROM_1 = List.of(1, 2, 3);
  private static final List<Integer> AMOUNTS_FROM_2 = List.of(2, 3, 4);
  private static final List<Integer> AMOUNT_COLUMN_4 = List.of(3);
  private static final String PDF = ".pdf";

  private final AdjustmentQueryService queries;
  private final InvoiceLedgerQueryService ledger;
  private final AccountQueryService accounts;
  private final DocTemplateService templates;
  private final DocumentComposer composer;
  private final OrganizationService organization;
  private final DocumentNumberService numbers;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param queries requests
   * @param ledger Operations ledger (payment and remittance status)
   * @param accounts accounts (sum insured)
   * @param templates document templates
   * @param composer PDF composer
   * @param organization company name
   * @param numbers slip numbers
   * @param lovs labels
   * @param audit audit trail
   * @param clock clock
   */
  public AdjustmentDocuments(
      AdjustmentQueryService queries,
      InvoiceLedgerQueryService ledger,
      AccountQueryService accounts,
      DocTemplateService templates,
      DocumentComposer composer,
      OrganizationService organization,
      DocumentNumberService numbers,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.queries = queries;
    this.ledger = ledger;
    this.accounts = accounts;
    this.templates = templates;
    this.composer = composer;
    this.organization = organization;
    this.numbers = numbers;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The endorsement slip of a request (ADJID.015, MKTID.008).
   *
   * @param id request
   * @return PDF
   */
  public Generated endorsementSlip(Long id) {
    EndorsementRequest r = queries.get(id);
    if (r.getRequestClass() == RequestClass.INTERNAL) {
      throw new BusinessRuleException(
          "ADJ_NO_SLIP_FOR_INTERNAL", "Internal adjustments have no endorsement slip (ADJID.015)");
    }
    LocalDate today = LocalDate.now(clock);
    r.assignSlip(numbers.next("ES-" + today.getYear()));
    OpsInvoice invoice = ledger.require(r.getSubject().invoiceNo());
    Account account = accounts.requireByArn(r.getSubject().arn());
    MergedText intro = templates.merge("ENDORSEMENT_SLIP", today, values(r, today));
    DocumentSpec spec =
        new DocumentSpec(
            organization.getCompany(r.getCompanyId()).getName(),
            "ENDORSEMENT SLIP",
            r.getSlipNo(),
            List.of(
                new Text(intro.title(), intro.text()),
                new Fields("Request", requestFields(r, today)),
                new Fields("Policy", policyFields(r, invoice, account)),
                new Fields("Collection and remittance", paymentFields(invoice)),
                changesTable(r)),
            List.of("Prepared by (Marketing AO)", "Approved by (Marketing TL)"),
            intro.versionTag());
    audit.record(Adjustments.ENTITY, r.getRequestNo(), AuditAction.EXPORT, "Endorsement slip");
    return new Generated(r.getSlipNo() + PDF, composer.pdf(spec));
  }

  /**
   * The validation slip of a validated request (ADJID.018).
   *
   * @param id request
   * @return PDF
   */
  @Transactional(readOnly = true)
  public Generated validationSlip(Long id) {
    EndorsementRequest r = queries.get(id);
    if (!r.getStage().isValidated()) {
      throw new BusinessRuleException(
          "ADJ_NOT_VALIDATED", r.getRequestNo() + " has not passed validation yet");
    }
    LocalDate today = LocalDate.now(clock);
    Map<String, String> values = values(r, today);
    MergedText intro = templates.merge("VALIDATION_SLIP", today, values);
    DocumentSpec spec =
        new DocumentSpec(
            organization.getCompany(r.getCompanyId()).getName(),
            "VALIDATION SLIP",
            r.getRequestNo(),
            List.of(
                new Text(intro.title(), intro.text()),
                new Fields("Request", requestFields(r, today)),
                new Fields("Validation", validationFields(r)),
                changesTable(r),
                sharesTable(r),
                glTable(queries.journalLines(r))),
            List.of("Validated by", "Approved by"),
            intro.versionTag());
    return new Generated("VS-" + r.getRequestNo() + PDF, composer.pdf(spec));
  }

  private static Map<String, String> values(EndorsementRequest r, LocalDate today) {
    return Map.of(
        "requestNo", r.getRequestNo(),
        "invoiceNo", r.getSubject().invoiceNo(),
        "arn", r.getSubject().arn(),
        "policyNo", text(r.getSubject().policyNo()),
        "assured", r.getSubject().assuredName(),
        "effectiveDate", r.getTerms().effectiveDate().toString(),
        "validatedOn", text(DocText.date(r.trail().validatedAt())),
        "status", r.getStage().name(),
        "today", today.toString());
  }

  private List<Field> requestFields(EndorsementRequest r, LocalDate today) {
    RequestTerms t = r.getTerms();
    return List.of(
        new Field("Date", today.toString()),
        new Field("Endorsement Request No.", r.getRequestNo()),
        new Field("Invoice No.", r.getSubject().invoiceNo()),
        new Field("Account Reference No.", r.getSubject().arn()),
        new Field("Endorsement Type", lovs.label(RequestRules.TYPE_LOV, t.endorsementType())),
        new Field("Request Type", text(lovs.label(RequestRules.REQUEST_TYPE_LOV, t.requestType()))),
        new Field(
            "Reason for Cancellation", text(lovs.label(RequestRules.REASON_LOV, t.reasonCode()))),
        new Field("Effective Date", t.effectiveDate().toString()),
        new Field("Insurer Endorsement Ref.", text(t.endorsementRef())),
        new Field("Description", t.description()),
        new Field("Additional / Other Instructions", text(t.instructions())));
  }

  private static List<Field> policyFields(
      EndorsementRequest r, OpsInvoice invoice, Account account) {
    String coInsurers =
        invoice.getShares().stream()
            .filter(s -> !s.lead())
            .map(OpsInvoiceShare::insurerCode)
            .collect(Collectors.joining(", "));
    BigDecimal change = r.getTerms().sumInsuredChange();
    return List.of(
        new Field("Assured", r.getSubject().assuredName()),
        new Field("Insurer", r.getSubject().insurerCode()),
        new Field("Co-insurer/s", coInsurers.isEmpty() ? DocText.NONE : coInsurers),
        new Field("Policy No.", text(r.getSubject().policyNo())),
        new Field("Risk Code", text(invoice.getClassification().riskCode())),
        new Field("Risk Description", text(r.getSubject().productLine())),
        new Field(
            "Period of Cover",
            invoice.getClassification().inceptionDate()
                + " to "
                + invoice.getClassification().expiryDate()),
        new Field("Marketing AO", text(r.getSubject().aoUsername())),
        new Field("Market Segment", text(r.getSubject().segment())),
        new Field("Total Sum Insured", amount(account.getTotalSumInsured())),
        new Field("Sum Insured Change", amount(change)),
        new Field("Premium Rate (%)", text(r.getTerms().ratePercent())),
        new Field("Approved by", text(r.trail().approvedBy())));
  }

  private static List<Field> paymentFields(OpsInvoice invoice) {
    BigDecimal balance = invoice.premiumBalance();
    return List.of(
        new Field("Payment Status", invoice.getPaymentStatus().name()),
        new Field("Amount Paid", amount(invoice.getGrossPremium().subtract(balance))),
        new Field("Remaining AR", amount(balance)),
        new Field("Remittance Status", invoice.getRemittanceStatus().name()));
  }

  private static List<Field> validationFields(EndorsementRequest r) {
    return List.of(
        new Field("Status", r.getStage().name()),
        new Field("Validated by", text(r.trail().validatedBy())),
        new Field("Validation Date", text(DocText.date(r.trail().validatedAt()))),
        new Field("Approved by", text(r.trail().approvedBy())),
        new Field("Validation Batch No.", text(r.outcome().batchNo())),
        new Field("Endorsement No.", text(r.outcome().endorsementNo())),
        new Field("Invoice Booked", text(r.outcome().newInvoiceNo())),
        new Field("Service Invoices", text(r.outcome().serviceInvoices())),
        new Field("AR Insurer", amount(r.outcome().arInsurerAmount())));
  }

  private static Table changesTable(EndorsementRequest r) {
    List<List<String>> rows =
        r.getChanges().stream()
            .filter(c -> c.delta().signum() != 0 || c.before().signum() != 0)
            .map(
                (ComponentChange c) ->
                    List.of(
                        c.component().name(),
                        amount(c.before()),
                        amount(c.delta()),
                        amount(c.after())))
            .toList();
    return new Table(
        "Before and after (" + r.getSubject().currency() + ")",
        List.of("Component", "Before", "Change", "After"),
        rows,
        AMOUNTS_FROM_1);
  }

  private static Table sharesTable(EndorsementRequest r) {
    List<List<String>> rows =
        r.getShares().stream()
            .map(
                s ->
                    List.of(
                        s.insurerCode() + (s.lead() ? " (lead)" : ""),
                        s.sharePct().stripTrailingZeros().toPlainString() + "%",
                        amount(s.premiumDelta()),
                        amount(s.commissionDelta()),
                        amount(s.vatDelta())))
            .toList();
    return new Table(
        "Per insurer",
        List.of("Insurer", "Share", "Premium", "Commission", "VAT"),
        rows,
        AMOUNTS_FROM_2);
  }

  private static Table glTable(List<GlLine> lines) {
    List<List<String>> rows =
        lines.stream()
            .map(
                l ->
                    List.of(
                        l.batchNo(),
                        l.accountCode() + " " + l.accountName(),
                        l.side().name(),
                        amount(l.amount()),
                        text(l.partyCode())))
            .toList();
    return new Table(
        "Accounting entries",
        List.of("Journal", "GL Account", "Dr / Cr", "Amount", "Party"),
        rows,
        AMOUNT_COLUMN_4);
  }

  /**
   * A generated document.
   *
   * @param fileName file name
   * @param content PDF bytes
   */
  public record Generated(String fileName, byte[] content) {}
}
