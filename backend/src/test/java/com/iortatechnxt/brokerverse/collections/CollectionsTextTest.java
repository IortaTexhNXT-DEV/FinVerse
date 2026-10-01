package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.collections.bulk.service.CollectionsBulkUpdateHandler;
import com.iortatechnxt.brokerverse.collections.common.service.ClxText;
import com.iortatechnxt.brokerverse.collections.disposition.service.DispositionSupport;
import com.iortatechnxt.brokerverse.collections.disposition.service.PrDispositionService;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The texts a Collections user reads (screen standards): dates, times and amounts as the screens
 * show them, codes in words, and refusals without permission, role or list codes.
 */
class CollectionsTextTest {

  /**
   * A code as stored: two or more capitals joined by underscores (CLX_ESCALATE, MKT_TL, PERMANENT).
   */
  private static final String CODE =
      ".*\\b([A-Z]{2,}_[A-Z_]+|PERMANENT|TEMPORARY|CASH|CERTIFICATE)\\b.*";

  @Test
  void datesTimesAndAmountsReadAsOnTheScreens() {
    assertThat(ClxText.date(LocalDate.of(2026, 10, 5))).isEqualTo("05-Oct-2026");
    assertThat(ClxText.date(null)).isEmpty();
    assertThat(ClxText.dateTime(Instant.parse("2026-10-05T00:00:00Z")))
        .isEqualTo("05-Oct-2026 08:00");
    assertThat(ClxText.amount("PHP", new BigDecimal("1000000"))).isEqualTo("PHP 1,000,000.00");
    assertThat(ClxText.amount("PHP", new BigDecimal("5567.175"))).isEqualTo("PHP 5,567.18");
    assertThat(ClxText.amount(null, BigDecimal.TEN)).isEqualTo("10.00");
  }

  @Test
  void codesReadInWords() {
    assertThat(ClxText.words("APPLY_TO_INVOICE")).isEqualTo("Apply to invoice");
    assertThat(ClxText.words("APPLIED")).isEqualTo("Applied");
    assertThat(ClxText.words(null)).isEmpty();
  }

  @Test
  void refusalsNameNoPermissionRoleOrListCode() {
    List<String> texts =
        List.of(
            PrDispositionService.RESERVED,
            DispositionSupport.BULK_NOT_ALLOWED,
            AssignmentService.REASSIGN_KIND,
            CollectionsBulkUpdateHandler.NOT_ALLOWED_TO_ESCALATE);
    assertThat(texts).allSatisfy(text -> assertThat(text).doesNotMatch(CODE));
    assertThat(PrDispositionService.RESERVED)
        .isEqualTo("This disposition is reserved to other roles");
    assertThat(AssignmentService.REASSIGN_KIND).contains("permanent").contains("temporary");
  }
}
