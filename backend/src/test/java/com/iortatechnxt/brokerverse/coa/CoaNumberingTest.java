package com.iortatechnxt.brokerverse.coa;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.coa.domain.CoaNumbering;
import org.junit.jupiter.api.Test;

/** Child account numbers of a numbering scheme (FRBS 2.3.2). */
class CoaNumberingTest {

  @Test
  void codesArePaddedAndParsedBack() {
    CoaNumbering dotted = new CoaNumbering(1L, "1210", ".", 2);
    assertThat(dotted.codeOf(7)).isEqualTo("1210.07");
    assertThat(dotted.codeOf(12)).isEqualTo("1210.12");
    assertThat(dotted.sequenceOf("1210.07")).isEqualTo(7);
    assertThat(dotted.sequenceOf("1210.7")).isEqualTo(-1);
    assertThat(dotted.sequenceOf("1211.07")).isEqualTo(-1);
    assertThat(dotted.sequenceOf("1210.AB")).isEqualTo(-1);

    CoaNumbering plain = new CoaNumbering(1L, "56", "", 3);
    assertThat(plain.codeOf(1)).isEqualTo("56001");
    assertThat(plain.sequenceOf("56014")).isEqualTo(14);
    plain.change("-", 1, false);
    assertThat(plain.codeOf(3)).isEqualTo("56-3");
    assertThat(plain.isActive()).isFalse();
  }
}
