package com.iortatechnxt.brokerverse.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The identity events are append-only in the database (V2823): what was received never changes and
 * no row is removed; only the outcome of a reprocessing is recorded.
 */
@IntegrationTest
class IdentityEventAppendOnlyIT {

  private static final String REFUSED = "idn_identity_event rows are append-only";

  @Autowired private JdbcTemplate jdbc;

  @Test
  void theReceivedEventCannotBeChangedOrRemovedButItsOutcomeCan() {
    String userId = "a" + UUID.randomUUID().toString().replace("-", "").substring(0, 9);
    Long id =
        jdbc.queryForObject(
            "insert into idn_identity_event (received_at, source, event_type, user_id,"
                + " uidm_request_no, payload, status) values (now(), 'UIDM_ISC', 'JOINER', ?,"
                + " 'REQ-1', '{}', 'REFUSED') returning id",
            Long.class,
            userId);

    jdbc.update(
        "update idn_identity_event set status = 'APPLIED', message = 'Reprocessed',"
            + " processed_at = now(), processed_by = 'tester', attempts = attempts + 1"
            + " where id = ?",
        id);
    assertThat(
            jdbc.queryForObject(
                "select status from idn_identity_event where id = ?", String.class, id))
        .isEqualTo("APPLIED");

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "update idn_identity_event set payload = '{\"tampered\":true}' where id = ?",
                    id))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining(REFUSED);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "update idn_identity_event set uidm_request_no = 'REQ-2' where id = ?", id))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining(REFUSED);
    assertThatThrownBy(() -> jdbc.update("delete from idn_identity_event where id = ?", id))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining(REFUSED);
    // The dismissals reference the events (V2828), so a truncate is refused before the guard runs.
    assertThatThrownBy(() -> jdbc.execute("truncate idn_identity_event"))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(() -> jdbc.execute("truncate idn_identity_event cascade"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining(REFUSED);
  }
}
