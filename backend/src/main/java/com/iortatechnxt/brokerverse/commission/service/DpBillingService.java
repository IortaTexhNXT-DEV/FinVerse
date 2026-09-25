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
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commission billings of direct payment accounts (CMRID.009, workflow {@code OPS_DP_BILLING}): the
 * accounts confirmed for billing are sorted by insurer into one billing each, assigned to the
 * handler who prepares it; a billing not yet sent can be cancelled, which puts its accounts back.
 * The stage of each billing mirrors its work case. Sending, feedback and collection are in {@link
 * DpBillingSender}, {@link DpFeedbackService} and {@link DpCollectionService}.
 */
@Service
@Transactional
public class DpBillingService {

  /** Entity type of the billings' work cases. */
  public static final String ENTITY = "DpBilling";

  private final DpBillingRepository billings;
  private final DpItemRepository items;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param billings billings
   * @param items DP accounts
   * @param workflow workflow engine
   * @param numbers billing numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public DpBillingService(
      DpBillingRepository billings,
      DpItemRepository items,
      WorkflowService workflow,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.billings = billings;
    this.items = items;
    this.workflow = workflow;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Sorts the accounts confirmed for billing by insurer into billings (CMRID.009).
   *
   * @param companyId company
   * @param itemIds accounts to bill; every account of the company "DP for billing" when empty
   * @return the billings created
   */
  public List<DpBilling> prepare(Long companyId, List<Long> itemIds) {
    List<DpItem> ready =
        itemIds == null || itemIds.isEmpty()
            ? items.findByCompanyIdAndTagOrderByIdAsc(companyId, DpTag.DP_FOR_BILLING)
            : items.findAllById(itemIds).stream()
                .filter(i -> i.getCompanyId().equals(companyId))
                .toList();
    Map<String, List<DpItem>> byInsurer = new LinkedHashMap<>();
    for (DpItem item : ready) {
      if (item.getTag() != DpTag.DP_FOR_BILLING || item.getBillingId() != null) {
        throw new BusinessRuleException(
            "DP_ITEM_STATE",
            "Account "
                + item.getInvoiceNo()
                + " is not confirmed for billing ("
                + item.getTag()
                + ")");
      }
      byInsurer.computeIfAbsent(item.getInsurerCode(), k -> new ArrayList<>()).add(item);
    }
    if (byInsurer.isEmpty()) {
      throw new BusinessRuleException("DP_NOTHING_TO_BILL", "No account is confirmed for billing");
    }
    List<DpBilling> created = new ArrayList<>();
    byInsurer.forEach((insurer, list) -> created.add(open(companyId, insurer, list)));
    return created;
  }

  private DpBilling open(Long companyId, String insurerCode, List<DpItem> list) {
    String no = numbers.next("CRB-" + LocalDate.now(clock).getYear());
    DpBilling billing =
        billings.save(new DpBilling(companyId, no, insurerCode, currentUser.username()));
    list.forEach(i -> i.assign(billing.getId()));
    billing.total(list);
    workflow.start(
        new StartCase(
            companyId,
            DpBilling.WORKFLOW,
            new CaseRecord(
                ENTITY,
                String.valueOf(billing.getId()),
                no,
                "Direct payment commission billing " + insurerCode,
                "/commission/billings/" + billing.getId(),
                null),
            null));
    audit.record(
        ENTITY,
        no,
        AuditAction.CREATE,
        list.size() + " account(s) of " + insurerCode + ", net " + billing.getTotalNet());
    return billing;
  }

  /**
   * Cancels a billing not yet sent: its accounts are for billing again.
   *
   * @param id billing
   * @param comment reason
   * @return the billing
   */
  public DpBilling cancel(Long id, String comment) {
    DpBilling billing = require(id);
    if (!DpBilling.FOR_BILLING.equals(billing.getStage())) {
      throw new BusinessRuleException(
          "DP_BILLING_SENT", "Billing " + billing.getBillingNo() + " was already sent");
    }
    items.findByBillingIdOrderByIdAsc(id).forEach(DpItem::release);
    workflow.transition(ENTITY, String.valueOf(id), "cancel", TransitionNote.comment(comment));
    audit.record(ENTITY, billing.getBillingNo(), AuditAction.UPDATE, "Cancelled: " + comment);
    return billing;
  }

  /**
   * A billing.
   *
   * @param id billing
   * @return billing
   */
  @Transactional(readOnly = true)
  public DpBilling require(Long id) {
    return billings.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Billings of a company.
   *
   * @param companyId company
   * @param stage stage, null for all
   * @param insurer insurer, null for all
   * @param pageable page
   * @return billings, newest first
   */
  @Transactional(readOnly = true)
  public Page<DpBilling> search(Long companyId, String stage, String insurer, Pageable pageable) {
    return billings.search(companyId, stage, insurer, pageable);
  }

  /**
   * Accounts of a billing.
   *
   * @param id billing
   * @return accounts
   */
  @Transactional(readOnly = true)
  public List<DpItem> itemsOf(Long id) {
    require(id);
    return items.findByBillingIdOrderByIdAsc(id);
  }

  /**
   * Mirrors the work case stage on the billing.
   *
   * @param event transition
   */
  @EventListener
  public void onTransition(WorkCaseTransitioned event) {
    if (ENTITY.equals(event.entityType())) {
      billings
          .findById(Long.valueOf(event.entityId()))
          .ifPresent(b -> b.mirrorStage(event.toStage()));
    }
  }
}
