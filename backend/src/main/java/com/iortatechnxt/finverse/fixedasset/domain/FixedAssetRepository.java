package com.iortatechnxt.finverse.fixedasset.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link FixedAsset}. */
public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long> {

  /**
   * Finds an asset by tag number.
   *
   * @param companyId company
   * @param tagNo tag number
   * @return asset if present
   */
  Optional<FixedAsset> findByCompanyIdAndTagNo(Long companyId, String tagNo);

  /**
   * Checks whether a tag number is taken.
   *
   * @param companyId company
   * @param tagNo tag number
   * @return true when taken
   */
  boolean existsByCompanyIdAndTagNo(Long companyId, String tagNo);

  /**
   * Lists assets in one status across companies (approval inbox).
   *
   * @param status status
   * @return assets, oldest first
   */
  List<FixedAsset> findByStatusOrderById(AssetStatus status);

  /**
   * Lists assets in the given statuses (depreciation run candidates).
   *
   * @param companyId company
   * @param statuses statuses
   * @return assets ordered by tag number
   */
  List<FixedAsset> findByCompanyIdAndStatusInOrderByTagNo(
      Long companyId, Collection<AssetStatus> statuses);

  /**
   * Searches the register.
   *
   * @param companyId company
   * @param statuses statuses to include
   * @param branchId branch filter (null = all)
   * @param categoryId category filter (null = all)
   * @param term lower-case fragment of tag number or description ("" = all)
   * @return assets ordered by tag number
   */
  @Query(
      """
      select a from FixedAsset a
      where a.companyId = :companyId and a.status in :statuses
        and (:branchId is null or a.branchId = :branchId)
        and (:categoryId is null or a.category.id = :categoryId)
        and (lower(a.tagNo) like concat('%', :term, '%')
             or lower(a.description) like concat('%', :term, '%'))
      order by a.tagNo
      """)
  List<FixedAsset> search(
      @Param("companyId") Long companyId,
      @Param("statuses") Collection<AssetStatus> statuses,
      @Param("branchId") Long branchId,
      @Param("categoryId") Long categoryId,
      @Param("term") String term);
}
