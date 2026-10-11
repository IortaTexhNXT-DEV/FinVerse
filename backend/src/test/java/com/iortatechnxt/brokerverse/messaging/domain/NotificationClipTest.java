package com.iortatechnxt.brokerverse.messaging.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** A notice longer than its columns is kept, cut at a word, rather than refused. */
class NotificationClipTest {

  @Test
  void aLongTitleIsCutAtAWordWithAnEllipsis() {
    String title = "Security setting to approve: " + "password rules ".repeat(20);
    Notification n =
        new Notification("admin", new Notice(title, "body", null, null, null), Instant.EPOCH);
    assertThat(n.getTitle()).hasSizeLessThanOrEqualTo(Notification.TITLE_LENGTH).endsWith("…");
    assertThat(n.getTitle()).doesNotEndWith(" …");
    assertThat(Notification.clip("short", 200)).isEqualTo("short");
    assertThat(Notification.clip(null, 200)).isNull();
  }
}
