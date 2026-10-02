package com.iortatechnxt.brokerverse.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iortatechnxt.brokerverse.cache.service.CacheValueSerializer;
import com.iortatechnxt.brokerverse.lov.service.LovEntries;
import com.iortatechnxt.brokerverse.organization.service.OrganizationDirectory.BranchRef;
import com.iortatechnxt.brokerverse.security.service.RolePermissionLookup.RolePermissions;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.SerializationException;

/** JSON round trip of cached values on Redis, and the type allow-list. */
class CacheValueSerializerTest {

  private final CacheValueSerializer serializer =
      new CacheValueSerializer(
          new ObjectMapper()
              .registerModule(new JavaTimeModule())
              .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

  @Test
  void recordsWithNestedListsAndDatesRoundTrip() {
    LovEntries entries =
        new LovEntries(
            "MARKET_SEGMENT",
            true,
            List.of(
                new LovEntries.Entry(
                    "RETAIL", "Retail", null, true, LocalDate.of(2026, 1, 1), null),
                new LovEntries.Entry(
                    "SME",
                    "Small",
                    "RETAIL",
                    false,
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2030, 12, 31))));
    Object back = serializer.deserialize(serializer.serialize(entries));
    assertThat(back).isEqualTo(entries);
    assertThat(((LovEntries) back).entries()).isUnmodifiable();
  }

  @Test
  void listsSetsAndScalarsRoundTrip() {
    List<BranchRef> branches =
        List.of(
            new BranchRef(1L, 2L, "HO", "Head office", true, true),
            new BranchRef(3L, 2L, "MKT", "Makati", false, true));
    assertThat(serializer.deserialize(serializer.serialize(branches))).isEqualTo(branches);
    assertThat(serializer.deserialize(serializer.serialize(Set.of("A")))).isEqualTo(Set.of("A"));
    assertThat(serializer.deserialize(serializer.serialize(List.of()))).isEqualTo(List.of());
    assertThat(serializer.deserialize(serializer.serialize("30"))).isEqualTo("30");
    assertThat(serializer.deserialize(serializer.serialize(new BigDecimal("1.50"))))
        .isEqualTo(new BigDecimal("1.50"));
    RolePermissions permissions = new RolePermissions("AUDITOR", List.of("AUDIT_VIEW"));
    assertThat(serializer.deserialize(serializer.serialize(permissions))).isEqualTo(permissions);
    assertThat(serializer.serialize(null)).isEmpty();
    assertThat(serializer.deserialize(new byte[0])).isNull();
  }

  @Test
  void typesOutsideTheAllowListAreRefused() {
    byte[] hostile =
        "{\"t\":\"java.lang.ProcessBuilder\",\"v\":{}}".getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> serializer.deserialize(hostile))
        .isInstanceOf(SerializationException.class);
    byte[] hostileList =
        "{\"t\":\"list\",\"e\":\"java.net.URL\",\"v\":[\"http://x\"]}"
            .getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> serializer.deserialize(hostileList))
        .isInstanceOf(SerializationException.class);
    assertThatThrownBy(() -> serializer.deserialize("not json".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(SerializationException.class);
  }
}
