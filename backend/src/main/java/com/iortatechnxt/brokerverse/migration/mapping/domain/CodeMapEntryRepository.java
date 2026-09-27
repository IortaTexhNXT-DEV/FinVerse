package com.iortatechnxt.brokerverse.migration.mapping.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Code map entries. */
public interface CodeMapEntryRepository extends JpaRepository<CodeMapEntry, Long> {

  /**
   * Entries of a version.
   *
   * @param versionId version
   * @return entries by source and legacy code
   */
  List<CodeMapEntry> findByVersionIdOrderBySourceSystemAscLegacyCodeAscIdAsc(Long versionId);

  /**
   * Entries of several versions.
   *
   * @param versionIds versions
   * @return entries
   */
  List<CodeMapEntry> findByVersionIdIn(Collection<Long> versionIds);

  /**
   * Number of entries of a version.
   *
   * @param versionId version
   * @return count
   */
  long countByVersionId(Long versionId);
}
