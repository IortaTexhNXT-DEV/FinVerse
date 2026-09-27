package com.iortatechnxt.brokerverse.migration.load.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Legacy key cross-references. */
public interface KeyXrefRepository extends JpaRepository<KeyXref, Long> {

  /**
   * The cross-reference of a legacy key.
   *
   * @param companyId company
   * @param sourceSystem source system
   * @param objectCode object
   * @param legacyKey legacy key
   * @return entry
   */
  Optional<KeyXref> findByCompanyIdAndSourceSystemAndObjectCodeAndLegacyKey(
      Long companyId, String sourceSystem, String objectCode, String legacyKey);

  /**
   * Live cross-references of an object with any of the legacy keys (any source system).
   *
   * @param companyId company
   * @param objectCode object
   * @param legacyKeys keys
   * @return entries
   */
  List<KeyXref> findByCompanyIdAndObjectCodeAndLegacyKeyInAndRolledBackAtIsNull(
      Long companyId, String objectCode, Collection<String> legacyKeys);

  /**
   * Live cross-references loaded by a batch, newest first (rollback order).
   *
   * @param batchId batch
   * @return entries
   */
  List<KeyXref> findByBatchIdAndRolledBackAtIsNullOrderByIdDesc(Long batchId);

  /**
   * Live cross-references of a target record.
   *
   * @param targetEntity entity type
   * @param targetCode code
   * @return entries
   */
  List<KeyXref> findByTargetEntityAndTargetCodeAndRolledBackAtIsNull(
      String targetEntity, String targetCode);

  /**
   * Live cross-references of an object.
   *
   * @param companyId company
   * @param objectCode object
   * @return entries
   */
  List<KeyXref> findByCompanyIdAndObjectCodeAndRolledBackAtIsNull(
      Long companyId, String objectCode);

  /**
   * Search by legacy key or target code.
   *
   * @param legacyKey legacy key fragment (lower case, with wildcards)
   * @param targetCode target code fragment (lower case, with wildcards)
   * @param pageable page
   * @return entries
   */
  Page<KeyXref> findByLegacyKeyLikeIgnoreCaseOrTargetCodeLikeIgnoreCaseOrderByIdDesc(
      String legacyKey, String targetCode, Pageable pageable);

  /**
   * Number of live cross-references of a batch.
   *
   * @param batchId batch
   * @return count
   */
  long countByBatchIdAndRolledBackAtIsNull(Long batchId);
}
