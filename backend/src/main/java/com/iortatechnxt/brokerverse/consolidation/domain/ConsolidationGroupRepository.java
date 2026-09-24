package com.iortatechnxt.brokerverse.consolidation.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link ConsolidationGroup}. */
public interface ConsolidationGroupRepository extends JpaRepository<ConsolidationGroup, Long> {

  /** Members are always shown with the group. */
  String MEMBERS = "members";

  /**
   * Lists groups with members.
   *
   * @return groups ordered by code
   */
  @EntityGraph(attributePaths = MEMBERS, type = EntityGraphType.LOAD)
  List<ConsolidationGroup> findAllByOrderByCode();

  /**
   * Finds a group with members.
   *
   * @param id id
   * @return group
   */
  @Override
  @EntityGraph(attributePaths = MEMBERS, type = EntityGraphType.LOAD)
  Optional<ConsolidationGroup> findById(Long id);

  /**
   * Finds a group by code.
   *
   * @param code code
   * @return group
   */
  @EntityGraph(attributePaths = MEMBERS, type = EntityGraphType.LOAD)
  Optional<ConsolidationGroup> findByCode(String code);

  /**
   * Checks whether a code is taken.
   *
   * @param code code
   * @return true when taken
   */
  boolean existsByCode(String code);
}
