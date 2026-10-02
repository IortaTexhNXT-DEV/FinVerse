package com.iortatechnxt.brokerverse.acsl.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Rows of the insurer SOA uploads (ACSL 2.4.0 upload log). */
public interface SoaLineRepository extends JpaRepository<SoaLine, Long> {

  /**
   * Rows of an upload in file order.
   *
   * @param uploadId upload
   * @return rows
   */
  List<SoaLine> findByUploadIdOrderByRowNo(Long uploadId);

  /**
   * Rows of an upload with a status, in file order.
   *
   * @param uploadId upload
   * @param status LOADED or FAILED
   * @return rows
   */
  List<SoaLine> findByUploadIdAndStatusOrderByRowNo(Long uploadId, String status);
}
