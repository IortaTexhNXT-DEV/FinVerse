package com.iortatechnxt.brokerverse.collections.common.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Attributes of the Collections LOV values (V1000). */
public interface LovAttributeRepository extends JpaRepository<LovAttribute, Long> {

  /**
   * Attributes of one LOV value.
   *
   * @param typeCode LOV type
   * @param code value code
   * @return attributes
   */
  List<LovAttribute> findByTypeCodeAndCode(String typeCode, String code);

  /**
   * Attributes of every value of a type.
   *
   * @param typeCode LOV type
   * @return attributes by code and attribute
   */
  List<LovAttribute> findByTypeCodeOrderByCodeAscAttributeAsc(String typeCode);

  /**
   * One attribute.
   *
   * @param typeCode LOV type
   * @param code value code
   * @param attribute attribute
   * @return attribute
   */
  Optional<LovAttribute> findByTypeCodeAndCodeAndAttribute(
      String typeCode, String code, String attribute);
}
