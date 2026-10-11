package com.iortatechnxt.brokerverse.payables.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.Scope;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.payables.domain.ApprovableDocument;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.domain.InvoiceStatus;
import com.iortatechnxt.brokerverse.payables.domain.PaymentVoucherRepository;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashDisbursementRepository;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashDocument;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashFund;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashFundRepository;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashReimbursementRepository;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashStatus;
import com.iortatechnxt.brokerverse.payables.domain.SupplierInvoiceRepository;
import com.iortatechnxt.brokerverse.payables.domain.VoucherStatus;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Approval inbox source of payables.
 *
 * <ul>
 *   <li>Supplier invoices, payment vouchers, petty cash disbursements and reimbursement claims
 *       pending approval ({@code RECEIPT_PAYMENT_AUTHORIZE}). As on approval, the creator and the
 *       submitter never see their own documents, and documents above the viewer's authorization
 *       limit (in base currency) are not offered.
 *   <li>Bank accounts and petty cash funds pending authorization ({@code MASTER_AUTHORIZE}).
 * </ul>
 */
@Component
public class PayablesApprovalSource implements PendingApprovalSource {

  /** Module code of payables items. */
  public static final String MODULE = PayablesSupport.MODULE;

  private static final String PERMISSION = "RECEIPT_PAYMENT_AUTHORIZE";
  private static final String PETTY_CASH_ROUTE = "/payables/petty-cash";

  private final SupplierInvoiceRepository invoices;
  private final PaymentVoucherRepository vouchers;
  private final PettyCashDisbursementRepository disbursements;
  private final PettyCashReimbursementRepository reimbursements;
  private final PettyCashFundRepository funds;
  private final MasterRecordApprovals records;
  private final PayablesSupport support;
  private final UserDirectory users;

  /**
   * Creates the source.
   *
   * @param invoices supplier invoices
   * @param vouchers payment vouchers
   * @param disbursements petty cash vouchers
   * @param reimbursements petty cash reimbursement claims
   * @param funds petty cash funds (currency)
   * @param records master record helper (bank accounts, funds)
   * @param support payables helper (base currency conversion)
   * @param users user facts (authorization limits)
   */
  public PayablesApprovalSource(
      SupplierInvoiceRepository invoices,
      PaymentVoucherRepository vouchers,
      PettyCashDisbursementRepository disbursements,
      PettyCashReimbursementRepository reimbursements,
      PettyCashFundRepository funds,
      MasterRecordApprovals records,
      PayablesSupport support,
      UserDirectory users) {
    this.invoices = invoices;
    this.vouchers = vouchers;
    this.disbursements = disbursements;
    this.reimbursements = reimbursements;
    this.funds = funds;
    this.records = records;
    this.support = support;
    this.users = users;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> items = new ArrayList<>();
    Scope masterData = new Scope(MODULE, MasterRecordApprovals.PERMISSION);
    items.addAll(
        records.pending(viewer, masterData, BankAccount.class, PayablesApprovalSource::facts));
    items.addAll(
        records.pending(viewer, masterData, PettyCashFund.class, PayablesApprovalSource::facts));
    if (viewer.can(PERMISSION)) {
      BigDecimal limit =
          viewer.systemView() ? null : users.authorizationLimit(viewer.username()).orElse(null);
      Checker checker = new Checker(viewer, limit);
      items.addAll(invoices(checker));
      items.addAll(vouchers(checker));
      items.addAll(pettyCash(checker));
    }
    return items;
  }

  private List<PendingApproval> invoices(Checker checker) {
    return invoices.findByStatusOrderById(InvoiceStatus.PENDING_APPROVAL).stream()
        .filter(
            i ->
                checker.mayApprove(
                    i,
                    () ->
                        support.toBase(
                            i.getCompanyId(),
                            i.getCurrency(),
                            i.getPayableAmount(),
                            i.getInvoiceDate())))
        .map(
            i ->
                new PendingApproval(
                    MODULE,
                    "Supplier invoice",
                    i.getDocumentNo(),
                    i.getPartyCode() + " " + i.getSupplierInvoiceNo(),
                    i.getPayableAmount(),
                    i.getCurrency(),
                    i.maker(),
                    i.getSubmittedAt(),
                    i.getCompanyId(),
                    "/payables/invoices"))
        .toList();
  }

