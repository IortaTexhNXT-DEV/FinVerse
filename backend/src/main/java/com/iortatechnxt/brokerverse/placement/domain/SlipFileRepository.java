package com.iortatechnxt.brokerverse.placement.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Rendered placement slip files. */
public interface SlipFileRepository extends JpaRepository<SlipFile, Long> {

  /**
   * One file of a slip.
   *
   * @param slipId slip
   * @param format PDF or XLSX
   * @return file
   */
  Optional<SlipFile> findBySlipIdAndFormat(Long slipId, String format);

  /**
   * All files of a slip.
   *
   * @param slipId slip
   * @return files
   */
  List<SlipFile> findBySlipIdOrderByFormatAsc(Long slipId);
}
