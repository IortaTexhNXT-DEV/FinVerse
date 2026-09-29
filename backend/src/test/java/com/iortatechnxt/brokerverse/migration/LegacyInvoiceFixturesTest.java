package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.migration.intake.service.HmacMaskingProvider;
import com.iortatechnxt.brokerverse.migration.intake.service.Masker;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MaskingRule;
import com.iortatechnxt.brokerverse.migration.matching.service.MatchKeys;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The clients of the legacy invoice fixtures stay distinct people once masked: two clients sharing
 * the masked person key (K3) merge automatically on load, and the reconciliation of the second then
 * reads back the e-mail of the first (an L4 break).
 */
class LegacyInvoiceFixturesTest {

  @Test
  void consecutiveFixtureClientsNeverShareTheMaskedPersonKey() {
    Masker masker = new Masker(new HmacMaskingProvider(UUID.randomUUID().toString()));
    long start = Long.parseLong(LegacyInvoiceFixtures.token());
    Set<String> keys = new HashSet<>();
    for (long i = 0; i < LegacyInvoiceFixtures.BIRTH_DATE_SLOTS; i++) {
      String t = Long.toString(100_000L + (start - 100_000L + i) % 900_000L);
      Map<String, String> row = LegacyInvoiceFixtures.clientRow(t);
      Map<String, String> masked = new HashMap<>(row);
      masked.put("last_name", masker.apply(MaskingRule.Kind.LAST_NAME, row.get("last_name")));
      masked.put("first_name", masker.apply(MaskingRule.Kind.FIRST_NAME, row.get("first_name")));
      masked.put("birth_date", masker.apply(MaskingRule.Kind.BIRTH_DATE, row.get("birth_date")));
      assertThat(keys.add(MatchKeys.of(masked).person())).as("person key of token %s", t).isTrue();
    }
  }
}
