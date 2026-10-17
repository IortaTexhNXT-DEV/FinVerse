package com.iortatechnxt.brokerverse.renewal.audit.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Every entry of the Renewal audit logs shows its time, whatever type the database returns. */
class AuditEntryTimeTest {

  private static final Instant AT = Instant.parse("2026-10-10T04:05:00Z");

  @Test
  void timestampsWithAndWithoutTimeZoneAreRead() {
    assertThat(RenewalAuditLogService.instant(Timestamp.from(AT))).isEqualTo(AT);
    assertThat(RenewalAuditLogService.instant(OffsetDateTime.ofInstant(AT, ZoneOffset.UTC)))
        .isEqualTo(AT);
    assertThat(RenewalAuditLogService.instant(AT)).isEqualTo(AT);
    assertThat(RenewalAuditLogService.instant(null)).isNull();
  }

  @Test
  void epochMillisOfTheQueryAreRead() {
    // the report reader turns a timestamp column into a date, so the query also returns the time as
    // millis
    assertThat(RenewalAuditLogService.instant(AT.toEpochMilli())).isEqualTo(AT);
  }
}
