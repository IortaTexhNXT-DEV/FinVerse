package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Files received by the simulated receiving system. */
public interface MasterInboxFileRepository extends JpaRepository<MasterInboxFile, Long> {

  /**
   * The latest files.
   *
   * @return files, newest first
   */
  List<MasterInboxFile> findTop50ByOrderByIdDesc();
}
