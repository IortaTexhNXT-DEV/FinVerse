package com.iortatechnxt.finverse.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.support.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** The audit trail is insert-only in the database itself (V26 triggers), not only in the code. */
@IntegrationTest
class AuditTrailImmutabilityIT {

  @Autowired private AuditTrailService audit;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void updateDeleteAndTruncateAreRejected() {
    String key = "IMMUTABLE-" + UUID.randomUUID();
    audit.recordIndependently("tester", "AuditTest", key, AuditAction.CREATE, "Original entry");
    Long id = jdbc.queryForObject("select id from audit_log where entity_id = ?", Long.class, key);

    assertThatThrownBy(
            () -> jdbc.update("update audit_log set summary = 'Tampered' where id = ?", id))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("audit_log rows are immutable");
    assertThatThrownBy(() -> jdbc.update("delete from audit_log where id = ?", id))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("audit_log rows are immutable");
    assertThatThrownBy(() -> jdbc.execute("truncate audit_log"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("audit_log rows are immutable");

    assertThat(jdbc.queryForObject("select summary from audit_log where id = ?", String.class, id))
        .isEqualTo("Original entry");
  }
}
