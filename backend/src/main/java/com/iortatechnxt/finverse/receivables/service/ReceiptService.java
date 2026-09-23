package com.iortatechnxt.finverse.receivables.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.currency.domain.RateType;
import com.iortatechnxt.finverse.currency.service.CurrencyService;
import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.period.service.PeriodService;
import com.iortatechnxt.finverse.receivables.api.dto.AllocationRequest;
import com.iortatechnxt.finverse.receivables.api.dto.ReceiptRequest;
import com.iortatechnxt.finverse.receivables.api.dto.ReversalRequest;
import com.iortatechnxt.finverse.receivables.domain.AllocationMethod;
import com.iortatechnxt.finverse.receivables.domain.PayerType;
import com.iortatechnxt.finverse.receivables.domain.PdcRepository;
import com.iortatechnxt.finverse.receivables.domain.PdcStatus;
import com.iortatechnxt.finverse.receivables.domain.PostDatedCheque;
import com.iortatechnxt.finverse.receivables.domain.Receipt;
import com.iortatechnxt.finverse.receivables.domain.ReceiptMode;
import com.iortatechnxt.finverse.receivables.domain.ReceiptRepository;
import com.iortatechnxt.finverse.receivables.domain.ReceiptSearch;
import com.iortatechnxt.finverse.receivables.domain.ReceiptValues;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entry and inquiry of official receipts (maker side). Approval, cancellation and bounced cheques
 * are handled by {@link ReceiptPostingService}.
 */
@Service
@Transactional
public class ReceiptService {

  static final String ENTITY = "Receipt";

  private final ReceiptRepository receipts;
  private final PdcRepository pdcs;
  private final PdcStatusRecorder pdcStatus;
  private final AllocationPlanner planner;
  private final PartyService parties;
  private final BankAccountDirectory banks;
  private final OrganizationService organization;
  private final CurrencyService currencies;
  private final PeriodService periods;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param receipts receipt repository
   * @param pdcs PDC repository
   * @param pdcStatus PDC status recorder
   * @param planner allocation planner
   * @param parties party service
   * @param banks bank account directory
   * @param organization organization service
   * @param currencies currency service
   * @param periods period service
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser current user
   */
  public ReceiptService(
      ReceiptRepository receipts,
      PdcRepository pdcs,
      PdcStatusRecorder pdcStatus,
      AllocationPlanner planner,
      PartyService parties,
      BankAccountDirectory banks,
      OrganizationService organization,
      CurrencyService currencies,
      PeriodService periods,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser) {
    this.receipts = receipts;
    this.pdcs = pdcs;
    this.pdcStatus = pdcStatus;
    this.planner = planner;
    this.parties = parties;
    this.banks = banks;
    this.organization = organization;
    this.currencies = currencies;
    this.periods = periods;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
  }

  /**
   * Enters a receipt (pending approval). Manual allocations are validated now and matched at
   * approval; FIFO allocations are computed at approval.
   *
   * @param r request
   * @return receipt
   */
  public Receipt create(ReceiptRequest r) {
    if (r.mode() == ReceiptMode.PDC) {
      throw new BusinessRuleException(
          "USE_PDC_REGISTER", "Register post-dated cheques in the PDC register");
    }
    if (r.mode() == ReceiptMode.CHEQUE && (isBlank(r.instrumentNo()) || isBlank(r.draweeBank()))) {
      throw new BusinessRuleException(
          "CHEQUE_DETAILS_REQUIRED", "Cheque number and drawee bank are required for cheques");
    }
    Payer payer = payer(r);
    AllocationMethod method =
        r.payerType().hasParty() ? r.allocationMethod() : AllocationMethod.NONE;
    ReceiptValues values =
        new ReceiptValues(
            r.companyId(),
            r.branchId(),
            null,
            r.receiptDate(),
            r.payerType(),
            payer.partyId(),
            payer.partyCode(),
            payer.name(),
            blankToNull(r.department()),
            r.mode(),
            blankToNull(r.instrumentNo()),
            r.mode() == ReceiptMode.CHEQUE && r.instrumentDate() == null
                ? r.receiptDate()
                : r.instrumentDate(),
            blankToNull(r.draweeBank()),
            r.currency(),
            null,
            r.amount(),
            null,
            r.bankAccountCode(),
            r.payerType() == PayerType.OTHER ? r.incomeAccountCode() : null,
            method,
            blankToNull(r.narration()),
            null);
    Receipt receipt = open(values);
    if (method == AllocationMethod.MANUAL) {
      addManualAllocations(receipt, r.allocations());
    }
    return receipt;
  }

