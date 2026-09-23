package com.iortatechnxt.finverse.receivables.api;

import com.iortatechnxt.finverse.receivables.service.BankAccountDirectory;
import com.iortatechnxt.finverse.receivables.service.BankAccountDirectory.BankAccount;
import com.iortatechnxt.finverse.receivables.service.ReceiptService;
import com.iortatechnxt.finverse.subledger.api.dto.OpenItemResponse;
import com.iortatechnxt.finverse.subledger.service.AgeingService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Look-ups of the receivables screens: bank accounts, open debit items and ageing slot labels. The
 * party statement screen is the sub-ledger one of the GL menu ({@code /gl/party-statement}); the
 * matched / unmatched statement of a period is report FIN-ARAP-SOA-MATCH.
 */
@RestController
@RequestMapping("/api/v1/receivables")
public class ReceivablesController {

  private final BankAccountDirectory banks;
  private final ReceiptService receipts;
  private final AgeingService ageing;

  /**
   * Creates the controller.
   *
   * @param banks bank account directory
   * @param receipts receipt service
   * @param ageing sub-ledger ageing (default slots)
   */
  public ReceivablesController(
      BankAccountDirectory banks, ReceiptService receipts, AgeingService ageing) {
    this.banks = banks;
    this.receipts = receipts;
    this.ageing = ageing;
  }

  /**
   * Lists the bank and cash GL accounts that can receive money.
   *
   * @param companyId company
   * @return bank accounts
   */
  @GetMapping("/bank-accounts")
  @PreAuthorize(ReceivablesAccess.VIEW_OR_RECONCILE)
  public List<BankAccount> bankAccounts(@RequestParam Long companyId) {
    return banks.list(companyId);
  }

  /**
   * Lists the open debit items (debit notes) of a party for allocation.
   *
   * @param companyId company
   * @param partyCode party
   * @param currency currency filter
   * @return items oldest due first
   */
  @GetMapping("/open-items")
  @PreAuthorize(ReceivablesAccess.VIEW)
  public List<OpenItemResponse> openItems(
      @RequestParam Long companyId,
      @RequestParam String partyCode,
      @RequestParam(required = false) String currency) {
    return receipts.openDebitItems(companyId, partyCode, currency).stream()
        .map(OpenItemResponse::from)
        .toList();
  }

  /**
   * Ageing slot labels for a slot definition (UI preview).
   *
   * @param slots slot text, e.g. "30,60,90,120" (blank = company default)
   * @return labels
   */
  @GetMapping("/ageing-slots")
  @PreAuthorize(ReceivablesAccess.VIEW_OR_REPORT)
  public List<String> ageingSlots(@RequestParam(required = false) String slots) {
    return ageing.slotsOrDefault(slots).labels();
  }
}
