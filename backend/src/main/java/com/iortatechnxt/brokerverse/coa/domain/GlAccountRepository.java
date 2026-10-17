package com.iortatechnxt.brokerverse.coa.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link GlAccount}. */
public interface GlAccountRepository extends JpaRepository<GlAccount, Long> {

  /**
   * Associations always needed when an account is shown, loaded in the same query. LOAD semantics
   * keep the default (eager) fetching of the restriction sets.
   */
  String PARENT = "parent";

  /** See {@link #PARENT}. */
  String CATEGORY = "category";

  /**
   * Finds an account with its parent and category.
   *
   * @param id id
   * @return account if present
   */
  @Override
  @EntityGraph(
      attributePaths = {PARENT, CATEGORY},
      type = EntityGraphType.LOAD)
  Optional<GlAccount> findById(Long id);

  /**
   * Lists the chart of accounts of a company ordered by code.
   *
   * @param companyId company id
   * @return accounts
   */
  @EntityGraph(
      attributePaths = {PARENT, CATEGORY},
      type = EntityGraphType.LOAD)
  List<GlAccount> findByCompanyIdOrderByCode(Long companyId);

  /**
   * Finds an account by company and code.
   *
   * @param companyId company id
   * @param code account code
   * @return account if present
   */
  @EntityGraph(
      attributePaths = {PARENT, CATEGORY},
      type = EntityGraphType.LOAD)
  Optional<GlAccount> findByCompanyIdAndCode(Long companyId, String code);

  /**
   * Finds accounts by codes.
   *
   * @param companyId company id
   * @param codes account codes
   * @return accounts
   */
  List<GlAccount> findByCompanyIdAndCodeIn(Long companyId, Collection<String> codes);

  /**
   * Checks whether an account code is taken within a company.
   *
   * @param companyId company id
   * @param code account code
   * @return true when taken
   */
  boolean existsByCompanyIdAndCode(Long companyId, String code);

  /**
   * Checks whether an account has children.
   *
   * @param parentId parent id
   * @return true when children exist
   */
  boolean existsByParentId(Long parentId);

  /**
   * Finds an account by its short code (case-insensitive, FRBS 2.3.3 / 2.8.1).
   *
   * @param companyId company id
   * @param shortName short code
   * @return account if present
   */
  @EntityGraph(
      attributePaths = {PARENT, CATEGORY},
      type = EntityGraphType.LOAD)
  Optional<GlAccount> findByCompanyIdAndShortNameIgnoreCase(Long companyId, String shortName);

  /**
   * Accounts by short codes (case-insensitive), for journal lines keyed by short code.
   *
   * @param companyId company id
   * @param shortNames short codes, lower case
   * @return accounts
   */
  @Query(
      "select a from GlAccount a where a.companyId = :companyId and lower(a.shortName) in"
          + " :shortNames")
  List<GlAccount> findByShortNames(
      @Param("companyId") Long companyId, @Param("shortNames") Collection<String> shortNames);

  /**
   * Codes starting with a prefix (numbering, FRBS 2.3.2: a generated code never collides with an
   * existing one, linked to the parent or not).
   *
   * @param companyId company id
   * @param prefix code prefix
   * @return codes
   */
  @Query(
      "select a.code from GlAccount a where a.companyId = :companyId and a.code like"
          + " concat(:prefix, '%')")
  List<String> codesStartingWith(
      @Param("companyId") Long companyId, @Param("prefix") String prefix);

  /**
   * Searches accounts by code prefix, name fragment or short code.
   *
   * @param companyId company id
   * @param term search term
   * @return the matching accounts, ordered by code
   */
  @EntityGraph(
      attributePaths = {PARENT, CATEGORY},
      type = EntityGraphType.LOAD)
  @Query(
      """
      select a from GlAccount a
      where a.companyId = :companyId
        and (lower(a.code) like lower(concat(:term, '%'))
             or lower(a.name) like lower(concat('%', :term, '%'))
             or lower(a.shortName) like lower(concat(:term, '%')))
      order by a.code
      """)
  List<GlAccount> search(@Param("companyId") Long companyId, @Param("term") String term);
}
