package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Quotation options of the comparative tables. */
public interface TermsOptionRepository extends JpaRepository<TermsOption, Long> {

  /**
   * The options of a record.
   *
   * @param recordType record type
   * @param recordId record id
   * @return options by insurer and number
   */
  List<TermsOption> findByRecordTypeAndRecordIdOrderByInsurerCodeAscOptionNoAsc(
      String recordType, Long recordId);
}
