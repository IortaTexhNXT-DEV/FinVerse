package com.iortatechnxt.brokerverse.consolidation.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link IntercompanyRelationship}. */
public interface IntercompanyRelationshipRepository
    extends JpaRepository<IntercompanyRelationship, Long> {

  /**
   * Lists the relationships a company takes part in.
   *
   * @param companyId company
   * @return relationships
   */
  @Query(
      "select r from IntercompanyRelationship r"
          + " where r.companyAId = :companyId or r.companyBId = :companyId order by r.id")
  List<IntercompanyRelationship> findInvolving(@Param("companyId") Long companyId);

  /**
   * Finds the relationship of a pair in either orientation.
   *
   * @param first one company
   * @param second the other company
   * @return relationships (at most one)
   */
  @Query(
      "select r from IntercompanyRelationship r"
          + " where (r.companyAId = :first and r.companyBId = :second)"
          + " or (r.companyAId = :second and r.companyBId = :first)")
  List<IntercompanyRelationship> findPair(@Param("first") Long first, @Param("second") Long second);

  /**
   * Lists all relationships ordered by id.
   *
   * @return relationships
   */
  List<IntercompanyRelationship> findAllByOrderById();
}
