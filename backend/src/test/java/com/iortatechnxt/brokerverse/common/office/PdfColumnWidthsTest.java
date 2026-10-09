package com.iortatechnxt.brokerverse.common.office;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.lowagie.text.Font;
import org.junit.jupiter.api.Test;

class PdfColumnWidthsTest {

  private static final Font FONT = new Font(Font.HELVETICA, 8, Font.BOLD);

  @Test
  void aNarrowColumnIsWidenedToItsLongestHeadingWordAndTheOthersShareTheRest() {
    float word = PdfColumnWidths.longestWord("Commission", FONT);
    float[] widths =
        new PdfColumnWidths(new float[] {1, 10}, 4f)
            .heading(0, "Realized Commission", FONT)
            .fit(200);
    assertThat(widths[0]).isCloseTo(word + 4f + 3f, within(0.01f));
    assertThat(widths[0] + widths[1]).isCloseTo(200f, within(0.01f));
  }

  @Test
  void widthsFollowTheWeightsWhenEveryWordFits() {
    float[] widths = new PdfColumnWidths(new float[] {1, 3}, 4f).heading(0, "No.", FONT).fit(400);
    assertThat(widths[0]).isCloseTo(100f, within(0.01f));
    assertThat(widths[1]).isCloseTo(300f, within(0.01f));
  }

  @Test
  void aVeryLongValueDoesNotTakeTheWholeTable() {
    float[] widths =
        new PdfColumnWidths(new float[] {1, 9}, 4f).value(0, "A".repeat(200), FONT).fit(300);
    assertThat(widths[0]).isCloseTo(300 * 0.3f + 4f + 3f, within(0.01f));
  }

  @Test
  void aDateIsOnePartAReferenceBreaksAfterItsHyphens() {
    assertThat(PdfColumnWidths.longestPart("02-Oct-2026", FONT))
        .isEqualTo(PdfColumnWidths.longestWord("02-Oct-2026", FONT));
    assertThat(PdfColumnWidths.longestPart("BI-HO-2026-000001", FONT))
        .isEqualTo(PdfColumnWidths.longestWord("000001", FONT));
  }
}
