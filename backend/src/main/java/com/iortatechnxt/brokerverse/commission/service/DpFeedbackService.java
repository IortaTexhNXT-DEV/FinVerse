package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.DpTag;
import com.iortatechnxt.brokerverse.commission.domain.DpBilling;
import com.iortatechnxt.brokerverse.commission.domain.DpBillingRepository;
import com.iortatechnxt.brokerverse.commission.domain.DpItem;
import com.iortatechnxt.brokerverse.commission.domain.DpItemRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.service.port.CollectionFeed;
import com.iortatechnxt.brokerverse.opsledger.service.port.FeedItem;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The insurer's answer to a commission billing (CMRID.008/009/012): each billed account is approved
 * or rejected with a reason from {@code DP_FEEDBACK_REASON} (OQ40); rejected accounts are returned
 * to Collection with the time and reason through the {@code CollectionFeed} port (feed {@code
 * COLLECTION_DP_RETURNED}); once every account is answered the billing moves to approved (at least
 * one approval) or returned to Collection. Answers come from the screen or the flow-in feed {@code
 * INSURER_DP_RESPONSE}.
 */
@Service
@Transactional
public class DpFeedbackService {

  /** Outbound feed of the rejected accounts. */
  public static final String RETURNED_FEED = "COLLECTION_DP_RETURNED";

  /** List of the feedback reasons. */
  public static final String REASONS = "DP_FEEDBACK_REASON";

  private final DpBillingRepository billings;
  private final DpItemRepository items;
  private final LovService lovs;
  private final ObjectProvider<CollectionFeed> collection;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param billings billings
   * @param items DP accounts
   * @param lovs lists of values
   * @param collection Collection system port (looked up lazily: its default adapter uses the
   *     flow-in framework, which uses this module's feed handlers)
   * @param workflow workflow engine
   * @param audit audit trail
   * @param clock clock
   */
  public DpFeedbackService(
      DpBillingRepository billings,
      DpItemRepository items,
      LovService lovs,
      ObjectProvider<CollectionFeed> collection,
      WorkflowService workflow,
      AuditTrailService audit,
      Clock clock) {
    this.billings = billings;
    this.items = items;
    this.lovs = lovs;
    this.collection = collection;
    this.workflow = workflow;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records the insurer's answers on a billing.
   *
   * @param billingId billing
   * @param answers one answer per account (by invoice number)
   * @return the billing
   */
  public DpBilling answer(Long billingId, List<Answer> answers) {
    DpBilling billing =
        billings
            .findById(billingId)
            .orElseThrow(() -> new ResourceNotFoundException(DpBillingService.ENTITY, billingId));
    if (!DpBilling.AWAITING.equals(billing.getStage())) {
      throw new BusinessRuleException(
          "DP_BILLING_NOT_AWAITING",
          "Billing " + billing.getBillingNo() + " is not waiting for the insurer");
    }
    Map<String, DpItem> byInvoice = new LinkedHashMap<>();
    items.findByBillingIdOrderByIdAsc(billingId).forEach(i -> byInvoice.put(i.getInvoiceNo(), i));
    List<DpItem> rejected = new ArrayList<>();
    for (Answer a : answers) {
      DpItem item = byInvoice.get(a.invoiceNo());
      if (item == null) {
        throw new BusinessRuleException(
            "DP_NOT_ON_BILLING",
            "Invoice " + a.invoiceNo() + " is not on billing " + billing.getBillingNo());
      }
      apply(item, a);
      if (!a.approved()) {
        rejected.add(item);
      }
    }
    returnToCollection(billing, rejected);
    settle(billing, byInvoice.values());
    return billing;
  }

  private void apply(DpItem item, Answer a) {
    if (!a.approved()) {
      if (a.reason() == null || a.reason().isBlank()) {
        throw new BusinessRuleException(
            "DP_REASON_REQUIRED", "Give the insurer's reason for rejecting " + item.getInvoiceNo());
      }
      lovs.requireValid(REASONS, a.reason(), LocalDate.now(clock));
    }
    item.answered(a.approved(), blank(a.reason()), blank(a.comment()), clock.instant());
    audit.record(
        "DpItem",
        item.getId(),
        AuditAction.UPDATE,
        (a.approved() ? "Approved" : "Rejected (" + a.reason() + ")") + " by the insurer");
  }

  private void returnToCollection(DpBilling billing, List<DpItem> rejected) {
    if (rejected.isEmpty()) {
      return;
    }
    List<FeedItem> feed = new ArrayList<>();
    for (DpItem i : rejected) {
      Map<String, String> fields = new LinkedHashMap<>();
      fields.put("Invoice No.", i.getInvoiceNo());
      fields.put("Policy No.", i.getPolicyNo() == null ? "" : i.getPolicyNo());
      fields.put("Insurer", i.getInsurerCode());
      fields.put("Branch", i.getBranchCode() == null ? "" : i.getBranchCode());
      fields.put("Billing No.", billing.getBillingNo());
      fields.put("Reason", i.getFeedbackReason());
      fields.put("Comment", i.getFeedbackComment() == null ? "" : i.getFeedbackComment());
      fields.put("Returned At", String.valueOf(i.getRespondedAt()));
      feed.add(new FeedItem("DPRET:" + i.getId(), fields));
    }
    String run = collection.getObject().send(billing.getCompanyId(), RETURNED_FEED, feed);
    rejected.forEach(i -> i.returned(run));
  }

  private void settle(DpBilling billing, Collection<DpItem> all) {
    if (all.stream().anyMatch(i -> i.getTag() == DpTag.BILLED)) {
      return;
    }
    billing.responded(clock.instant());
    boolean anyApproved = all.stream().anyMatch(i -> i.getTag() == DpTag.APPROVED);
    workflow.systemTransition(
        DpBillingService.ENTITY,
        String.valueOf(billing.getId()),
        anyApproved ? "insurer_approved" : "insurer_rejected",
        TransitionNote.comment("Insurer answered every account"));
  }

  private static String blank(String v) {
    return v == null || v.isBlank() ? null : v.strip();
  }

  /**
   * The insurer's answer on an account.
   *
   * @param invoiceNo invoice
   * @param approved approved or rejected
   * @param reason reason of a rejection (LOV DP_FEEDBACK_REASON)
   * @param comment comment
   */
  public record Answer(String invoiceNo, boolean approved, String reason, String comment) {}
}
