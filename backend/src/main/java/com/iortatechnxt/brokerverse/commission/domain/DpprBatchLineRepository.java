package com.iortatechnxt.brokerverse.commission.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Lines of the legacy direct payment PR reversal batches. */
public interface DpprBatchLineRepository extends JpaRepository<DpprBatchLine, Long> {

  /**
   * Lines of a batch.
   *
   * @param batchId batch
   * @return lines by number
   */
  List<DpprBatchLine> findByBatchIdOrderByLineNoAsc(Long batchId);

  /**
   * Whether an invoice is on a live batch.
   *
   * @param invoiceNo invoice
   * @return true when a line not refused is on a batch not cancelled
   */
  @Query(
      "select count(l) > 0 from DpprBatchLine l, DpprBatch b where b.id = l.batchId"
          + " and l.invoiceNo = ?1 and l.status <> 'FAILED'"
          + " and b.status <> com.iortatechnxt.brokerverse.commission.domain.DpprBatch.Status.CANCELLED")
  boolean taken(String invoiceNo);
}
