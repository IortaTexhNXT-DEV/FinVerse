package com.iortatechnxt.brokerverse.configpromo.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Dataset lines of the imports. */
public interface ImportDatasetRepository extends JpaRepository<ImportDataset, Long> {

  /**
   * Lines of an import in load order.
   *
   * @param importId import
   * @return lines
   */
  List<ImportDataset> findByImportIdOrderBySeq(Long importId);

  /**
   * One line.
   *
   * @param importId import
   * @param datasetCode dataset
   * @return line
   */
  Optional<ImportDataset> findByImportIdAndDatasetCode(Long importId, String datasetCode);

  /**
   * Removes the lines of an import before a new dry run.
   *
   * @param importId import
   */
  @Modifying
  @Query("delete from ImportDataset d where d.importId = :importId")
  void deleteByImportId(Long importId);
}
