package com.iortatechnxt.brokerverse.nbadmin.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The status of a bulk batch mirrors its lines (BRD 1.009). */
class AccessRequestBatchTest {

  @Test
  void statusMirrorsTheLines() {
    assertThat(AccessRequestBatch.statusOf(List.of())).isEqualTo(AccessBatchStatus.DRAFT);
    assertThat(
            AccessRequestBatch.statusOf(
                List.of(AccessRequestStatus.DRAFT, AccessRequestStatus.CANCELLED)))
        .isEqualTo(AccessBatchStatus.DRAFT);
    assertThat(
            AccessRequestBatch.statusOf(
                List.of(AccessRequestStatus.PENDING_SECOND, AccessRequestStatus.APPROVED)))
        .isEqualTo(AccessBatchStatus.PENDING);
    assertThat(AccessRequestBatch.statusOf(List.of(AccessRequestStatus.RETURNED)))
        .isEqualTo(AccessBatchStatus.RETURNED);
    assertThat(AccessRequestBatch.statusOf(List.of(AccessRequestStatus.CANCELLED)))
        .isEqualTo(AccessBatchStatus.CANCELLED);
    assertThat(AccessRequestBatch.statusOf(List.of(AccessRequestStatus.REJECTED)))
        .isEqualTo(AccessBatchStatus.REJECTED);
    assertThat(
            AccessRequestBatch.statusOf(
                List.of(AccessRequestStatus.APPROVED, AccessRequestStatus.SCHEDULED)))
        .isEqualTo(AccessBatchStatus.APPROVED);
    assertThat(
            AccessRequestBatch.statusOf(
                List.of(AccessRequestStatus.APPROVED, AccessRequestStatus.REJECTED)))
        .isEqualTo(AccessBatchStatus.PARTIAL);
    AccessRequestBatch batch = new AccessRequestBatch("BLK-1", "f.csv");
    batch.addLine();
    batch.mirror(List.of(AccessRequestStatus.APPROVED));
    assertThat(batch.getLines()).isEqualTo(1);
    assertThat(batch.getStatus()).isEqualTo(AccessBatchStatus.APPROVED);
  }
}
