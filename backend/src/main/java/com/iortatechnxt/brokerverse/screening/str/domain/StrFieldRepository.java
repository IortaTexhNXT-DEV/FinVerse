package com.iortatechnxt.brokerverse.screening.str.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** STR field values (SNSRP-705). */
public interface StrFieldRepository extends JpaRepository<StrField, Long> {

  /**
   * The field values of an STR.
   *
   * @param strId STR
   * @return values
   */
  List<StrField> findByStrId(Long strId);
}