  /**
   * Converts a post-dated cheque into a receipt when it is banked (pending approval). The receipt
   * is allocated to the linked debit note when it is still open, otherwise first-in-first-out.
   *
   * @param pdc cheque
   * @param date deposit date (receipt date)
   * @return receipt
   */
  public Receipt createFromPdc(PostDatedCheque pdc, LocalDate date) {
    Party party = parties.getByCode(pdc.getCompanyId(), pdc.getPartyCode());
    ReceiptValues values =
        new ReceiptValues(
            pdc.getCompanyId(),
            pdc.getBranchId(),
            null,
            date,
            PayerType.of(party.getPartyType()),
            pdc.getPartyId(),
            pdc.getPartyCode(),
            pdc.getPayerName(),
            pdc.getDepartment(),
            ReceiptMode.PDC,
            pdc.getChequeNo(),
            pdc.getChequeDate(),
            pdc.getDraweeBank(),
            pdc.getCurrency(),
            null,
            pdc.getAmount(),
            null,
            pdc.getBankAccountCode(),
            null,
            linkedItemOpen(pdc) ? AllocationMethod.MANUAL : AllocationMethod.FIFO,
            "PDC " + pdc.getPdcNo() + (pdc.getNarration() == null ? "" : " " + pdc.getNarration()),
            pdc.getId());
    Receipt receipt = open(values);
    receipt.markDeposited(date);
    if (receipt.getAllocationMethod() == AllocationMethod.MANUAL) {
      OpenItem item =
          planner.openDebits(pdc.getCompanyId(), pdc.getPartyId(), null).stream()
              .filter(i -> i.getId().equals(pdc.getDebitItemId()))
              .findFirst()
              .orElseThrow();
      addManualAllocations(
          receipt,
          List.of(new AllocationRequest(item.getId(), item.outstanding().min(pdc.getAmount()))));
    }
    return receipt;
  }

  /**
   * Gets a receipt with its allocations.
   *
   * @param id id
   * @return receipt
   */
  @Transactional(readOnly = true)
  public Receipt get(Long id) {
    return receipts
        .findWithAllocationsById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Searches receipts.
   *
   * @param criteria criteria
   * @param page page
   * @return receipts
   */
  @Transactional(readOnly = true)
  public Page<Receipt> search(ReceiptSearch criteria, Pageable page) {
    return receipts.findAll(criteria.toSpecification(), page);
  }

  /**
   * Lists the open debit items of a party (allocation screen).
   *
   * @param companyId company
   * @param partyCode party
   * @param currency currency filter (null = all)
   * @return items oldest due first
   */
  @Transactional(readOnly = true)
  public List<OpenItem> openDebitItems(Long companyId, String partyCode, String currency) {
    Party party = parties.getByCode(companyId, partyCode);
    return planner.openDebits(companyId, party.getId(), blankToNull(currency));
  }

  /**
   * Rejects a pending receipt (checker). A receipt raised from a PDC puts the cheque back to DUE.
   *
   * @param id receipt
   * @param request date and reason
   * @return receipt
   */
  public Receipt reject(Long id, ReversalRequest request) {
    Receipt r = get(id);
    r.reject(currentUser.username(), request.date(), request.reason());
    if (r.getPdcId() != null) {
      PostDatedCheque pdc = pdcs.findById(r.getPdcId()).orElseThrow();
      pdcStatus.transition(
          pdc, PdcStatus.DUE, request.date(), "Receipt rejected: " + request.reason(), null);
      pdc.linkReceipt(null);
    }
    audit.record(ENTITY, r.getReceiptNo(), AuditAction.REJECT, request.reason());
    return r;
  }

  private Receipt open(ReceiptValues v) {
    Branch branch = organization.requireActiveBranch(v.branchId());
    if (!branch.getCompany().getId().equals(v.companyId())) {
      throw new BusinessRuleException("INVALID_BRANCH", "Branch belongs to another company");
    }
    periods.requirePostingPeriod(v.companyId(), v.receiptDate(), true);
    banks.require(v.companyId(), v.bankAccountCode(), v.currency());
    String base = organization.getCompany(v.companyId()).getBaseCurrency();
    BigDecimal rate = currencies.rateOn(base, v.currency(), RateType.SPOT, v.receiptDate());
    String no = numbers.next("OR-" + branch.getCode() + "-" + v.receiptDate().getYear());
    Receipt receipt =
        receipts.save(new Receipt(v.numbered(no, rate, Money.convert(v.amount(), rate))));
    audit.record(
        ENTITY,
        no,
        AuditAction.CREATE,
        "Receipt of " + v.currency() + " " + v.amount() + " from " + v.payerName());
    return receipt;
  }

  private void addManualAllocations(Receipt receipt, List<AllocationRequest> requests) {
    planner
        .manual(receipt.getPartyId(), receipt.getCurrency(), receipt.getAmount(), requests)
        .forEach(
            p -> receipt.addAllocation(p.item().getId(), p.item().getDocumentNo(), p.amount()));
  }

  private boolean linkedItemOpen(PostDatedCheque pdc) {
    return pdc.getDebitItemId() != null
        && planner.openDebits(pdc.getCompanyId(), pdc.getPartyId(), pdc.getCurrency()).stream()
            .anyMatch(i -> i.getId().equals(pdc.getDebitItemId()));
  }

  private Payer payer(ReceiptRequest r) {
    if (!r.payerType().hasParty()) {
      if (isBlank(r.payerName()) || isBlank(r.incomeAccountCode())) {
        throw new BusinessRuleException(
            "PAYER_DETAILS_REQUIRED", "Payer name and income account are required");
      }
      banks.requireIncomeAccount(r.companyId(), r.incomeAccountCode());
      return new Payer(null, null, r.payerName().trim());
    }
    if (isBlank(r.partyCode())) {
      throw new BusinessRuleException("PAYER_DETAILS_REQUIRED", "Select the paying party");
    }
    Party party = parties.requireActive(r.companyId(), r.partyCode(), r.payerType().partyTypes());
    return new Payer(party.getId(), party.getCode(), party.getName());
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static String blankToNull(String s) {
    return isBlank(s) ? null : s.trim();
  }

  private record Payer(Long partyId, String partyCode, String name) {}
}
