package com.iortatechnxt.brokerverse.remittance.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Lines of remittance batches. */
public interface BatchLineRepository
    extends JpaRepository<BatchLine, Long>, JpaSpecificationExecutor<BatchLine> {

  /**
   * Lines of an invoice with their batch, newest first (invoice 360, RMTID.026).
   *
   * @param invoiceNo invoice
   * @return lines
   */
  @Query(
      "select l from BatchLine l join fetch l.batch where l.invoiceNo = :invoiceNo"
          + " order by l.id desc")
  List<BatchLine> findByInvoiceNo(@Param("invoiceNo") String invoiceNo);

  /**
   * Lines updated by an insurer OR upload run (RMTID.016).
   *
   * @param runNo upload run
   * @return lines
   */
  @Query("select l from BatchLine l join fetch l.batch where l.orRunNo = :runNo order by l.id")
  List<BatchLine> findByOrRunNo(@Param("runNo") String runNo);

  /**
   * Lines already carrying an insurer OR number for a client (duplicate OR check, RMTID.012).
   *
   * @param clientCode client
   * @param orNo OR number
   * @return lines
   */
  @Query("select l from BatchLine l where l.clientCode = :clientCode and l.insurerOrNo = :orNo")
  List<BatchLine> linesWithInsurerOr(
      @Param("clientCode") String clientCode, @Param("orNo") String orNo);
}
