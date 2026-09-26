package com.iortatechnxt.brokerverse.brokerclaims.insurer.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurer reserve history (BRCLM.023/024); insert-only. */
public interface ReserveChangeRepository extends JpaRepository<ReserveChange, Long> {

  /**
   * The reserve history of insurer lines, newest first.
   *
   * @param insurerClaimIds insurer lines
   * @return amendments
   */
  List<ReserveChange> findByInsurerClaimIdInOrderByChangedAtDescIdDesc(
      Collection<Long> insurerClaimIds);
}
