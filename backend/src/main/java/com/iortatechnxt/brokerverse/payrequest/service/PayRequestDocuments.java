package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.payrequest.domain.Liquidation;
import com.iortatechnxt.brokerverse.payrequest.domain.LiquidationLine;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLine;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestKind;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestTrail;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The request forms of Appendix D (MKT 1.10.0), composed with docgen from the templates of V895:
 * the Refund Request Form with its accounts, the Request for Payment of a cash advance and the Cash
 * Advance Liquidation Form. Layouts and signatories are drafts (AQ18).
 */
@Service
@Transactional
public class PayRequestDocuments {

  private static final String PDF = ".pdf";
  private static final String NONE = "-";
  private static final List<Integer> RRF_AMOUNT = List.of(4);
  private static final List<Integer> LIQUIDATION_AMOUNTS = List.of(2, 3, 4, 5, 6, 7);

  private final PayRequestQueryService queries;
  private final LiquidationService liquidations;
  private final DocTemplateService templates;
  private final DocumentComposer composer;
  private final OrganizationService organization;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param queries requests
   * @param liquidations liquidations
   * @param templates document templates
   * @param composer PDF composer
   * @param organization company name
   * @param lovs labels
   * @param audit audit trail
   * @param clock clock
   */
  public PayRequestDocuments(
      PayRequestQueryService queries,
      LiquidationService liquidations,
      DocTemplateService templates,
      DocumentComposer composer,
      OrganizationService organization,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.queries = queries;
    this.liquidations = liquidations;
    this.templates = templates;
    this.composer = composer;
    this.organization = organization;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The form of a request: the RRF of a refund, the RFP of a cash advance (MKT 1.10.0).
   *
   * @param id request
   * @return PDF
   */
  public Generated form(Long id) {
    PaymentRequest r = queries.get(id);
    if (!r.getKind().pays()) {
      throw new BusinessRuleException(
          "PRQ_NO_FORM", "A check cancellation has no request form; print the paid request's");
    }
    boolean refund = r.getKind() == RequestKind.REFUND;
    LocalDate today = LocalDate.now(clock);
    MergedText intro = templates.merge(refund ? "PRQ_RRF" : "PRQ_RFP", today, values(r));
    List<DocumentSpec.Section> sections =
        refund
            ? List.of(
                new Text(intro.title(), intro.text()),
                new Fields("Request", requestFields(r)),
                refundTable(r))
            : List.of(
                new Text(intro.title(), intro.text()), new Fields("Request", requestFields(r)));
    DocumentSpec spec =
        new DocumentSpec(
            organization.getCompany(r.getCompanyId()).getName(),
            refund ? "REFUND REQUEST FORM" : "REQUEST FOR PAYMENT",
            r.getRequestNo(),
            sections,
            signatories(r.trailOrNone(), r),
            intro.versionTag());
    audit.record(PayRequests.ENTITY, r.getRequestNo(), AuditAction.EXPORT, "Request form");
    return new Generated(r.getRequestNo() + PDF, composer.pdf(spec));
  }

  /**
   * The Cash Advance Liquidation Form of a cash advance (Appendix D).
   *
   * @param id cash-advance request
   * @return PDF
   */
  public Generated liquidationForm(Long id) {
    PaymentRequest r = queries.get(id);
    Liquidation l =
        liquidations
            .of(id)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "PRQ_NO_LIQUIDATION", r.getRequestNo() + " has no liquidation yet"));
    Map<String, String> values = values(r);
    values.put("liquidationNo", l.getLiquidationNo());
    values.put("cashAdvanced", l.getCashAdvanced().toPlainString());
    values.put("totalExpenses", l.getTotalExpenses().toPlainString());
    values.put("overShort", l.getOverShort().toPlainString());
    MergedText intro = templates.merge("PRQ_LIQUIDATION", LocalDate.now(clock), values);
    DocumentSpec spec =
        new DocumentSpec(
            organization.getCompany(r.getCompanyId()).getName(),
            "CASH ADVANCE LIQUIDATION FORM",
            l.getLiquidationNo(),
            List.of(
                new Text(intro.title(), intro.text()),
                new Fields(
                    "Employee",
                    List.of(
                        new Field("Name", r.getPayee().name()),
                        new Field("Job Level", text(l.getJobLevel())),
                        new Field("Purpose", text(r.getContent().purpose())),
                        new Field("Cash Advanced", l.getCashAdvanced().toPlainString()),
                        new Field("Over / (Short)", l.getOverShort().toPlainString()))),
                liquidationTable(l)),
            List.of(
                "Employee", "Checked and validated (Disbursement)", "Received for booking (GL)"),
            intro.versionTag());
    return new Generated(l.getLiquidationNo() + PDF, composer.pdf(spec));
  }

  private Map<String, String> values(PaymentRequest r) {
    Map<String, String> values = new HashMap<>();
    values.put("requestNo", r.getRequestNo());
    values.put("reference", text(r.getContent().referenceText()));
    values.put("requestDate", r.getRequestDate().toString());
    values.put("segment", text(r.getContent().segment()));
    values.put("currency", r.getContent().currency());
    values.put("amount", r.getAmount().toPlainString());
    values.put("payeeName", r.getPayee().name());
    values.put("mode", text(lovs.label(PayRequestRules.MODE_LOV, r.getPayee().mode())));
    values.put("rfpType", text(lovs.label("PRQ_RFP_TYPE", r.getContent().rfpType())));
    values.put("purpose", text(r.getContent().purpose()));
    return values;
  }

  private List<Field> requestFields(PaymentRequest r) {
    return List.of(
        new Field("Request No.", r.getRequestNo()),
        new Field("Request Date", r.getRequestDate().toString()),
        new Field("Reference", text(r.getContent().referenceText())),
        new Field("Segment", text(r.getContent().segment())),
        new Field("Requesting Unit", text(r.getContent().requestingUnit())),
        new Field("Payee", r.getPayee().name() + " (" + r.getPayee().code() + ")"),
        new Field(
            "Mode of Payment", text(lovs.label(PayRequestRules.MODE_LOV, r.getPayee().mode()))),
        new Field("Account No.", text(r.getPayee().accountNo())),
        new Field("Account / Check Name", text(r.getPayee().accountName())),
        new Field("Amount", r.getContent().currency() + " " + r.getAmount().toPlainString()),
        new Field("Purpose", text(r.getContent().purpose())));
  }

  private Table refundTable(PaymentRequest r) {
    List<List<String>> rows =
        r.getLines().stream()
            .map(
                (RefundLine l) ->
                    List.of(
                        String.valueOf(l.getLineNo()),
                        l.getArNo(),
                        l.getClientCode(),
                        l.getAssuredName(),
                        l.getAmount().toPlainString(),
                        text(lovs.label(PayRequestRules.REASON_LOV, l.getReasonCode())),
                        text(l.getBranchUnit()),
                        text(l.getCategoryA()) + " / " + text(l.getCategoryB()),
                        text(l.getAccountName())))
            .toList();
    return new Table(
        "Accounts (" + r.getContent().currency() + ")",
        List.of(
            "Item",
            "AR No.",
            "Client No.",
            "Assured / Client",
            "Amount",
            "Reason",
            "Branch / Unit",
            "Category A / B",
            "Account / Check Name"),
        rows,
        RRF_AMOUNT);
  }

  private static Table liquidationTable(Liquidation l) {
    List<List<String>> rows =
        l.getLines().stream()
            .map(
                (LiquidationLine d) ->
                    List.of(
                        d.getFieldworkDate().toString(),
                        d.getParticulars(),
                        plain(d.getPerDiem()),
                        plain(d.getRepresentation()),
                        plain(d.getTransport()),
                        plain(d.getLodging()),
                        plain(d.getOthers()),
                        plain(d.total())))
            .toList();
    return new Table(
        "Fieldwork",
        List.of(
            "Date",
            "Particulars",
            "Per Diem",
            "Representation",
            "Transportation",
            "Lodging",
            "Others",
            "Total"),
        rows,
        LIQUIDATION_AMOUNTS);
  }

  private static List<String> signatories(RequestTrail t, PaymentRequest r) {
    return List.of(
        "Prepared by: " + r.getCreatedBy(),
        "Checked by: " + text(t.reviewedBy()),
        "Approved by: " + text(t.approvedBy()));
  }

  private static String plain(BigDecimal amount) {
    return amount.toPlainString();
  }

  private static String text(String value) {
    return value == null || value.isBlank() ? NONE : value;
  }

  /**
   * A generated document.
   *
   * @param fileName file name
   * @param content PDF bytes
   */
  public record Generated(String fileName, byte[] content) {}
}
