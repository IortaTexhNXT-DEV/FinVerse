package com.iortatechnxt.brokerverse.docgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentRenditionService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** A schedule of many columns is printed in landscape so that no word is broken. */
class DocumentLayoutTest {

  private final DocumentComposer composer =
      new DocumentComposer(
          Clock.fixed(Instant.parse("2026-09-27T00:00:00Z"), ZoneOffset.UTC),
          mock(DocumentRenditionService.class));

  private static DocumentSpec withColumns(int count) {
    List<String> headers = IntStream.range(0, count).mapToObj(i -> "Column " + i).toList();
    List<String> row = Collections.nCopies(count, "1,000.00");
    return new DocumentSpec(
        "BDO Insurance and Reinsurance Brokers, Inc.",
        "Remittance Schedule",
        "RMB-INS-MGIC-2026-000001",
        List.of(new Table("Accounts", headers, List.of(row), List.of())),
        List.of(),
        null);
  }

  private Rectangle pageOf(DocumentSpec spec) throws IOException {
    try (PdfReader reader = new PdfReader(composer.pdf(spec))) {
      return reader.getPageSizeWithRotation(1);
    }
  }

  @Test
  void aWideTableIsPrintedInLandscapeAndANarrowOneInPortrait() throws IOException {
    Rectangle wide = pageOf(withColumns(15));
    assertThat(wide.getWidth()).isGreaterThan(wide.getHeight());
    Rectangle narrow = pageOf(withColumns(4));
    assertThat(narrow.getWidth()).isLessThan(narrow.getHeight());
  }
}
