package com.iortatechnxt.finverse.receivables.api;

import com.iortatechnxt.finverse.receivables.report.AgeingSlots;
import com.iortatechnxt.finverse.receivables.service.BankAccountDirectory;
import com.iortatechnxt.finverse.receivables.service.BankAccountDirectory.BankAccount;
import com.iortatechnxt.finverse.receivables.service.PartyStatementService;
import com.iortatechnxt.finverse.receivables.service.PartyStatementService.PartyStatement;
import com.iortatechnxt.finverse.receivables.service.ReceiptService;
import com.iortatechnxt.finverse.subledger.api.dto.OpenItemResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Look-ups of the receivables screens: bank accounts, open debit items, party statement. */
@RestController
@RequestMapping("/api/v1/receivables")
public class ReceivablesController {

  private final BankAccountDirectory banks;
  private final ReceiptService receipts;
  private final PartyStatementService statements;

  /**
   * Creates the controller.
   *
   * @param banks bank account directory
   * @param receipts receipt service
   * @param statements party statement service
   */
  public ReceivablesController(
      BankAccountDirectory banks, ReceiptService receipts, PartyStatementService statements) {
    this.banks = banks;
    this.receipts = receipts;
    this.statements = statements;
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
   * Statement of account of a party with matched and unmatched details.
   *
   * @param companyId company
   * @param partyCode party
   * @param from first document date
   * @param to last document date
   * @param foreign document currency (true) or base currency (false)
   * @return statement (empty when the party has no documents in the period)
   */
  @GetMapping("/party-statement")
  @PreAuthorize(ReceivablesAccess.VIEW_OR_REPORT)
  public List<PartyStatement> partyStatement(
      @RequestParam Long companyId,
      @RequestParam String partyCode,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "true") boolean foreign) {
    return statements.statements(
        companyId, from, to, foreign, i -> i.partyCode().equals(partyCode));
  }

  /**
   * Ageing slot labels for a slot definition (UI preview).
   *
   * @param slots slot text, e.g. "30,60,90,120"
   * @return labels
   */
  @GetMapping("/ageing-slots")
  @PreAuthorize(ReceivablesAccess.VIEW_OR_REPORT)
  public List<String> ageingSlots(@RequestParam(required = false) String slots) {
    return AgeingSlots.parse(slots).labels();
  }
}
