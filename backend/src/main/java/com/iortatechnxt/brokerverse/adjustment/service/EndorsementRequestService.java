package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest.Content;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequestRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestSubject;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Raising and changing endorsement requests (ADJID.001-004/008/020/022/023): one request per
 * invoice, for one or several invoices at once; each request is validated, checked for duplicates,
 * recomputed (before / after kept on the request), locks its invoice and opens its {@code
 * OPS_ENDORSEMENT} work case in DRAFT.
 */
@Service
@Transactional
public class EndorsementRequestService {

  private static final int MAX_INVOICES = 50;

  private static final int TEXT_SIZE = 128;

  private final EndorsementRequestRepository requests;
  private final InvoiceLedgerQueryService ledger;
  private final RequestRules rules;
  private final RequestConflicts conflicts;
  private final RecomputeService recompute;
  private final InvoiceGuard guard;
  private final WorkflowService workflow;
  private final HandoffService handoffs;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param ledger Operations ledger
   * @param rules validation
   * @param conflicts invoice, compatibility and duplicate checks
   * @param recompute recompute
   * @param guard invoice lock and flag
   * @param workflow workflow engine
   * @param handoffs hand-offs (quotation required)
   * @param numbers document numbers
   * @param audit audit trail
   * @param clock clock
   */
  public EndorsementRequestService(
      EndorsementRequestRepository requests,
      InvoiceLedgerQueryService ledger,
      RequestRules rules,
      RequestConflicts conflicts,
      RecomputeService recompute,
      InvoiceGuard guard,
      WorkflowService workflow,
      HandoffService handoffs,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.requests = requests;
    this.ledger = ledger;
    this.rules = rules;
    this.conflicts = conflicts;
    this.recompute = recompute;
    this.guard = guard;
    this.workflow = workflow;
    this.handoffs = handoffs;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Raises one request per invoice with the same terms (ADJID.001 single or multiple accounts).
   *
   * @param invoiceNos invoices (at least one, distinct)
   * @param draft terms, amounts and overrides
   * @return the requests, in invoice order
   */
  public List<EndorsementRequest> create(List<String> invoiceNos, RequestDraft draft) {
    Set<String> distinct = new LinkedHashSet<>();
    invoiceNos.stream()
        .filter(n -> n != null && !n.isBlank())
        .map(String::strip)
        .forEach(distinct::add);
    if (distinct.isEmpty() || distinct.size() > MAX_INVOICES) {
      throw new BusinessRuleException(
          "ADJ_INVOICES_REQUIRED", "Select between 1 and " + MAX_INVOICES + " invoices");
    }
    List<EndorsementRequest> created = new ArrayList<>();
    for (String invoiceNo : distinct) {
      created.add(create(draft.on(invoiceNo)));
    }
    return created;
  }

  /**
   * Raises a request on one invoice.
   *
   * @param draft invoice, terms, amounts and overrides
   * @return the draft request
   */
  public EndorsementRequest create(RequestDraft draft) {
    OpsInvoice invoice = ledger.require(draft.invoiceNo());
    conflicts.requireEligible(invoice);
    Content content = rules.content(draft, invoice);
    conflicts.requireCompatible(null, invoice.getInvoiceNo(), content.computation());
    requireNoDuplicate(draft, null);
    Recompute result =
        recompute.compute(
            invoice.getInvoiceNo(), content.computation(), content.terms(), content.amounts());
    String number = numbers.next("ENR-" + LocalDate.now(clock).getYear());
    EndorsementRequest request =
        new EndorsementRequest(
            invoice.getCompanyId(), invoice.getBranchId(), number, subjectOf(invoice), content);
    request.overrides(draft.duplicateOverride(), draft.baselineOverride());
    request.recordRecompute(result.components(), result.shares());
    request.quotationRequired(result.quotationRequired(), null);
    EndorsementRequest saved = requests.save(request);
    workflow.start(
        new StartCase(
            saved.getCompanyId(),
            Adjustments.WORKFLOW,
            new CaseRecord(
                Adjustments.ENTITY,
                String.valueOf(saved.getId()),
                number,
                invoice.getInvoiceNo() + " - " + invoice.getAssuredName(),
                Adjustments.link(saved.getId()),
                invoice.getClassification().segment()),
            null));
    guard.hold(saved);
    audit.record(
        Adjustments.ENTITY,
        number,
        AuditAction.CREATE,
        content.requestClass()
            + " "
            + content.terms().endorsementType()
            + " on "
            + invoice.getInvoiceNo()
            + " ("
            + invoice.getArn()
            + ")"
            + overrideText(draft));
    return saved;
  }

  /**
   * Changes a draft or returned request (the invoice stays).
   *
   * @param id request
   * @param draft new terms, amounts and overrides
   * @return the request
   */
  public EndorsementRequest update(Long id, RequestDraft draft) {
    EndorsementRequest request = get(id);
    String invoiceNo = request.getSubject().invoiceNo();
    RequestDraft onInvoice = draft.on(invoiceNo);
    OpsInvoice invoice = ledger.require(invoiceNo);
    Content content = rules.content(onInvoice, invoice);
    conflicts.requireCompatible(request, invoiceNo, content.computation());
    requireNoDuplicate(onInvoice, id);
    request.revise(content);
    Recompute result =
        recompute.compute(invoiceNo, content.computation(), content.terms(), content.amounts());
    request.overrides(onInvoice.duplicateOverride(), onInvoice.baselineOverride());
    request.recordRecompute(result.components(), result.shares());
    request.quotationRequired(result.quotationRequired(), request.getHandoffRef());
    audit.record(
        Adjustments.ENTITY,
        request.getRequestNo(),
        AuditAction.UPDATE,
        "Changed: " + content.terms().endorsementType() + overrideText(onInvoice));
    return request;
  }

  /**
   * Links the quotation Marketing prepared for a TSI increase above the package limit, by reference
   * only (ADJID.008), and closes the hand-off.
   *
   * @param id request
   * @param quotationRef quotation number
   * @return the request
   */
  public EndorsementRequest linkQuotation(Long id, String quotationRef) {
    EndorsementRequest request = get(id);
    if (!request.isQuotationRequired() || !request.getStage().isOpen()) {
      throw new BusinessRuleException(
          "ADJ_QUOTATION_NOT_REQUIRED", request.getRequestNo() + " needs no quotation link");
    }
    if (quotationRef == null || quotationRef.isBlank()) {
      throw new BusinessRuleException("ADJ_QUOTATION_REF_REQUIRED", "Enter the quotation number");
    }
    request.linkQuotation(quotationRef.strip());
    if (request.getHandoffRef() != null) {
      handoffs.close(
          Long.valueOf(request.getHandoffRef()), "Quotation " + quotationRef.strip() + " linked");
    }
    audit.record(
        Adjustments.ENTITY,
        request.getRequestNo(),
        AuditAction.UPDATE,
        "Quotation linked: " + quotationRef.strip());
    return request;
  }

  /**
   * Validates and recomputes a draft without saving it (new request wizard): before / after,
   * insurer breakdown, service invoice impact and the possible duplicates (ADJID.014/023).
   *
   * @param draft invoice, terms and amounts
   * @return preview
   */
  @Transactional(readOnly = true)
  public Preview preview(RequestDraft draft) {
    OpsInvoice invoice = ledger.require(draft.invoiceNo());
    conflicts.requireEligible(invoice);
    Content content = rules.content(draft, invoice);
    Recompute result =
        recompute.compute(
            invoice.getInvoiceNo(), content.computation(), content.terms(), content.amounts());
    List<String> duplicates =
        conflicts.duplicates(draft, null).stream().map(EndorsementRequest::getRequestNo).toList();
    return new Preview(content, result, duplicates);
  }

  /**
   * Recomputes an open request with the amounts in force (request page); a posted or cancelled
   * request shows the recompute recorded at the time.
   *
   * @param id request
   * @return preview (no duplicates)
   */
  @Transactional(readOnly = true)
  public Preview recompute(Long id) {
    EndorsementRequest request = get(id);
    Recompute result =
        request.getStage().isOpen()
            ? recompute.compute(
                request.getSubject().invoiceNo(),
                request.getComputation(),
                request.getTerms(),
                request.getAmounts())
            : RecomputeService.recorded(request);
    return new Preview(
        new Content(
            request.getRequestClass(),
            request.getComputation(),
            request.getTerms(),
            request.getAmounts(),
            request.isNeedsApproval(),
            request.isNegative()),
        result,
        List.of());
  }

  /**
   * A request.
   *
   * @param id request id
   * @return request
   */
  public EndorsementRequest get(Long id) {
    return requests
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(Adjustments.ENTITY, id));
  }

