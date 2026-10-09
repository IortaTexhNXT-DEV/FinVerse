package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.domain.CashBankAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.CashBankAccount.Details;
import com.iortatechnxt.brokerverse.cashiering.service.CashBankAccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The maintained list "Post to Bank Account" of Cashiering Setup (FRS.CSH.02.01.02). */
@RestController
@RequestMapping("/api/v1/cashiering/bank-accounts")
public class CashBankAccountController {

  private static final String MAINTAIN =
      "hasAnyAuthority('SYSTEM_PARAMETER_MANAGE', 'LOV_MANAGE', 'CASH_SERIES_MANAGE')";

  private final CashBankAccountService accounts;

  /**
   * Creates the controller.
   *
   * @param accounts the list
   */
  public CashBankAccountController(CashBankAccountService accounts) {
    this.accounts = accounts;
  }

  /**
   * The accounts of a company.
   *
   * @param companyId company
   * @return accounts
   */
  @GetMapping
  @PreAuthorize(CashAccess.VIEW + " or " + MAINTAIN)
  public List<BankAccountResponse> list(@RequestParam Long companyId) {
    return accounts.list(companyId).stream().map(BankAccountResponse::from).toList();
  }

  /**
   * Adds or changes an account.
   *
   * @param request account
   * @return the account
   */
  @PostMapping
  @PreAuthorize(MAINTAIN)
  public BankAccountResponse save(@Valid @RequestBody BankAccountRequest request) {
    return BankAccountResponse.from(
        accounts.save(
            request.companyId(),
            request.code(),
            new Details(
                request.name(),
                request.currency(),
                request.glAccountCode(),
                request.defaultForCurrency(),
                request.active())));
  }

  /**
   * An account to add or change.
   *
   * @param companyId company
   * @param code code
   * @param name name
   * @param currency currency
   * @param glAccountCode GL account
   * @param defaultForCurrency default of its currency
   * @param active offered on the screens
   */
  public record BankAccountRequest(
      @NotNull Long companyId,
      @NotBlank String code,
      @NotBlank String name,
      @NotBlank String currency,
      String glAccountCode,
      boolean defaultForCurrency,
      boolean active) {}

  /**
   * An account of the list.
   *
   * @param id id
   * @param code code
   * @param name name
   * @param currency currency
   * @param glAccountCode GL account
   * @param defaultForCurrency default of its currency
   * @param active offered on the screens
   */
  public record BankAccountResponse(
      Long id,
      String code,
      String name,
      String currency,
      String glAccountCode,
      boolean defaultForCurrency,
      boolean active) {

    static BankAccountResponse from(CashBankAccount a) {
      return new BankAccountResponse(
          a.getId(),
          a.getCode(),
          a.getName(),
          a.getCurrency(),
          a.getGlAccountCode(),
          a.isDefaultForCurrency(),
          a.isActive());
    }
  }
}
