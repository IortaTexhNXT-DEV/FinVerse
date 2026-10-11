package com.iortatechnxt.brokerverse.receivables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.receivables.domain.StatementLayoutValues;
import com.iortatechnxt.brokerverse.receivables.service.StatementFileImportService;
import com.iortatechnxt.brokerverse.receivables.service.StatementFileImportService.FileImport;
import com.iortatechnxt.brokerverse.receivables.service.StatementFileImportService.Result;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestCompanies;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Spreadsheet statements with a column layout per bank account (FRBS 3.3.1) and the cheque number
 * and amount rule (FRBS 3.3.2), in a company of its own.
 */
@IntegrationTest
class StatementFileImportIT {

  private static final String BANK = "1101";

  @Autowired private StatementFileImportService service;
  @Autowired private TestCompanies companies;
  @Autowired private AsUser as;

  @Test
  void layoutMapsTheFileAndChequeNumberMatchesOutsideTheDateWindow() {
    Long company = companies.create("TGLR", "PHP").getId();
    companies.openYear(company, 2030);
    BigDecimal amount = new BigDecimal("500.00");
    companies.post(
        company,
        LocalDate.of(2030, 5, 2),
        "PHP",
        List.of(
            new JournalLineRequest(
                "5603",
                BalanceSide.DEBIT,
                amount,
                null,
                null,
                null,
                "FIN",
                null,
                null,
                null,
                "Office rent"),
            new JournalLineRequest(
                BANK,
                BalanceSide.CREDIT,
                amount,
                null,
                null,
                null,
                null,
                null,
                null,
                "004567",
                "Check 004567 to landlord")));

    assertThatThrownBy(
            () ->
                as.run(
                    "fmanager",
                    () ->
                        service.saveLayout(
                            company,
                            BANK,
                            new StatementLayoutValues(
                                "No amounts",
                                "Txn Date",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null))))
        .isInstanceOf(BusinessRuleException.class);
    as.run(
        "fmanager",
        () ->
            service.saveLayout(
                company,
                BANK,
                new StatementLayoutValues(
                    "Bank export",
                    "Txn Date",
                    "Details",
                    "Check No",
                    null,
                    null,
                    "Amount",
                    "Running Balance",
                    "MM/dd/yyyy")));
    as.run(
        "fmanager",
        () -> {
          service.setChequeNumberFirst(company, BANK, true);
          return null;
        });
    assertThat(service.chequeNumberFirst(company, BANK)).isTrue();
    assertThat(service.layouts(company)).hasSize(1);

    String file =
        "Txn Date,Details,Check No,Amount,Running Balance\n"
            + "06/05/2030,Check encashed,4567,-500.00,\n"
            + "06/06/2030,\"Deposit, branch\",,1250.00,\n";
    Result result =
        as.run(
            "fmanager",
            () ->
                service.importFile(
                    new FileImport(
                        company,
                        BANK,
                        "june.csv",
                        file.getBytes(StandardCharsets.UTF_8),
                        "TGLR-JUNE",
                        BigDecimal.ZERO)));
    assertThat(result.statement().getLineCount()).isEqualTo(2);
    assertThat(result.statement().getClosingBalance()).isEqualByComparingTo("750.00");
    assertThat(result.matched()).isEqualTo(1);

    String bad = "Txn Date,Details,Check No,Amount,Running Balance\n13/45/2030,x,,1.00,\n";
    assertThatThrownBy(
            () ->
                as.run(
                    "fmanager",
                    () ->
                        service.importFile(
                            new FileImport(
                                company,
                                BANK,
                                "bad.csv",
                                bad.getBytes(StandardCharsets.UTF_8),
                                "TGLR-BAD",
                                null))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("invalid date");
  }
}