  private void requireNoDuplicate(RequestDraft draft, Long selfId) {
    List<EndorsementRequest> duplicates = conflicts.duplicates(draft, selfId);
    if (!duplicates.isEmpty() && draft.duplicateOverride() == null) {
      throw new BusinessRuleException(
          "ADJ_DUPLICATE_REQUEST",
          "Possible duplicate of "
              + duplicates.stream()
                  .map(EndorsementRequest::getRequestNo)
                  .collect(Collectors.joining(", "))
              + " (same invoice, request type, reason and endorsement reference): give the"
              + " justification to proceed");
    }
  }

  private static String overrideText(RequestDraft draft) {
    StringBuilder text = new StringBuilder(TEXT_SIZE);
    if (draft.duplicateOverride() != null) {
      text.append("; duplicate override: ").append(draft.duplicateOverride());
    }
    if (draft.baselineOverride() != null) {
      text.append("; baseline override: ").append(draft.baselineOverride());
    }
    return text.toString();
  }

  /**
   * A validated, recomputed draft.
   *
   * @param content class, computation, terms, approval and sign
   * @param recompute recompute
   * @param duplicates possible duplicates (request numbers)
   */
  public record Preview(Content content, Recompute recompute, List<String> duplicates) {

    /** Defensive copy. */
    public Preview {
      duplicates = List.copyOf(duplicates);
    }
  }

  private static RequestSubject subjectOf(OpsInvoice invoice) {
    return new RequestSubject(
        invoice.getInvoiceNo(),
        invoice.getArn(),
        invoice.getPolicyNo(),
        invoice.getClientCode(),
        invoice.getAssuredName(),
        invoice.getInsurerCode(),
        invoice.getCurrency(),
        invoice.getClassification().segment(),
        invoice.getClassification().aoUsername(),
        invoice.getClassification().productLine());
  }
}
