package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Revision history of package insurer responses. */
public interface PackageResponseHistoryRepository
    extends JpaRepository<PackageResponseHistory, Long> {

  /**
   * Snapshots of the given responses, newest first.
   *
   * @param responseIds responses
   * @return snapshots
   */
  List<PackageResponseHistory> findByResponseIdInOrderByIdDesc(Collection<Long> responseIds);
}
