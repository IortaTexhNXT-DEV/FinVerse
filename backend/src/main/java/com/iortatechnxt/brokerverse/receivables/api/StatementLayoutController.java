package com.iortatechnxt.brokerverse.receivables.api;

import com.iortatechnxt.brokerverse.receivables.api.dto.StatementFileImportResponse;
import com.iortatechnxt.brokerverse.receivables.api.dto.StatementLayoutRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.StatementLayoutResponse;
import com.iortatechnxt.brokerverse.receivables.service.StatementFileImportService;
import com.iortatechnxt.brokerverse.receivables.service.StatementFileImportService.FileImport;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Spreadsheet bank statements (FRBS 3.3.1): column layouts per bank account, the cheque-number
 * matching rule (FRBS 3.3.2) and the file import that triggers the auto reconciliation.
 */
@RestController
@RequestMapping("/api/v1/receivables/bank-rec")
@PreAuthorize("hasAuthority('RECONCILIATION_MANAGE')")
public class StatementLayoutController {

  private final StatementFileImportService service;

  /**
   * Creates the controller.
   *
   * @param service layouts and import
   */
  public StatementLayoutController(StatementFileImportService service) {
    this.service = service;
  }

  /**
   * Layouts of a company.
   *
   * @param companyId company
   * @return layouts
   */
  @GetMapping("/layouts")
  public List<StatementLayoutResponse> layouts(@RequestParam Long companyId) {
    return service.layouts(companyId).stream()
        .map(
            l ->
                StatementLayoutResponse.from(
                    l, service.chequeNumberFirst(companyId, l.getBankAccountCode())))
        .toList();
  }

  /**
   * Creates or changes the layout and matching rule of a bank account.
   *
   * @param request layout
   * @return layout
   */
  @PutMapping("/layouts")
  public StatementLayoutResponse saveLayout(@Valid @RequestBody StatementLayoutRequest request) {
    var layout =
        service.saveLayout(request.companyId(), request.bankAccountCode(), request.values());
    service.setChequeNumberFirst(
        request.companyId(), request.bankAccountCode(), request.chequeNumberFirst());
    return StatementLayoutResponse.from(layout, request.chequeNumberFirst());
  }

  /**
   * Imports a spreadsheet statement (.xlsx, .ods or .csv) and auto-matches it.
   *
   * @param companyId company
   * @param bankAccountCode GL bank account
   * @param statementRef statement reference (defaults to the file name)
   * @param openingBalance balance before the first line
   * @param file statement file
   * @return imported statement and number of matches
   * @throws IOException when the file cannot be read
   */
  @PostMapping(value = "/statements/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public StatementFileImportResponse importFile(
      @RequestParam Long companyId,
      @RequestParam String bankAccountCode,
      @RequestParam(required = false) String statementRef,
      @RequestParam(required = false) BigDecimal openingBalance,
      @RequestParam MultipartFile file)
      throws IOException {
    return StatementFileImportResponse.from(
        service.importFile(
            new FileImport(
                companyId,
                bankAccountCode,
                file.getOriginalFilename(),
                file.getBytes(),
                statementRef,
                openingBalance)));
  }
}
