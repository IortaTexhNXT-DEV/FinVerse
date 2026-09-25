package com.iortatechnxt.brokerverse.disbursement.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.disbursement.api.dto.OperationDtos.BankResponse;
import com.iortatechnxt.brokerverse.disbursement.api.dto.OperationDtos.BankStatusRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.OperationDtos.BookRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.OperationDtos.EodRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.OperationDtos.EodRunResponse;
import com.iortatechnxt.brokerverse.disbursement.domain.EodOutput;
import com.iortatechnxt.brokerverse.disbursement.domain.EodRun;
import com.iortatechnxt.brokerverse.disbursement.service.EodService;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.service.BankAccountQueryService;
import com.iortatechnxt.brokerverse.payables.service.BankAccountService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

/**
 * End of day and the paying accounts (DIS 2.16.0-2.16.6, 2.7.12, 2.23.0-2.24.2): the runs and their
 * output files, a new run for a date, the payment confirmations of a run, and the BDOIR bank
 * accounts with their check series: active / inactive tag with maker-checker, new and edited check
 * series. The bank account master itself stays in Payables.
 */
@RestController
@RequestMapping("/api/v1/disbursement")
public class OperationsController {

  private final EodService eod;
  private final BankAccountQueryService banks;
  private final BankAccountService bankService;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param eod end of day
   * @param banks bank account reads
   * @param bankService bank account maintenance
   * @param clock clock
   */
  public OperationsController(
      EodService eod, BankAccountQueryService banks, BankAccountService bankService, Clock clock) {
    this.eod = eod;
    this.banks = banks;
    this.bankService = bankService;
    this.clock = clock;
  }

  /**
   * End-of-day runs, newest date first.
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return runs
   */
  @GetMapping("/eod/runs")
  @PreAuthorize(DisbursementAccess.EOD_READ)
  public PageResponse<EodRunResponse> runs(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        eod.runs(companyId, DisbursementAccess.plain(page, size)),
        r -> EodRunResponse.from(r, List.of()));
  }

  /**
   * One run with its outputs.
   *
   * @param id run
   * @return run
   */
  @GetMapping("/eod/runs/{id}")
  @PreAuthorize(DisbursementAccess.EOD_READ)
  public EodRunResponse run(@PathVariable Long id) {
    return EodRunResponse.from(eod.get(id), eod.outputs(id));
  }

  /**
   * Runs the end of day of a date (DIS 2.16.0).
   *
   * @param body company and date
   * @return run
   */
  @PostMapping("/eod/runs")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(DisbursementAccess.EOD)
  public EodRunResponse start(@Valid @RequestBody EodRequest body) {
    EodRun run = eod.run(body.companyId(), body.businessDate());
    return EodRunResponse.from(run, eod.outputs(run.getId()));
  }

  /**
   * E-mails the payment confirmations of a run (DIS 2.7.12).
   *
   * @param id run
   * @return run
   */
  @PostMapping("/eod/runs/{id}/confirm")
  @PreAuthorize(DisbursementAccess.EOD)
  public EodRunResponse confirm(@PathVariable Long id) {
    return EodRunResponse.from(eod.confirm(id), eod.outputs(id));
  }

  /**
   * An output file of a run.
   *
   * @param outputId output
   * @return file
   */
  @GetMapping("/eod/outputs/{outputId}")
  @PreAuthorize(DisbursementAccess.EOD_READ)
  public ResponseEntity<byte[]> output(@PathVariable Long outputId) {
    EodOutput o = eod.output(outputId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(o.getFileName()))
        .contentType(MediaType.parseMediaType(o.getContentType()))
        .body(o.getContent());
  }

  /**
   * The BDOIR bank accounts with their check series (DIS 2.23.0, 2.24.0).
   *
   * @param companyId company
   * @return accounts
   */
  @GetMapping("/banks")
  @PreAuthorize(DisbursementAccess.BANK_READ)
  public List<BankResponse> banks(@RequestParam Long companyId) {
    return banks.list(companyId).stream().map(this::bank).toList();
  }

  private BankResponse bank(BankAccount a) {
    return BankResponse.from(a, banks.chequeBooks(a.getId()));
  }

  /**
   * Asks to tag a bank account active or inactive (DIS 2.24.2).
   *
   * @param id bank account
   * @param body status
   * @return account
   */
  @PostMapping("/banks/{id}/status")
  @PreAuthorize(DisbursementAccess.REVIEW)
  public BankResponse requestStatus(
      @PathVariable Long id, @Valid @RequestBody BankStatusRequest body) {
    return bank(bankService.requestStatus(id, body.status()));
  }

  /**
   * Authorises the pending change of a bank account (four eyes).
   *
   * @param id bank account
   * @return account
   */
  @PostMapping("/banks/{id}/authorize")
  @PreAuthorize(DisbursementAccess.APPROVE)
  public BankResponse authorize(@PathVariable Long id) {
    return bank(bankService.authorize(id));
  }

  /**
   * Adds the beginning check series of a new check book (DIS 2.23.1).
   *
   * @param id bank account
   * @param body range
   * @return account
   */
  @PostMapping("/banks/{id}/cheque-books")
  @PreAuthorize(DisbursementAccess.REVIEW)
  public BankResponse addBook(@PathVariable Long id, @Valid @RequestBody BookRequest body) {
    bankService.addChequeBook(
        id,
        body.firstNo(),
        body.lastNo(),
        body.receivedOn() == null ? LocalDate.now(clock) : body.receivedOn());
    return bank(banks.get(id));
  }

  /**
   * Edits the beginning check series of an unused check book (DIS 2.23.2).
   *
   * @param bookId check book
   * @param body range
   * @return account
   */
  @PutMapping("/cheque-books/{bookId}")
  @PreAuthorize(DisbursementAccess.REVIEW)
  public BankResponse editBook(@PathVariable Long bookId, @Valid @RequestBody BookRequest body) {
    return bank(
        banks.get(
            bankService.editChequeBook(bookId, body.firstNo(), body.lastNo()).getBankAccountId()));
  }
}
