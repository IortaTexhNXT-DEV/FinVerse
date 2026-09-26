package com.iortatechnxt.brokerverse.brokerclaims.location.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Insurer location references (BRCLM.042). */
public interface LocationRefRepository extends JpaRepository<LocationRef, Long> {

  /**
   * Every reference of a cover, current and past, by location, insurer and start.
   *
   * @param companyId company
   * @param arn account reference number
   * @return references
   */
  List<LocationRef> findByCompanyIdAndArnOrderByAccountItemNoAscInsurerCodeAscEffectiveFromDesc(
      Long companyId, String arn);

  /**
   * The open (not end-dated) reference of a location and insurer.
   *
   * @param companyId company
   * @param arn account reference number
   * @param itemNo item number
   * @param insurerCode insurer
   * @return the open reference
   */
  Optional<LocationRef> findByCompanyIdAndArnAndAccountItemNoAndInsurerCodeAndEffectiveToIsNull(
      Long companyId, String arn, int itemNo, String insurerCode);

  /**
   * References by ARN, insurer, reference or location key fragment, newest first (the maintenance
   * screen).
   *
   * @param companyId company
   * @param text lower-case text with wildcards ({@code %} for all)
   * @param pageable page
   * @return references
   */
  @Query(
      "select r from LocationRef r where r.companyId = :companyId"
          + " and (lower(r.arn) like :text or lower(r.insurerCode) like :text"
          + " or lower(r.insurerLocationRef) like :text or lower(r.locationKey) like :text)"
          + " order by r.arn, r.accountItemNo, r.insurerCode, r.effectiveFrom desc")
  Page<LocationRef> search(
      @Param("companyId") Long companyId, @Param("text") String text, Pageable pageable);
}
