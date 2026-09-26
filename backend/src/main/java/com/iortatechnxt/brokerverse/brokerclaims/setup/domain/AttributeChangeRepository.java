package com.iortatechnxt.brokerverse.brokerclaims.setup.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Attribute changes of the Claims lists ({@code bcl_attribute_change}). */
public interface AttributeChangeRepository extends JpaRepository<AttributeChange, Long> {

  /**
   * The changes of one value in a record status.
   *
   * @param typeCode list
   * @param code value
   * @param status record status
   * @return changes
   */
  List<AttributeChange> findByTypeCodeAndCodeAndRecordStatus(
      String typeCode, String code, RecordStatus status);

  /**
   * The changes of a list in a record status.
   *
   * @param typeCode list
   * @param status record status
   * @return changes
   */
  List<AttributeChange> findByTypeCodeAndRecordStatus(String typeCode, RecordStatus status);
}
