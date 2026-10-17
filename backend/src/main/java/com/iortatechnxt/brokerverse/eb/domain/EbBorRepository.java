package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Broker on Record versions of the EB cycles. */
public interface EbBorRepository extends JpaRepository<EbBor, Long> {

  /**
   * The versions of a cycle, latest first.
   *
   * @param cycleId cycle
   * @return versions
   */
  List<EbBor> findByCycleIdOrderByVersionNoDesc(Long cycleId);

  /**
   * The versions of a programme, latest first.
   *
   * @param programmeId programme
   * @return versions
   */
  List<EbBor> findByProgrammeIdOrderByIdDesc(Long programmeId);
}
