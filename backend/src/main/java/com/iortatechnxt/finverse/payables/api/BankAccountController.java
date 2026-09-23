package com.iortatechnxt.finverse.payables.api;

import com.iortatechnxt.finverse.payables.api.dto.BankAccountRequest;
import com.iortatechnxt.finverse.payables.api.dto.BankAccountResponse;
import com.iortatechnxt.finverse.payables.api.dto.ChequeBookRequest;
import com.iortatechnxt.finverse.payables.api.dto.ChequeBookResponse;
import com.iortatechnxt.finverse.payables.service.BankAccountQueryService;
import com.iortatechnxt.finverse.payables.service.BankAccountService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Company bank accounts and cheque books. */
@RestController
@RequestMapping("/api/v1/payables/bank-accounts")
public class BankAccountController {

  private final BankAccountService service;
  private final BankAccountQueryService query;

  /**
   * Creates the controller.
   *
   * @param service maintenance service
   * @param query read service
   */
  public BankAccountController(BankAccountService service, BankAccountQueryService query) {
    this.service = service;
    this.query = query;
  }

  /**
   * Lists bank accounts.
   *
   * @param companyId company
   * @param activeOnly only authorized accounts
   * @param currency currency filter (active accounts only)
   * @return accounts
   */
  @GetMapping
  @PreAuthorize(PayablesAccess.MASTER_VIEW)
  public List<BankAccountResponse> list(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "false") boolean activeOnly,
      @RequestParam(required = false) String currency) {
    var accounts =
        activeOnly || currency != null
            ? query.listActive(companyId, currency)
            : query.list(companyId);
    return accounts.stream().map(BankAccountResponse::from).toList();
  }

  /**
   * Gets a bank account.
   *
   * @param id id
   * @return account
   */
  @GetMapping("/{id}")
  @PreAuthorize(PayablesAccess.MASTER_VIEW)
  public BankAccountResponse get(@PathVariable Long id) {
    return BankAccountResponse.from(query.get(id));
  }

  /**
   * Creates a bank account.
   *
   * @param request request
   * @return account
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(PayablesAccess.MASTER_MAINTAIN)
  public BankAccountResponse create(@Valid @RequestBody BankAccountRequest request) {
    return BankAccountResponse.from(service.create(request.toCommand()));
  }

  /**
   * Updates a bank account.
   *
   * @param id id
   * @param request request
   * @return account
   */
  @PutMapping("/{id}")
  @PreAuthorize(PayablesAccess.MASTER_MAINTAIN)
  public BankAccountResponse update(
      @PathVariable Long id, @Valid @RequestBody BankAccountRequest request) {
    return BankAccountResponse.from(service.update(id, request.toCommand()));
  }

  /**
   * Authorizes a bank account.
   *
   * @param id id
   * @return account
   */
  @PostMapping("/{id}/authorize")
  @PreAuthorize(PayablesAccess.MASTER_AUTHORIZE)
  public BankAccountResponse authorize(@PathVariable Long id) {
    return BankAccountResponse.from(service.authorize(id));
  }

  /**
   * Lists the cheque books of a bank account.
   *
   * @param id bank account
   * @return books
   */
  @GetMapping("/{id}/cheque-books")
  @PreAuthorize(PayablesAccess.MASTER_VIEW)
  public List<ChequeBookResponse> chequeBooks(@PathVariable Long id) {
    return query.chequeBooks(id).stream().map(ChequeBookResponse::from).toList();
  }

  /**
   * Registers a cheque book.
   *
   * @param id bank account
   * @param request range
   * @return book
   */
  @PostMapping("/{id}/cheque-books")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(PayablesAccess.MASTER_MAINTAIN)
  public ChequeBookResponse addChequeBook(
      @PathVariable Long id, @Valid @RequestBody ChequeBookRequest request) {
    return ChequeBookResponse.from(
        service.addChequeBook(id, request.firstNo(), request.lastNo(), request.receivedOn()));
  }

  /**
   * Withdraws a cheque book.
   *
   * @param bookId book
   * @return book
   */
  @PostMapping("/cheque-books/{bookId}/cancel")
  @PreAuthorize(PayablesAccess.MASTER_MAINTAIN)
  public ChequeBookResponse cancelChequeBook(@PathVariable Long bookId) {
    return ChequeBookResponse.from(service.cancelChequeBook(bookId));
  }
}