  private List<PendingApproval> vouchers(Checker checker) {
    return vouchers.findByStatusOrderById(VoucherStatus.PENDING_APPROVAL).stream()
        .filter(
            v ->
                checker.mayApprove(
                    v,
                    () ->
                        support.toBase(
                            v.getCompanyId(), v.getCurrency(), v.getAmount(), v.getVoucherDate())))
        .map(
            v ->
                new PendingApproval(
                    MODULE,
                    "Payment voucher",
                    v.getVoucherNo(),
                    v.getPayeeName(),
                    v.getAmount(),
                    v.getCurrency(),
                    v.maker(),
                    v.getSubmittedAt(),
                    v.getCompanyId(),
                    "/payables/vouchers"))
        .toList();
  }

  private List<PendingApproval> pettyCash(Checker checker) {
    Map<Long, String> currencies = new HashMap<>();
    List<PendingApproval> items = new ArrayList<>();
    disbursements.findByStatusOrderById(PettyCashStatus.PENDING_APPROVAL).stream()
        .filter(d -> checker.mayApprove(d, d::getAmount))
        .map(d -> pettyCashItem(d, "Petty cash voucher", d.getPayee(), d.getAmount(), currencies))
        .forEach(items::add);
    reimbursements.findByStatusOrderById(PettyCashStatus.PENDING_APPROVAL).stream()
        .filter(r -> checker.mayApprove(r, r::getAmount))
        .map(
            r ->
                pettyCashItem(
                    r, "Petty cash reimbursement", r.getNarration(), r.getAmount(), currencies))
        .forEach(items::add);
    return items;
  }

  private PendingApproval pettyCashItem(
      PettyCashDocument doc,
      String type,
      String description,
      BigDecimal amount,
      Map<Long, String> currencies) {
    return new PendingApproval(
        MODULE,
        type,
        doc.getDocumentNo(),
        description,
        amount,
        currencies.computeIfAbsent(
            doc.getFundId(), id -> funds.findById(id).map(PettyCashFund::getCurrency).orElse(null)),
        doc.maker(),
        doc.getCreatedAt(),
        doc.getCompanyId(),
        PETTY_CASH_ROUTE);
  }

  private static RecordFacts facts(BankAccount b) {
    return new RecordFacts(
        "Bank account", b.getCode(), b.getName(), b.getCompanyId(), "/payables/bank-accounts");
  }

  private static RecordFacts facts(PettyCashFund f) {
    return new RecordFacts(
        "Petty cash fund", f.getCode(), f.getName(), f.getCompanyId(), PETTY_CASH_ROUTE);
  }

  /**
   * The approval checks of {@link PayablesSupport#checker}: segregation of duties and the
   * authorization limit.
   *
   * @param viewer viewer
   * @param limit viewer's authorization limit in base currency (null = unlimited)
   */
  private record Checker(ApprovalViewer viewer, BigDecimal limit) {

    boolean mayApprove(ApprovableDocument doc, Supplier<BigDecimal> baseAmount) {
      return viewer.mayApproveItemOf(doc.getCreatedBy())
          && viewer.mayApproveItemOf(doc.maker())
          && (limit == null || withinLimit(baseAmount, limit));
    }

    /**
     * A document whose base amount cannot be computed yet (missing exchange rate) stays visible:
     * the approval itself reports the missing rate.
     */
    private static boolean withinLimit(Supplier<BigDecimal> baseAmount, BigDecimal limit) {
      try {
        return baseAmount.get().compareTo(limit) <= 0;
      } catch (BusinessRuleException missingRate) {
        return true;
      }
    }
  }
}
