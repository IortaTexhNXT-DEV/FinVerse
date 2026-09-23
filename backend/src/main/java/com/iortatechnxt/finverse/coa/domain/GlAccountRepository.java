package com.iortatechnxt.finverse.coa.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link GlAccount}. */
public interface GlAccountRepository extends JpaRepository<GlAccount, Long> {

  /**
   * Lists the chart of accounts of a company ordered by code.
   *
   * @param companyId company id
   * @return accounts
   */
  List<GlAccount> findByCompanyIdOrderByCode(Long companyId);

  /**
   * Finds an account by company and code.
   *
   * @param companyId company id
   * @param code account code
   * @return account if present
   */
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
   * Searches accounts by code prefix or name fragment.
   *
   * @param companyId company id
   * @param term search term
   * @return up to the matching accounts, ordered by code
   */
  @Query(
      """
      select a from GlAccount a
      where a.companyId = :companyId
        and (lower(a.code) like lower(concat(:term, '%'))
             or lower(a.name) like lower(concat('%', :term, '%')))
      order by a.code
      """)
  List<GlAccount> search(@Param("companyId") Long companyId, @Param("term") String term);
}
