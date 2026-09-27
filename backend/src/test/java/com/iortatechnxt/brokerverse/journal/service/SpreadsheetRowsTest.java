package com.iortatechnxt.brokerverse.journal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpreadsheetRowsTest {

  @Test
  void parsesQuotedFieldsLineBreaksAndBom() {
    String text = "\uFEFFa,b,c\r\n\"x, y\",\"say \"\"hi\"\"\",\"two\nlines\"\n\n1,,3";
    assertThat(SpreadsheetRows.parseCsv(text))
        .containsExactly(
            List.of("a", "b", "c"),
            List.of("x, y", "say \"hi\"", "two\nlines"),
            List.of("1", "", "3"));
  }

  @Test
  void templatesRoundTrip() {
    LocalDate date = LocalDate.of(2026, 5, 4);
    List<List<String>> csv = SpreadsheetRows.read("t.csv", JournalUploadTemplate.csv(date), 100);
    List<List<String>> xlsx = SpreadsheetRows.read("t.XLSX", JournalUploadTemplate.xlsx(date), 100);
    assertThat(csv.get(0)).isEqualTo(UploadLine.ALL_COLUMNS);
    assertThat(csv).hasSize(5);
    assertThat(xlsx.get(1).get(3)).isEqualTo("2026-05-04");
    assertThat(xlsx.get(1).subList(0, 9)).isEqualTo(csv.get(1).subList(0, 9));
  }

  @Test
  void rejectsUnsupportedUnreadableAndOversizedFiles() {
    byte[] bytes = "a\nb\nc".getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> SpreadsheetRows.read("data.txt", bytes, 10))
        .extracting("code")
        .isEqualTo("UNSUPPORTED_FILE");
    assertThatThrownBy(() -> SpreadsheetRows.read(null, bytes, 10))
        .extracting("code")
        .isEqualTo("UNSUPPORTED_FILE");
    assertThatThrownBy(() -> SpreadsheetRows.read("data.xlsx", bytes, 10))
        .extracting("code")
        .isEqualTo("UNREADABLE_FILE");
    assertThatThrownBy(() -> SpreadsheetRows.read("data.csv", bytes, 2))
        .extracting("code")
        .isEqualTo("TOO_MANY_ROWS");
  }

  @Test
  void rowValidation() {
    UploadLine ok =
        UploadLine.parse(
            2,
            java.util.Map.of(
                "voucher_key",
                "V1",
                "account_code",
                "1111",
                "credit",
                "1,000.50",
                "value_date",
                "2026-01-31",
                "journal_type",
                "accrual",
                "line_currency",
                "usd",
                "exchange_rate",
                "57.5"));
    assertThat(ok.valid()).isTrue();
    assertThat(ok.line().amount()).isEqualByComparingTo("1000.50");
    assertThat(ok.line().currency()).isEqualTo("USD");
    assertThat(UploadLine.journalType("accrual").name()).isEqualTo("ACCRUAL");
    assertThat(UploadLine.journalType("").name()).isEqualTo("MANUAL");

    UploadLine bad =
        UploadLine.parse(
            3,
            java.util.Map.of(
                "debit",
                "10.123",
                "credit",
                "5",
                "value_date",
                "31/01/2026",
                "journal_type",
                "CLOSING",
                "currency",
                "PESO",
                "exchange_rate",
                "-1"));
    assertThat(bad.valid()).isFalse();
    assertThat(bad.line()).isNull();
    assertThat(bad.errors())
        .anyMatch(e -> e.startsWith("voucher_key"))
        .anyMatch(e -> e.startsWith("account_code"))
        .anyMatch(e -> e.startsWith("debit"))
        .anyMatch(e -> e.startsWith("value_date"))
        .anyMatch(e -> e.startsWith("journal_type"))
        .anyMatch(e -> e.startsWith("currency"))
        .anyMatch(e -> e.startsWith("exchange_rate"));
    assertThat(
            UploadLine.parse(4, java.util.Map.of("voucher_key", "V", "account_code", "1")).errors())
        .contains("enter either a debit or a credit amount greater than zero");
  }
}
