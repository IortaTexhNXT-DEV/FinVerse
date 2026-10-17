package com.iortatechnxt.brokerverse.receivables.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.currency.domain.RateType;
import com.iortatechnxt.brokerverse.currency.service.CurrencyService;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.receivables.api.dto.DateRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.PdcReplaceRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.PdcRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.ReversalRequest;
import com.iortatechnxt.brokerverse.receivables.domain.PayerType;
import com.iortatechnxt.brokerverse.receivables.domain.PdcEvent;
import com.iortatechnxt.brokerverse.receivables.domain.PdcEventRepository;
import com.iortatechnxt.brokerverse.receivables.domain.PdcRepository;
import com.iortatechnxt.brokerverse.receivables.domain.PdcStatus;
import com.iortatechnxt.brokerverse.receivables.domain.PdcValues;
import com.iortatechnxt.brokerverse.receivables.domain.PostDatedCheque;
import com.iortatechnxt.brokerverse.receivables.domain.Receipt;
import com.iortatechnxt.brokerverse.receivables.domain.ReceiptStatus;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.subledger.service.OpenItemService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Register of post-dated cheques received.
 *
 * <p>Accounting treatment: a PDC on hand is a memorandum item (no GL posting). On maturity the
 * cheque is banked and converted into an official receipt (mode PDC), approved and accounted for
 * like any receipt; clearing only confirms the realisation, a bounce reverses the receipt.
 */
@Service
@Transactional
public class PdcService {

  private final PdcRepository pdcs;
  private final PdcEventRepository events;
  private final PdcStatusRecorder status;
  private final ReceiptService receipts;
  private final ReceiptPostingService posting;
  private final PartyService parties;
  private final OpenItemService openItems;
  private final BankAccountDirectory banks;
  private final OrganizationService organization;
  private final CurrencyService currencies;
  private final DocumentNumberService numbers;

  /**
   * Creates the service.
   *
   * @param pdcs PDC repository
   * @param events PDC event repository
   * @param status status recorder
   * @param receipts receipt service
   * @param posting receipt posting service
   * @param parties party service
   * @param openItems open item service
   * @param banks bank account directory
   * @param organization organization service
   * @param currencies currency service
   * @param numbers document numbers
   */
  public PdcService(
      PdcRepository pdcs,
      PdcEventRepository events,
      PdcStatusRecorder status,
      ReceiptService receipts,
      ReceiptPostingService posting,
      PartyService parties,
      OpenItemService openItems,
      BankAccountDirectory banks,
      OrganizationService organization,
      CurrencyService currencies,
      DocumentNumberService numbers) {
    this.pdcs = pdcs;
    this.events = events;
    this.status = status;
    this.receipts = receipts;
    this.posting = posting;
    this.parties = parties;
    this.openItems = openItems;
    this.banks = banks;
    this.organization = organization;
    this.currencies = currencies;
    this.numbers = numbers;
  }

  /**
   * Registers a cheque received (ON_HAND).
   *
   * @param r request
   * @return cheque
   */
  public PostDatedCheque register(PdcRequest r) {
    if (r.chequeDate().isBefore(r.receivedDate())) {
      throw new BusinessRuleException(
          "INVALID_PDC_DATE", "The cheque date cannot precede the received date");
    }
    Branch branch = organization.requireActiveBranch(r.branchId());
    Party party = parties.requireActive(r.companyId(), r.partyCode(), PayerType.debtorPartyTypes());
    banks.require(r.companyId(), r.bankAccountCode(), r.currency());
    if (r.debitItemId() != null) {
      requireLinkable(openItems.get(r.debitItemId()), party, r.currency());
    }
    String base = organization.getCompany(r.companyId()).getBaseCurrency();
    BigDecimal rate = currencies.rateOn(base, r.currency(), RateType.SPOT, r.receivedDate());
    PostDatedCheque pdc =
        pdcs.save(
            new PostDatedCheque(
                new PdcValues(
                    r.companyId(),
                    r.branchId(),
                    numbers.next("PDC-" + branch.getCode() + "-" + r.receivedDate().getYear()),
                    r.receivedDate(),
                    party.getId(),
                    party.getCode(),
                    party.getName(),
                    r.department() == null || r.department().isBlank() ? null : r.department(),
                    r.chequeNo().trim(),
                    r.chequeDate(),
                    r.draweeBank().trim(),
                    r.currency(),
                    rate,
                    r.amount(),
                    Money.convert(r.amount(), rate),
                    r.bankAccountCode(),
                    r.debitItemId(),
                    r.narration())));
    status.registered(pdc);
    return pdc;
  }

  /**
   * Gets a cheque.
   *
   * @param id id
   * @return cheque
   */
  @Transactional(readOnly = true)
  public PostDatedCheque get(Long id) {
    return pdcs.findById(id).orElseThrow(() -> new ResourceNotFoundException("PDC", id));
  }

  /**
   * Lists cheques.
   *
   * @param companyId company
   * @param statusFilter status (null = all)
   * @return cheques
   */
  @Transactional(readOnly = true)
  public List<PostDatedCheque> list(Long companyId, PdcStatus statusFilter) {
    return statusFilter == null
        ? pdcs.findByCompanyIdOrderByChequeDateDescIdDesc(companyId)
        : pdcs.findByCompanyIdAndStatusInOrderByChequeDateAscIdAsc(companyId, Set.of(statusFilter));
  }

