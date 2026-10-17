package com.iortatechnxt.brokerverse.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TextContentTest {

  @Test
  void textMayHoldLineBreaksTabsFormFeedsAndEscapes() {
    assertThat(
            TextContent.isText(
                "a\tb\r\nc\fd\u000be\u001b$Bf\u001a".getBytes(StandardCharsets.UTF_8)))
        .isTrue();
    assertThat(TextContent.isText("Caf\u00e9".getBytes(StandardCharsets.ISO_8859_1))).isTrue();
  }

  @Test
  void aNulOrABinaryControlCharacterAnywhereIsNotText() {
    byte[] late = new byte[10_000];
    Arrays.fill(late, (byte) 'a');
    late[9_000] = 0;
    assertThat(TextContent.isText(late)).isFalse();
    assertThat(TextContent.isText(new byte[] {'a', 0x07, 'b'})).isFalse();
    assertThat(TextContent.isText(new byte[] {'M', 'Z', (byte) 0x90, 0x00})).isFalse();
  }

  @Test
  void dataFilesAreStrictUtf8WithoutTheByteOrderMark() {
    assertThat(TextContent.utf8("\uFEFFCode;Name\n1;Jos\u00e9".getBytes(StandardCharsets.UTF_8)))
        .isEqualTo("Code;Name\n1;Jos\u00e9");
    assertThatThrownBy(() -> TextContent.utf8("Jos\u00e9".getBytes(StandardCharsets.ISO_8859_1)))
        .isInstanceOfSatisfying(
            BusinessRuleException.class,
            e -> assertThat(e.getCode()).isEqualTo("FILE_TEXT_ENCODING"));
    assertThatThrownBy(() -> TextContent.utf8(new byte[] {'a', 0, 'b'}))
        .isInstanceOfSatisfying(
            BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("FILE_NOT_TEXT"));
  }

  @Test
  void dataFilesAboveTheLimitAreRefused() {
    byte[] large = new byte[(int) TextContent.MAX_TEXT_BYTES + 1];
    Arrays.fill(large, (byte) 'a');

    assertThatThrownBy(() -> TextContent.utf8(large))
        .isInstanceOfSatisfying(
            BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("FILE_TOO_LARGE"));
  }
}
