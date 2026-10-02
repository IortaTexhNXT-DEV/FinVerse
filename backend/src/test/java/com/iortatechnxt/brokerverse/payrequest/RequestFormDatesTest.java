package com.iortatechnxt.brokerverse.payrequest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The Refund Request Form and the liquidation form print their dates as every BIBS document does
 * (dd-MMM-yyyy), never in the ISO form of the database. The check reads the document source.
 */
class RequestFormDatesTest {

  @Test
  void theFormsPrintTheirDatesDayMonthYear() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/com/iortatechnxt/brokerverse/payrequest/service/PayRequestDocuments.java"),
            StandardCharsets.UTF_8);
    assertThat(source)
        .doesNotContain("getRequestDate().toString()")
        .doesNotContain("getFieldworkDate().toString()")
        .contains("DisplayFormat.date(r.getRequestDate())");
  }
}