  /**
   * Status history (confirmation audit trail) of a cheque.
   *
   * @param id cheque
   * @return events oldest first
   */
  @Transactional(readOnly = true)
  public List<PdcEvent> history(Long id) {
    return events.findByPdcIdOrderByIdAsc(get(id).getId());
  }

  /**
   * Marks cheques on hand whose cheque date has been reached as DUE (to be banked).
   *
   * @param companyId company
   * @param asOf date
   * @return cheques marked due
   */
  public List<PostDatedCheque> markDue(Long companyId, LocalDate asOf) {
    List<PostDatedCheque> due =
        pdcs.findByCompanyIdAndStatusAndChequeDateLessThanEqual(companyId, PdcStatus.ON_HAND, asOf);
    due.forEach(p -> status.transition(p, PdcStatus.DUE, asOf, "Due to be banked", null));
    return due;
  }

  /**
   * Banks a cheque: converts it into a receipt (pending approval) and marks it DEPOSITED.
   *
   * @param id cheque
   * @param request deposit date and remarks
   * @return the receipt raised
   */
  public Receipt deposit(Long id, DateRequest request) {
    PostDatedCheque pdc = get(id);
    if (request.date().isBefore(pdc.getChequeDate())) {
      throw new BusinessRuleException(
          "PDC_NOT_DUE", "Cheque " + pdc.getChequeNo() + " is dated " + pdc.getChequeDate());
    }
    if (!pdc.getStatus().isHeld()) {
      throw new BusinessRuleException("PDC_STATUS", "PDC " + pdc.getPdcNo() + " is not on hand");
    }
    Receipt receipt = receipts.createFromPdc(pdc, request.date());
    pdc.linkReceipt(receipt.getId());
    status.transition(
        pdc, PdcStatus.DEPOSITED, request.date(), request.remarks(), receipt.getReceiptNo());
    return receipt;
  }

  /**
   * Confirms that the bank realised the cheque.
   *
   * @param id cheque
   * @param request clearing date
   * @return cheque
   */
  public PostDatedCheque clear(Long id, DateRequest request) {
    PostDatedCheque pdc = get(id);
    Receipt receipt = requireApprovedReceipt(pdc);
    status.transition(
        pdc, PdcStatus.CLEARED, request.date(), request.remarks(), receipt.getReceiptNo());
    return pdc;
  }

  /**
   * Records a dishonoured cheque: the receipt raised on deposit is reversed.
   *
   * @param id cheque
   * @param request date and reason
   * @return cheque
   */
  public PostDatedCheque bounce(Long id, ReversalRequest request) {
    PostDatedCheque pdc = get(id);
    if (pdc.getStatus() != PdcStatus.DEPOSITED) {
      throw new BusinessRuleException(
          "PDC_STATUS", "Only a deposited cheque can bounce (" + pdc.getStatus() + ")");
    }
    posting.bounce(requireApprovedReceipt(pdc).getId(), request);
    return pdc;
  }

  /**
   * Returns a cheque on hand to the customer.
   *
   * @param id cheque
   * @param request date and reason
   * @return cheque
   */
  public PostDatedCheque returnCheque(Long id, ReversalRequest request) {
    PostDatedCheque pdc = get(id);
    status.transition(pdc, PdcStatus.RETURNED, request.date(), request.reason(), null);
    return pdc;
  }

  /**
   * Replaces a cheque on hand by a new one (new number, date or amount).
   *
   * @param id cheque
   * @param r replacement cheque
   * @return the new cheque
   */
  public PostDatedCheque replace(Long id, PdcReplaceRequest r) {
    PostDatedCheque old = get(id);
    status.transition(old, PdcStatus.REPLACED, r.date(), r.remarks(), null);
    PostDatedCheque replacement =
        register(
            new PdcRequest(
                old.getCompanyId(),
                old.getBranchId(),
                r.date(),
                old.getPartyCode(),
                old.getDepartment(),
                r.chequeNo(),
                r.chequeDate(),
                r.draweeBank(),
                old.getCurrency(),
                r.amount(),
                old.getBankAccountCode(),
                old.getDebitItemId(),
                "Replaces " + old.getPdcNo()));
    old.replacedBy(replacement.getId());
    return replacement;
  }

  private Receipt requireApprovedReceipt(PostDatedCheque pdc) {
    if (pdc.getReceiptId() == null) {
      throw new BusinessRuleException(
          "PDC_NOT_DEPOSITED", "PDC " + pdc.getPdcNo() + " has not been banked");
    }
    Receipt receipt = receipts.get(pdc.getReceiptId());
    if (receipt.getStatus() != ReceiptStatus.APPROVED) {
      throw new BusinessRuleException(
          "RECEIPT_NOT_APPROVED",
          "Receipt " + receipt.getReceiptNo() + " of the cheque is " + receipt.getStatus());
    }
    return receipt;
  }

  private static void requireLinkable(OpenItem item, Party party, String currency) {
    if (item.getDirection() != ItemDirection.DEBIT
        || !Objects.equals(item.getPartyId(), party.getId())
        || !item.getCurrency().equals(currency)) {
      throw new BusinessRuleException(
          "INVALID_ALLOCATION",
          "Item " + item.getDocumentNo() + " is not a " + currency + " debit item of the payer");
    }
  }
}
