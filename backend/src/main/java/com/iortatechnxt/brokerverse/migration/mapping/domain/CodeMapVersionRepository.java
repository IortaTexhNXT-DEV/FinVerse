package com.iortatechnxt.brokerverse.migration.mapping.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Code map versions. */
public interface CodeMapVersionRepository extends JpaRepository<CodeMapVersion, Long> {

  /**
   * The versions of a set, newest first.
   *
   * @param setCode set
   * @return versions
   */
  List<CodeMapVersion> findBySetCodeOrderByVersionNoDesc(String setCode);

  /**
   * A version of a set.
   *
   * @param setCode set
   * @param versionNo version
   * @return version
   */
  Optional<CodeMapVersion> findBySetCodeAndVersionNo(String setCode, int versionNo);

  /**
   * The version of a set in a status (APPROVED is unique per set).
   *
   * @param setCode set
   * @param status status
   * @return versions
   */
  List<CodeMapVersion> findBySetCodeAndStatus(String setCode, MapVersionStatus status);

  /**
   * Every version in a status.
   *
   * @param status status
   * @return versions
   */
  List<CodeMapVersion> findByStatusOrderBySetCodeAsc(MapVersionStatus status);

  /**
   * The highest version number of a set.
   *
   * @param setCode set
   * @return number, 0 when none
   */
  @Query("select coalesce(max(v.versionNo), 0) from CodeMapVersion v where v.setCode = ?1")
  int maxVersionNo(String setCode);
}
