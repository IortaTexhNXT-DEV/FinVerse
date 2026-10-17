package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Choices of the comparative tables. */
public interface TermsSettingRepository extends JpaRepository<TermsSetting, Long> {

  /**
   * The choices of a record.
   *
   * @param recordType record type
   * @param recordId record id
   * @return choices, empty when none were made
   */
  Optional<TermsSetting> findByRecordTypeAndRecordId(String recordType, Long recordId);
}
