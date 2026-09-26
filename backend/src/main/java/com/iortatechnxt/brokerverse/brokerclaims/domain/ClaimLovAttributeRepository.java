package com.iortatechnxt.brokerverse.brokerclaims.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Attributes of the Claims list values ({@code bcl_lov_attribute}, BRCLM.010/014). */
public interface ClaimLovAttributeRepository extends JpaRepository<ClaimLovAttribute, Long> {

  /**
   * Every attribute of one list, e.g. all status attributes for a set-up screen or a cache.
   *
   * @param typeCode list
   * @return attributes by code and name
   */
  List<ClaimLovAttribute> findByTypeCodeOrderByCodeAscAttributeAsc(String typeCode);

  /**
   * One attribute of one value.
   *
   * @param typeCode list
   * @param code value code
   * @param attribute attribute name
   * @return the attribute when set
   */
  Optional<ClaimLovAttribute> findByTypeCodeAndCodeAndAttribute(
      String typeCode, String code, String attribute);

  /**
   * Values of a list whose attribute has a given value, e.g. the statuses flagged {@code
   * awaiting_premium_remittance = true} or the statuses of phase {@code TEMP_CLOSED}.
   *
   * @param typeCode list
   * @param attribute attribute name
   * @param value attribute value
   * @return attributes (their codes are the matching values)
   */
  List<ClaimLovAttribute> findByTypeCodeAndAttributeAndValueIgnoreCase(
      String typeCode, String attribute, String value);
}
