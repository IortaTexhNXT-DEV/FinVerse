package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.dimension.domain.DimensionType;
import com.iortatechnxt.finverse.dimension.service.DimensionService;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.payables.domain.AllocationValues;
import com.iortatechnxt.finverse.payables.domain.BankAccount;
import com.iortatechnxt.finverse.payables.domain.PaymentCategory;
import com.iortatechnxt.finverse.payables.domain.PaymentMode;
import com.iortatechnxt.finverse.payables.domain.PaymentVoucher;
import com.iortatechnxt.finverse.payables.domain.PaymentVoucherRepository;
import com.iortatechnxt.finverse.payables.domain.VoucherHeader;
import com.iortatechnxt.finverse.payables.domain.VoucherStatus;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment voucher capture (maker side): select open payables of a party, choose mode and bank,
 * submit for approval. Approval and posting are in {@link PaymentApprovalService}.
 */
@Service
@Transactional
public class PaymentVoucherService {

  private static final String ENTITY = "PaymentVoucher";

  private final PaymentVoucherRepository vouchers;
  private final PartyService parties;
  private final BankAccountQueryService banks;
  private final OpenItemService openItems;
  private final PayableItemGuard guard;
  private final DimensionService dimensions;
  private final PayablesSupport support;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param vouchers repository
   * @param parties party master
   * @param banks bank accounts
   * @param openItems sub-ledger
   * @param guard payable item checks
   * @param dimensions dimensions
   * @param support shared helpers
   * @param audit audit trail
   * @param clock clock
   */
  public PaymentVoucherService(
      PaymentVoucherRepository vouchers,
      PartyService parties,
      BankAccountQueryService banks,
      OpenItemService openItems,
      PayableItemGuard guard,
      DimensionService dimensions,
      PayablesSupport support,
      AuditTrailService audit,
      Clock clock) {
    this.vouchers = vouchers;
    this.parties = parties;
    this.banks = banks;
    this.openItems = openItems;
    this.guard = guard;
    this.dimensions = dimensions;
    this.support = support;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Lists the open payables of a party that can still be paid.
   *
   * @param companyId company
   * @param partyCode party
   * @param voucherId voucher being edited (its own reservations stay available), may be null
   * @return payable items, oldest due first
   */
  @Transactional(readOnly = true)
  public List<PayableItem> payableItems(Long companyId, String partyCode, Long voucherId) {
    Party party = parties.getByCode(companyId, partyCode);
    return openItems.partyItems(companyId, party.getId()).stream()
        .filter(i -> i.getDirection() == ItemDirection.CREDIT && i.outstanding().signum() > 0)
        .map(i -> new PayableItem(i, guard.available(i, voucherId)))
        .filter(p -> p.available().signum() > 0)
        .sorted((a, b) -> a.item().getDueDate().compareTo(b.item().getDueDate()))
        .toList();
  }

  /**
   * Searches vouchers.
   *
   * @param companyId company
   * @param status status or null
   * @param partyCode party or null
   * @param from from date
   * @param to to date
   * @param pageable page
   * @return page
   */
  @Transactional(readOnly = true)
  public Page<PaymentVoucher> search(
      Long companyId,
      VoucherStatus status,
      String partyCode,
      LocalDate from,
      LocalDate to,
      Pageable pageable) {
    return vouchers.search(companyId, status, partyCode, from, to, pageable);
  }

  /**
   * Gets a voucher with its allocations.
   *
   * @param id id
   * @return voucher
   */
  @Transactional(readOnly = true)
  public PaymentVoucher get(Long id) {
    return vouchers
        .findWithAllocationsById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Payment voucher", id));
  }

  /**
   * Captures a draft voucher.
   *
   * @param c values
   * @return voucher
   */
  public PaymentVoucher create(PaymentCommand c) {
    Party party = parties.getByCode(c.companyId(), c.partyCode());
    BankAccount bank = banks.requireActive(c.bankAccountId());
    List<AllocationValues> allocations =
        guard.allocations(party, bank.getCurrency(), c.items(), null);
    VoucherHeader header = header(c, party, bank, allocations);
    PaymentVoucher voucher =
        new PaymentVoucher(support.nextNumber("PV", c.branchId(), c.voucherDate()), header);
    voucher.replaceAllocations(allocations);
    PaymentVoucher saved = vouchers.save(voucher);
    audit.record(
        ENTITY,
        saved.getVoucherNo(),
        AuditAction.CREATE,
        "Payment of " + saved.getAmount() + " to " + party.getCode());
    return saved;
  }

  /**
   * Updates a draft voucher.
   *
   * @param id id
   * @param c values (party cannot change)
   * @return voucher
   */
  public PaymentVoucher update(Long id, PaymentCommand c) {
    PaymentVoucher voucher = get(id);
    Party party = parties.get(voucher.getPartyId());
    BankAccount bank = banks.requireActive(c.bankAccountId());
    List<AllocationValues> allocations =
        guard.allocations(party, bank.getCurrency(), c.items(), voucher.getId());
    voucher.updateHeader(header(c, party, bank, allocations));
    voucher.clearAllocations();
    vouchers.flush();
    voucher.replaceAllocations(allocations);
    audit.record(ENTITY, voucher.getVoucherNo(), AuditAction.UPDATE, "Updated draft voucher");
    return voucher;
  }

  /**
   * Submits a draft for approval.
   *
   * @param id id
   * @return voucher
   */
  public PaymentVoucher submit(Long id) {
    PaymentVoucher voucher = get(id);
    voucher.submit(support.user(), clock.instant());
    audit.record(ENTITY, voucher.getVoucherNo(), AuditAction.SUBMIT, "Submitted for approval");
    return voucher;
  }

  /**
   * Returns a submitted voucher to draft.
   *
   * @param id id
   * @param reason reason
   * @return voucher
   */
  public PaymentVoucher reject(Long id, String reason) {
    PaymentVoucher voucher = get(id);
    voucher.reject(support.user(), reason);
    audit.record(ENTITY, voucher.getVoucherNo(), AuditAction.REJECT, reason);
    return voucher;
  }

  /**
   * Cancels an unapproved voucher (its items become available again).
   *
   * @param id id
   * @param reason reason
   * @return voucher
   */
  public PaymentVoucher cancel(Long id, String reason) {
    PaymentVoucher voucher = get(id);
    voucher.cancel(reason);
    audit.record(ENTITY, voucher.getVoucherNo(), AuditAction.DEACTIVATE, "Cancelled: " + reason);
    return voucher;
  }

  private VoucherHeader header(
      PaymentCommand c, Party party, BankAccount bank, List<AllocationValues> allocations) {
    PaymentCategory category = category(c.category(), party, allocations);
    if (c.mode() == PaymentMode.PDC && bank.getPdcClearingAccountCode() == null) {
      throw new BusinessRuleException(
          "NO_PDC_CLEARING_ACCOUNT",
          "Bank account " + bank.getCode() + " has no PDC clearing account");
    }
    dimensions.validateOptional(c.companyId(), DimensionType.DEPARTMENT, c.department());
    String payee =
        c.payeeName() == null || c.payeeName().isBlank() ? party.getName() : c.payeeName();
    return new VoucherHeader(
        c.companyId(),
        c.branchId(),
        party.getId(),
        party.getCode(),
        payee,
        category,
        c.mode(),
        bank.getId(),
        c.voucherDate(),
        c.chequeDate(),
        bank.getCurrency(),
        c.department(),
        c.narration());
  }

  private static PaymentCategory category(
      PaymentCategory requested, Party party, List<AllocationValues> allocations) {
    Set<String> types =
        allocations.stream().map(AllocationValues::documentType).collect(Collectors.toSet());
    PaymentCategory category =
        requested != null ? requested : PaymentCategory.defaultFor(party.getPartyType(), types);
    if (category == null || !category.accepts(party.getPartyType())) {
      throw new BusinessRuleException(
          "INVALID_PAYMENT_CATEGORY",
          "A " + party.getPartyType() + " cannot receive a " + category + " payment");
    }
    return category;
  }
}
