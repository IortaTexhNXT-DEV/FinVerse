package com.iortatechnxt.brokerverse.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.audit.service.MasterChanges;
import com.iortatechnxt.brokerverse.audit.service.MasterChanges.FieldChange;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MasterChangesTest {

  private static final String[] NAMES = {"name", "rate", "version", "recordStatus", "branches"};
  private static final boolean[] LOGGABLE = {true, true, true, true, false};

  @Test
  void keepsTheChangedBusinessFieldsOnly() {
    Object[] before = {"A", new BigDecimal("1.50"), 1L, RecordStatus.ACTIVE, "x"};
    Object[] after = {"B", new BigDecimal("1.5"), 2L, RecordStatus.PENDING_AUTHORIZATION, "y"};

    assertThat(MasterChanges.diff(NAMES, LOGGABLE, before, after))
        .containsExactly(new FieldChange("name", "A", "B"));
  }

  @Test
  void keepsADeactivation() {
    Object[] before = {"A", null, 1L, RecordStatus.ACTIVE, null};
    Object[] after = {"A", null, 2L, RecordStatus.INACTIVE, null};

    assertThat(MasterChanges.diff(NAMES, LOGGABLE, before, after))
        .containsExactly(new FieldChange("recordStatus", "ACTIVE", "INACTIVE"));
    assertThat(MasterChanges.authorizes(NAMES, before, after)).isFalse();
  }

  @Test
  void recognisesAnAuthorization() {
    Object[] before = {"A", null, 1L, RecordStatus.PENDING_AUTHORIZATION, null};
    Object[] after = {"A", null, 2L, RecordStatus.ACTIVE, null};

    assertThat(MasterChanges.authorizes(NAMES, before, after)).isTrue();
    assertThat(MasterChanges.diff(NAMES, LOGGABLE, before, after)).isEmpty();
  }

  @Test
  void anEmptyValueIsNull() {
    Object[] before = {"", null, 1L, null, null};
    Object[] after = {null, BigDecimal.TEN, 1L, null, null};

    assertThat(MasterChanges.diff(NAMES, LOGGABLE, before, after))
        .containsExactly(new FieldChange("rate", null, "10"));
  }
}
